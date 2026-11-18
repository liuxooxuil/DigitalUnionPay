package com.digitalunionpay.user.service.impl;

import com.digitalunionpay.common.Ids;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.support.FieldCipher;
import com.digitalunionpay.support.Outbox;
import com.digitalunionpay.user.auth.Digests;
import com.digitalunionpay.user.auth.LongIds;
import com.digitalunionpay.user.service.CustomerForms;
import com.digitalunionpay.user.service.CustomerViews;
import com.digitalunionpay.user.service.KycService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** KYC 状态机、OCR、活体 HMAC、制裁筛查和 Airwallex 待同步。类不是 final，事务代理要生成子类。 */
@Service
public class KycServiceImpl implements KycService {

  private static final CustomerViews.KycView EMPTY = new CustomerViews.KycView(
      "NOT_SUBMITTED", "", "", "", "", "", "", "", "");

  private final JdbcTemplate jdbc;

  private final FieldCipher cipher;

  private final Outbox outbox;

  private final RfiExpiry expiry;

  private final Clock clock;

  private final Path directory;

  private final int threshold;

  private final int rfiDays;

  private final String livenessSecret;

  /** 阈值、补充材料天数和活体密钥来自配置。目录为空时用临时目录。 */
  public KycServiceImpl(
      JdbcTemplate jdbc,
      FieldCipher cipher,
      Outbox outbox,
      RfiExpiry expiry,
      Clock clock,
      @Value("${dup.kyc.dir:}") String directory,
      @Value("${dup.kyc.pass-threshold:80}") int threshold,
      @Value("${dup.kyc.rfi-days:7}") int rfiDays,
      @Value("${dup.kyc.liveness-secret:DigitalUnionPayLivenessKey!!!!}") String livenessSecret) {
    this.jdbc = jdbc;
    this.cipher = cipher;
    this.outbox = outbox;
    this.expiry = expiry;
    this.clock = clock;
    this.directory = directory == null || directory.isBlank()
        ? Path.of(System.getProperty("java.io.tmpdir"), "dup-kyc")
        : Path.of(directory);
    this.threshold = threshold;
    this.rfiDays = rfiDays;
    this.livenessSecret = livenessSecret;
  }

  /** 未提交、未通过或账户不是正常时不允许出入金、兑换和出金。 */
  @Override
  public boolean fundingAllowed(String userId) {
    Ids.required(userId, "userId");
    Profile profile = profile(userId);
    return profile != null
        && "APPROVED".equals(profile.kycStatus)
        && "ACTIVE".equals(profile.userStatus)
        && "NORMAL".equals(profile.accountStatus);
  }

  /** 内容落在 KYC 目录，库里只存摘要和相对路径。 */
  @Override
  @Transactional
  public CustomerViews.DocumentReceipt upload(String userId, CustomerForms.DocumentUpload form) {
    Ids.required(userId, "userId");
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String side = side(form.side());
    byte[] body = decode(form.contentBase64(), 2 * 1024 * 1024);
    String sha = Digests.sha256(body);
    String type = form.contentType() == null || form.contentType().isBlank() ? "application/octet-stream" : form.contentType().trim();
    if (type.length() > 64) {
      throw new IllegalArgumentException("contentType");
    }
    try {
      Path folder = directory.resolve(userId);
      Files.createDirectories(folder);
      Files.write(folder.resolve(sha), body);
    } catch (java.io.IOException exception) {
      throw new IllegalStateException("document");
    }
    jdbc.update(
        "INSERT INTO kyc_document (id, user_id, side, sha256, content_type, stored_path, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
        LongIds.next(),
        userId,
        side,
        sha,
        type,
        userId + "/" + sha,
        Timestamp.from(clock.instant()));
    return new CustomerViews.DocumentReceipt(side, sha);
  }

  /** 只有以 OCR1 开头、且六项都齐全的文本才算识别成功。 */
  @Override
  public CustomerViews.OcrResult ocr(CustomerForms.OcrRequest form) {
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    byte[] body = decode(form.contentBase64(), 2 * 1024 * 1024);
    String text = new String(body, StandardCharsets.UTF_8);
    if (!text.startsWith("OCR1")) {
      return unrecognized();
    }
    String name = "";
    String birth = "";
    String nationality = "";
    String address = "";
    String idType = "";
    String idNumber = "";
    for (String line : text.split("\\R")) {
      int colon = line.indexOf(':');
      if (colon < 1) {
        continue;
      }
      String key = line.substring(0, colon).trim();
      String value = line.substring(colon + 1).trim();
      switch (key) {
        case "name" -> name = value;
        case "birthDate" -> birth = value;
        case "nationality" -> nationality = value;
        case "address" -> address = value;
        case "idType" -> idType = value;
        case "idNumber" -> idNumber = value;
        default -> {
        }
      }
    }
    if (name.isBlank() || birth.isBlank() || nationality.isBlank() || address.isBlank() || idType.isBlank() || idNumber.isBlank()) {
      return unrecognized();
    }
    return new CustomerViews.OcrResult(true, name, birth, nationality, address, canonicalType(idType), idNumber);
  }

  /** 会话号交给活体服务签名，十分钟内有效。 */
  @Override
  @Transactional
  public CustomerViews.LivenessTicket liveness(String userId) {
    Ids.required(userId, "userId");
    String sessionId = HexFormat.of().formatHex(random());
    Instant expires = clock.instant().plus(10, ChronoUnit.MINUTES);
    jdbc.update(
        "INSERT INTO liveness_session (id, user_id, session_id, expires_at, consumed_at) VALUES (?, ?, ?, ?, NULL)",
        LongIds.next(),
        userId,
        sessionId,
        Timestamp.from(expires));
    return new CustomerViews.LivenessTicket(sessionId, expires.toString());
  }

  /** 确认后的字段、正反面摘要和活体证明都要齐。命中制裁名单时停留在原状态。 */
  @Override
  @Transactional
  public CustomerViews.KycView submit(String userId, CustomerForms.KycSubmit form) {
    Ids.required(userId, "userId");
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    CaseRow latest = latest(userId);
    if (latest != null && "RFI".equals(latest.status) && latest.rfiDue != null && clock.instant().isAfter(latest.rfiDue)) {
      expiry.expire(latest.id, userId, clock.instant());
      throw new BusinessException(ErrorCode.FORBIDDEN, "rfi-expired");
    }
    Fields fields = fields(form);
    requireDocument(userId, "FRONT", fields.front);
    requireDocument(userId, "BACK", fields.back);
    requireLiveness(userId, form);
    if (sanctioned(fields.name)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "sanctions");
    }
    Instant now = clock.instant();
    if (latest != null && "RFI".equals(latest.status)) {
      jdbc.update(
          """
          UPDATE kyc_case
          SET status = 'IN_REVIEW', full_name = ?, birth_date = ?, nationality = ?, address = ?, id_type = ?,
              id_number_cipher = ?, front_sha256 = ?, back_sha256 = ?, liveness_score = ?, face_score = ?,
              sanctions_result = 'CLEAR', rfi_due_at = NULL, reject_reason = NULL, submitted_at = ?
          WHERE id = ?
          """,
          fields.name,
          Date.valueOf(fields.birth),
          fields.nationality,
          fields.address,
          fields.idType,
          cipher.encrypt(fields.idNumber),
          fields.front,
          fields.back,
          form.livenessScore(),
          form.faceScore(),
          Timestamp.from(now),
          latest.id);
    } else if (latest != null && ("IN_REVIEW".equals(latest.status) || "APPROVED".equals(latest.status))) {
      throw new BusinessException(ErrorCode.CONFLICT, "status");
    } else {
      jdbc.update(
          """
          INSERT INTO kyc_case (
            id, user_id, status, full_name, birth_date, nationality, address, id_type, id_number_cipher,
            front_sha256, back_sha256, liveness_score, face_score, sanctions_result, rfi_due_at, reject_reason,
            submitted_at, decided_at)
          VALUES (?, ?, 'IN_REVIEW', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CLEAR', NULL, NULL, ?, NULL)
          """,
          LongIds.next(),
          userId,
          fields.name,
          Date.valueOf(fields.birth),
          fields.nationality,
          fields.address,
          fields.idType,
          cipher.encrypt(fields.idNumber),
          fields.front,
          fields.back,
          form.livenessScore(),
          form.faceScore(),
          Timestamp.from(now));
    }
    jdbc.update(
        "UPDATE liveness_session SET consumed_at = ? WHERE user_id = ? AND session_id = ?",
        Timestamp.from(now),
        userId,
        form.livenessSessionId().trim());
    jdbc.update("UPDATE user_profile SET kyc_status = 'IN_REVIEW' WHERE user_id = ?", userId);
    return current(userId);
  }

  /** 通过后用户变为可登录后的激活态，并排队同步 Airwallex，不把未接通的通道写成已同步。 */
  @Override
  @Transactional
  public CustomerViews.KycView decide(String userId, CustomerForms.KycDecision form) {
    Ids.required(userId, "userId");
    if (form == null) {
      throw new IllegalArgumentException("request");
    }
    String status = Ids.required(form.status(), "status").trim().toUpperCase(Locale.ROOT);
    if (!"APPROVED".equals(status) && !"REJECTED".equals(status) && !"RFI".equals(status)) {
      throw new IllegalArgumentException("status");
    }
    CaseRow latest = latest(userId);
    if (latest == null || !"IN_REVIEW".equals(latest.status)) {
      throw new BusinessException(ErrorCode.CONFLICT, "status");
    }
    Instant now = clock.instant();
    if ("APPROVED".equals(status)) {
      jdbc.update(
          "UPDATE kyc_case SET status = 'APPROVED', decided_at = ?, reject_reason = NULL WHERE id = ?",
          Timestamp.from(now),
          latest.id);
      jdbc.update(
          """
          UPDATE user_profile
          SET kyc_status = 'APPROVED', kyc_level = 'BASIC',
              user_status = CASE WHEN account_status = 'NORMAL' AND user_status = 'PENDING_KYC' THEN 'ACTIVE' ELSE user_status END
          WHERE user_id = ?
          """,
          userId);
      jdbc.update(
          """
          UPDATE fund_account SET status = 'NORMAL'
          WHERE status = 'PENDING_KYC' AND account_no IN (SELECT account_no FROM user_fund_account WHERE user_id = ?)
          """,
          userId);
      jdbc.update(
          """
          INSERT INTO airwallex_account (user_id, external_id, status, updated_at)
          SELECT ?, NULL, 'PENDING', ? FROM DUAL
          WHERE NOT EXISTS (SELECT 1 FROM airwallex_account WHERE user_id = ?)
          """,
          userId,
          Timestamp.from(now),
          userId);
      outbox.append("user.airwallex", userId + "|PENDING", "airwallex:" + userId);
    } else if ("REJECTED".equals(status)) {
      String reason = Ids.required(form.reason(), "reason").trim();
      jdbc.update(
          "UPDATE kyc_case SET status = 'REJECTED', reject_reason = ?, decided_at = ? WHERE id = ?",
          reason,
          Timestamp.from(now),
          latest.id);
      jdbc.update("UPDATE user_profile SET kyc_status = 'REJECTED' WHERE user_id = ?", userId);
    } else {
      String reason = Ids.required(form.reason(), "reason").trim();
      jdbc.update(
          "UPDATE kyc_case SET status = 'RFI', reject_reason = ?, rfi_due_at = ?, decided_at = ? WHERE id = ?",
          reason,
          Timestamp.from(now.plus(rfiDays, ChronoUnit.DAYS)),
          Timestamp.from(now),
          latest.id);
      jdbc.update("UPDATE user_profile SET kyc_status = 'RFI' WHERE user_id = ?", userId);
    }
    return current(userId);
  }

  /** 没有案件时返回未提交，不把缺失当成错误。 */
  @Override
  public CustomerViews.KycView current(String userId) {
    Ids.required(userId, "userId");
    CaseRow row = latest(userId);
    if (row == null) {
      return EMPTY;
    }
    return new CustomerViews.KycView(
        row.status,
        row.name,
        row.birth.toString(),
        row.nationality,
        row.address,
        row.idType,
        cipher.decrypt(row.idCipher),
        row.reason == null ? "" : row.reason,
        row.rfiDue == null ? "" : row.rfiDue.toString());
  }

  /** 正反面必须是这个用户已经上传过的摘要。 */
  private void requireDocument(String userId, String side, String sha) {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM kyc_document WHERE user_id = ? AND side = ? AND sha256 = ?",
        Integer.class,
        userId,
        side,
        sha);
    if (count == null || count == 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "document");
    }
  }

  /** 分数达到阈值，且 HMAC 与会话一致。不采信没有证明的整数。 */
  private void requireLiveness(String userId, CustomerForms.KycSubmit form) {
    if (form.livenessScore() < 0 || form.livenessScore() > 100 || form.faceScore() < 0 || form.faceScore() > 100) {
      throw new IllegalArgumentException("score");
    }
    String sessionId = Ids.required(form.livenessSessionId(), "livenessSessionId").trim();
    Instant expires = jdbc.query(
        """
        SELECT expires_at FROM liveness_session
        WHERE user_id = ? AND session_id = ? AND consumed_at IS NULL
        """,
        rs -> rs.next() ? rs.getTimestamp(1).toInstant() : null,
        userId,
        sessionId);
    if (expires == null || clock.instant().isAfter(expires)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "liveness");
    }
    if (form.livenessScore() < threshold || form.faceScore() < threshold || !proofMatches(form)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "liveness");
    }
  }

  /** 证明是 sessionId|livenessScore|faceScore 的 HMAC-SHA256，十六进制。 */
  private boolean proofMatches(CustomerForms.KycSubmit form) {
    String presented = form.livenessProof() == null ? "" : form.livenessProof().trim().toLowerCase(Locale.ROOT);
    String message = form.livenessSessionId().trim() + "|" + form.livenessScore() + "|" + form.faceScore();
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(livenessSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      String expected = HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
      return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("liveness");
    }
  }

  /** 名字去掉空白后转成大写，和名单做精确匹配。空表表示没有命中。 */
  private boolean sanctioned(String name) {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM sanctions_entry WHERE name_key = ?",
        Integer.class,
        nameKey(name));
    return count != null && count > 0;
  }

  /** 折叠空白。 */
  static String nameKey(String name) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (!Character.isWhitespace(ch)) {
        builder.append(Character.toUpperCase(ch));
      }
    }
    return builder.toString();
  }

  /** 校验用户确认过的字段。 */
  private Fields fields(CustomerForms.KycSubmit form) {
    String name = Ids.required(form.fullName(), "fullName").trim();
    if (name.length() > 128) {
      throw new IllegalArgumentException("fullName");
    }
    LocalDate birth;
    try {
      birth = LocalDate.parse(Ids.required(form.birthDate(), "birthDate").trim());
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("birthDate");
    }
    LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    if (birth.isAfter(today) || birth.isBefore(LocalDate.of(1900, 1, 1))) {
      throw new IllegalArgumentException("birthDate");
    }
    String nationality = Ids.required(form.nationality(), "nationality").trim().toUpperCase(Locale.ROOT);
    if (!nationality.matches("[A-Z]{2}")) {
      throw new IllegalArgumentException("nationality");
    }
    String address = Ids.required(form.address(), "address").trim();
    if (address.length() > 500) {
      throw new IllegalArgumentException("address");
    }
    String idNumber = Ids.required(form.idNumber(), "idNumber").trim();
    if (!idNumber.matches("[A-Za-z0-9]{4,32}")) {
      throw new IllegalArgumentException("idNumber");
    }
    return new Fields(
        name,
        birth,
        nationality,
        address,
        canonicalType(form.idType()),
        idNumber,
        sha(form.frontSha256()),
        sha(form.backSha256()));
  }

  /** 摘要必须是 64 位十六进制。 */
  private static String sha(String value) {
    String sha = Ids.required(value, "sha256").trim().toLowerCase(Locale.ROOT);
    if (!sha.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("sha256");
    }
    return sha;
  }

  /** 身份证、护照、回乡证。中文和英文代码都可以。 */
  static String canonicalType(String value) {
    String type = Ids.required(value, "idType").trim();
    return switch (type) {
      case "身份证", "ID_CARD" -> "ID_CARD";
      case "护照", "PASSPORT" -> "PASSPORT";
      case "回乡证", "HOME_RETURN" -> "HOME_RETURN";
      default -> throw new IllegalArgumentException("idType");
    };
  }

  /** 只接受正面和反面。 */
  private static String side(String value) {
    String side = Ids.required(value, "side").trim().toUpperCase(Locale.ROOT);
    if (!"FRONT".equals(side) && !"BACK".equals(side)) {
      throw new IllegalArgumentException("side");
    }
    return side;
  }

  /** Base64 内容，超过上限时拒绝。 */
  private static byte[] decode(String contentBase64, int max) {
    try {
      byte[] body = Base64.getDecoder().decode(Ids.required(contentBase64, "content").trim());
      if (body.length == 0 || body.length > max) {
        throw new IllegalArgumentException("content");
      }
      return body;
    } catch (IllegalArgumentException exception) {
      if ("content".equals(exception.getMessage())) {
        throw exception;
      }
      throw new IllegalArgumentException("content");
    }
  }

  /** 16 字节会话号。 */
  private static byte[] random() {
    byte[] raw = new byte[16];
    new java.security.SecureRandom().nextBytes(raw);
    return raw;
  }

  /** 档案上的三项状态。 */
  private Profile profile(String userId) {
    return jdbc.query(
        "SELECT user_status, kyc_status, account_status FROM user_profile WHERE user_id = ?",
        rs -> rs.next() ? new Profile(rs.getString(1), rs.getString(2), rs.getString(3)) : null,
        userId);
  }

  /** 这个用户最近一笔案件。 */
  private CaseRow latest(String userId) {
    return jdbc.query(
        """
        SELECT id, status, full_name, birth_date, nationality, address, id_type, id_number_cipher,
               reject_reason, rfi_due_at
        FROM kyc_case WHERE user_id = ? ORDER BY submitted_at DESC, id DESC LIMIT 1
        """,
        rs -> {
          if (!rs.next()) {
            return null;
          }
          return new CaseRow(
              rs.getLong(1),
              rs.getString(2),
              rs.getString(3),
              rs.getDate(4).toLocalDate(),
              rs.getString(5),
              rs.getString(6),
              rs.getString(7),
              rs.getString(8),
              rs.getString(9),
              rs.getTimestamp(10) == null ? null : rs.getTimestamp(10).toInstant());
        },
        userId);
  }

  /** 识别失败时字段留空。 */
  private static CustomerViews.OcrResult unrecognized() {
    return new CustomerViews.OcrResult(false, "", "", "", "", "", "");
  }

  /** 档案三项。 */
  private record Profile(String userStatus, String kycStatus, String accountStatus) {
  }

  /** 用户确认后的证件字段。 */
  private record Fields(
      String name,
      LocalDate birth,
      String nationality,
      String address,
      String idType,
      String idNumber,
      String front,
      String back) {
  }

  /** 最近一笔案件。 */
  private record CaseRow(
      long id,
      String status,
      String name,
      LocalDate birth,
      String nationality,
      String address,
      String idType,
      String idCipher,
      String reason,
      Instant rfiDue) {
  }
}

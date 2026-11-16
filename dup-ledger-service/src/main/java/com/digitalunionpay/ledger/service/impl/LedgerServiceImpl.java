package com.digitalunionpay.ledger.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.digitalunionpay.common.error.BusinessException;
import com.digitalunionpay.common.error.ErrorCode;
import com.digitalunionpay.ledger.FreezeId;
import com.digitalunionpay.ledger.FreezeRequest;
import com.digitalunionpay.ledger.JournalCommand;
import com.digitalunionpay.ledger.Posting;
import com.digitalunionpay.ledger.entity.FreezeEntity;
import com.digitalunionpay.ledger.entity.JournalEntity;
import com.digitalunionpay.ledger.entity.PostingEntity;
import com.digitalunionpay.ledger.mapper.FreezeMapper;
import com.digitalunionpay.ledger.mapper.JournalMapper;
import com.digitalunionpay.ledger.mapper.PostingMapper;
import com.digitalunionpay.ledger.service.LedgerService;
import com.digitalunionpay.support.Abac;
import com.digitalunionpay.support.AccessContext;
import com.digitalunionpay.support.AuditLog;
import com.digitalunionpay.support.IdempotencyStore;
import com.digitalunionpay.support.Outbox;
import com.digitalunionpay.support.PlatformMeters;
import com.digitalunionpay.support.TccFence;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 入账、预冻结和消息落在同一个本地事务里。TCC 的空回滚和悬挂由栅栏挡住。类不是 final，事务代理要生成子类。 */
@Service
public class LedgerServiceImpl implements LedgerService {

  private final FreezeMapper freezeMapper;

  private final JournalMapper journalMapper;

  private final PostingMapper postingMapper;

  private final IdempotencyStore idempotency;

  private final TccFence fence;

  private final Outbox outbox;

  private final AccountBalances balances;

  private final AuditLog audit;

  private final PlatformMeters meters;

  private final JwtDecoder decoder;

  private final JdbcTemplate jdbc;

  /** 接入账本表、余额、幂等、栅栏、审计和本地消息。 */
  public LedgerServiceImpl(
      FreezeMapper freezeMapper,
      JournalMapper journalMapper,
      PostingMapper postingMapper,
      IdempotencyStore idempotency,
      TccFence fence,
      Outbox outbox,
      JdbcTemplate jdbc,
      AuditLog audit,
      PlatformMeters meters,
      JwtDecoder decoder) {
    this.freezeMapper = freezeMapper;
    this.journalMapper = journalMapper;
    this.postingMapper = postingMapper;
    this.idempotency = idempotency;
    this.fence = fence;
    this.outbox = outbox;
    this.jdbc = jdbc;
    this.balances = new AccountBalances(jdbc, meters);
    this.audit = audit;
    this.meters = meters;
    this.decoder = decoder;
  }

  /** 按 reference 幂等预冻结。Cancel 已经先到时按悬挂拒绝。 */
  @Override
  @Transactional
  public FreezeId tryFreeze(FreezeRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request");
    }
    fence.tryBegin(request.reference());
    if (!idempotency.claim("freeze", request.reference())) {
      return new FreezeId(request.reference());
    }
    FreezeEntity row = new FreezeEntity();
    row.setTraceId(request.traceId());
    row.setReference(request.reference());
    row.setAccountId(request.accountId());
    row.setMinorUnits(request.amount().minorUnits());
    row.setScale(request.amount().scale());
    row.setCurrency(request.amount().currency());
    row.setStatus("TRIED");
    guard(request.accountId(), request.amount().currency());
    balances.reserve(request.accountId(), request.amount().currency(), request.amount().scale(), request.amount().minorUnits());
    freezeMapper.insert(row);
    outbox.append("ledger.freeze", request.reference(), request.reference());
    audit.record("ledger.freeze", request.accountId(), request.reference());
    return new FreezeId(request.reference());
  }

  /** 确认一笔已经 Try 过的冻结，并把状态改成已确认。 */
  @Override
  @Transactional
  public void confirm(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    FreezeEntity row = findFreeze(freezeId.value());
    fence.confirm(freezeId.value());
    if (row != null && "TRIED".equals(row.getStatus())) {
      balances.capture(row.getAccountId(), row.getCurrency(), row.getScale(), row.getMinorUnits());
    }
    freezeMapper.update(
        null,
        new LambdaUpdateWrapper<FreezeEntity>()
            .eq(FreezeEntity::getReference, freezeId.value())
            .set(FreezeEntity::getStatus, "CONFIRMED"));
  }

  /** 回滚预冻结。没有 Try 记录时只写空回滚栅栏。 */
  @Override
  @Transactional
  public void cancel(FreezeId freezeId) {
    if (freezeId == null) {
      throw new IllegalArgumentException("freezeId");
    }
    FreezeEntity row = findFreeze(freezeId.value());
    fence.cancel(freezeId.value());
    if (row != null && "TRIED".equals(row.getStatus())) {
      balances.release(row.getAccountId(), row.getCurrency(), row.getScale(), row.getMinorUnits());
    }
    freezeMapper.update(
        null,
        new LambdaUpdateWrapper<FreezeEntity>()
            .eq(FreezeEntity::getReference, freezeId.value())
            .set(FreezeEntity::getStatus, "CANCELLED"));
  }

  /** 幂等键、分录和本地消息在同一个事务里。重复的 reference 返回冲突。 */
  @Override
  @Transactional
  public void post(JournalCommand command) {
    if (command == null) {
      throw new IllegalArgumentException("command");
    }
    command.requireBalanced();
    if (!idempotency.claim("journal", command.reference())) {
      meters.mark(PlatformMeters.LEDGER_DUPLICATE);
      throw new BusinessException(ErrorCode.CONFLICT, command.reference());
    }
    JournalEntity journal = new JournalEntity();
    journal.setTraceId(command.traceId());
    journal.setReference(command.reference());
    journalMapper.insert(journal);
    for (Posting posting : command.postings()) {
      PostingEntity row = new PostingEntity();
      row.setJournalId(journal.getId());
      row.setAccountId(posting.accountId());
      row.setSide(posting.side().name());
      row.setMinorUnits(posting.amount().minorUnits());
      row.setScale(posting.amount().scale());
      row.setCurrency(posting.amount().currency());
      postingMapper.insert(row);
      guard(posting.accountId(), posting.amount().currency());
      try {
        balances.apply(
            posting.accountId(),
            posting.amount().currency(),
            posting.amount().scale(),
            "DEBIT".equals(posting.side().name()),
            posting.amount().minorUnits());
      } catch (BusinessException exception) {
        if (exception.code() == ErrorCode.INSUFFICIENT_FUNDS) {
          meters.mark(PlatformMeters.LEDGER_INSUFFICIENT);
        }
        throw exception;
      }
    }
    outbox.append("journal.posted", command.reference(), command.reference());
    audit.record("journal.posted", command.reference(), command.reference());
  }

  /** 当前 token 里的 uid 必须是账户归属人。没有 token 或没有归属人时放行服务间入账。 */
  private void guard(String accountId, String currency) {
    String token = AccessContext.get();
    if (token == null || token.isBlank()) {
      return;
    }
    String actor;
    try {
      Jwt jwt = decoder.decode(token);
      actor = jwt.getClaimAsString("uid");
    } catch (JwtException exception) {
      return;
    }
    if (!Abac.permits(actor, balances.owner(accountId, currency))) {
      throw new BusinessException(ErrorCode.FORBIDDEN, accountId);
    }
  }

  /** 按业务单号找冻结。没有时返回 null。 */
  private FreezeEntity findFreeze(String reference) {
    return jdbc.query(
        "SELECT account_id, minor_units, scale, currency, status FROM ledger_freeze WHERE reference = ?",
        rs -> {
          if (!rs.next()) {
            return null;
          }
          FreezeEntity row = new FreezeEntity();
          row.setAccountId(rs.getString(1));
          row.setMinorUnits(rs.getLong(2));
          row.setScale(rs.getInt(3));
          row.setCurrency(rs.getString(4));
          row.setStatus(rs.getString(5));
          return row;
        },
        reference);
  }
}

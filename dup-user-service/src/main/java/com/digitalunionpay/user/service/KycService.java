package com.digitalunionpay.user.service;

import com.digitalunionpay.user.KycGate;

/** KYC 提交、审核和出入金资格。 */
public interface KycService extends KycGate {

  /** 保存证件的一面，返回内容摘要。 */
  CustomerViews.DocumentReceipt upload(String userId, CustomerForms.DocumentUpload form);

  /** 识别 OCR1 文本。普通图片返回 recognized=false。 */
  CustomerViews.OcrResult ocr(CustomerForms.OcrRequest form);

  /** 签发活体会话。分数必须用服务端密钥做 HMAC 才能在提交时通过。 */
  CustomerViews.LivenessTicket liveness(String userId);

  /** 用户确认证件信息后提交。制裁名单命中时不进入审核。 */
  CustomerViews.KycView submit(String userId, CustomerForms.KycSubmit form);

  /** 审核中的案件改为通过、拒绝或补充材料。 */
  CustomerViews.KycView decide(String userId, CustomerForms.KycDecision form);

  /** 最新一笔 KYC。还没有案件时状态是未提交。 */
  CustomerViews.KycView current(String userId);
}

package com.digitalunionpay.valuation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 报价，表 valuation_quote。 */
@TableName("valuation_quote")
public class QuoteEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String sourceCurrency;

  private String targetCurrency;

  private Long sourceMinorUnits;

  private Integer sourceScale;

  private Long quoteMinorUnits;

  private Integer quoteScale;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getSourceCurrency() {
    return sourceCurrency;
  }

  public void setSourceCurrency(String sourceCurrency) {
    this.sourceCurrency = sourceCurrency;
  }

  public String getTargetCurrency() {
    return targetCurrency;
  }

  public void setTargetCurrency(String targetCurrency) {
    this.targetCurrency = targetCurrency;
  }

  public Long getSourceMinorUnits() {
    return sourceMinorUnits;
  }

  public void setSourceMinorUnits(Long sourceMinorUnits) {
    this.sourceMinorUnits = sourceMinorUnits;
  }

  public Integer getSourceScale() {
    return sourceScale;
  }

  public void setSourceScale(Integer sourceScale) {
    this.sourceScale = sourceScale;
  }

  public Long getQuoteMinorUnits() {
    return quoteMinorUnits;
  }

  public void setQuoteMinorUnits(Long quoteMinorUnits) {
    this.quoteMinorUnits = quoteMinorUnits;
  }

  public Integer getQuoteScale() {
    return quoteScale;
  }

  public void setQuoteScale(Integer quoteScale) {
    this.quoteScale = quoteScale;
  }
}

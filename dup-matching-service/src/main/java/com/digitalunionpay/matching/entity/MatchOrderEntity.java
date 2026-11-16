package com.digitalunionpay.matching.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 撮合订单，表 matching_order。 */
@TableName("matching_order")
public class MatchOrderEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String orderId;
  private String pairSymbol;
  private String side;
  private Long priceMinorUnits;
  private Integer priceScale;
  private String priceCurrency;
  private Long quantityMinor;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public String getPairSymbol() {
    return pairSymbol;
  }

  public void setPairSymbol(String pairSymbol) {
    this.pairSymbol = pairSymbol;
  }

  public String getSide() {
    return side;
  }

  public void setSide(String side) {
    this.side = side;
  }

  public Long getPriceMinorUnits() {
    return priceMinorUnits;
  }

  public void setPriceMinorUnits(Long priceMinorUnits) {
    this.priceMinorUnits = priceMinorUnits;
  }

  public Integer getPriceScale() {
    return priceScale;
  }

  public void setPriceScale(Integer priceScale) {
    this.priceScale = priceScale;
  }

  public String getPriceCurrency() {
    return priceCurrency;
  }

  public void setPriceCurrency(String priceCurrency) {
    this.priceCurrency = priceCurrency;
  }

  public Long getQuantityMinor() {
    return quantityMinor;
  }

  public void setQuantityMinor(Long quantityMinor) {
    this.quantityMinor = quantityMinor;
  }
}

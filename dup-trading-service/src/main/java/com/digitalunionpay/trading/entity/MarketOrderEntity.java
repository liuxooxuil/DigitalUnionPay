package com.digitalunionpay.trading.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 表 trading_market_order。 */
@TableName("trading_market_order")
public class MarketOrderEntity {

  @TableId(type = IdType.ASSIGN_ID)
  private Long id;

  private String userId;

  private String symbol;

  private String side;

  private Long lots;

  private String ticket;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSymbol() {
    return symbol;
  }

  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }

  public String getSide() {
    return side;
  }

  public void setSide(String side) {
    this.side = side;
  }

  public Long getLots() {
    return lots;
  }

  public void setLots(Long lots) {
    this.lots = lots;
  }

  public String getTicket() {
    return ticket;
  }

  public void setTicket(String ticket) {
    this.ticket = ticket;
  }
}

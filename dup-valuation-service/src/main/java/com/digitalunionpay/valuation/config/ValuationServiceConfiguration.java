package com.digitalunionpay.valuation.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** 本模块 Mapper 扫描。 */
@Configuration
@MapperScan("com.digitalunionpay.valuation.mapper")
public class ValuationServiceConfiguration {
}

package com.digitalunionpay.risk.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** 本模块 Mapper 扫描。 */
@Configuration
@MapperScan("com.digitalunionpay.risk.mapper")
public class RiskServiceConfiguration {
}

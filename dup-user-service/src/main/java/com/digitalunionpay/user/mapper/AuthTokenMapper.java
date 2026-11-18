package com.digitalunionpay.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.digitalunionpay.user.entity.AuthTokenEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 表 auth_token 的 Mapper。 */
public interface AuthTokenMapper extends BaseMapper<AuthTokenEntity> {

  /** 按 token 摘要查找。没有时返回 null。 */
  @Select("""
      SELECT id, token_hash, user_id, expires_at, device_id, device_name, client_ip, location_key, last_seen_at, revoked_at
      FROM auth_token WHERE token_hash = #{tokenHash}
      """)
  AuthTokenEntity findByHash(@Param("tokenHash") String tokenHash);
}

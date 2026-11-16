package com.digitalunionpay.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.digitalunionpay.user.entity.RbacUserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 表 rbac_user 的 Mapper。 */
public interface RbacUserMapper extends BaseMapper<RbacUserEntity> {

  /** 按登录名查账号。没有时返回 null。 */
  @Select("SELECT id, user_id, username, password_hash, status FROM rbac_user WHERE username = #{username}")
  RbacUserEntity findByUsername(@Param("username") String username);

  /** 按业务用户号查账号。没有时返回 null。 */
  @Select("SELECT id, user_id, username, password_hash, status FROM rbac_user WHERE user_id = #{userId}")
  RbacUserEntity findByUserId(@Param("userId") String userId);
}

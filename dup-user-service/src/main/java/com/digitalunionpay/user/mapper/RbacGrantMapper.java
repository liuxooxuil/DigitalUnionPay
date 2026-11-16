package com.digitalunionpay.user.mapper;

import com.digitalunionpay.user.entity.RbacGrant;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 从用户角色和角色权限查出接口权限。 */
public interface RbacGrantMapper {

  /** 返回该用户全部角色带出的方法和路径。 */
  @Select("""
      SELECT p.http_method, p.path_pattern
      FROM rbac_user_role ur
      JOIN rbac_role_permission rp ON rp.role_code = ur.role_code
      JOIN rbac_permission p ON p.code = rp.permission_code
      WHERE ur.user_id = #{userId}
      """)
  List<RbacGrant> findByUserId(@Param("userId") String userId);
}

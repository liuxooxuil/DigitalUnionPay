package com.digitalunionpay.user.entity;

/** 某个用户经角色得到的一条接口权限。不是独立表。 */
public class RbacGrant {

  private String httpMethod;

  private String pathPattern;

  public String getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
  }

  public String getPathPattern() {
    return pathPattern;
  }

  public void setPathPattern(String pathPattern) {
    this.pathPattern = pathPattern;
  }
}

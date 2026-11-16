package com.digitalunionpay.gateway;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 验签已经读过报文体，后面的控制器还要能再读一次。 */
final class CachedBody extends HttpServletRequestWrapper {

  private final byte[] body;

  CachedBody(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body == null ? new byte[0] : body;
  }

  /** 每次调用都从头读缓存。 */
  @Override
  public ServletInputStream getInputStream() {
    ByteArrayInputStream input = new ByteArrayInputStream(body);
    return new ServletInputStream() {
      @Override
      public int read() {
        return input.read();
      }

      @Override
      public boolean isFinished() {
        return input.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener listener) {
      }
    };
  }

  /** 字符读取也走同一份缓存。 */
  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
  }

  /** 让内容长度和缓存一致。 */
  @Override
  public int getContentLength() {
    return body.length;
  }

  /** 让内容长度和缓存一致。 */
  @Override
  public long getContentLengthLong() {
    return body.length;
  }
}

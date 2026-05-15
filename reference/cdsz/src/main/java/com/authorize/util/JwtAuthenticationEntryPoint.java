package com.authorize.util;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
  *  @author  Bxsheng
  *  @blogAddress  www.kdream.cn
  *  @createTIme  2020/9/17
  *  since  JDK  1.8
  */
@Component
public  class  JwtAuthenticationEntryPoint  implements AuthenticationEntryPoint {
        @Override
        public  void  commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e)  throws IOException, ServletException {
                System.out.println("用户访问资源没有携带正确的token");
                System.out.println(e.getMessage());
                httpServletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED,  e==null?"用户访问资源没有携带正确的token":e.getMessage());

        }
}
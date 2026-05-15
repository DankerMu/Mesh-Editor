package com.authorize.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityJwtConfig extends WebSecurityConfigurerAdapter {


    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
       http.cors().and().csrf().disable().authorizeRequests()
               .antMatchers(HttpMethod.OPTIONS,"/**").permitAll()
               .antMatchers(HttpMethod.POST, "/**").permitAll()
               .antMatchers(
                       "/doc.html",                    // Knife4j 前端页面
                       "/webjars/**",                  // Swagger 静态资源
                       "/swagger-resources/**",        // Swagger 资源
                       "/v2/api-docs",                 // Swagger 文档接口
                       "/v3/api-docs",                // OpenAPI 3.0 接口
                       "/swagger-ui/**",               // Swagger UI 路径
                       "/favicon.ico"                  // 图标
               ).permitAll()
               .antMatchers("/*/**").permitAll()
               .antMatchers("/").permitAll()
               //login 不拦截
               .antMatchers("/login").permitAll()
               .antMatchers("/station/indb").permitAll()
               .antMatchers("/verifyCode").permitAll()
               .anyRequest().authenticated()
               //授权
               .and()
//               .anonymous().disable()// 关闭匿名用户
               // 禁用session
               .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
		       // 使用自己定义的拦截机制，拦截jwt
		        http.addFilterBefore(new JwtAuthenticationFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
		        //授权错误信息处理
                .exceptionHandling()
                //用户访问资源没有携带正确的token
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                //用户访问没有授权资源
                .accessDeniedHandler(jwtAccessDeniedHandler);
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        //使用的密码比较方式
        return  new BCryptPasswordEncoder();
    }

}
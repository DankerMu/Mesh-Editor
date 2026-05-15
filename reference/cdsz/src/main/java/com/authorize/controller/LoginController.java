package com.authorize.controller;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.authorize.util.JwtAuthenticationFilter;
import com.authorize.util.JwtTokenUtils;
import com.authorize.util.TokenBlacklist;
import com.log.pojo.LogRecordParams;
import com.log.service.inf.LogService;
import com.tool.VerifyCode;
import com.user.pojo.User;
import com.user.service.inf.UserService;

/**
 * @category
 * @date 2025/3/24 15:59
 * @description TODO
 */
@RestController
public class LoginController {
    @Resource
    private UserDetailsService userDetailsService;
    @Resource
    private UserService userService;
    @Resource
    private LogService logService;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public LoginController(AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    @PostMapping("/login")
//    public ResponseEntity<Map<String, Object>> login(@RequestParam String u, @RequestParam String p, @RequestParam String verifyCode, HttpServletRequest request){
    public ResponseEntity<Map<String, Object>> login(@RequestParam String u, @RequestParam String p, HttpServletRequest request){
        Map<String, Object> response = new HashMap<>();
//        HttpSession session = request.getSession();
//        System.out.println("实际的"+session.getAttribute("verify"));
//        System.out.println("我输入的："+login.getImgcode());
//        verifyCode = verifyCode.toLowerCase();
//        if(session.getAttribute("verify").equals(verifyCode))
//        {
//            UserDetails user = userDetailsService.loadUserByUsername(u);
            User user = userService.findByUsername(u);
            if(user == null)
            {
            	response.put("message", "用户名或密码错误");

                LogRecordParams params = new LogRecordParams(u,"登录", "登录失败:用户名或密码错误");
                logService.addLogRecord(params);

                return ResponseEntity.status(401).body(response);
            }
//            Optional<UserDetails> userDetails = Optional.ofNullable(user);
            if (user.getPassword().equals(p)) {
                String token = JwtTokenUtils.createToken(u, u, true);
                response.put("token", token);
                response.put("message", "登录成功");
                
//                List<GrantedAuthority> list = AuthorityUtils.createAuthorityList("");
//                Authentication authenticationToken = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword(), list);
//                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                
             // 1. 构建用户权限集合（根据实际情况设置）
                Collection<? extends GrantedAuthority> authorities = 
                    AuthorityUtils.createAuthorityList("admin", "test"); // 示例角色
                
                // 2. 创建认证对象
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getUsername(),  // 用户名
                    user.getPassword(),  // 密码
                    authorities          // 权限集合
                );
                
                // 3. 存入SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
                if(attributes != null)
                {
                	HttpServletRequest httpServletRequest = attributes.getRequest();
                	HttpSession session = httpServletRequest.getSession();
                	session.setAttribute("username", u);
                }
                UserNameHolder.userName = u;
                LogRecordParams params = new LogRecordParams(u, "登录", "登录成功");
                logService.addLogRecord(params);

                return ResponseEntity.ok(response);
            } else {
                response.put("message", "用户名或密码错误");

                LogRecordParams params = new LogRecordParams(u,"登录", "登录失败:用户名或密码错误");
                logService.addLogRecord(params);

                return ResponseEntity.status(401).body(response);
            }
//        }
//        else
//        {
//            response.put("message", "验证码错误");
//            return ResponseEntity.status(401).body(response);
//        }


    }

    /**
     * 获取验证码
     * @param request
     * @param response
     * @throws Exception
     */
    @GetMapping("/verifyCode")
    public void VerifyCode(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");

        VerifyCode code = new VerifyCode();
        BufferedImage image = code.getImage();  //得到验证码图片
        String text = code.getText().toLowerCase(); //得到验证码的文本

        //保存验证码的值
        HttpSession session = request.getSession(); //将验证码的值存放到session中
        session.setAttribute("verify", text);
        response.setHeader("Pragma", "no-cache");
        //设置响应头
        response.setHeader("Cache-Control", "no-cache");
        //在代理服务器端防止缓冲
        response.setDateHeader("Expires", 0);
        //设置响应内容类型
        response.setContentType("image/jpeg");

        VerifyCode.output(image, response.getOutputStream()); //将验证码图片输出到前端页面
        response.getOutputStream().flush();
    }

    @PostMapping("/exit")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = JwtAuthenticationFilter.resolveToken(request);
        if (token != null) {
            TokenBlacklist.addToBlacklist(token);
        }
        return ResponseEntity.ok("登出成功");
    }

    @GetMapping("/role")
    @PreAuthorize("hasRole('test')")
    public String roleInfo(){
        return "需要获得bxsheng权限，才可以访问";
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('kdream')")
    public String rolekdream(){
        return "需要获得kdream权限，才可以访问";
    }
}

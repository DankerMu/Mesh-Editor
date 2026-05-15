package com.authorize.util;

import io.jsonwebtoken.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSONObject;

/**
 * @createTIme 2020/9/16
 * since JDK 1.8
 */
public class JwtTokenUtils {
    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String SECRET = "jwtsecret22222222222222jwtsecret22222222222222jwtsecret22222222222222jwtsecret22222222222222";
    public static final String ISS = "echisan";

    private static final Long EXPIRATION = 60 * 60 * 24 * 31L; //过期时间20小时

    private static final String ROLE = "role";

    //创建token
    public static String createToken(String username, String role, boolean isRememberMe){
        Map map = new HashMap();
        map.put(ROLE, role);
        return Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .setClaims(map)
                .setIssuer(ISS)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 1000))
                .compact();
    }

    //从token中获取用户名(此处的token是指去掉前缀之后的)
    public static String getUserName(String token){
        String username;
        try {
            username = getTokenBody(token).getSubject();
        } catch (    Exception e){
            username = null;
        }
        return username;
    }

    public static String getUserRole(String token){
        return (String) getTokenBody(token).get(ROLE);
    }

    public static Claims getTokenBody(String token){
        Claims claims = null;
        try{
            claims = Jwts.parser().setSigningKey(SECRET).parseClaimsJws(token).getBody();
            System.out.println("getTokenBody get token: " + JSONObject.toJSONString(claims.getExpiration()));
        } catch(ExpiredJwtException e){
        	System.out.println("ExpiredJwtException: " + JSONObject.toJSONString(claims.getExpiration()));
            e.printStackTrace();
        } catch(UnsupportedJwtException e){
            e.printStackTrace();
        } catch(MalformedJwtException e){
            e.printStackTrace();
        } catch(SignatureException e){
            e.printStackTrace();
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
        return claims;
    }

    //是否已过期
    public static boolean isExpiration(String token){
        try{
            return getTokenBody(token).getExpiration().before(new Date());
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
        return true;
    }
}
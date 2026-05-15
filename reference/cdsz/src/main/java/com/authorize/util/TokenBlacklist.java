package com.authorize.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class TokenBlacklist {
    // 使用 Caffeine 缓存（自动过期）
    private static final Cache<String, Boolean> blacklist = 
        Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS) // 与 Token 有效期一致
                .build();

    // 将 Token 加入黑名单
    public static void addToBlacklist(String token) {
        blacklist.put(token, true);
    }

    // 检查 Token 是否在黑名单
    public static boolean isBlacklisted(String token) {
        return blacklist.getIfPresent(token) != null;
    }
}
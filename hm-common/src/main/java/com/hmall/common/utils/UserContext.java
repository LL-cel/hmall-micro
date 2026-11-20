package com.hmall.common.utils;

import com.hmall.common.exception.UnauthorizedException;

public class UserContext {
    public static final String USER_ID_HEADER = "X-User-Id";
    private static final ThreadLocal<Long> tl = new ThreadLocal<>();

    /**
     * 保存当前登录用户信息到ThreadLocal
     * @param userId 用户id
     */
    public static void setUser(Long userId) {
        tl.set(userId);
    }

    /**
     * 获取当前登录用户信息
     * @return 用户id
     */
    public static Long getUser() {
        return tl.get();
    }

    public static Long requireUser() {
        Long userId = getUser();
        if (userId == null) {
            throw new UnauthorizedException("未登录");
        }
        return userId;
    }

    /**
     * 移除当前登录用户信息
     */
    public static void removeUser(){
        tl.remove();
    }
}

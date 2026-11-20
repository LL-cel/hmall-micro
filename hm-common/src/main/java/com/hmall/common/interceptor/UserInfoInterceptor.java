package com.hmall.common.interceptor;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.UserContext;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserInfoInterceptor implements AsyncHandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.removeUser();
        String userId = request.getHeader(UserContext.USER_ID_HEADER);
        if (userId != null) {
            try {
                UserContext.setUser(Long.valueOf(userId));
            } catch (NumberFormatException e) {
                throw new UnauthorizedException("无效的用户标识", e);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.removeUser();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.removeUser();
    }
}

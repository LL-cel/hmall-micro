package com.hmall.common.interceptor;

import com.hmall.common.exception.UnauthorizedException;
import com.hmall.common.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserInfoInterceptorTest {
    private final UserInfoInterceptor interceptor = new UserInfoInterceptor();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final Object handler = new Object();

    @AfterEach
    void clearUser() {
        UserContext.removeUser();
    }

    @Test
    void bindsUserAndClearsContextWhenTheRequestFails() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(UserContext.USER_ID_HEADER, "42");

        assertTrue(interceptor.preHandle(request, response, handler));
        assertEquals(42L, UserContext.requireUser());

        interceptor.afterCompletion(request, response, handler, new IllegalStateException());
        assertNull(UserContext.getUser());
    }

    @Test
    void acceptsAnonymousRequestsAndRejectsMalformedUserIds() throws Exception {
        var request = new MockHttpServletRequest();
        UserContext.setUser(42L);

        assertTrue(interceptor.preHandle(request, response, handler));
        assertNull(UserContext.getUser());
        assertThrows(UnauthorizedException.class, UserContext::requireUser);

        request.addHeader(UserContext.USER_ID_HEADER, "invalid");
        assertThrows(UnauthorizedException.class, () -> interceptor.preHandle(request, response, handler));
        assertNull(UserContext.getUser());
    }
}

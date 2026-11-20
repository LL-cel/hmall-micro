package com.hmall.api.config;

import com.hmall.common.utils.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UserInfoFeignConfig {

    @Bean
    public RequestInterceptor userInfoFeignInterceptor() {
        return template -> {
            Long userId = UserContext.getUser();
            if (userId != null) {
                template.removeHeader(UserContext.USER_ID_HEADER);
                template.header(UserContext.USER_ID_HEADER, userId.toString());
            }
        };
    }
}

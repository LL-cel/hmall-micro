package com.hmall.api.item.config;

import com.hmall.api.item.fallback.ItemClientFallbackFactory;
import org.springframework.context.annotation.Bean;

/** 由 ItemClient 的 Feign 子容器加载，不依赖业务服务的组件扫描范围。 */
public class ItemClientConfiguration {

    @Bean
    public ItemClientFallbackFactory itemClientFallbackFactory() {
        return new ItemClientFallbackFactory();
    }
}

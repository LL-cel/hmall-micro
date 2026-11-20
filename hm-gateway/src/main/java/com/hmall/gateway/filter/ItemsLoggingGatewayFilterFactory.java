package com.hmall.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ItemsLoggingGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {

    public ItemsLoggingGatewayFilterFactory() {
        super(Object.class);
    }

    @Override
    public GatewayFilter apply(Object config) {
        System.out.println("调用了局部过滤器");
        return (exchange, chain) -> {
            log.info("调用了商品局部过滤器 {}", exchange.getRequest().getPath().value());
            return chain.filter(exchange);
        };
    }
}

package com.hmall.api.cart;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;

@FeignClient(name = "cart-service", path = "/carts")
public interface CartClient {

    /** 删除当前用户购物车中指定商品对应的条目。 */
    @DeleteMapping
    void removeByItemIds(@RequestParam("ids") Collection<Long> itemIds);
}
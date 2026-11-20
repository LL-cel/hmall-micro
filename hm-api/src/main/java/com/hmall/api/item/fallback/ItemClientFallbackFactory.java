package com.hmall.api.item.fallback;

import com.hmall.api.item.ItemClient;
import com.hmall.api.item.dto.ItemDTO;
import com.hmall.api.item.dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ItemClientFallbackFactory implements FallbackFactory<ItemClient> {

    @Override
    public ItemClient create(Throwable cause) {
        return new ItemClient() {
            @Override
            public List<ItemDTO> queryItemByIds(Collection<Long> ids) {
                log.error("批量查询商品失败，触发降级，商品ids：{}", ids, cause);
                return Collections.emptyList();
            }

            @Override
            public void deductStock(List<OrderDetailDTO> items) {
                log.error("扣减商品库存失败，触发降级，商品明细：{}", items, cause);
                // 写操作失败必须中断下单，不能以无操作的方式伪装扣库存成功。
                throw new IllegalStateException("扣减商品库存失败，请稍后重试", cause);
            }
        };
    }
}

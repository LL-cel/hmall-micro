package com.hmall.cart.service.impl;

import com.hmall.common.utils.UserContext;
import cn.hutool.core.util.StrUtil;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmall.api.item.ItemClient;
import com.hmall.api.item.dto.ItemDTO;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.common.utils.BeanUtils;
import com.hmall.common.utils.CollUtils;
import com.hmall.cart.domain.dto.CartFormDTO;
import com.hmall.cart.domain.po.Cart;
import com.hmall.cart.domain.vo.CartVO;
import com.hmall.cart.mapper.CartMapper;
import com.hmall.cart.service.ICartService;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 订单详情表 服务实现类
 * </p>
 *
 * @author 黑马程序员
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements ICartService {
    private final ItemClient itemClient;

    @Override
    public void addItem2Cart(CartFormDTO cartFormDTO) {
        var userId = UserContext.requireUser();

        // 2.判断是否已经存在
        if(checkItemExists(cartFormDTO.getItemId(), userId)){
            // 2.1.存在，则更新数量
            baseMapper.updateNum(cartFormDTO.getItemId(), userId);
            return;
        }
        // 2.2.不存在，判断是否超过购物车数量
        checkCartsFull(userId);

        // 3.新增购物车条目
        // 3.1.转换PO
        var cart = BeanUtils.copyBean(cartFormDTO, Cart.class);
        // 3.2.保存当前用户
        cart.setUserId(userId);
        // 3.3.保存到数据库
        save(cart);
    }

    @Override
    @SentinelResource(value = "queryMyCarts", blockHandler = "queryMyCartsBlockHandler",
            fallback = "queryMyCartsFallback")
    public List<CartVO> queryMyCarts() {
        // cn.hutool.core.thread.ThreadUtil.sleep(2000); // 临时测试：阻塞 2 秒，模拟购物车慢查询
        // 1.查询我的购物车列表
        var carts = lambdaQuery().eq(Cart::getUserId, UserContext.requireUser()).list();
        if (CollUtils.isEmpty(carts)) {
            return CollUtils.emptyList();
        }

        // 2.转换VO
        var vos = BeanUtils.copyList(carts, CartVO.class);

        // 3.处理VO中的商品信息
        handleCartItems(vos);

        // 4.返回
        return vos;
    }

    public List<CartVO> queryMyCartsBlockHandler(BlockException e) {
        log.error("查询购物车被 Sentinel 限流或熔断，返回空集合", e);
        return CollUtils.emptyList();
    }

    public List<CartVO> queryMyCartsFallback(Throwable e) {
        log.error("查询购物车发生异常，触发降级，返回空集合", e);
        return CollUtils.emptyList();
    }

    private void handleCartItems(List<CartVO> vos) {
        var itemIds = vos.stream().map(CartVO::getItemId).collect(Collectors.toSet());
        var items = itemClient.queryItemByIds(itemIds);
        if (CollUtils.isEmpty(items)) {
            return;
        }
        var itemMap = items.stream().collect(Collectors.toMap(ItemDTO::getId, Function.identity()));
        for (CartVO vo : vos) {
            var item = itemMap.get(vo.getItemId());
            if (item == null) {
                continue;
            }
            vo.setNewPrice(item.getPrice());
            vo.setStatus(item.getStatus());
            vo.setStock(item.getStock());
        }
    }

    @Override
    public void removeByItemIds(Collection<Long> itemIds) {
        // 1.构建删除条件，userId和itemId
        var queryWrapper = new QueryWrapper<Cart>();
        queryWrapper.lambda()
                .eq(Cart::getUserId, UserContext.requireUser())
                .in(Cart::getItemId, itemIds);
        // 2.删除
        remove(queryWrapper);
    }

    private void checkCartsFull(Long userId) {
        var count = lambdaQuery().eq(Cart::getUserId, userId).count();
        if (count >= 10) {
            throw new BizIllegalException(StrUtil.format("用户购物车课程不能超过{}", 10));
        }
    }

    private boolean checkItemExists(Long itemId, Long userId) {
        var count = lambdaQuery()
                .eq(Cart::getUserId, userId)
                .eq(Cart::getItemId, itemId)
                .count();
        return count > 0;
    }
}

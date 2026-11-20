package com.hmall.trade;

import com.hmall.api.cart.CartClient;
import com.hmall.api.item.ItemClient;
import com.hmall.api.item.dto.ItemDTO;
import com.hmall.api.item.dto.OrderDetailDTO;
import com.hmall.common.exception.BadRequestException;
import com.hmall.common.utils.UserContext;
import com.hmall.trade.domain.dto.OrderFormDTO;
import com.hmall.trade.domain.po.Order;
import com.hmall.trade.service.IOrderDetailService;
import com.hmall.trade.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private ItemClient items;
    private CartClient carts;
    private IOrderDetailService details;
    private OrderServiceImpl service;
    private OrderFormDTO form;

    @BeforeEach
    void setUp() {
        UserContext.setUser(42L);
        items = mock(ItemClient.class);
        carts = mock(CartClient.class);
        details = mock(IOrderDetailService.class);
        service = spy(new OrderServiceImpl(items, details, carts));
        form = new OrderFormDTO();
        form.setPaymentType(3);
        form.setDetails(List.of(new OrderDetailDTO().setItemId(11L).setNum(2)));
    }

    @AfterEach
    void clearUser() {
        UserContext.removeUser();
    }

    private void prepareItemAndPersistence() {
        var item = new ItemDTO();
        item.setId(11L);
        item.setPrice(200);
        item.setName("商品");
        when(items.queryItemByIds(Set.of(11L))).thenReturn(List.of(item));
        doAnswer(call -> {
            call.<Order>getArgument(0).setId(99L);
            return true;
        }).when(service).save(any(Order.class));
    }

    @Test
    void createsOrderAndDetailsAndCallsBothRemoteServices() {
        prepareItemAndPersistence();
        assertEquals(99L, service.createOrder(form));
        var order = ArgumentCaptor.forClass(Order.class);
        verify(service).save(order.capture());
        assertEquals(400, order.getValue().getTotalFee());
        assertEquals(42L, order.getValue().getUserId());
        assertEquals(1, order.getValue().getStatus());
        verify(details).saveBatch(argThat(rows -> rows.size() == 1 && rows.stream().allMatch(
                row -> row.getOrderId().equals(99L) && row.getItemId().equals(11L)
                        && row.getNum() == 2 && row.getPrice() == 200)));
        verify(carts).removeByItemIds(Set.of(11L));
        verify(items).deductStock(form.getDetails());
    }

    @Test
    void missingItemStopsOrderAndRemoteMutations() {
        when(items.queryItemByIds(any())).thenReturn(List.of());
        assertThrows(BadRequestException.class, () -> service.createOrder(form));
        verify(service, never()).save(any(Order.class));
        verifyNoInteractions(details, carts);
        verify(items, never()).deductStock(any());
    }

    @Test
    void stockFailureIsNotReportedAsOrderSuccess() {
        prepareItemAndPersistence();
        var failure = new IllegalStateException("remote unavailable");
        doThrow(failure).when(items).deductStock(any());
        var error = assertThrows(RuntimeException.class, () -> service.createOrder(form));
        assertSame(failure, error.getCause());
    }
}

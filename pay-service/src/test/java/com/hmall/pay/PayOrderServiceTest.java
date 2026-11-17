package com.hmall.pay;

import com.hmall.api.trade.TradeClient;
import com.hmall.api.user.UserClient;
import com.hmall.common.exception.BizIllegalException;
import com.hmall.pay.domain.dto.PayOrderFormDTO;
import com.hmall.pay.domain.po.PayOrder;
import com.hmall.pay.enums.PayStatus;
import com.hmall.pay.service.impl.PayOrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PayOrderServiceTest {
    private UserClient users;
    private TradeClient trades;
    private PayOrderServiceImpl service;
    private PayOrderFormDTO form;

    @BeforeEach
    void setUp() {
        users = mock(UserClient.class);
        trades = mock(TradeClient.class);
        service = spy(new PayOrderServiceImpl(users, trades));
        form = PayOrderFormDTO.builder().id(10L).pw("test-password").build();
        doReturn(new PayOrder().setId(10L).setBizOrderNo(20L).setAmount(300)
                .setStatus(PayStatus.WAIT_BUYER_PAY.getValue())).when(service).getById(10L);
    }

    @Test
    void deductsStoredAmountThenMarksPaymentAndBusinessOrder() {
        doReturn(true).when(service).markPayOrderSuccess(eq(10L), any());
        service.tryPayOrderByBalance(form);
        var sequence = inOrder(users, service, trades);
        sequence.verify(users).deductMoney("test-password", 300);
        sequence.verify(service).markPayOrderSuccess(eq(10L), any());
        sequence.verify(trades).markOrderPaySuccess(20L);
    }

    @Test
    void alreadyPaidDoesNotDeductAgain() {
        doReturn(new PayOrder().setStatus(PayStatus.TRADE_SUCCESS.getValue()))
                .when(service).getById(10L);
        assertThrows(BizIllegalException.class, () -> service.tryPayOrderByBalance(form));
        verifyNoInteractions(users, trades);
    }

    @Test
    void deductionFailureStopsPaymentAndOrderUpdates() {
        doThrow(new IllegalStateException("remote failure")).when(users).deductMoney(anyString(), anyInt());
        assertThrows(IllegalStateException.class, () -> service.tryPayOrderByBalance(form));
        verify(service, never()).markPayOrderSuccess(anyLong(), any());
        verifyNoInteractions(trades);
    }

    @Test
    void tradeFailurePropagatesToLocalTransaction() {
        doReturn(true).when(service).markPayOrderSuccess(eq(10L), any());
        doThrow(new IllegalStateException("trade unavailable")).when(trades).markOrderPaySuccess(20L);
        assertThrows(IllegalStateException.class, () -> service.tryPayOrderByBalance(form));
    }

    @Test
    void concurrentStatusChangeStopsTradeUpdate() {
        doReturn(false).when(service).markPayOrderSuccess(eq(10L), any());
        assertThrows(BizIllegalException.class, () -> service.tryPayOrderByBalance(form));
        verifyNoInteractions(trades);
    }
}

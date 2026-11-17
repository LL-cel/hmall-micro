package com.hmall.pay;

import com.hmall.api.trade.TradeClient;
import com.hmall.api.user.UserClient;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.support.SpringMvcContract;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeignContractTest {
    private <T> T client(Class<T> type, int port) {
        return Feign.builder().contract(new SpringMvcContract()).target(type,
                "http://127.0.0.1:" + port + type.getAnnotation(FeignClient.class).path());
    }

    @Test
    void sendsRequestsMatchingUserAndTradeControllersAndPropagatesFailure() throws Exception {
        var requests = new ArrayList<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            exchange.sendResponseHeaders(requests.size() == 3 ? 500 : 204, -1);
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            var users = client(UserClient.class, port);
            users.deductMoney("test+password", 300);
            client(TradeClient.class, port).markOrderPaySuccess(20L);
            assertThrows(FeignException.class, () -> users.deductMoney("test", 300));
            assertEquals(List.of("PUT /users/money/deduct?pw=test%2Bpassword&amount=300",
                    "PUT /orders/20", "PUT /users/money/deduct?pw=test&amount=300"), requests);
        } finally {
            server.stop(0);
        }
    }
}

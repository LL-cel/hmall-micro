package com.hmall.trade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.api.cart.CartClient;
import com.hmall.api.item.ItemClient;
import com.hmall.api.item.dto.OrderDetailDTO;
import com.sun.net.httpserver.HttpServer;
import feign.Feign;
import feign.FeignException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FeignContractTest {
    private HttpServer server;
    private final List<String> requests = new ArrayList<>();
    private final List<String> bodies = new ArrayList<>();
    private int status = 200;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            bodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "[{\"id\":11,\"price\":200}]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private <T> T client(Class<T> type) {
        var converters = new HttpMessageConverters(new MappingJackson2HttpMessageConverter());
        String path = type.getAnnotation(FeignClient.class).path();
        return Feign.builder().contract(new SpringMvcContract())
                .encoder(new SpringEncoder(() -> converters))
                .decoder(new SpringDecoder(() -> converters))
                .target(type, "http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    void sendsItemQueriesStockJsonAndCartDeletionUsingExistingRoutes() throws Exception {
        var items = client(ItemClient.class);
        assertEquals(200, items.queryItemByIds(List.of(11L, 12L)).get(0).getPrice());
        items.deductStock(List.of(new OrderDetailDTO().setItemId(11L).setNum(2)));
        client(CartClient.class).removeByItemIds(List.of(11L, 12L));

        assertEquals("GET /items?ids=11&ids=12", requests.get(0));
        assertEquals("PUT /items/stock/deduct", requests.get(1));
        var body = new ObjectMapper().readTree(bodies.get(1));
        assertEquals(11L, body.get(0).get("itemId").asLong());
        assertEquals(2, body.get(0).get("num").asInt());
        assertEquals("DELETE /carts?ids=11&ids=12", requests.get(2));
    }

    @Test
    void propagatesRemoteStockFailure() {
        status = 500;
        assertThrows(FeignException.class, () -> client(ItemClient.class)
                .deductStock(List.of(new OrderDetailDTO().setItemId(11L).setNum(2))));
    }
}

package com.hmall.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmall.gateway.config.AuthProperties;
import com.hmall.gateway.security.JwtTokenValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AuthProperties.class)
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final List<String> DEFAULT_EXCLUDE_PATHS = List.of(
            "/error", "/favicon.ico", "/v2/**", "/v3/**", "/swagger-resources/**", "/webjars/**", "/doc.html");

    private final AuthProperties authProperties;
    private final JwtTokenValidator tokenValidator;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        var path = request.getPath().pathWithinApplication().value();
        if (!requiresToken(path)) {
            var anonymousRequest = request.mutate().headers(headers -> headers.remove(USER_ID_HEADER)).build();
            return chain.filter(exchange.mutate().request(anonymousRequest).build());
        }

        long userId;
        try {
            userId = tokenValidator.validate(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        } catch (ResponseStatusException e) {
            return writeUnauthorized(exchange.getResponse(), e);
        }
        var authenticatedRequest = request.mutate()
                .headers(headers -> headers.set(USER_ID_HEADER, Long.toString(userId)))
                .build();
        return chain.filter(exchange.mutate().request(authenticatedRequest).build());
    }

    private boolean requiresToken(String path) {
        var includes = authProperties.getIncludePaths();
        return (includes.isEmpty() || matches(includes, path))
                && !matches(authProperties.getExcludePaths(), path)
                && !matches(DEFAULT_EXCLUDE_PATHS, path);
    }

    private boolean matches(List<String> patterns, String path) {
        return patterns.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> writeUnauthorized(ServerHttpResponse response, ResponseStatusException e) {
        response.setStatusCode(e.getStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var body = new AuthError(e.getStatus().value(), e.getReason(), null);
        return response.writeWith(Mono.fromCallable(() -> objectMapper.writeValueAsBytes(body))
                .map(response.bufferFactory()::wrap));
    }

    @Override
    public int getOrder() {
        return -2;
    }

    private record AuthError(int code, String msg, Object data) {
    }
}

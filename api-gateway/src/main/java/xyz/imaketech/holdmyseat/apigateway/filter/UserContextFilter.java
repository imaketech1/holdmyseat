package xyz.imaketech.holdmyseat.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Strips spoofable X-User-* headers, checks the logout denylist, forwards user context. */
@Component
public class UserContextFilter implements GlobalFilter, Ordered {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLES = "X-User-Roles";
    public static final String CORRELATION_ID = "X-Correlation-Id";
    private static final String DENYLIST_PREFIX = "denylist:";

    private final ReactiveStringRedisTemplate redis;

    public UserContextFilter(ReactiveStringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID);
        String cid = correlationId != null ? correlationId : UUID.randomUUID().toString();

        // Resolve to Optional first: switchIfEmpty() on chain.filter() would run the chain twice.
        return exchange.getPrincipal()
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(auth -> auth.isEmpty()
                        ? chain.filter(withHeaders(exchange, forwardedHeaders(exchange, cid)))
                        : filterAuthenticated(exchange, chain, auth.get(), cid));
    }

    private Mono<Void> filterAuthenticated(ServerWebExchange exchange, GatewayFilterChain chain,
                                           JwtAuthenticationToken auth, String cid) {
        String jti = auth.getToken().getId();
        Mono<Boolean> denied = jti == null ? Mono.just(false) : redis.hasKey(DENYLIST_PREFIX + jti);
        return denied.flatMap(isDenied -> {
            if (isDenied) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
            List<String> roles = auth.getToken().getClaimAsStringList("roles");
            HttpHeaders headers = forwardedHeaders(exchange, cid);
            headers.set(USER_ID, auth.getToken().getSubject());
            headers.set(USER_ROLES, roles == null ? "" : String.join(",", roles));
            return chain.filter(withHeaders(exchange, headers));
        });
    }

    /** Copies headers manually: mutate().headers() is read-only once Spring Security wraps the request. */
    private static HttpHeaders forwardedHeaders(ServerWebExchange exchange, String cid) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        headers.remove(USER_ID);
        headers.remove(USER_ROLES);
        headers.set(CORRELATION_ID, cid);
        return headers;
    }

    private static ServerWebExchange withHeaders(ServerWebExchange exchange, HttpHeaders headers) {
        ServerHttpRequest req = new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        };
        return exchange.mutate().request(req).build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}

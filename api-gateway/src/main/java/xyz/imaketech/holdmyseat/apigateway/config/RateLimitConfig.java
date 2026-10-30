package xyz.imaketech.holdmyseat.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.Optional;

@Configuration
public class RateLimitConfig {

    /** Per user; anonymous traffic falls back to client IP. */
    @Bean
    @Primary
    KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(p -> "user:" + p.getName())
                .switchIfEmpty(Mono.fromSupplier(() -> "ip:" + Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(InetSocketAddress::getHostString).orElse("unknown")));
    }

    /** Per IP, to slow credential stuffing. */
    @Bean
    KeyResolver ipKeyResolver() {
        return exchange -> Mono.just("ip:" + Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(InetSocketAddress::getHostString).orElse("unknown"));
    }
}

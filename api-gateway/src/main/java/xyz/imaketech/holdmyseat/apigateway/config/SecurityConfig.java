package xyz.imaketech.holdmyseat.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        .pathMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        .pathMatchers("/actuator/health/**").permitAll()
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o
                        .bearerTokenConverter(bearerTokenConverter())
                        .jwt(j -> j.jwtAuthenticationConverter(rolesConverter())))
                .build();
    }

    /** Also accepts ?access_token= for the WebSocket handshake. */
    private ServerBearerTokenAuthenticationConverter bearerTokenConverter() {
        ServerBearerTokenAuthenticationConverter converter = new ServerBearerTokenAuthenticationConverter();
        converter.setAllowUriQueryParameter(true);
        return converter;
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> rolesConverter() {
        return jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            var authorities = (roles == null ? List.<String>of() : roles).stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
            return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
        };
    }
}

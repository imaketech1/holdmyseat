package xyz.imaketech.holdmyseat.auth.token;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import xyz.imaketech.holdmyseat.auth.config.JwtProperties;
import xyz.imaketech.holdmyseat.auth.user.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    public static final String DENYLIST_PREFIX = "denylist:";

    private final JwtEncoder encoder;
    private final JwtProperties props;
    private final RefreshTokenRepository refreshTokens;
    private final StringRedisTemplate redis;
    private final SecureRandom random = new SecureRandom();

    public TokenService(JwtEncoder encoder, JwtProperties props,
                        RefreshTokenRepository refreshTokens, StringRedisTemplate redis) {
        this.encoder = encoder;
        this.props = props;
        this.refreshTokens = refreshTokens;
        this.redis = redis;
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .subject(user.getId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.accessTtl()))
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(props.keyId()).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public String issueRefreshToken(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(user.getId(), hash(raw), Instant.now().plus(props.refreshTtl())));
        return raw;
    }

    /** Denylists an access token until it expires; checked by the gateway. */
    public void denylist(Jwt jwt) {
        Duration remaining = Duration.between(Instant.now(), jwt.getExpiresAt());
        if (!remaining.isNegative()) {
            redis.opsForValue().set(DENYLIST_PREFIX + jwt.getId(), "1", remaining);
        }
    }

    public long accessTtlSeconds() {
        return props.accessTtl().toSeconds();
    }

    public static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

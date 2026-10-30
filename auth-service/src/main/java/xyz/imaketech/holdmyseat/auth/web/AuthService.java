package xyz.imaketech.holdmyseat.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import xyz.imaketech.holdmyseat.auth.token.RefreshToken;
import xyz.imaketech.holdmyseat.auth.token.RefreshTokenRepository;
import xyz.imaketech.holdmyseat.auth.token.TokenService;
import xyz.imaketech.holdmyseat.auth.user.User;
import xyz.imaketech.holdmyseat.auth.user.UserRepository;
import xyz.imaketech.holdmyseat.auth.web.Dtos.*;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       TokenService tokens, PasswordEncoder encoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.encoder = encoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User user = users.save(new User(req.email().toLowerCase(), encoder.encode(req.password()), req.fullName()));
        return toResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        return issue(user);
    }

    /** Rotates the refresh token; reusing a revoked one revokes all sessions. */
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public TokenResponse refresh(RefreshRequest req) {
        RefreshToken stored = refreshTokens.findByTokenHash(TokenService.hash(req.refreshToken()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (!stored.isActive()) {
            refreshTokens.revokeAllForUser(stored.getUserId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired or reused");
        }
        stored.revoke();
        User user = users.findById(stored.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return issue(user);
    }

    @Transactional
    public void logout(Jwt jwt) {
        tokens.denylist(jwt);
        refreshTokens.revokeAllForUser(UUID.fromString(jwt.getSubject()));
    }

    public UserResponse me(Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject()))
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private TokenResponse issue(User user) {
        return new TokenResponse(tokens.issueAccessToken(user), tokens.issueRefreshToken(user),
                "Bearer", tokens.accessTtlSeconds());
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId().toString(), u.getEmail(), u.getFullName(), u.getRole().name());
    }
}

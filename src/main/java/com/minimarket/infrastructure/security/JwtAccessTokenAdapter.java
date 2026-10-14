package com.minimarket.infrastructure.security;

import com.minimarket.domain.model.User;
import com.minimarket.domain.port.out.AccessTokenPort;
import com.minimarket.infrastructure.config.SecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class JwtAccessTokenAdapter implements AccessTokenPort {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtAccessTokenAdapter(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenExpiration());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.username())
                .issuer(properties.jwt().issuer())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("user_id", user.id())
                .claim("role", user.role().name())
                .claim("token_type", "access")
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(value, properties.jwt().accessTokenExpiration().toSeconds());
    }
}

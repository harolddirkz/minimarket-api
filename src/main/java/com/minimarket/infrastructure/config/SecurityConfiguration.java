package com.minimarket.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.minimarket.domain.model.AuthenticationPolicy;
import com.minimarket.infrastructure.security.RestAccessDeniedHandler;
import com.minimarket.infrastructure.security.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {

    @Bean
    AuthenticationPolicy authenticationPolicy(SecurityProperties properties) {
        return new AuthenticationPolicy(
                properties.refreshTokenExpiration(),
                properties.loginAttempts().maximum(),
                properties.loginAttempts().window(),
                properties.loginAttempts().blockDuration());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecretKey jwtSecretKey(SecurityProperties properties) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(properties.jwt().secretBase64());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must contain valid Base64", exception);
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes");
        }
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey, SecurityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        OAuth2TokenValidator<Jwt> standard = JwtValidators.createDefaultWithIssuer(properties.jwt().issuer());
        OAuth2TokenValidator<Jwt> accessType = jwt -> "access".equals(jwt.getClaimAsString("token_type"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token type", null));
        OAuth2TokenValidator<Jwt> requiredClaims = jwt -> hasRequiredClaims(jwt)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing required claims", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(standard, accessType, requiredClaims));
        return decoder;
    }

    private static boolean hasRequiredClaims(Jwt jwt) {
        Object userId = jwt.getClaim("user_id");
        String role = jwt.getClaimAsString("role");
        return jwt.getSubject() != null && !jwt.getSubject().isBlank()
                && jwt.getId() != null && !jwt.getId().isBlank()
                && userId instanceof Number
                && Set.of("ADMIN", "VENDEDOR").contains(role);
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        converter.setPrincipalClaimName("user_id");
        return converter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityProperties properties,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll();
                    authorize.requestMatchers("/actuator/health").permitAll();
                    if (properties.documentationPublic()) {
                        authorize.requestMatchers(
                                "/swagger.yml", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    authorize.requestMatchers("/api/v1/users/**", "/actuator/info", "/api/v1/system/info")
                            .hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.POST, "/api/v1/products", "/api/v1/categories")
                            .hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.PATCH, "/api/v1/products/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN");
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**")
                            .hasAnyRole("ADMIN", "VENDEDOR");
                    authorize.requestMatchers(HttpMethod.GET, "/api/v1/sales/summary")
                            .hasRole("ADMIN");
                    authorize.requestMatchers("/api/v1/sales/**")
                            .hasAnyRole("ADMIN", "VENDEDOR");
                    authorize.requestMatchers("/api/v1/auth/logout", "/api/v1/auth/me").authenticated();
                    authorize.anyRequest().authenticated();
                })
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        if (properties.requireHttps()) {
            http.redirectToHttps(Customizer.withDefaults());
        }
        return http.build();
    }
}

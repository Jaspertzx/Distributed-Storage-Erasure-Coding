/**
 * This configuration class sets up the security configuration for the application.
 * It defines password encoding, JWT decoding, and HTTP security settings to manage access to different
 * endpoints based on JWT authentication. The configuration integrates with Spring Security's OAuth2
 * resource server support for securing REST endpoints. This means that only certain endpoints can be accessed by
 * authorized users (authenticated through their JWT token).
 * Dependencies:
 * - BCryptPasswordEncoder: Used for encoding passwords.
 * - JwtDecoder: Decodes and verifies JWT tokens using HMAC-SHA512 algorithm.
 * - SecurityFilterChain: Configures HTTP security for the application.
 * Author: Jasper Tan
 */
package com.example.restservice.security;

import com.example.restservice.user.UserDetailsServiceHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    @Autowired
    private UserDetailsServiceHandler userDetailsServiceHandler;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Value("${jwt.secret}")
    private String JWT_SECRET;

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(JWT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS512).build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/auth/register", "/auth/login", "/auth/test","/file/upload", "/file/retrieve").permitAll()
                    .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(new JwtAuthenticationConverter()))
            );

        return http.build();
    }
}

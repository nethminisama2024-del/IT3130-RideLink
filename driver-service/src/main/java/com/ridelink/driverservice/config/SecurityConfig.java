package com.ridelink.driverservice.config;

import com.ridelink.driverservice.security.RideServiceKeyFilter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            @Value("${ride.service.key}") String rideServiceKey) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**",
                                "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/drivers/available")
                                .hasAnyRole("RIDE_SERVICE", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/drivers").hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/drivers/*")
                                .hasAnyRole("DRIVER", "ADMIN", "RIDE_SERVICE")
                        .requestMatchers(HttpMethod.PUT, "/api/drivers/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/drivers/*/availability")
                                .hasAnyRole("DRIVER", "ADMIN", "RIDE_SERVICE")
                        .requestMatchers(HttpMethod.PATCH, "/api/drivers/*/location")
                                .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/drivers/*/vehicle")
                                .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/drivers/*/vehicle")
                                .hasAnyRole("DRIVER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/drivers/*/vehicle")
                                .hasAnyRole("DRIVER", "ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .addFilterBefore(new RideServiceKeyFilter(rideServiceKey), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter((Jwt jwt) -> {
            String role = jwt.getClaimAsString("role");
            if ("PASSENGER".equals(role) || "DRIVER".equals(role) || "ADMIN".equals(role)) {
                return List.of(new SimpleGrantedAuthority("ROLE_" + role));
            }
            return List.of();
        });
        return converter;
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        SecretKey key = new SecretKeySpec(bytes, "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("RideLink"));
        return decoder;
    }
}

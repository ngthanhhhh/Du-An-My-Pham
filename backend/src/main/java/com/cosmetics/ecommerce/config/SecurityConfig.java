package com.cosmetics.ecommerce.config;

import com.cosmetics.ecommerce.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * Cấu hình bảo mật chính cho REST API.
     *
     * Quy ước route:
     * - /api/v1/auth/**        : Public, dùng cho đăng ký/đăng nhập.
     * - /api/v1/admin/**       : Chỉ ADMIN được truy cập.
     * - /api/v1/users/**       : User đã đăng nhập, dùng cho profile/tài khoản hiện tại.
     * - /api/v1/cart/**        : Chỉ CUSTOMER được thao tác giỏ hàng.
     * - /api/v1/orders/**      : Chỉ CUSTOMER được thao tác đơn hàng cá nhân.
     * - GET products/categories: Public để xem sản phẩm, danh mục, review.
     * - POST product reviews   : Chỉ CUSTOMER được viết review.
     * - VNPay return           : Public callback để VNPay redirect về hệ thống.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Cho phép frontend React/Vite gọi API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Tắt CSRF vì hệ thống dùng JWT stateless
                .csrf(csrf -> csrf.disable())

                // Không dùng session phía server, mỗi request xác thực bằng JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // Swagger/OpenAPI và error endpoint
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()

                        // Auth public
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // Public read APIs
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()

                        // VNPay callback/redirect public
                        .requestMatchers(HttpMethod.GET, "/api/v1/payments/vnpay-return").permitAll()

                        // Admin APIs
                        .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")

                        // Customer APIs
                        .requestMatchers("/api/v1/cart/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers("/api/v1/orders/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/vnpay/**").hasAuthority("ROLE_CUSTOMER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews").hasAuthority("ROLE_CUSTOMER")

                        // User profile/current account APIs
                        .requestMatchers("/api/v1/users/**").authenticated()

                        // Các request còn lại phải đăng nhập
                        .anyRequest().authenticated()
                )

                // Gắn JWT filter trước filter mặc định của Spring Security
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Cấu hình CORS cho frontend React/Vite.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
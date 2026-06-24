package com.devcine.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public — không cần token
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/movies/**").permitAll()
                .requestMatchers("/api/showtimes/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/api/formats/**").permitAll()
                .requestMatchers("/api/seats/**").permitAll()
                .requestMatchers("/api/fnbs/**").permitAll()
                // TẠM THỜI: mở public toàn bộ POS bán vé (chưa set phân quyền) — cần siết lại trước khi production
                .requestMatchers("/api/ticketing/**").permitAll()
                .requestMatchers("/api/settings/**").permitAll()
                // Xem đánh giá phim công khai; gửi đánh giá vẫn yêu cầu đăng nhập
                .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                // Danh sách hệ thống rạp — công khai cho trang Cụm rạp
                .requestMatchers(HttpMethod.GET, "/api/v1/cinemas/**").permitAll()
                // Danh mục Tỉnh/Thành & Quận/Huyện — công khai (dropdown form cụm rạp)
                .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()
                // FAQ trang Hỗ trợ — GET công khai; ghi vẫn được @PreAuthorize bảo vệ
                .requestMatchers(HttpMethod.GET, "/api/faqs/**").permitAll()
                // Danh sách khuyến mãi đang chạy — công khai cho trang Khuyến mãi
                .requestMatchers(HttpMethod.GET, "/api/marketing/promotions/active").permitAll()
                .requestMatchers("/api/payment/vnpay_return").permitAll()
                .requestMatchers("/api/system/**").permitAll()
                .requestMatchers("/api/upload/**").permitAll()
                // Yêu cầu xác thực
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

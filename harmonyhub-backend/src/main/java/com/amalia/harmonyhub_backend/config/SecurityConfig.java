package com.amalia.harmonyhub_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Added explicit HttpMethod import
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS always first
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()

                        // WebSocket
                        .requestMatchers("/ws-events/**").permitAll()

                        // Specific POST endpoints that must be public BEFORE the authenticated wildcard
                        .requestMatchers(HttpMethod.POST, "/api/events/seed").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events/stats/heavy/evict").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/events/observations/ai-analysis").hasAuthority("ROLE_ADMIN")
                        // Admin-only GET endpoints BEFORE the general GET wildcard
                        .requestMatchers(HttpMethod.GET, "/api/events/logs").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/events/observations").hasAuthority("ROLE_ADMIN")

                        // Public GET endpoints
                        .requestMatchers(HttpMethod.GET, "/api/events/stats/heavy").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/generator-status").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()

                        // Authenticated modifications
                        .requestMatchers(HttpMethod.POST, "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtRequestFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "https://harmonyhub-frontend.netlify.app/",
                "https://localhost:5173",
                "https://10.212.192.97:5173",
                "http://10.212.192.97:5173",
                "http://localhost:8080",
                "http://localhost:5173"
        ));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public net.datafaker.Faker faker() {
        return new net.datafaker.Faker();
    }
}

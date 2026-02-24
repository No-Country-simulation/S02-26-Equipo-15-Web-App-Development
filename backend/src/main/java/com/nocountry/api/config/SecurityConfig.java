package com.nocountry.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String DEFAULT_ADMIN_USER = "admin";
    private static final String DEFAULT_DEV_ADMIN_PASS = "admin123";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/track", "/api/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(Environment environment, PasswordEncoder passwordEncoder) {
        String adminUser = resolveAdminUser(environment);
        String adminPass = resolveAdminPass(environment);

        UserDetails admin = User.withUsername(adminUser)
                .password(passwordEncoder.encode(adminPass))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    private String resolveAdminUser(Environment environment) {
        String configuredUser = environment.getProperty("ADMIN_USER");
        if (StringUtils.hasText(configuredUser)) {
            return configuredUser.trim();
        }
        return DEFAULT_ADMIN_USER;
    }

    private String resolveAdminPass(Environment environment) {
        String configuredPass = environment.getProperty("ADMIN_PASS");
        if (StringUtils.hasText(configuredPass)) {
            return configuredPass;
        }

        if (isLocalOrDevProfile(environment)) {
            logger.warn("ADMIN_PASS is not set. Using local development default password.");
            return DEFAULT_DEV_ADMIN_PASS;
        }

        logger.warn("ADMIN_PASS is not set outside local/dev profile. Configure ADMIN_PASS to enable admin authentication.");
        return UUID.randomUUID().toString();
    }

    private boolean isLocalOrDevProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> profile.equals("local") || profile.equals("dev"));
    }
}

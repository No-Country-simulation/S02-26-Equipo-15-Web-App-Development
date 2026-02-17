package com.nocountry.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;
    private final Environment environment;

    public CorsConfig(AppProperties appProperties, Environment environment) {
        this.appProperties = appProperties;
        this.environment = environment;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> allowedOriginPatterns = appProperties.getCors().getAllowedOrigins();
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        if (isProd && allowedOriginPatterns.stream().anyMatch("*"::equals)) {
            throw new IllegalStateException("CORS wildcard '*' is not allowed in prod profile");
        }

        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin")
                .exposedHeaders("Location", "Authorization")
                .maxAge(3600)
                .allowCredentials(false);
    }
}

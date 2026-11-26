package uz.tuit.faceid.recognition.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration.
 * <p>
 * The React dev server runs on a different port (Vite 5173, CRA 3000), so the
 * browser blocks the request unless CORS headers are present. A
 * {@code localhost:*} pattern is used rather than a fixed list, because dev
 * ports change often.
 * <p>
 * In production this belongs at the api-gateway, restricted to the real domain.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders(CorrelationIdFilter.HEADER)
                .allowCredentials(true)
                .maxAge(3600);
    }
}

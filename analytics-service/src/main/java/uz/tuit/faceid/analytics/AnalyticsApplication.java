package uz.tuit.faceid.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import uz.tuit.faceid.analytics.config.properties.AnalyticsProperties;

import java.util.Arrays;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnalyticsApplication {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }

    @Bean
    ApplicationRunner startupRunner(Environment environment, AnalyticsProperties properties) {
        return args -> {
            log.info("Profile : {}", Arrays.toString(environment.getActiveProfiles()));
            log.info("Upstream: {} (connect {} ms, read {} ms)",
                    properties.getRecognitionBaseUrl(),
                    properties.getConnectTimeoutMs(),
                    properties.getReadTimeoutMs());
            log.info("Swagger : http://localhost:{}/swagger-ui.html",
                    environment.getProperty("server.port", "8084"));
        };
    }
}

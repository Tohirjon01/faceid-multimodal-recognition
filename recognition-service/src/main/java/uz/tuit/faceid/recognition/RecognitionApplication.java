package uz.tuit.faceid.recognition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import uz.tuit.faceid.recognition.config.properties.DemoProperties;
import uz.tuit.faceid.recognition.repository.TemplateRepository;
import uz.tuit.faceid.recognition.service.DemoDataService;

import java.util.Arrays;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RecognitionApplication {

    private static final Logger log = LoggerFactory.getLogger(RecognitionApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RecognitionApplication.class, args);
    }

    /**
     * Prints the effective state once at startup and seeds the gallery when
     * asked to. Under the {@code prod} profile {@code auto-seed} is disabled,
     * so synthetic data never reaches a production database.
     */
    @Bean
    ApplicationRunner startupRunner(Environment environment,
                                    DemoProperties demoProperties,
                                    DemoDataService demoDataService,
                                    TemplateRepository templateRepository) {
        return args -> {
            log.info("Profile : {}", Arrays.toString(environment.getActiveProfiles()));
            log.info("Backend : {}", templateRepository.getBackendName());

            if (demoProperties.isAutoSeed()) {
                demoDataService.seedIfEmpty();
            } else {
                log.info("Auto-seed disabled — gallery left untouched");
            }

            String port = environment.getProperty("server.port", "8082");
            log.info("Panel   : http://localhost:{}/", port);
            log.info("Swagger : http://localhost:{}/swagger-ui.html", port);
        };
    }
}

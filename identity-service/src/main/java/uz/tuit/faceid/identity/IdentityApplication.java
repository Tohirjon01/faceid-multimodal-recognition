package uz.tuit.faceid.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import uz.tuit.faceid.identity.repository.PersonRepository;

import java.util.Arrays;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityApplication {

    private static final Logger log = LoggerFactory.getLogger(IdentityApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }

    @Bean
    ApplicationRunner startupRunner(Environment environment, PersonRepository personRepository) {
        return args -> {
            log.info("Profile : {}", Arrays.toString(environment.getActiveProfiles()));
            log.info("Backend : {} ({} records)",
                    personRepository.getBackendName(), personRepository.count());
            log.info("Swagger : http://localhost:{}/swagger-ui.html",
                    environment.getProperty("server.port", "8081"));
        };
    }
}

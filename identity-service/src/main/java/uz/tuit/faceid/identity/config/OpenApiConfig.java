package uz.tuit.faceid.identity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI identityOpenApi(@Value("${server.port:8081}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("identity-service API")
                        .version("1.0.0")
                        .description("""
                                Registry of people, roles and biometric consent.

                                Consent and retention are part of the person record itself:
                                under ZRU-547 (Uzbek personal data protection law) biometric
                                data may only be processed while consent is in force.

                                TUIT master's thesis — multimodal face recognition system.""")
                        .contact(new Contact().name("TUIT · master's thesis")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + port)
                        .description("Local run")));
    }
}

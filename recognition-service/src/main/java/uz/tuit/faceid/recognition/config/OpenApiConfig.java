package uz.tuit.faceid.recognition.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI document, served at {@code /swagger-ui.html}.
 * <p>
 * The Python inference-service is written against this contract, so the
 * document is generated from the code itself rather than maintained by hand:
 * when an endpoint changes, the documentation changes with it and the two
 * cannot drift apart.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI recognitionOpenApi(@Value("${server.port:8082}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("recognition-service API")
                        .version("1.0.0")
                        .description("""
                                Biometric template search and decision making.

                                Main flow: the Python inference-service sends a 512-dimensional
                                embedding, a liveness score and the expression probabilities.
                                This service finds the closest subject by cosine similarity and
                                returns a GRANTED / UNKNOWN / SPOOF decision.

                                TUIT master's thesis — multimodal face recognition system.""")
                        .contact(new Contact().name("TUIT · master's thesis"))
                        .license(new License().name("Academic use")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + port)
                        .description("Local run")));
    }
}

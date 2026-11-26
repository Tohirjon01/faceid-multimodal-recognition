package uz.tuit.faceid.analytics.config;

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
    OpenAPI analyticsOpenApi(@Value("${server.port:8084}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("analytics-service API")
                        .version("1.0.0")
                        .description("""
                                Reporting and statistics over recognition events.

                                This service has no database of its own — it pulls events from
                                recognition-service over HTTP. Whoever owns the log stores it
                                (the data-ownership principle).

                                TUIT master's thesis — multimodal face recognition system.""")
                        .contact(new Contact().name("TUIT · master's thesis")))
                .servers(List.of(new Server()
                        .url("http://localhost:" + port)
                        .description("Local run")));
    }
}

package uz.tuit.faceid.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes the clock as a bean.
 * <p>
 * This is what lets tests pin the retention logic to a fixed date. With a
 * direct {@code LocalDate.now()} call there would be no way to assert what
 * happens three years from now.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}

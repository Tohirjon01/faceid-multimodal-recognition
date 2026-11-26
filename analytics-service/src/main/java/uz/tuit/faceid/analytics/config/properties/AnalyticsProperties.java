package uz.tuit.faceid.analytics.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "faceid.analytics")
public class AnalyticsProperties {

    /** Address of recognition-service. In a container this becomes a service name. */
    @NotBlank(message = "must not be blank")
    private String recognitionBaseUrl = "http://localhost:8082";

    /** How many events a report pulls by default. */
    @Min(value = 1, message = "must be at least 1")
    @Max(value = 1000, message = "must not exceed 1000")
    private int eventWindow = 200;

    /** Largest window a single request may ask for. */
    @Min(value = 1, message = "must be at least 1")
    private int maxEventWindow = 500;

    /** Connect timeout in ms — fail fast when the upstream is down. */
    @Min(value = 100, message = "must be at least 100 ms")
    private int connectTimeoutMs = 2_000;

    /** Read timeout in ms — do not hold a thread while the upstream is slow. */
    @Min(value = 100, message = "must be at least 100 ms")
    private int readTimeoutMs = 5_000;

    public String getRecognitionBaseUrl() {
        return recognitionBaseUrl;
    }

    public void setRecognitionBaseUrl(String recognitionBaseUrl) {
        this.recognitionBaseUrl = recognitionBaseUrl;
    }

    public int getEventWindow() {
        return eventWindow;
    }

    public void setEventWindow(int eventWindow) {
        this.eventWindow = eventWindow;
    }

    public int getMaxEventWindow() {
        return maxEventWindow;
    }

    public void setMaxEventWindow(int maxEventWindow) {
        this.maxEventWindow = maxEventWindow;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }
}

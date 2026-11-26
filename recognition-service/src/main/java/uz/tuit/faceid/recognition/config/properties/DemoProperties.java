package uz.tuit.faceid.recognition.config.properties;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Demo data settings.
 * <p>
 * <b>Important:</b> the embeddings generated here are random — they exercise
 * the <em>plumbing</em>, not the accuracy of the system. Under the {@code prod}
 * profile {@code autoSeed} is disabled, so synthetic templates can never reach
 * a production database.
 */
@Validated
@ConfigurationProperties(prefix = "faceid.demo")
public class DemoProperties {

    /** Populate the gallery at startup when it is empty. */
    private boolean autoSeed = true;

    @Min(value = 1, message = "must be at least 1")
    private int subjects = 30;

    @Min(value = 1, message = "must be at least 1")
    private int templatesPerSubject = 2;

    /**
     * Noise level used to build a genuine pair. At 512 dimensions, 0.039 gives
     * a cosine similarity of about 0.75, which is typical for ArcFace.
     */
    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private double genuineNoise = 0.039;

    /** Liveness score applied by the spoof simulation. */
    @DecimalMin(value = "0.0") @DecimalMax(value = "1.0")
    private double spoofLiveness = 0.18;

    public boolean isAutoSeed() {
        return autoSeed;
    }

    public void setAutoSeed(boolean autoSeed) {
        this.autoSeed = autoSeed;
    }

    public int getSubjects() {
        return subjects;
    }

    public void setSubjects(int subjects) {
        this.subjects = subjects;
    }

    public int getTemplatesPerSubject() {
        return templatesPerSubject;
    }

    public void setTemplatesPerSubject(int templatesPerSubject) {
        this.templatesPerSubject = templatesPerSubject;
    }

    public double getGenuineNoise() {
        return genuineNoise;
    }

    public void setGenuineNoise(double genuineNoise) {
        this.genuineNoise = genuineNoise;
    }

    public double getSpoofLiveness() {
        return spoofLiveness;
    }

    public void setSpoofLiveness(double spoofLiveness) {
        this.spoofLiveness = spoofLiveness;
    }
}

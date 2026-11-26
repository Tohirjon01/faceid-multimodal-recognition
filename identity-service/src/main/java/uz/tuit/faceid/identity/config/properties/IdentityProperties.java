package uz.tuit.faceid.identity.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Registry policy.
 * <p>
 * The retention period is configurable: ZRU-547 does not fix a duration, it
 * depends on the organisation's own policy. Hard-coding it would mean a
 * recompile every time the legal requirement changes.
 */
@Validated
@ConfigurationProperties(prefix = "faceid.identity")
public class IdentityProperties {

    /** How long data is retained from the date consent was given, in years. */
    @Min(value = 1, message = "must be at least 1 year")
    @Max(value = 50, message = "must not exceed 50 years")
    private int retentionYears = 3;

    /** Default window for the "expiring soon" report, in days. */
    @Min(value = 1, message = "must be at least 1 day")
    private int defaultExpiryWindowDays = 30;

    public int getRetentionYears() {
        return retentionYears;
    }

    public void setRetentionYears(int retentionYears) {
        this.retentionYears = retentionYears;
    }

    public int getDefaultExpiryWindowDays() {
        return defaultExpiryWindowDays;
    }

    public void setDefaultExpiryWindowDays(int defaultExpiryWindowDays) {
        this.defaultExpiryWindowDays = defaultExpiryWindowDays;
    }
}

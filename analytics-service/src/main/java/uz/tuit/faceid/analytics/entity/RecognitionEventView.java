package uz.tuit.faceid.analytics.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An event as received from {@code recognition-service}.
 * <p>
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} is deliberate: when
 * recognition-service adds a field to its response, analytics must not break.
 * That is what keeps the contract between the two services
 * forward-compatible.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RecognitionEventView(
        Long id,
        Instant occurredAt,
        UUID subjectId,
        String fullName,
        String cameraId,
        String decision,
        Double matchSimilarity,
        Double livenessScore,
        String expression,
        List<String> modalitiesUsed
) {
}

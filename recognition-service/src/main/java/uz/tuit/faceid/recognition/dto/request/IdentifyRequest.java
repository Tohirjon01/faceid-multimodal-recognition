package uz.tuit.faceid.recognition.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request sent by the Python inference-service.
 * <p>
 * Only the computed vector is transferred, never the image itself — this keeps
 * the critical path short.
 * <p>
 * Validation happens at two levels: here, at the HTTP boundary with precise
 * error messages, and inside the service, so that direct calls are guarded too.
 */
@Schema(description = "Identification request")
public record IdentifyRequest(

        @Schema(description = "512-dimensional vector produced by the ArcFace head")
        @NotNull(message = "is a required field")
        @Size(min = 512, max = 512, message = "must have exactly 512 dimensions")
        float[] embedding,

        @Schema(description = "Model version; falls back to the active model when omitted",
                example = "1")
        @Positive(message = "must be positive")
        Integer modelId,

        @Schema(description = "Liveness score in [0..1]; below 0.5 the result is SPOOF",
                example = "0.97")
        @DecimalMin(value = "0.0", message = "must not be below 0.0")
        @DecimalMax(value = "1.0", message = "must not exceed 1.0")
        Double livenessScore,

        @Schema(description = "Camera identifier", example = "cam-01")
        @Size(max = 100, message = "must not exceed 100 characters")
        String cameraId,

        @Schema(description = "Which modalities contributed to this embedding",
                example = "[\"rgb\",\"depth\",\"ir\"]")
        List<String> modalitiesUsed,

        @Schema(description = "Probability distribution over the 7 expression classes")
        float[] expressionProbabilities
) {
}

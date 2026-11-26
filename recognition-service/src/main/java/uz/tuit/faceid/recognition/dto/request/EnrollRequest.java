package uz.tuit.faceid.recognition.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Request to attach a new biometric template to a subject")
public record EnrollRequest(

        @Schema(description = "Identifier of an existing subject")
        @NotNull(message = "is a required field")
        UUID subjectId,

        @Schema(description = "512-dimensional embedding")
        @NotNull(message = "is a required field")
        @Size(min = 512, max = 512, message = "must have exactly 512 dimensions")
        float[] embedding,

        @Schema(description = "Model version; falls back to the active model when omitted")
        @Positive(message = "must be positive")
        Integer modelId,

        @Schema(description = "Modalities this template was derived from")
        List<String> modalities,

        @Schema(description = "Enrollment quality in [0..1]", example = "0.93")
        @DecimalMin(value = "0.0", message = "must not be below 0.0")
        @DecimalMax(value = "1.0", message = "must not exceed 1.0")
        Double qualityScore,

        @Schema(description = "Reference to the cropped image in object storage")
        @Size(max = 512, message = "must not exceed 512 characters")
        String cropUri
) {
}

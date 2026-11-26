package uz.tuit.faceid.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.tuit.faceid.identity.entity.enums.ConsentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Biometric consent state")
public record ConsentResponse(

        UUID subjectId,

        ConsentStatus status,

        Instant givenAt,

        @Schema(description = "When consent was withdrawn; empty while in force")
        Instant revokedAt,

        LocalDate retentionUntil,

        @Schema(description = "Whether biometric data may be processed right now")
        boolean valid
) {
}

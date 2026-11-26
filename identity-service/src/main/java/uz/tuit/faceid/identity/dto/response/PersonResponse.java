package uz.tuit.faceid.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.tuit.faceid.identity.entity.enums.ConsentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Person record")
public record PersonResponse(

        UUID id,

        String fullName,

        String role,

        ConsentStatus consentStatus,

        Instant consentGivenAt,

        @Schema(description = "Date on which the retention period expires")
        LocalDate retentionUntil
) {
}

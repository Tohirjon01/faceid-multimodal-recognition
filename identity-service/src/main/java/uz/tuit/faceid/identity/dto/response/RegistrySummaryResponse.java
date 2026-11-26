package uz.tuit.faceid.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Compliance summary for the registry")
public record RegistrySummaryResponse(

        int total,

        @Schema(description = "Consent in force")
        long active,

        @Schema(description = "Consent withdrawn — templates must be deleted")
        long revoked,

        @Schema(description = "Retention period elapsed")
        long expired
) {
}

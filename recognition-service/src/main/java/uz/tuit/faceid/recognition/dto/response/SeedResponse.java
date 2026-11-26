package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of seeding demo data")
public record SeedResponse(

        @Schema(description = "False when the gallery was already populated and seeding was skipped")
        boolean seeded,

        int subjects,

        int templates
) {
}

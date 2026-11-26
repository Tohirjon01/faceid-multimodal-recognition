package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Subject record")
public record SubjectResponse(

        UUID id,

        String fullName,

        String role,

        @Schema(description = "When consent was obtained — required under ZRU-547")
        Instant consentGivenAt,

        int templateCount
) {
}

package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.tuit.faceid.recognition.entity.enums.Decision;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Entry in the recognition audit log")
public record EventResponse(

        Long id,

        Instant occurredAt,

        UUID subjectId,

        String fullName,

        String cameraId,

        Decision decision,

        Double matchSimilarity,

        Double livenessScore,

        String expression,

        List<String> modalitiesUsed
) {
}

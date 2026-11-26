package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A single candidate from the nearest-neighbour search")
public record CandidateResponse(

        @Schema(description = "Rank, starting at 1", example = "1")
        int rank,

        UUID subjectId,

        String fullName,

        @Schema(description = "Cosine similarity in [-1..1]", example = "0.751")
        double similarity,

        @Schema(description = "Whether the similarity clears the threshold", example = "true")
        boolean aboveThreshold
) {
}

package uz.tuit.faceid.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uz.tuit.faceid.recognition.entity.enums.Decision;

import java.util.List;
import java.util.UUID;

/**
 * Recognition result.
 *
 * @param candidates the top-K candidates. This carries far more information
 *                   than a bare GRANTED/UNKNOWN verdict: it shows how well the
 *                   system actually separates identities.
 */
@Schema(description = "Recognition result and decision")
public record IdentifyResponse(

        Decision decision,

        @Schema(description = "Populated only when the decision is GRANTED")
        UUID subjectId,

        String fullName,

        @Schema(description = "Similarity of the closest candidate")
        Double matchSimilarity,

        @Schema(description = "Threshold in effect", example = "0.65")
        double threshold,

        String expression,

        List<CandidateResponse> candidates,

        @Schema(description = "Time spent in the vector search alone, in milliseconds")
        double searchMs,

        @Schema(description = "Total request time, in milliseconds")
        double totalMs,

        Long eventId,

        @Schema(description = "Which storage backend served the request")
        String backend,

        @Schema(description = "Human-readable reason for the decision")
        String note
) {
}

package uz.tuit.faceid.recognition.entity;

import uz.tuit.faceid.recognition.entity.enums.Decision;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * An entry in the recognition audit log.
 *
 * @param expressionProbabilities the full probability distribution. Storing
 *                                only the argmax would make later calibration
 *                                and confusion-matrix analysis impossible.
 */
public record RecognitionEvent(
        Long id,
        Instant occurredAt,
        UUID subjectId,
        String fullName,
        String cameraId,
        int modelId,
        Decision decision,
        Double matchSimilarity,
        Double livenessScore,
        String expression,
        float[] expressionProbabilities,
        List<String> modalitiesUsed
) {
}

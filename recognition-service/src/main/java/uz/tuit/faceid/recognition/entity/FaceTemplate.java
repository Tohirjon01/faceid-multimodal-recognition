package uz.tuit.faceid.recognition.entity;

import java.util.List;
import java.util.UUID;

/**
 * A stored biometric template.
 *
 * @param modelId   which model version produced this embedding. Without this
 *                  field, embeddings computed before and after a retraining
 *                  cannot be compared at all.
 * @param embedding L2-normalised vector (output of the ArcFace head)
 */
public record FaceTemplate(
        UUID id,
        UUID subjectId,
        int modelId,
        float[] embedding,
        Double qualityScore,
        List<String> modalities,
        String cropUri
) {
}

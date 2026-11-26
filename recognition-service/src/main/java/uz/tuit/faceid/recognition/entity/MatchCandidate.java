package uz.tuit.faceid.recognition.entity;

import java.util.UUID;

/**
 * A single candidate returned by the nearest-neighbour search.
 *
 * @param similarity cosine similarity in [-1, 1]. The pgvector {@code <=>}
 *                   operator yields cosine <em>distance</em>, so
 *                   similarity = 1 − distance.
 */
public record MatchCandidate(
        UUID subjectId,
        String fullName,
        double similarity
) {
}

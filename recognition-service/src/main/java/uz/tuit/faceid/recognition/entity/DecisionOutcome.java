package uz.tuit.faceid.recognition.entity;

import uz.tuit.faceid.recognition.entity.enums.Decision;

/**
 * Result produced by the decision policy.
 *
 * @param bestCandidate the closest candidate; populated for SPOOF and UNKNOWN
 *                      as well, because the similarity is still written to the
 *                      audit log
 * @param note          human-readable reason for the decision
 */
public record DecisionOutcome(
        Decision decision,
        MatchCandidate bestCandidate,
        String note
) {
}

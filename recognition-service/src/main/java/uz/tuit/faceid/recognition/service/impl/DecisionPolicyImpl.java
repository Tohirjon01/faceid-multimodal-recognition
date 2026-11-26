package uz.tuit.faceid.recognition.service.impl;

import org.springframework.stereotype.Service;
import uz.tuit.faceid.recognition.config.properties.MatchingProperties;
import uz.tuit.faceid.recognition.entity.DecisionOutcome;
import uz.tuit.faceid.recognition.entity.MatchCandidate;
import uz.tuit.faceid.recognition.entity.enums.Decision;
import uz.tuit.faceid.recognition.service.DecisionPolicy;

import java.util.List;

/**
 * Default decision policy.
 * <p>
 * <b>Order matters: liveness is checked first.</b> If similarity were checked
 * first, an attacker holding a printed photo could clear the threshold — a
 * photograph does produce a high similarity. The anti-spoofing check must
 * always come <em>before</em> matching.
 */
@Service
public class DecisionPolicyImpl implements DecisionPolicy {

    private final MatchingProperties matchingProperties;

    public DecisionPolicyImpl(MatchingProperties matchingProperties) {
        this.matchingProperties = matchingProperties;
    }

    @Override
    public DecisionOutcome evaluate(Double livenessScore, List<MatchCandidate> candidates) {
        MatchCandidate best = candidates.isEmpty() ? null : candidates.get(0);

        if (livenessScore != null && livenessScore < matchingProperties.getLivenessThreshold()) {
            return new DecisionOutcome(Decision.SPOOF, best,
                    "Liveness %.2f < %.2f — not a live face (photo, screen or mask)"
                            .formatted(livenessScore, matchingProperties.getLivenessThreshold()));
        }

        if (best == null) {
            return new DecisionOutcome(Decision.UNKNOWN, null,
                    "Gallery is empty — nothing to match against");
        }

        if (best.similarity() < matchingProperties.getSimilarityThreshold()) {
            return new DecisionOutcome(Decision.UNKNOWN, best,
                    "Closest candidate %.3f < %.3f threshold — subject is not enrolled"
                            .formatted(best.similarity(), matchingProperties.getSimilarityThreshold()));
        }

        return new DecisionOutcome(Decision.GRANTED, best,
                "Similarity %.3f >= %.3f threshold"
                        .formatted(best.similarity(), matchingProperties.getSimilarityThreshold()));
    }
}

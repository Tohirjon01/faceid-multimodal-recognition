package uz.tuit.faceid.recognition.service;

import uz.tuit.faceid.recognition.entity.DecisionOutcome;
import uz.tuit.faceid.recognition.entity.MatchCandidate;

import java.util.List;

/**
 * Turns a search result into a GRANTED / UNKNOWN / SPOOF decision.
 * <p>
 * Kept behind its own interface because the policy is the part most likely to
 * change: thresholds may eventually depend on the subject's role or on the
 * camera location. The search logic itself does not change.
 */
public interface DecisionPolicy {

    /**
     * @param livenessScore liveness score; when {@code null} the check is skipped
     * @param candidates    candidates sorted by descending similarity
     */
    DecisionOutcome evaluate(Double livenessScore, List<MatchCandidate> candidates);
}

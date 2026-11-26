package uz.tuit.faceid.recognition.service;

import uz.tuit.faceid.recognition.dto.response.SeedResponse;
import uz.tuit.faceid.recognition.entity.DemoProbe;

/**
 * Demo gallery and synthetic probe vectors.
 * <p>
 * <b>Important:</b> the embeddings generated here are random. They exercise the
 * <em>plumbing</em> of the system, not its accuracy. Real accuracy figures come
 * from the trained model and the ablation study.
 */
public interface DemoDataService {

    /** Populates the gallery only when it is empty; repeated calls do nothing. */
    SeedResponse seedIfEmpty();

    SeedResponse seed(int subjectCount, int templatesPerSubject);

    /** A perturbed vector of a random enrolled subject — GRANTED is expected. */
    DemoProbe createKnownProbe();

    /** A vector of nobody in the gallery — UNKNOWN is expected. */
    DemoProbe createStrangerProbe();

    /** A plausible expression distribution: one dominant class, summing to 1. */
    float[] sampleExpressionProbabilities();

    /** Liveness score used by the spoof simulation. */
    double getSpoofLivenessScore();
}

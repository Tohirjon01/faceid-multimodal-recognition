package uz.tuit.faceid.recognition.service;

import uz.tuit.faceid.recognition.dto.request.EnrollRequest;
import uz.tuit.faceid.recognition.dto.request.IdentifyRequest;
import uz.tuit.faceid.recognition.dto.response.EnrollResponse;
import uz.tuit.faceid.recognition.dto.response.IdentifyResponse;

/** Biometric matching and enrollment. */
public interface MatchingService {

    /** Identifies a subject from an embedding, decides, and records the event. */
    IdentifyResponse identify(IdentifyRequest request);

    /** Attaches a new biometric template to an existing subject. */
    EnrollResponse enroll(EnrollRequest request);

    /**
     * Returns the query execution plan.
     *
     * @throws uz.tuit.faceid.recognition.exception.ExplainNotSupportedException
     *         the in-memory backend has no notion of a plan
     */
    String explainSearch(float[] probe, Integer modelId);
}

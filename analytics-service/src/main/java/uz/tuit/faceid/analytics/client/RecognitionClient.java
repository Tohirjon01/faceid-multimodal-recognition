package uz.tuit.faceid.analytics.client;

import uz.tuit.faceid.analytics.entity.RecognitionEventView;

import java.util.List;

/**
 * Access to recognition-service.
 * <p>
 * Kept behind an interface so that tests can substitute a stub without
 * starting HTTP, and so that a change of transport later (gRPC, Kafka) leaves
 * the service layer untouched.
 */
public interface RecognitionClient {

    /**
     * Fetches the most recent events.
     *
     * @throws uz.tuit.faceid.analytics.exception.UpstreamUnavailableException
     *         when the upstream service does not answer
     */
    List<RecognitionEventView> fetchRecentEvents(int limit);

    /** Upstream address, echoed in the report. */
    String getSourceUrl();
}

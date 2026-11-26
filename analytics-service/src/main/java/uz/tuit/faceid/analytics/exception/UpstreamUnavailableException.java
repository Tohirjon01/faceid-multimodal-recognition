package uz.tuit.faceid.analytics.exception;

/**
 * The upstream service did not answer.
 * <p>
 * This is surfaced as 503 rather than 500, and the difference matters: it says
 * "I am healthy, but a service I depend on is not answering". Monitoring reads
 * that correctly, and the caller knows a retry may succeed.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String baseUrl, Throwable cause) {
        super("recognition-service is unavailable: " + baseUrl
                + " — start that service first", cause);
    }
}

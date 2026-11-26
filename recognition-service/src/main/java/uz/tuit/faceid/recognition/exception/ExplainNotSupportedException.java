package uz.tuit.faceid.recognition.exception;

/**
 * Query plans are only available on the PostgreSQL backend.
 * The in-memory store has no notion of an execution plan.
 */
public class ExplainNotSupportedException extends RuntimeException {

    public ExplainNotSupportedException(String backend) {
        super("EXPLAIN is only available on the PostgreSQL backend. Current backend: " + backend
                + ". Start the service with the 'preprod' or 'prod' profile to inspect the HNSW plan.");
    }
}

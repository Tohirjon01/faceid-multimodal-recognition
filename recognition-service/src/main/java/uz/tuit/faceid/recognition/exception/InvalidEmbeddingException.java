package uz.tuit.faceid.recognition.exception;

/**
 * The embedding is missing or has an unexpected dimension.
 * <p>
 * Bean validation also checks this at the HTTP boundary, but the guard stays
 * here as well so the service remains safe when called directly (from tests
 * or from another component).
 */
public class InvalidEmbeddingException extends RuntimeException {

    public InvalidEmbeddingException(String message) {
        super(message);
    }

    public static InvalidEmbeddingException missing() {
        return new InvalidEmbeddingException("embedding is a required field");
    }

    public static InvalidEmbeddingException wrongDimension(int expected, int actual) {
        return new InvalidEmbeddingException(
                "embedding must have dimension %d, received %d".formatted(expected, actual));
    }
}

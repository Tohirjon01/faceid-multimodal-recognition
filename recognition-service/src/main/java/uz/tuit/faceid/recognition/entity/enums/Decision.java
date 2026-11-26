package uz.tuit.faceid.recognition.entity.enums;

/**
 * Outcome of the recognition attempt.
 *
 * <ul>
 *   <li>{@code GRANTED} — a template was matched above the similarity threshold</li>
 *   <li>{@code UNKNOWN} — no sufficiently close template exists in the gallery</li>
 *   <li>{@code SPOOF}   — liveness check failed (photo, screen replay, mask)</li>
 *   <li>{@code DENIED}  — subject recognised but not authorised (returned by access-control)</li>
 * </ul>
 */
public enum Decision {
    GRANTED,
    UNKNOWN,
    SPOOF,
    DENIED
}

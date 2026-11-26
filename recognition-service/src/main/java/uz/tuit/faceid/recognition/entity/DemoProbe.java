package uz.tuit.faceid.recognition.entity;

import java.util.UUID;

/**
 * A synthetic probe vector generated for the demo panel.
 *
 * @param expectedSubjectId the subject the probe was derived from;
 *                          {@code null} for a stranger probe
 */
public record DemoProbe(
        UUID expectedSubjectId,
        String expectedFullName,
        float[] embedding
) {
}

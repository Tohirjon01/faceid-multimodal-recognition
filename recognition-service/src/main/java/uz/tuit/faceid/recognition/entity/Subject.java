package uz.tuit.faceid.recognition.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A person enrolled in the biometric system.
 *
 * @param consentGivenAt when consent was obtained — mandatory under ZRU-547
 *                       (Uzbek personal data protection law)
 * @param retentionUntil the date on which the retention period expires
 */
public record Subject(
        UUID id,
        String fullName,
        String role,
        Instant consentGivenAt,
        LocalDate retentionUntil
) {
}

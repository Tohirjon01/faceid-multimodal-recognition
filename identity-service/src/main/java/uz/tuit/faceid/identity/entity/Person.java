package uz.tuit.faceid.identity.entity;

import uz.tuit.faceid.identity.entity.enums.ConsentStatus;
import uz.tuit.faceid.identity.entity.enums.PersonRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A person in the system together with their biometric consent.
 * <p>
 * Consent and retention are deliberately part of the person record itself:
 * without them there is no legal basis for processing biometric data, so these
 * fields cannot be optional.
 *
 * @param consentRevokedAt when consent was withdrawn; {@code null} while in force
 */
public record Person(
        UUID id,
        String fullName,
        PersonRole role,
        Instant consentGivenAt,
        Instant consentRevokedAt,
        LocalDate retentionUntil,
        Instant createdAt
) {

    /** Consent is valid when it has not been withdrawn and has not expired. */
    public boolean isConsentValid(LocalDate today) {
        return consentRevokedAt == null && !today.isAfter(retentionUntil);
    }

    public ConsentStatus resolveConsentStatus(LocalDate today) {
        if (consentRevokedAt != null) {
            return ConsentStatus.REVOKED;
        }
        if (today.isAfter(retentionUntil)) {
            return ConsentStatus.EXPIRED;
        }
        return ConsentStatus.ACTIVE;
    }

    public Person withConsentRevoked(Instant revokedAt) {
        return new Person(id, fullName, role, consentGivenAt, revokedAt, retentionUntil, createdAt);
    }

    public Person withConsentRenewed(Instant renewedAt, LocalDate newRetentionUntil) {
        return new Person(id, fullName, role, renewedAt, null, newRetentionUntil, createdAt);
    }
}

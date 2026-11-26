package uz.tuit.faceid.identity.service;

import uz.tuit.faceid.identity.dto.response.ConsentResponse;

import java.util.UUID;

/**
 * Biometric consent management.
 * <p>
 * Kept separate from {@code PersonService} because consent is a legal process
 * with its own audit trail, and it will eventually emit events of its own —
 * when consent is withdrawn, recognition-service has to delete the templates.
 */
public interface ConsentService {

    ConsentResponse getConsent(UUID personId);

    /** Withdraws consent, which means the stored templates must be deleted. */
    ConsentResponse revoke(UUID personId);

    /** Renews consent and recalculates the retention period. */
    ConsentResponse renew(UUID personId);
}

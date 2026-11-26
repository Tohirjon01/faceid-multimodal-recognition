package uz.tuit.faceid.identity.entity.enums;

/**
 * State of a subject's biometric consent.
 * <p>
 * Under ZRU-547 (Uzbek personal data protection law), biometric data may only
 * be processed while consent is in force.
 */
public enum ConsentStatus {

    /** Consent is in force — biometric data may be processed. */
    ACTIVE,

    /** The subject withdrew consent — their templates must be deleted. */
    REVOKED,

    /** The retention period has elapsed — delete unless consent is renewed. */
    EXPIRED
}

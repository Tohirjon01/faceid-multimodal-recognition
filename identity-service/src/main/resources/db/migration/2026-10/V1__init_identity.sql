-- ════════════════════════════════════════════════════════════════════
--  identity-service — initial schema
--  Person registry and biometric consent
-- ════════════════════════════════════════════════════════════════════

CREATE SCHEMA IF NOT EXISTS identity;


-- ── People ──────────────────────────────────────────────────────────
-- UUID rather than SERIAL: sequential identifiers invite enumeration
-- attacks once they are exposed through a public API.
--
-- consent_given_at and retention_until are NOT NULL because under ZRU-547
-- biometric data cannot be processed without recorded consent. Making the
-- columns mandatory enforces that at the schema level.
CREATE TABLE identity.persons (
    id                 UUID         PRIMARY KEY,
    full_name          TEXT         NOT NULL,
    role               TEXT         NOT NULL DEFAULT 'employee',
    consent_given_at   TIMESTAMPTZ  NOT NULL,
    -- NULL means consent is in force; a value records when it was withdrawn
    consent_revoked_at TIMESTAMPTZ,
    retention_until    DATE         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_persons_role
        CHECK (role IN ('employee', 'manager', 'guest', 'security')),

    CONSTRAINT chk_persons_revoked_after_given
        CHECK (consent_revoked_at IS NULL OR consent_revoked_at >= consent_given_at)
);

COMMENT ON TABLE identity.persons IS
    'Person registry. Consent and retention are mandatory under ZRU-547.';

COMMENT ON COLUMN identity.persons.consent_revoked_at IS
    'NULL while consent is in force. Once set, biometric templates must be deleted.';


-- Supports the compliance report: find people whose retention is about to end.
-- A partial index, since those who withdrew consent are excluded anyway.
CREATE INDEX idx_persons_retention_active
    ON identity.persons (retention_until)
    WHERE consent_revoked_at IS NULL;

CREATE INDEX idx_persons_full_name ON identity.persons (full_name);

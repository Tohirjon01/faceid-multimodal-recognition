-- ════════════════════════════════════════════════════════════════════
--  FaceID — initial schema
--  Multimodal face recognition and expression analysis
-- ════════════════════════════════════════════════════════════════════

-- The extensions are installed into the public schema on purpose: during a
-- Flyway migration the search_path is set to 'faceid', but runtime connections
-- use the database default. The 'vector' type therefore has to live in public.
CREATE EXTENSION IF NOT EXISTS vector SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pgcrypto SCHEMA public;

CREATE SCHEMA IF NOT EXISTS faceid;


-- ── Model registry ──────────────────────────────────────────────────
-- Records which model produced every embedding. Without this column, after a
-- retraining the old and new vectors no longer live in the same space, and
-- there is no option left but to re-enroll the entire gallery.
CREATE TABLE faceid.model_versions (
    id          SMALLSERIAL  PRIMARY KEY,
    name        TEXT         NOT NULL,
    git_sha     TEXT,
    embed_dim   SMALLINT     NOT NULL,
    modalities  TEXT[]       NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT FALSE,
    deployed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

COMMENT ON TABLE faceid.model_versions IS
    'Embedding model versions. Referenced by face_templates and recognition_events.';


-- ── Subjects ────────────────────────────────────────────────────────
-- UUID rather than SERIAL: sequential identifiers invite enumeration attacks
-- once they are exposed through a public API.
CREATE TABLE faceid.subjects (
    id               UUID         PRIMARY KEY,
    full_name        TEXT         NOT NULL,
    role             TEXT         NOT NULL DEFAULT 'employee',
    -- Required by ZRU-547 (Uzbek personal data protection law): biometric data
    -- may only be processed on the basis of recorded consent.
    consent_given_at TIMESTAMPTZ  NOT NULL,
    retention_until  DATE         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);


-- ── Biometric templates ─────────────────────────────────────────────
CREATE TABLE faceid.face_templates (
    id            UUID               PRIMARY KEY,
    subject_id    UUID               NOT NULL REFERENCES faceid.subjects(id) ON DELETE CASCADE,
    model_id      SMALLINT           NOT NULL REFERENCES faceid.model_versions(id),
    embedding     public.vector(512) NOT NULL,
    quality_score DOUBLE PRECISION,
    modalities    TEXT[]             NOT NULL DEFAULT '{}',
    crop_uri      TEXT,
    created_at    TIMESTAMPTZ        NOT NULL DEFAULT now()
);

CREATE INDEX idx_tpl_subject ON faceid.face_templates (subject_id);

-- One HNSW index PER MODEL VERSION (a partial index). Vectors produced by
-- different models do not share a space, so they cannot be mixed in one index.
--
-- Note: this index is only used when the query orders by
--   ORDER BY embedding <=> probe
-- Writing  ORDER BY 1 - (embedding <=> probe) DESC  makes the planner drop the
-- index and scan the whole table instead.
--
-- On pgvector below 0.5 there is no hnsw access method; use instead:
--   USING ivfflat (embedding public.vector_cosine_ops) WITH (lists = 100)
CREATE INDEX idx_tpl_hnsw_m1 ON faceid.face_templates
    USING hnsw (embedding public.vector_cosine_ops)
    WHERE model_id = 1;


-- ── Recognition audit log ───────────────────────────────────────────
-- An unpartitioned table becomes unqueryable within months at 50 cameras and
-- thousands of rows per day.
--
-- In a partitioned table the primary key MUST contain the partition key,
-- hence PRIMARY KEY (id, occurred_at).
CREATE TABLE faceid.recognition_events (
    id               BIGSERIAL,
    occurred_at      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    subject_id       UUID             REFERENCES faceid.subjects(id) ON DELETE SET NULL,
    camera_id        TEXT             NOT NULL,
    model_id         SMALLINT         NOT NULL,
    decision         TEXT             NOT NULL,
    match_similarity DOUBLE PRECISION,
    liveness_score   DOUBLE PRECISION,
    expression       TEXT,
    -- Storing only the argmax would make later calibration work and confusion
    -- matrix analysis impossible, so the full distribution is kept.
    expression_probs REAL[],
    modalities_used  TEXT[]           NOT NULL DEFAULT '{}',
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

CREATE TABLE faceid.recognition_events_2026_10 PARTITION OF faceid.recognition_events
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

CREATE TABLE faceid.recognition_events_2026_11 PARTITION OF faceid.recognition_events
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

-- Default partition: inserts keep working even if someone forgets to create
-- next month's partition.
CREATE TABLE faceid.recognition_events_default PARTITION OF faceid.recognition_events DEFAULT;

CREATE INDEX idx_events_occurred ON faceid.recognition_events (occurred_at DESC);
CREATE INDEX idx_events_subject  ON faceid.recognition_events (subject_id);


-- ── Active model ────────────────────────────────────────────────────
-- This row gets id = 1, matching faceid.matching.active-model-id in application.yml.
INSERT INTO faceid.model_versions (name, git_sha, embed_dim, modalities, is_active)
VALUES ('mm-arcface-rgbd-ir', 'dev-local', 512, '{rgb,depth,ir}', TRUE);

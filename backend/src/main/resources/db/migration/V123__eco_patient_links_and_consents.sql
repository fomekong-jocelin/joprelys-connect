-- ECO-20261107 S3 : liaison d'un profil d'une application de l'écosystème (AllôPharma) à un DPU, et consentement.
-- Aucune donnée d'identité copiée : le lien porte des identifiants opaques, le DPU et le sujet Joprelys ID.

CREATE TABLE eco_patient_links (
    id                    UUID PRIMARY KEY,
    patient_id            UUID         NOT NULL REFERENCES patients (id),
    dpu                   VARCHAR(64)  NOT NULL,
    issuer                VARCHAR(80)  NOT NULL,
    application           VARCHAR(50)  NOT NULL,
    external_profile_id   UUID         NOT NULL,
    relationship          VARCHAR(20)  NOT NULL,
    identity_subject      VARCHAR(255) NOT NULL,
    client_id             VARCHAR(100) NOT NULL,
    status                VARCHAR(20)  NOT NULL,
    version_id            INTEGER      NOT NULL,
    created_at            TIMESTAMP    NOT NULL,
    revoked_at            TIMESTAMP,
    CONSTRAINT ck_eco_patient_links_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_eco_patient_links_relationship CHECK (relationship IN ('SELF', 'CHILD', 'DEPENDENT_ADULT')),
    CONSTRAINT ck_eco_patient_links_version CHECK (version_id >= 1)
);

CREATE INDEX idx_eco_patient_links_patient ON eco_patient_links (patient_id);
CREATE INDEX idx_eco_patient_links_profile ON eco_patient_links (application, external_profile_id);

CREATE TABLE eco_consents (
    id                 UUID PRIMARY KEY,
    patient_link_id    UUID         NOT NULL REFERENCES eco_patient_links (id),
    status             VARCHAR(20)  NOT NULL,
    scopes             VARCHAR(255) NOT NULL,
    grantee_type       VARCHAR(20)  NOT NULL,
    grantee_reference  VARCHAR(100) NOT NULL,
    period_start       TIMESTAMP    NOT NULL,
    period_end         TIMESTAMP,
    granted_at         TIMESTAMP    NOT NULL,
    revoked_at         TIMESTAMP,
    revoked_by         VARCHAR(20),
    revocation_reason  VARCHAR(200),
    version_id         INTEGER      NOT NULL,
    CONSTRAINT ck_eco_consents_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_eco_consents_grantee_type CHECK (grantee_type IN ('APPLICATION', 'ORGANIZATION', 'PRACTITIONER')),
    CONSTRAINT ck_eco_consents_revoked_by CHECK (revoked_by IS NULL OR revoked_by IN ('PATIENT', 'GUARDIAN', 'SYSTEM')),
    CONSTRAINT ck_eco_consents_revocation CHECK (
        (status = 'ACTIVE' AND revoked_at IS NULL) OR (status = 'REVOKED' AND revoked_at IS NOT NULL AND revoked_by IS NOT NULL)),
    CONSTRAINT ck_eco_consents_version CHECK (version_id >= 1)
);

CREATE INDEX idx_eco_consents_link ON eco_consents (patient_link_id);

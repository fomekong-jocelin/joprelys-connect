CREATE TABLE emergency_third_parties (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40),
    email VARCHAR(255),
    id_document VARCHAR(120),
    relationship_to_patient VARCHAR(80),
    circumstances VARCHAR(1000),
    consent_to_contact BOOLEAN NOT NULL DEFAULT FALSE,
    legal_representative_claimed BOOLEAN NOT NULL DEFAULT FALSE,
    source_type VARCHAR(40) NOT NULL,
    confidence_level VARCHAR(24) NOT NULL,
    proof_reference VARCHAR(255),
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_emergency_third_party_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_third_party_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_emergency_third_party_dossier
    ON emergency_third_parties (organization_id, emergency_id, created_at);

CREATE TABLE emergency_third_party_qualities (
    third_party_id UUID NOT NULL,
    quality VARCHAR(48) NOT NULL,
    PRIMARY KEY (third_party_id, quality),
    CONSTRAINT fk_emergency_third_party_quality
        FOREIGN KEY (third_party_id) REFERENCES emergency_third_parties(id) ON DELETE CASCADE
);

CREATE TABLE emergency_identity_statements (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID NOT NULL,
    third_party_id UUID,
    field_name VARCHAR(80) NOT NULL,
    declared_value TEXT NOT NULL,
    confidence_level VARCHAR(24) NOT NULL,
    proof_reference VARCHAR(255),
    declared_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_emergency_identity_statement_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_identity_statement_third_party
        FOREIGN KEY (third_party_id) REFERENCES emergency_third_parties(id) ON DELETE SET NULL,
    CONSTRAINT fk_emergency_identity_statement_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_emergency_identity_statement_dossier
    ON emergency_identity_statements (organization_id, emergency_id, declared_at);

CREATE TABLE emergency_capacity_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID NOT NULL,
    capacity_status VARCHAR(24) NOT NULL,
    consciousness_level VARCHAR(32),
    clinical_reason VARCHAR(1000) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_emergency_capacity_event_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_capacity_event_recorder
        FOREIGN KEY (recorded_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_emergency_capacity_event_dossier
    ON emergency_capacity_events (organization_id, emergency_id, effective_at);

CREATE TABLE emergency_legal_bases (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID NOT NULL,
    basis_type VARCHAR(48) NOT NULL,
    justification VARCHAR(1500) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    closure_reason VARCHAR(500),
    created_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_emergency_legal_basis_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_legal_basis_creator
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_emergency_legal_basis_dossier
    ON emergency_legal_bases (organization_id, emergency_id, starts_at);

CREATE TABLE emergency_legal_basis_acts (
    legal_basis_id UUID NOT NULL,
    covered_act VARCHAR(255) NOT NULL,
    PRIMARY KEY (legal_basis_id, covered_act),
    CONSTRAINT fk_emergency_legal_basis_act
        FOREIGN KEY (legal_basis_id) REFERENCES emergency_legal_bases(id) ON DELETE CASCADE
);

CREATE TABLE emergency_belongings (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID NOT NULL,
    category VARCHAR(48) NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity INTEGER NOT NULL,
    item_condition VARCHAR(255),
    seal_number VARCHAR(80),
    custody_status VARCHAR(32) NOT NULL,
    deposited_by_name VARCHAR(160),
    received_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_emergency_belonging_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_belonging_receiver
        FOREIGN KEY (received_by_user_id) REFERENCES users(id),
    CONSTRAINT chk_emergency_belonging_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_emergency_belonging_dossier
    ON emergency_belongings (organization_id, emergency_id, created_at);

CREATE TABLE emergency_belonging_transfers (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    belonging_id UUID NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    from_custodian VARCHAR(160),
    recipient_name VARCHAR(160),
    recipient_id_document VARCHAR(120),
    notes VARCHAR(1000),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    performed_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_emergency_belonging_transfer_item
        FOREIGN KEY (belonging_id) REFERENCES emergency_belongings(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_belonging_transfer_actor
        FOREIGN KEY (performed_by_user_id) REFERENCES users(id)
);

CREATE INDEX idx_emergency_belonging_transfer_history
    ON emergency_belonging_transfers (organization_id, belonging_id, occurred_at);

-- Preserve the single accompanying person captured by V59 as the first structured third party.
INSERT INTO emergency_third_parties (
    id, organization_id, emergency_id, full_name, phone, id_document,
    relationship_to_patient, circumstances, consent_to_contact,
    legal_representative_claimed, source_type, confidence_level,
    created_by_user_id, created_at, updated_at, version
)
SELECT
    e.id, e.organization_id, e.id, e.third_party_name, e.third_party_phone,
    e.third_party_id_document, e.third_party_relationship,
    e.third_party_circumstances, e.third_party_consent_to_contact,
    FALSE, 'ACCOMPANYING_PERSON', 'LOW', e.created_by_user_id,
    e.created_at, e.updated_at, 0
FROM emergencies e
WHERE e.third_party_name IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM emergency_third_parties p WHERE p.id = e.id
  );

INSERT INTO emergency_third_party_qualities (third_party_id, quality)
SELECT e.id, 'ACCOMPANYING_PERSON'
FROM emergencies e
WHERE e.third_party_name IS NOT NULL
  AND EXISTS (
      SELECT 1 FROM emergency_third_parties p WHERE p.id = e.id
  )
  AND NOT EXISTS (
      SELECT 1 FROM emergency_third_party_qualities q
      WHERE q.third_party_id = e.id AND q.quality = 'ACCOMPANYING_PERSON'
  );

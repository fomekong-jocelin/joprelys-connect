-- Reconciliation events are immutable evidence and must never reference an unknown
-- patient or an actor from another tenant. V62 deliberately preserved raw identifiers;
-- this hardening migration restores restrictive composite foreign keys.

ALTER TABLE users
    ADD CONSTRAINT uq_users_id_organization UNIQUE (id, organization_id);

ALTER TABLE patient_reconciliation_events
    ADD CONSTRAINT fk_patient_reconciliation_event_source
        FOREIGN KEY (source_patient_id, organization_id)
        REFERENCES patients(id, organization_id);

ALTER TABLE patient_reconciliation_events
    ADD CONSTRAINT fk_patient_reconciliation_event_candidate
        FOREIGN KEY (candidate_patient_id, organization_id)
        REFERENCES patients(id, organization_id);

ALTER TABLE patient_reconciliation_events
    ADD CONSTRAINT fk_patient_reconciliation_event_actor
        FOREIGN KEY (created_by_user_id, organization_id)
        REFERENCES users(id, organization_id);

-- Candidate preselection stays tenant-scoped through Hibernate and uses bounded exact
-- filters before Java scoring. These indexes keep the initial selection predictable on
-- large facilities without introducing database-specific full-text features.
CREATE INDEX idx_patients_reconciliation_birth_date
    ON patients (organization_id, identity_status, status, birth_date);

CREATE INDEX idx_patients_reconciliation_phone
    ON patients (organization_id, identity_status, status, phone);

CREATE INDEX idx_patients_reconciliation_city_gender
    ON patients (organization_id, identity_status, status, city, gender);

-- Test-only cleanup policy.
--
-- Production keeps canonical links and aliases restrictive. Reconciliation events
-- retain patient and actor identifiers as immutable evidence without lifecycle FKs.
-- The shared H2 database is reused by integration tests whose fixtures delete
-- patients directly, so cascades are enabled only for operational projections.

ALTER TABLE patient_canonical_links
    DROP CONSTRAINT fk_patient_canonical_link_event;
ALTER TABLE patient_canonical_links
    DROP CONSTRAINT fk_patient_canonical_link_source;
ALTER TABLE patient_canonical_links
    DROP CONSTRAINT fk_patient_canonical_link_target;
ALTER TABLE patient_identity_aliases
    DROP CONSTRAINT fk_patient_identity_alias_origin;
ALTER TABLE patient_identity_aliases
    DROP CONSTRAINT fk_patient_identity_alias_canonical;
ALTER TABLE patient_reconciliation_events
    DROP CONSTRAINT fk_patient_reconciliation_corrected_event;

ALTER TABLE patient_reconciliation_events
    ADD CONSTRAINT fk_patient_reconciliation_corrected_event
        FOREIGN KEY (corrected_event_id, organization_id)
        REFERENCES patient_reconciliation_events(id, organization_id)
        ON DELETE CASCADE;
ALTER TABLE patient_canonical_links
    ADD CONSTRAINT fk_patient_canonical_link_source
        FOREIGN KEY (source_patient_id, organization_id)
        REFERENCES patients(id, organization_id)
        ON DELETE CASCADE;
ALTER TABLE patient_canonical_links
    ADD CONSTRAINT fk_patient_canonical_link_target
        FOREIGN KEY (canonical_patient_id, organization_id)
        REFERENCES patients(id, organization_id)
        ON DELETE CASCADE;
ALTER TABLE patient_canonical_links
    ADD CONSTRAINT fk_patient_canonical_link_event
        FOREIGN KEY (decision_event_id, organization_id)
        REFERENCES patient_reconciliation_events(id, organization_id)
        ON DELETE CASCADE;
ALTER TABLE patient_identity_aliases
    ADD CONSTRAINT fk_patient_identity_alias_origin
        FOREIGN KEY (origin_patient_id, organization_id)
        REFERENCES patients(id, organization_id)
        ON DELETE CASCADE;
ALTER TABLE patient_identity_aliases
    ADD CONSTRAINT fk_patient_identity_alias_canonical
        FOREIGN KEY (canonical_patient_id, organization_id)
        REFERENCES patients(id, organization_id)
        ON DELETE CASCADE;
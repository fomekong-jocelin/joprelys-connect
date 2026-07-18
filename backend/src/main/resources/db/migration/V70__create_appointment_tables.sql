-- STORY-2601 (EPIC-0025) : fondations du module rendez-vous.
-- Trois tables tenantées : disponibilités récurrentes, exceptions d'indisponibilité, rendez-vous.
-- Anti double réservation : colonne technique active_start_at maintenue par l'entité
-- (start_at si statut actif, NULL si annulé) + index unique (doctor_id, active_start_at).
-- Forme portable H2/PostgreSQL : aucun index partiel n'existe dans les migrations V1 à V69.

CREATE TABLE doctor_availabilities (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    doctor_id UUID NOT NULL,
    -- 1 = lundi .. 7 = dimanche (ISO-8601)
    weekday INTEGER NOT NULL,
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_doctor_availabilities_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_doctor_availabilities_weekday CHECK (weekday BETWEEN 1 AND 7),
    CONSTRAINT ck_doctor_availabilities_time_range CHECK (end_time > start_time),
    CONSTRAINT ck_doctor_availabilities_validity CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE INDEX idx_doctor_availabilities_org_doctor
    ON doctor_availabilities (organization_id, doctor_id);
CREATE INDEX idx_doctor_availabilities_org_doctor_weekday
    ON doctor_availabilities (organization_id, doctor_id, weekday);

CREATE TABLE doctor_availability_exceptions (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    doctor_id UUID NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_doctor_availability_exceptions_doctor FOREIGN KEY (doctor_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_doctor_availability_exceptions_range CHECK (end_at > start_at)
);

CREATE INDEX idx_doctor_availability_exceptions_org_doctor
    ON doctor_availability_exceptions (organization_id, doctor_id);
CREATE INDEX idx_doctor_availability_exceptions_org_doctor_period
    ON doctor_availability_exceptions (organization_id, doctor_id, start_at);

CREATE TABLE appointments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    doctor_id UUID NOT NULL,
    patient_id UUID NOT NULL,
    start_at TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CONFIRMED',
    reason TEXT,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason VARCHAR(255),
    visit_id UUID,
    reminder_sent_at TIMESTAMP WITH TIME ZONE,
    -- = start_at si statut actif, NULL si annulé (maintenu côté entité)
    active_start_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_appointments_visit FOREIGN KEY (visit_id) REFERENCES visits(id) ON DELETE SET NULL,
    CONSTRAINT ck_appointments_time_range CHECK (end_at > start_at)
);

-- Un seul RDV actif par médecin et par créneau ; les NULL multiples (RDV annulés)
-- sont autorisés par PostgreSQL et H2, donc un créneau annulé redevient réservable.
CREATE UNIQUE INDEX uq_appointments_doctor_active_slot
    ON appointments (doctor_id, active_start_at);
CREATE INDEX idx_appointments_org_doctor_start
    ON appointments (organization_id, doctor_id, start_at);
CREATE INDEX idx_appointments_org_patient
    ON appointments (organization_id, patient_id);
CREATE INDEX idx_appointments_org_start
    ON appointments (organization_id, start_at);

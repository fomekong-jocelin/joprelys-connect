CREATE TABLE emergency_triage_assessments (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    emergency_id UUID NOT NULL,
    assessment_type VARCHAR(20) NOT NULL,
    sequence_number INTEGER NOT NULL,
    triage_level VARCHAR(20) NOT NULL,
    hemodynamic_status VARCHAR(50) NOT NULL,
    airway_status VARCHAR(32) NOT NULL,
    breathing_status VARCHAR(32) NOT NULL,
    circulation_status VARCHAR(32) NOT NULL,
    disability_status VARCHAR(32) NOT NULL,
    exposure_status VARCHAR(32) NOT NULL,
    bp_systolic INTEGER,
    bp_diastolic INTEGER,
    heart_rate INTEGER,
    respiratory_rate INTEGER,
    oxygen_saturation INTEGER,
    temperature NUMERIC(4,2),
    gcs_score INTEGER,
    pain_score INTEGER,
    recommended_orientation VARCHAR(32),
    clinical_notes TEXT,
    assessed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assessed_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_emergency_triage_assessment_emergency
        FOREIGN KEY (emergency_id) REFERENCES emergencies(id) ON DELETE CASCADE,
    CONSTRAINT fk_emergency_triage_assessment_actor
        FOREIGN KEY (assessed_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_emergency_triage_assessment_sequence
        UNIQUE (organization_id, emergency_id, sequence_number),
    CONSTRAINT chk_emergency_triage_assessment_sequence
        CHECK (sequence_number > 0),
    CONSTRAINT chk_emergency_triage_assessment_type
        CHECK (CASE assessment_type
            WHEN 'INITIAL' THEN TRUE
            WHEN 'REASSESSMENT' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_airway
        CHECK (CASE airway_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'PATENT' THEN TRUE
            WHEN 'AT_RISK' THEN TRUE
            WHEN 'OBSTRUCTED' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_breathing
        CHECK (CASE breathing_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'ADEQUATE' THEN TRUE
            WHEN 'DISTRESS' THEN TRUE
            WHEN 'FAILURE' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_circulation
        CHECK (CASE circulation_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'STABLE' THEN TRUE
            WHEN 'COMPROMISED' THEN TRUE
            WHEN 'SHOCK' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_disability
        CHECK (CASE disability_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'ALERT' THEN TRUE
            WHEN 'RESPONDS_TO_VOICE' THEN TRUE
            WHEN 'RESPONDS_TO_PAIN' THEN TRUE
            WHEN 'UNRESPONSIVE' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_exposure
        CHECK (CASE exposure_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'NO_CRITICAL_FINDING' THEN TRUE
            WHEN 'TRAUMA' THEN TRUE
            WHEN 'HYPOTHERMIA' THEN TRUE
            WHEN 'HYPERTHERMIA' THEN TRUE
            WHEN 'OTHER' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_orientation
        CHECK (recommended_orientation IS NULL OR CASE recommended_orientation
            WHEN 'RESUSCITATION' THEN TRUE
            WHEN 'OPERATING_ROOM' THEN TRUE
            WHEN 'HOSPITALIZATION' THEN TRUE
            WHEN 'CONSULTATION' THEN TRUE
            WHEN 'TRANSFER' THEN TRUE
            WHEN 'DISCHARGE' THEN TRUE
            WHEN 'DEATH' THEN TRUE
            ELSE FALSE
        END),
    CONSTRAINT chk_emergency_triage_respiratory_rate
        CHECK (respiratory_rate IS NULL OR respiratory_rate BETWEEN 0 AND 100),
    CONSTRAINT chk_emergency_triage_oxygen_saturation
        CHECK (oxygen_saturation IS NULL OR oxygen_saturation BETWEEN 0 AND 100),
    CONSTRAINT chk_emergency_triage_gcs
        CHECK (gcs_score IS NULL OR gcs_score BETWEEN 3 AND 15),
    CONSTRAINT chk_emergency_triage_pain
        CHECK (pain_score IS NULL OR pain_score BETWEEN 0 AND 10)
);

CREATE INDEX idx_emergency_triage_assessment_history
    ON emergency_triage_assessments (organization_id, emergency_id, sequence_number);

INSERT INTO emergency_triage_assessments (
    id,
    organization_id,
    emergency_id,
    assessment_type,
    sequence_number,
    triage_level,
    hemodynamic_status,
    airway_status,
    breathing_status,
    circulation_status,
    disability_status,
    exposure_status,
    bp_systolic,
    bp_diastolic,
    heart_rate,
    temperature,
    assessed_at,
    assessed_by_user_id,
    created_at
)
SELECT
    e.id,
    e.organization_id,
    e.id,
    'INITIAL',
    1,
    e.triage_level,
    e.hemodynamic_status,
    'NOT_ASSESSED',
    'NOT_ASSESSED',
    'NOT_ASSESSED',
    'NOT_ASSESSED',
    'NOT_ASSESSED',
    e.initial_bp_systolic,
    e.initial_bp_diastolic,
    e.initial_hr,
    e.initial_temp,
    e.created_at,
    CASE
        WHEN EXISTS (SELECT 1 FROM users u WHERE u.id = e.created_by_user_id)
            THEN e.created_by_user_id
        ELSE NULL
    END,
    e.created_at
FROM emergencies e
WHERE NOT EXISTS (
    SELECT 1
    FROM emergency_triage_assessments a
    WHERE a.emergency_id = e.id
);

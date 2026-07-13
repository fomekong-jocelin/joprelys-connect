ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_assessment_type;
ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_airway;
ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_breathing;
ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_circulation;
ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_disability;
ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_exposure;
ALTER TABLE emergency_triage_assessments
    DROP CONSTRAINT chk_emergency_triage_orientation;

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_assessment_type
        CHECK (CASE assessment_type
            WHEN 'INITIAL' THEN TRUE
            WHEN 'REASSESSMENT' THEN TRUE
            ELSE FALSE
        END);

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_airway
        CHECK (CASE airway_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'PATENT' THEN TRUE
            WHEN 'AT_RISK' THEN TRUE
            WHEN 'OBSTRUCTED' THEN TRUE
            ELSE FALSE
        END);

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_breathing
        CHECK (CASE breathing_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'ADEQUATE' THEN TRUE
            WHEN 'DISTRESS' THEN TRUE
            WHEN 'FAILURE' THEN TRUE
            ELSE FALSE
        END);

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_circulation
        CHECK (CASE circulation_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'STABLE' THEN TRUE
            WHEN 'COMPROMISED' THEN TRUE
            WHEN 'SHOCK' THEN TRUE
            ELSE FALSE
        END);

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_disability
        CHECK (CASE disability_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'ALERT' THEN TRUE
            WHEN 'RESPONDS_TO_VOICE' THEN TRUE
            WHEN 'RESPONDS_TO_PAIN' THEN TRUE
            WHEN 'UNRESPONSIVE' THEN TRUE
            ELSE FALSE
        END);

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_exposure
        CHECK (CASE exposure_status
            WHEN 'NOT_ASSESSED' THEN TRUE
            WHEN 'NO_CRITICAL_FINDING' THEN TRUE
            WHEN 'TRAUMA' THEN TRUE
            WHEN 'HYPOTHERMIA' THEN TRUE
            WHEN 'HYPERTHERMIA' THEN TRUE
            WHEN 'OTHER' THEN TRUE
            ELSE FALSE
        END);

ALTER TABLE emergency_triage_assessments
    ADD CONSTRAINT chk_emergency_triage_orientation
        CHECK (recommended_orientation IS NULL OR CASE recommended_orientation
            WHEN 'RESUSCITATION' THEN TRUE
            WHEN 'OPERATING_ROOM' THEN TRUE
            WHEN 'HOSPITALIZATION' THEN TRUE
            WHEN 'CONSULTATION' THEN TRUE
            WHEN 'TRANSFER' THEN TRUE
            WHEN 'DISCHARGE' THEN TRUE
            WHEN 'DEATH' THEN TRUE
            ELSE FALSE
        END);

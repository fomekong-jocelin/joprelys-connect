CREATE TABLE operating_reports (
    id UUID PRIMARY KEY,
    hospitalization_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    surgeon_id UUID,
    anesthetist_id UUID,
    operation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    pre_operative_diagnosis TEXT,
    post_operative_diagnosis TEXT,
    procedure_name VARCHAR(255) NOT NULL,
    procedure_description TEXT,
    anesthesia_type VARCHAR(100),
    anesthesia_description TEXT,
    k_surgeon_value DOUBLE PRECISION DEFAULT 0.0,
    k_anesthesist_value DOUBLE PRECISION DEFAULT 0.0,
    k_bloc_value DOUBLE PRECISION DEFAULT 0.0,
    validated BOOLEAN DEFAULT FALSE NOT NULL,
    validated_by VARCHAR(255),
    validated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_operating_reports_hosp ON operating_reports(hospitalization_id);
CREATE INDEX idx_operating_reports_org ON operating_reports(organization_id);

CREATE TABLE surgical_implants (
    id UUID PRIMARY KEY,
    operating_report_id UUID NOT NULL,
    hospitalization_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    implant_name VARCHAR(255) NOT NULL,
    lot_number VARCHAR(100),
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    manufacturer VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_implants_report FOREIGN KEY(operating_report_id) REFERENCES operating_reports(id) ON DELETE CASCADE
);

CREATE INDEX idx_surgical_implants_report ON surgical_implants(operating_report_id);
CREATE INDEX idx_surgical_implants_hosp ON surgical_implants(hospitalization_id);
CREATE INDEX idx_surgical_implants_org ON surgical_implants(organization_id);

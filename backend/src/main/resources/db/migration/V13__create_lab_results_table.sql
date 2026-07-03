CREATE TABLE lab_results (
    id UUID PRIMARY KEY,
    result_number VARCHAR(50) UNIQUE NOT NULL,
    lab_order_id UUID NOT NULL REFERENCES lab_orders(id) ON DELETE CASCADE,
    patient_id UUID NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id),
    validator_name VARCHAR(150) NOT NULL,
    analyte_name VARCHAR(100) NOT NULL,
    result_value VARCHAR(50) NOT NULL,
    unit VARCHAR(20),
    reference_range VARCHAR(50),
    interpretation VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    comment VARCHAR(255),
    pdf_file_path VARCHAR(500),
    sample_collected_at TIMESTAMP WITH TIME ZONE,
    result_at TIMESTAMP WITH TIME ZONE,
    validated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

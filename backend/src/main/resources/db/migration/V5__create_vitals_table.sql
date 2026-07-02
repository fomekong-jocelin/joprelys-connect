CREATE TABLE vitals (
    id UUID PRIMARY KEY,
    visit_id UUID NOT NULL UNIQUE REFERENCES visits(id) ON DELETE CASCADE,
    temperature DECIMAL(3,1),
    weight DECIMAL(4,1),
    height INTEGER,
    pulse INTEGER,
    systolic INTEGER,
    diastolic INTEGER,
    spo2 INTEGER,
    glycemia DECIMAL(3,2),
    respiratory_rate INTEGER,
    bmi DECIMAL(4,2),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_vitals_visit_id ON vitals (visit_id);

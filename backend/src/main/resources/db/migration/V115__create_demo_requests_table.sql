-- Flyway Migration V115: Create demo_requests table for landing page leads capture
CREATE TABLE demo_requests (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    role VARCHAR(100),
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    city VARCHAR(100),
    message TEXT,
    source VARCHAR(50) DEFAULT 'landing-page',
    locale VARCHAR(10) DEFAULT 'fr',
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    ip_address VARCHAR(100),
    user_agent TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_demo_requests_created_at ON demo_requests(created_at);
CREATE INDEX idx_demo_requests_status ON demo_requests(status);

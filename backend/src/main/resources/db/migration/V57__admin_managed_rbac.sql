CREATE TABLE roles (
    id UUID PRIMARY KEY,
    organization_id UUID,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    assignable BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_roles_org_code ON roles (organization_id, code);
CREATE INDEX idx_roles_system_code ON roles (system_role, code);

CREATE TABLE permissions (
    code VARCHAR(96) PRIMARY KEY,
    domain VARCHAR(64) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL,
    permission_code VARCHAR(96) NOT NULL,
    PRIMARY KEY (role_id, permission_code),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_code) REFERENCES permissions(code) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    organization_id UUID,
    assigned_by UUID,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_org_user ON user_roles (organization_id, user_id);
CREATE INDEX idx_user_roles_role ON user_roles (role_id);

CREATE TABLE rbac_audit_log (
    id UUID PRIMARY KEY,
    organization_id UUID,
    actor_user_id UUID NOT NULL,
    action VARCHAR(80) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(120) NOT NULL,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_rbac_audit_org_created ON rbac_audit_log (organization_id, created_at);
CREATE INDEX idx_rbac_audit_actor ON rbac_audit_log (actor_user_id);

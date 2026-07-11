export interface RbacPermission {
  readonly code: string;
  readonly domain: string;
  readonly name: string;
  readonly description?: string;
}

export interface RbacRole {
  readonly id: string;
  readonly organizationId?: string;
  readonly code: string;
  readonly name: string;
  readonly description?: string;
  readonly systemRole: boolean;
  readonly assignable: boolean;
  readonly enabled: boolean;
  readonly permissions: string[];
}

export interface EffectiveAccess {
  readonly userId: string;
  readonly roles: string[];
  readonly permissions: string[];
}

export interface RbacUserAccess {
  readonly id: string;
  readonly email: string;
  readonly displayName: string;
  readonly enabled: boolean;
  readonly currentUser: boolean;
  readonly roles: string[];
  readonly permissions: string[];
}

export interface RbacAuditEntry {
  readonly id: string;
  readonly actorUserId: string;
  readonly action: string;
  readonly targetType: string;
  readonly targetId: string;
  readonly details?: string;
  readonly createdAt: string;
}

export interface CreateRbacRoleRequest {
  readonly code: string;
  readonly name: string;
  readonly description?: string;
  readonly assignable: boolean;
  readonly permissionCodes: string[];
}

export interface UpdateRbacRoleRequest {
  readonly code: string;
  readonly name: string;
  readonly description?: string;
  readonly assignable: boolean;
  readonly enabled: boolean;
}

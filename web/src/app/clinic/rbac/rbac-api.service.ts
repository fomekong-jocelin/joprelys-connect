import { HttpClient, HttpParams } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { finalize, Observable, of, shareReplay, tap } from 'rxjs';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import {
  CreateRbacRoleRequest,
  EffectiveAccess,
  RbacAuditEntry,
  RbacPermission,
  RbacRole,
  RbacUserAccess,
  UpdateRbacRoleRequest,
} from './rbac.models';

@Injectable({ providedIn: 'root' })
export class RbacApiService {
  private readonly http = inject(HttpClient);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly cachedAccess = signal<EffectiveAccess | null>(null);
  private readonly accessOwnerSessionKey = signal<string | null>(null);
  private accessRequest$: Observable<EffectiveAccess> | null = null;
  private accessRequestOwnerSessionKey: string | null = null;
  private accessRequestGeneration = 0;

  /**
   * Effective permissions belong to the authenticated identity, not to one short-lived
   * access-token string. A transparent JWT refresh for the same user must therefore not
   * make the navigation collapse to Dashboard while /api/rbac/me is being revalidated.
   *
   * A real identity/role boundary still invalidates access immediately because
   * AuthTokenStorageService purges registered feature state when email or role changes.
   */
  readonly access = computed<EffectiveAccess | null>(() => {
    const currentSessionKey = this.currentSessionKey();
    return currentSessionKey !== null && this.accessOwnerSessionKey() === currentSessionKey
      ? this.cachedAccess()
      : null;
  });

  constructor() {
    this.tokenStorage.registerSessionBoundaryCleanup(() => this.clearAccess());
  }

  ensureMyAccess(force = false): Observable<EffectiveAccess> {
    const ownerSessionKey = this.currentSessionKey();
    const currentAccess = this.access();
    if (!force && currentAccess) {
      return of(currentAccess);
    }
    if (!force && this.accessRequest$ && this.accessRequestOwnerSessionKey === ownerSessionKey) {
      return this.accessRequest$;
    }

    const requestGeneration = ++this.accessRequestGeneration;
    const request$ = this.http.get<EffectiveAccess>('/api/rbac/me').pipe(
      tap((access) => {
        if (ownerSessionKey === null || this.currentSessionKey() !== ownerSessionKey) {
          throw new Error('RBAC response no longer belongs to the active session.');
        }
        if (this.accessRequestGeneration === requestGeneration) {
          this.cachedAccess.set(access);
          this.accessOwnerSessionKey.set(ownerSessionKey);
        }
      }),
      finalize(() => {
        if (this.accessRequestGeneration === requestGeneration) {
          this.accessRequest$ = null;
          this.accessRequestOwnerSessionKey = null;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.accessRequest$ = request$;
    this.accessRequestOwnerSessionKey = ownerSessionKey;
    return request$;
  }

  hasPermission(permission: string): boolean {
    return new Set(this.access()?.permissions ?? []).has(permission);
  }

  effectivePermissionSet(permissions: readonly string[]): Set<string> {
    return new Set(permissions);
  }

  clearAccess(): void {
    this.cachedAccess.set(null);
    this.accessOwnerSessionKey.set(null);
    this.accessRequest$ = null;
    this.accessRequestOwnerSessionKey = null;
    this.accessRequestGeneration++;
  }

  listRoles(organizationId?: string): Observable<RbacRole[]> {
    return this.http.get<RbacRole[]>('/api/rbac/roles', { params: this.scopeParams(organizationId) });
  }

  listPermissions(): Observable<RbacPermission[]> {
    return this.http.get<RbacPermission[]>('/api/rbac/permissions');
  }

  listUsers(organizationId?: string): Observable<RbacUserAccess[]> {
    return this.http.get<RbacUserAccess[]>('/api/rbac/users', { params: this.scopeParams(organizationId) });
  }

  createRole(request: CreateRbacRoleRequest, organizationId?: string): Observable<RbacRole> {
    return this.http.post<RbacRole>('/api/rbac/roles', request, { params: this.scopeParams(organizationId) });
  }

  updateRole(roleId: string, request: UpdateRbacRoleRequest, organizationId?: string): Observable<RbacRole> {
    return this.http.put<RbacRole>(`/api/rbac/roles/${roleId}`, request, {
      params: this.scopeParams(organizationId),
    });
  }

  replaceRolePermissions(roleId: string, permissionCodes: string[], organizationId?: string): Observable<RbacRole> {
    return this.http.put<RbacRole>(`/api/rbac/roles/${roleId}/permissions`, { permissionCodes }, {
      params: this.scopeParams(organizationId),
    });
  }

  replaceUserRoles(userId: string, roleIds: string[], organizationId?: string): Observable<RbacUserAccess> {
    return this.http.put<RbacUserAccess>(`/api/rbac/users/${userId}/roles`, { roleIds }, {
      params: this.scopeParams(organizationId),
    });
  }

  listAudit(organizationId?: string): Observable<RbacAuditEntry[]> {
    return this.http.get<RbacAuditEntry[]>('/api/rbac/audit', { params: this.scopeParams(organizationId) });
  }

  private scopeParams(organizationId?: string): HttpParams {
    return organizationId ? new HttpParams().set('organizationId', organizationId) : new HttpParams();
  }

  private currentSessionKey(): string | null {
    const session = this.tokenStorage.session();
    if (!session) return null;
    const email = session.email.trim().toLowerCase();
    const roles = session.role
      .split(',')
      .map(role => role.trim().toUpperCase())
      .filter(Boolean)
      .sort()
      .join(',');
    return `${email}|${roles}`;
  }
}

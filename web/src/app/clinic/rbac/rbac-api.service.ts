import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { finalize, Observable, of, shareReplay, tap } from 'rxjs';
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
  private accessRequest$: Observable<EffectiveAccess> | null = null;

  readonly access = signal<EffectiveAccess | null>(null);

  ensureMyAccess(force = false): Observable<EffectiveAccess> {
    if (!force && this.access()) {
      return of(this.access()!);
    }
    if (!force && this.accessRequest$) {
      return this.accessRequest$;
    }
    this.accessRequest$ = this.http.get<EffectiveAccess>('/api/rbac/me').pipe(
      tap((access) => this.access.set(access)),
      finalize(() => this.accessRequest$ = null),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    return this.accessRequest$;
  }

  hasPermission(permission: string): boolean {
    const permissions = this.effectivePermissionSet(this.access()?.permissions ?? []);
    return permissions.has(permission);
  }

  effectivePermissionSet(permissionCodes: readonly string[]): Set<string> {
    const permissions = new Set(permissionCodes);
    if (permissions.has('LAB_QUEUE_READ')) {
      permissions.add('LAB_ORDER_READ');
    }
    return permissions;
  }

  clearAccess(): void {
    this.access.set(null);
    this.accessRequest$ = null;
  }

  listRoles(): Observable<RbacRole[]> {
    return this.http.get<RbacRole[]>('/api/rbac/roles');
  }

  listPermissions(): Observable<RbacPermission[]> {
    return this.http.get<RbacPermission[]>('/api/rbac/permissions');
  }

  listUsers(): Observable<RbacUserAccess[]> {
    return this.http.get<RbacUserAccess[]>('/api/rbac/users');
  }

  createRole(request: CreateRbacRoleRequest): Observable<RbacRole> {
    return this.http.post<RbacRole>('/api/rbac/roles', request);
  }

  updateRole(roleId: string, request: UpdateRbacRoleRequest): Observable<RbacRole> {
    return this.http.put<RbacRole>(`/api/rbac/roles/${roleId}`, request);
  }

  replaceRolePermissions(roleId: string, permissionCodes: string[]): Observable<RbacRole> {
    return this.http.put<RbacRole>(`/api/rbac/roles/${roleId}/permissions`, { permissionCodes });
  }

  replaceUserRoles(userId: string, roleIds: string[]): Observable<RbacUserAccess> {
    return this.http.put<RbacUserAccess>(`/api/rbac/users/${userId}/roles`, { roleIds });
  }

  listAudit(): Observable<RbacAuditEntry[]> {
    return this.http.get<RbacAuditEntry[]>('/api/rbac/audit');
  }
}

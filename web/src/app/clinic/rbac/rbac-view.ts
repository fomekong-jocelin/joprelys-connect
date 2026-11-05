import { RbacPermission, RbacRole, RbacUserAccess } from './rbac.models';

export interface PermissionGroup {
  readonly domain: string;
  readonly items: readonly RbacPermission[];
}

/** Regroupe les autorisations par domaine, dans l'ordre du catalogue serveur. */
export function groupPermissions(permissions: readonly RbacPermission[]): PermissionGroup[] {
  const groups = new Map<string, RbacPermission[]>();
  for (const permission of permissions) {
    const items = groups.get(permission.domain) ?? [];
    items.push(permission);
    groups.set(permission.domain, items);
  }
  return [...groups.entries()].map(([domain, items]) => ({ domain, items }));
}

/**
 * Autorisations effectives d'une personne, avec leur nom lisible. Un code absent du catalogue reste affiché
 * (domaine `OTHER`) plutôt que d'être masqué : l'écran ne doit pas sous-estimer les droits réels.
 */
export function effectivePermissionGroups(codes: readonly string[], catalog: readonly RbacPermission[]): PermissionGroup[] {
  const byCode = new Map(catalog.map((permission) => [permission.code, permission]));
  const known = catalog.filter((permission) => codes.includes(permission.code));
  const unknown = codes
    .filter((code) => !byCode.has(code))
    .map((code): RbacPermission => ({ code, domain: 'OTHER', name: code }));
  return groupPermissions([...known, ...unknown]);
}

export function roleIdsOf(user: RbacUserAccess | null, roles: readonly RbacRole[]): string[] {
  if (!user) return [];
  return roles.filter((role) => user.roles.includes(role.code)).map((role) => role.id);
}

export function roleNamesOf(user: RbacUserAccess, roles: readonly RbacRole[]): string[] {
  return user.roles.map((code) => roles.find((role) => role.code === code)?.name ?? code);
}

export function sameMembers(left: readonly string[], right: readonly string[]): boolean {
  return left.length === right.length && left.every((item) => right.includes(item));
}

/** Recherche sans tenir compte des accents ni de la casse, sur le nom et l'e-mail. */
export function matchesUser(user: RbacUserAccess, query: string): boolean {
  const needle = fold(query.trim());
  return !needle || fold(user.displayName).includes(needle) || fold(user.email).includes(needle);
}

function fold(value: string): string {
  return value.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();
}

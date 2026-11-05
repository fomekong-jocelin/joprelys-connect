import { RBAC_PERMISSIONS, RBAC_ROLES, RBAC_USERS } from '../../../testing/rbac-fixtures';
import { effectivePermissionGroups, groupPermissions, matchesUser, roleIdsOf, roleNamesOf, sameMembers } from './rbac-view';

describe('rbac-view', () => {
  it('groups permissions by domain in catalog order', () => {
    const groups = groupPermissions(RBAC_PERMISSIONS);

    expect(groups.map((group) => group.domain)).toEqual(['CAISSE', 'ADMINISTRATION']);
    expect(groups[0].items.map((item) => item.name)).toEqual(['Consulter la file', 'Encaisser']);
  });

  it('names effective permissions and keeps unknown codes visible', () => {
    const groups = effectivePermissionGroups(['CASH_PAYMENT_COLLECT', 'LEGACY_RIGHT'], RBAC_PERMISSIONS);

    expect(groups).toEqual([
      { domain: 'CAISSE', items: [RBAC_PERMISSIONS[1]] },
      { domain: 'OTHER', items: [{ code: 'LEGACY_RIGHT', domain: 'OTHER', name: 'LEGACY_RIGHT' }] },
    ]);
  });

  it('maps role codes of a person to ids and readable names', () => {
    expect(roleIdsOf(RBAC_USERS[1], RBAC_ROLES)).toEqual(['role-cashier']);
    expect(roleIdsOf(null, RBAC_ROLES)).toEqual([]);
    expect(roleNamesOf({ ...RBAC_USERS[1], roles: ['CAISSIER', 'UNKNOWN'] }, RBAC_ROLES)).toEqual(['Caissier', 'UNKNOWN']);
  });

  it('searches people by name or email, ignoring accents and case', () => {
    expect(matchesUser(RBAC_USERS[1], 'helene')).toBe(true);
    expect(matchesUser(RBAC_USERS[1], 'CAISSE@')).toBe(true);
    expect(matchesUser(RBAC_USERS[1], 'admin')).toBe(false);
    expect(matchesUser(RBAC_USERS[1], '   ')).toBe(true);
  });

  it('compares role selections regardless of order', () => {
    expect(sameMembers(['a', 'b'], ['b', 'a'])).toBe(true);
    expect(sameMembers(['a'], ['a', 'b'])).toBe(false);
  });
});

import { buildUnitTree, canBeParent, defaultChildType, unitDisplayName } from './hospital-organization-hierarchy';
import { OrganizationalUnit, OrganizationalUnitType } from './hospital-organization.models';
import { isValidUnitCode, normalizeUnitCode, suggestUnitCode } from './org-unit-code';

const unit = (id: string, unitType: OrganizationalUnitType, parentId: string | null, name: string): OrganizationalUnit => ({
  id, parentId, code: id.toUpperCase(), name, unitType, serviceCatalogCode: null, active: true,
});

describe('hospital organization hierarchy', () => {
  it('follows the RF-06 parent rules', () => {
    expect(canBeParent('POLE', 'SERVICE')).toBe(true);
    expect(canBeParent('DEPARTMENT', 'POLE')).toBe(false);
    expect(canBeParent('SERVICE', 'CARE_UNIT')).toBe(true);
    expect(canBeParent('CARE_UNIT', 'CARE_UNIT')).toBe(false);
    expect(defaultChildType('POLE')).toBe('SERVICE');
    expect(defaultChildType('CARE_UNIT')).toBeNull();
  });

  it('nests children under their parent and sorts each level by label', () => {
    const tree = buildUnitTree([
      unit('svc-b', 'SERVICE', 'pole', 'Pédiatrie'),
      unit('pole', 'POLE', null, 'Mère-enfant'),
      unit('svc-a', 'SERVICE', 'pole', 'Maternité'),
      unit('care', 'CARE_UNIT', 'svc-b', 'Néonatologie'),
    ], (value) => value.name ?? value.code);

    expect(tree.map((node) => node.label)).toEqual(['Mère-enfant']);
    expect(tree[0].children.map((node) => node.label)).toEqual(['Maternité', 'Pédiatrie']);
    expect(tree[0].children[1].children[0].label).toBe('Néonatologie');
    expect(tree[0].addType).toBe('SERVICE');
  });

  it('lifts a unit whose parent is not loaded to the root instead of hiding it', () => {
    const tree = buildUnitTree([unit('svc', 'SERVICE', 'missing-pole', 'Urgences')], (value) => value.name ?? '');

    expect(tree.map((node) => node.label)).toEqual(['Urgences']);
  });

  it('prefers the local name, then the catalog label in the active locale', () => {
    const catalog = [{ code: 'PEDIATRICS', nameFr: 'Pédiatrie', nameEn: 'Pediatrics' }];
    const service = { ...unit('svc', 'SERVICE', null, ''), name: null, serviceCatalogCode: 'PEDIATRICS' };

    expect(unitDisplayName(service, catalog, 'en')).toBe('Pediatrics');
    expect(unitDisplayName({ ...service, name: 'Pédiatrie B' }, catalog, 'en')).toBe('Pédiatrie B');
  });
});

describe('organizational unit code', () => {
  it('mirrors the server normalization and pattern', () => {
    expect(normalizeUnitCode(' bloc-op central ')).toBe('BLOC_OP_CENTRAL');
    expect(isValidUnitCode('svc_1')).toBe(true);
    expect(isValidUnitCode('1SVC')).toBe(false);
    expect(isValidUnitCode('AB')).toBe(false);
  });

  it('suggests a readable, unique and valid code', () => {
    expect(suggestUnitCode('Médecine générale', new Set())).toBe('MEDECINE_GENERALE');
    expect(suggestUnitCode('Médecine générale', new Set(['MEDECINE_GENERALE']))).toBe('MEDECINE_GENERALE_2');
    expect(suggestUnitCode('3e étage', new Set())).toBe('U_3E_ETAGE');
    expect(suggestUnitCode('A', new Set())).toBe('A_UNIT');
    expect(suggestUnitCode('   ', new Set())).toBe('');
    expect(isValidUnitCode(suggestUnitCode('x'.repeat(200), new Set()))).toBe(true);
  });
});

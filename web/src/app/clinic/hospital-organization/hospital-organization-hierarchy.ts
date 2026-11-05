import {
  HospitalServiceCatalogEntry,
  OrganizationalUnit,
  OrganizationalUnitType,
} from './hospital-organization.models';

/** Niveaux du plus large au plus fin (RF-02). */
export const UNIT_LEVELS: readonly OrganizationalUnitType[] = ['POLE', 'DEPARTMENT', 'SERVICE', 'CARE_UNIT'];

/** Rattachements autorisés (RF-06). Le serveur reste l'autorité ; l'écran ne fait que filtrer. */
const ALLOWED_CHILDREN: Readonly<Record<OrganizationalUnitType, readonly OrganizationalUnitType[]>> = {
  POLE: ['DEPARTMENT', 'SERVICE'],
  DEPARTMENT: ['SERVICE'],
  SERVICE: ['CARE_UNIT'],
  CARE_UNIT: [],
};

/** Ce que l'action « Ajouter » d'une ligne crée par défaut : le cas le plus courant. */
const DEFAULT_CHILD: Readonly<Record<OrganizationalUnitType, OrganizationalUnitType | null>> = {
  POLE: 'SERVICE',
  DEPARTMENT: 'SERVICE',
  SERVICE: 'CARE_UNIT',
  CARE_UNIT: null,
};

export interface UnitNode {
  readonly unit: OrganizationalUnit;
  readonly label: string;
  readonly addType: OrganizationalUnitType | null;
  readonly children: readonly UnitNode[];
}

export function allowedChildTypes(parent: OrganizationalUnitType): readonly OrganizationalUnitType[] {
  return ALLOWED_CHILDREN[parent];
}

export function canBeParent(parent: OrganizationalUnitType, child: OrganizationalUnitType): boolean {
  return ALLOWED_CHILDREN[parent].includes(child);
}

export function requiresParent(type: OrganizationalUnitType): boolean {
  return type === 'CARE_UNIT';
}

export function defaultChildType(parent: OrganizationalUnitType): OrganizationalUnitType | null {
  return DEFAULT_CHILD[parent];
}

/** Intitulé local s'il existe, sinon libellé catalogue dans la langue active (RF-04). */
export function unitDisplayName(
  unit: OrganizationalUnit,
  catalog: readonly HospitalServiceCatalogEntry[],
  locale: string,
): string {
  if (unit.name?.trim()) return unit.name;
  if (unit.unitType !== 'SERVICE') return unit.code;
  const entry = catalog.find((candidate) => candidate.code === unit.serviceCatalogCode);
  if (!entry) return unit.serviceCatalogCode ?? unit.code;
  return locale === 'en' ? entry.nameEn : entry.nameFr;
}

/**
 * Construit l'arbre affiché, trié par libellé à chaque niveau.
 * Une unité dont le parent n'est pas chargé (filtre inactif) remonte à la racine plutôt que de disparaître.
 */
export function buildUnitTree(
  units: readonly OrganizationalUnit[],
  labelOf: (unit: OrganizationalUnit) => string,
): UnitNode[] {
  const loadedIds = new Set(units.map((unit) => unit.id));
  const childrenByParent = new Map<string | null, OrganizationalUnit[]>();
  for (const unit of units) {
    const key = unit.parentId && loadedIds.has(unit.parentId) ? unit.parentId : null;
    childrenByParent.set(key, [...(childrenByParent.get(key) ?? []), unit]);
  }

  const visited = new Set<string>();
  const build = (parentId: string | null): UnitNode[] => {
    const siblings = (childrenByParent.get(parentId) ?? []).filter((unit) => !visited.has(unit.id));
    siblings.forEach((unit) => visited.add(unit.id));
    return siblings
      .map((unit) => ({ unit, label: labelOf(unit) }))
      .sort((left, right) => left.label.localeCompare(right.label))
      .map(({ unit, label }) => ({
        unit,
        label,
        addType: defaultChildType(unit.unitType),
        children: build(unit.id),
      }));
  };

  return build(null);
}

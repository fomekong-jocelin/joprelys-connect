import { FacilityLocationNode, FacilityLocationNodeType, FacilitySpace, UnitSpaceAssignment } from '../spatial-configuration.models';

export const LOCATION_TYPES: readonly FacilityLocationNodeType[] = ['SITE', 'BUILDING', 'FLOOR', 'ZONE'];

/** Codes des lieux et salles : même règle que `HospitalLocationConfigurationService.CODE_PATTERN`. */
const PLACE_CODE_PATTERN = /^[A-Z][A-Z0-9_]{1,63}$/;

export function isValidPlaceCode(code: string): boolean {
  return PLACE_CODE_PATTERN.test(code);
}

export interface PlaceNode {
  readonly location: FacilityLocationNode;
  readonly children: readonly PlaceNode[];
  readonly spaces: readonly FacilitySpace[];
}

export interface PlaceTree {
  /** Salles rattachées directement à l'établissement, sans lieu. */
  readonly rootSpaces: readonly FacilitySpace[];
  readonly nodes: readonly PlaceNode[];
}

const byName = <T extends { readonly name: string }>(left: T, right: T) => left.name.localeCompare(right.name, undefined, { numeric: true });

/**
 * Range les salles sous leur lieu et les lieux sous leur parent. Un lieu dont le parent n'est pas chargé
 * remonte à la racine plutôt que de disparaître ; une boucle éventuelle est coupée.
 */
export function buildPlaceTree(
  locations: readonly FacilityLocationNode[],
  spaces: readonly FacilitySpace[],
  showInactive: boolean,
): PlaceTree {
  const visibleLocations = locations.filter((location) => showInactive || location.active);
  const visibleSpaces = spaces.filter((space) => showInactive || space.active);
  const known = new Set(visibleLocations.map((location) => location.id));
  const visited = new Set<string>();

  const build = (location: FacilityLocationNode): PlaceNode => {
    visited.add(location.id);
    return {
      location,
      children: visibleLocations
        .filter((child) => child.parentId === location.id && !visited.has(child.id))
        .sort(byName)
        .map(build),
      spaces: visibleSpaces.filter((space) => space.locationNodeId === location.id).sort(byName),
    };
  };

  const nodes = visibleLocations
    .filter((location) => !location.parentId || !known.has(location.parentId))
    .sort(byName)
    .map(build);
  return {
    rootSpaces: visibleSpaces.filter((space) => !space.locationNodeId || !known.has(space.locationNodeId)).sort(byName),
    nodes,
  };
}

/** Rattachements en cours ou à venir d'une salle ; les rattachements terminés relèvent de l'historique. */
export function currentAssignments(
  assignments: readonly UnitSpaceAssignment[],
  spaceId: string,
  now: Date = new Date(),
): UnitSpaceAssignment[] {
  return assignments.filter((assignment) =>
    assignment.spaceId === spaceId && (!assignment.validTo || new Date(assignment.validTo) > now));
}

export function inactiveCount(locations: readonly FacilityLocationNode[], spaces: readonly FacilitySpace[]): number {
  return locations.filter((location) => !location.active).length + spaces.filter((space) => !space.active).length;
}

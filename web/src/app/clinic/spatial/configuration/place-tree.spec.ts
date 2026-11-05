import { FacilityLocationNode, FacilitySpace, UnitSpaceAssignment } from '../spatial-configuration.models';
import { buildPlaceTree, currentAssignments, inactiveCount, isValidPlaceCode } from './place-tree';

const location = (id: string, name: string, parentId: string | null = null, active = true): FacilityLocationNode => ({
  id, parentId, code: id.toUpperCase(), name, nodeType: 'BUILDING', active,
});
const space = (id: string, name: string, locationNodeId: string | null, active = true): FacilitySpace => ({
  id, locationNodeId, code: id.toUpperCase(), name, spaceTypeCode: 'HOSPITAL_ROOM', inpatientProfile: true, active,
});

describe('buildPlaceTree', () => {
  const locations = [location('b', 'Bâtiment B'), location('a', 'Bâtiment A'), location('f1', 'Étage 1', 'a')];
  const spaces = [
    space('r10', 'Chambre 10', 'f1'),
    space('r9', 'Chambre 9', 'f1'),
    space('lobby', 'Accueil', null),
    space('lost', 'Salle orpheline', 'unknown'),
  ];

  it('nests floors under buildings and rooms under their place, in natural order', () => {
    const tree = buildPlaceTree(locations, spaces, false);

    expect(tree.nodes.map((node) => node.location.name)).toEqual(['Bâtiment A', 'Bâtiment B']);
    const floor = tree.nodes[0].children[0];
    expect(floor.location.name).toBe('Étage 1');
    expect(floor.spaces.map((room) => room.name)).toEqual(['Chambre 9', 'Chambre 10']);
  });

  it('keeps rooms without a known place at the root instead of hiding them', () => {
    const tree = buildPlaceTree(locations, spaces, false);

    expect(tree.rootSpaces.map((room) => room.name)).toEqual(['Accueil', 'Salle orpheline']);
  });

  it('hides deactivated items unless asked', () => {
    const withInactive = [...locations, location('old', 'Ancien bâtiment', null, false)];

    expect(buildPlaceTree(withInactive, [], false).nodes).toHaveLength(2);
    expect(buildPlaceTree(withInactive, [], true).nodes).toHaveLength(3);
    expect(inactiveCount(withInactive, [space('x', 'X', null, false)])).toBe(2);
  });
});

describe('currentAssignments', () => {
  const assignment = (id: string, validTo: string | null): UnitSpaceAssignment => ({
    id, organizationalUnitId: 'unit-1', spaceId: 'room-1', validFrom: '2026-01-01T00:00:00Z', validTo,
  });

  it('keeps ongoing and future links, and leaves ended ones to history', () => {
    const now = new Date('2026-11-05T10:00:00Z');
    const result = currentAssignments(
      [assignment('open', null), assignment('ended', '2026-06-01T00:00:00Z'), assignment('later', '2027-01-01T00:00:00Z')],
      'room-1',
      now,
    );

    expect(result.map((item) => item.id)).toEqual(['open', 'later']);
  });
});

describe('isValidPlaceCode', () => {
  it('follows the server rule', () => {
    expect(isValidPlaceCode('A1')).toBe(true);
    expect(isValidPlaceCode('CHAMBRE_201')).toBe(true);
    expect(isValidPlaceCode('A')).toBe(false);
    expect(isValidPlaceCode('1A')).toBe(false);
  });
});

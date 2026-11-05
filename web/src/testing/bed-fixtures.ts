import { BedConfiguration, SpaceOccupancyView } from '../app/clinic/spatial/spatial-configuration.models';

/** Lit fictif ouvert, prêt et libre ; les tests ne surchargent que ce qui les intéresse. */
export function bedFixture(overrides: Partial<BedConfiguration> = {}): BedConfiguration {
  return {
    id: 'bed-1',
    spaceId: 'space-1',
    bedNumber: 'Lit 1',
    status: 'FREE',
    capacityStatus: 'OPEN',
    readinessStatus: 'READY',
    usageStatus: 'UNASSIGNED',
    available: true,
    version: 0,
    ...overrides,
  };
}

export function roomFixture(overrides: Partial<SpaceOccupancyView> = {}): SpaceOccupancyView {
  return {
    spaceId: 'space-1',
    spaceCode: 'ROOM_1',
    spaceName: 'Chambre 1',
    installedBeds: 0,
    occupiedBeds: 0,
    availableBeds: 0,
    openBeds: 0,
    readyBeds: 0,
    beds: [],
    ...overrides,
  };
}

import { describe, expect, it } from 'vitest';
import { BedConfiguration } from '../clinic/spatial/spatial-configuration.models';
import { isAdmissibleBed } from './bed-placement-policies';

describe('bed placement policies', () => {
  const bed = (overrides: Partial<BedConfiguration> = {}): BedConfiguration => ({
    id: 'bed-1',
    spaceId: 'space-1',
    bedNumber: '101-A',
    status: 'FREE',
    capacityStatus: 'OPEN',
    readinessStatus: 'READY',
    usageStatus: 'UNASSIGNED',
    available: true,
    version: 0,
    ...overrides,
  });

  it('accepts a bed that is available, open and ready', () => {
    expect(isAdmissibleBed(bed())).toBe(true);
  });

  it('rejects an available bed that is not open or ready', () => {
    expect(isAdmissibleBed(bed({ capacityStatus: 'CLOSED' }))).toBe(false);
    expect(isAdmissibleBed(bed({ readinessStatus: 'CLEANING' }))).toBe(false);
  });

  it('rejects a bed that is not available', () => {
    expect(isAdmissibleBed(bed({ available: false }))).toBe(false);
  });
});

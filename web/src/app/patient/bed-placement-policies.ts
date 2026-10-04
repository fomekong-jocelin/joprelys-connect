import { BedConfiguration } from '../clinic/spatial/spatial-configuration.models';

/**
 * A bed can be proposed for an admission only when its operational state is
 * consistent with the atomic backend claim predicate.
 */
export function isAdmissibleBed(bed: BedConfiguration): boolean {
  return bed.available && bed.capacityStatus === 'OPEN' && bed.readinessStatus === 'READY';
}

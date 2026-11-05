import { UiIconName } from '../../shared/ui/icon.component';
import { BedConfiguration } from './spatial-configuration.models';

/**
 * Un seul état lisible par lit, dérivé des trois axes renvoyés par le serveur
 * (capacité, préparation, occupation). La présence d'un patient prime sur tout le reste.
 */
export type BedDisplayState = 'FREE' | 'OCCUPIED' | 'CLEANING' | 'MAINTENANCE' | 'CLOSED' | 'UNAVAILABLE';

export type BedActionId = 'START_CLEANING' | 'END_CLEANING' | 'START_MAINTENANCE' | 'END_MAINTENANCE' | 'CLOSE' | 'REOPEN';

export interface BedPermissions {
  readonly capacity: boolean;
  readonly cleaning: boolean;
  readonly maintenance: boolean;
}

export interface BedStateStyle {
  readonly icon: UiIconName;
  readonly tile: string;
  readonly pill: string;
}

/** États proposés comme filtres, dans l'ordre d'importance pour l'accueil d'un patient. */
export const FILTERABLE_BED_STATES: readonly BedDisplayState[] = ['FREE', 'OCCUPIED', 'CLEANING', 'MAINTENANCE', 'CLOSED'];

const NEUTRAL: BedStateStyle = {
  icon: 'x-mark',
  tile: 'border-[var(--app-border)] bg-[var(--app-surface-muted)]',
  pill: 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-secondary)]',
};

export const BED_STATE_STYLES: Readonly<Record<BedDisplayState, BedStateStyle>> = {
  FREE: {
    icon: 'check',
    tile: 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)]',
    pill: 'border-[var(--brand-success-muted)] bg-[var(--app-surface)] text-[var(--brand-success-text)]',
  },
  OCCUPIED: {
    icon: 'users',
    tile: 'border-[var(--brand-primary-border)] bg-[var(--brand-info-subtle)]',
    pill: 'border-[var(--brand-primary-border)] bg-[var(--app-surface)] text-[var(--brand-info-text)]',
  },
  CLEANING: {
    icon: 'sparkles',
    tile: 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)]',
    pill: 'border-[var(--brand-warning-border)] bg-[var(--app-surface)] text-[var(--brand-warning-text)]',
  },
  MAINTENANCE: { ...NEUTRAL, icon: 'wrench' },
  CLOSED: NEUTRAL,
  UNAVAILABLE: { ...NEUTRAL, icon: 'exclamation-triangle' },
};

export function bedDisplayState(bed: BedConfiguration): BedDisplayState {
  if (bed.usageStatus === 'OCCUPIED') return 'OCCUPIED';
  if (bed.capacityStatus === 'CLOSED') return 'CLOSED';
  if (bed.readinessStatus === 'CLEANING') return 'CLEANING';
  if (bed.readinessStatus === 'MAINTENANCE') return 'MAINTENANCE';
  return bed.available ? 'FREE' : 'UNAVAILABLE';
}

/** Actions permises sur un lit, la plus courante en premier. Aucune action sur un lit occupé. */
export function bedActions(bed: BedConfiguration, permissions: BedPermissions): BedActionId[] {
  const allowed = (condition: boolean, action: BedActionId) => (condition ? [action] : []);
  switch (bedDisplayState(bed)) {
    case 'FREE':
      return [
        ...allowed(permissions.cleaning, 'START_CLEANING'),
        ...allowed(permissions.maintenance, 'START_MAINTENANCE'),
        ...allowed(permissions.capacity, 'CLOSE'),
      ];
    case 'CLEANING':
      return [...allowed(permissions.cleaning, 'END_CLEANING'), ...allowed(permissions.capacity, 'CLOSE')];
    case 'MAINTENANCE':
      return [...allowed(permissions.maintenance, 'END_MAINTENANCE'), ...allowed(permissions.capacity, 'CLOSE')];
    case 'CLOSED':
      return allowed(permissions.capacity, 'REOPEN');
    default:
      return [];
  }
}

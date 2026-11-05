import {
  BedConfiguration,
  FacilityLocationNode,
  FacilitySpace,
  SaveBedPayload,
  SaveLocationPayload,
  SaveSpacePayload,
  SaveUnitSpaceAssignmentPayload,
  UnitSpaceAssignment,
} from '../spatial-configuration.models';

export const COMFORT_LEVELS = ['STANDARD', 'VIP', 'INTENSIVE_CARE', 'ISOLATION'] as const;
export type ComfortLevel = (typeof COMFORT_LEVELS)[number];

/** Ce que la fenêtre doit créer ou modifier, avec le contexte choisi depuis l'écran. */
export type PlaceEditorRequest =
  | { readonly kind: 'location'; readonly location?: FacilityLocationNode; readonly parentId?: string | null }
  | { readonly kind: 'space'; readonly space?: FacilitySpace; readonly locationNodeId?: string | null; readonly comfortLevel?: string }
  | { readonly kind: 'bed'; readonly space: FacilitySpace; readonly bed?: BedConfiguration }
  | { readonly kind: 'assignment'; readonly space: FacilitySpace; readonly assignment?: UnitSpaceAssignment };

export type PlaceSaveEvent =
  | { readonly kind: 'location'; readonly id: string | null; readonly payload: SaveLocationPayload }
  | { readonly kind: 'space'; readonly id: string | null; readonly payload: SaveSpacePayload; readonly comfortLevel: string | null }
  | { readonly kind: 'bed'; readonly id: string | null; readonly payload: SaveBedPayload }
  | { readonly kind: 'assignment'; readonly id: string | null; readonly payload: SaveUnitSpaceAssignmentPayload };

export interface UnitOption {
  readonly id: string;
  readonly label: string;
}

/** Erreur d'enregistrement : message traduit, et précision du serveur quand elle est dans la langue de l'écran. */
export interface EditorError {
  readonly message: string;
  readonly detail: string;
}

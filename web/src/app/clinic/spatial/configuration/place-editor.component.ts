import { DOCUMENT } from '@angular/common';
import {
  Component,
  ElementRef,
  Injector,
  OnDestroy,
  OnInit,
  afterNextRender,
  computed,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n/i18n.service';
import { normalizeUnitCode, suggestUnitCode } from '../../hospital-organization/org-unit-code';
import { FacilityLocationNode, FacilityLocationNodeType, FacilitySpace, SpaceTypeEntry } from '../spatial-configuration.models';
import { COMFORT_LEVELS, EditorError, PlaceEditorRequest, PlaceSaveEvent, UnitOption } from './place-editor.models';
import { LOCATION_TYPES, isValidPlaceCode } from './place-tree';

const FOCUSABLE_FIELD = 'input:not([disabled]), select:not([disabled])';

/** Fenêtre de saisie d'un lieu, d'une salle, d'un lit ou d'un rattachement à un service. */
@Component({
  selector: 'app-place-editor',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './place-editor.component.html',
})
export class PlaceEditorComponent implements OnInit, OnDestroy {
  private readonly i18n = inject(I18nService);
  private readonly injector = inject(Injector);
  private readonly opener = inject(DOCUMENT).activeElement as HTMLElement | null;
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  readonly request = input.required<PlaceEditorRequest>();
  readonly locations = input<readonly FacilityLocationNode[]>([]);
  readonly spaces = input<readonly FacilitySpace[]>([]);
  readonly spaceTypes = input<readonly SpaceTypeEntry[]>([]);
  readonly unitOptions = input<readonly UnitOption[]>([]);
  readonly busy = input(false);
  readonly error = input<EditorError | null>(null);
  readonly save = output<PlaceSaveEvent>();
  readonly remove = output<void>();
  readonly closed = output<void>();

  readonly locationTypes = LOCATION_TYPES;
  readonly comfortLevels = COMFORT_LEVELS;
  readonly t = (key: string) => this.i18n.t(key);

  readonly name = signal('');
  readonly nodeType = signal<FacilityLocationNodeType>('BUILDING');
  readonly parentId = signal('');
  readonly spaceTypeCode = signal('');
  readonly enableInpatientProfile = signal(false);
  readonly comfortLevel = signal('STANDARD');
  readonly unitId = signal('');
  readonly validFrom = signal('');
  readonly validTo = signal('');
  readonly submitted = signal(false);
  /** Deuxième étape de la suppression ou de la désactivation, confirmée dans la fenêtre. */
  readonly confirmingRemoval = signal(false);
  private readonly typedCode = signal('');
  private readonly codeEdited = signal(false);

  readonly kind = computed(() => this.request().kind);
  readonly editing = computed(() => {
    const request = this.request();
    switch (request.kind) {
      case 'location': return !!request.location;
      case 'space': return !!request.space;
      case 'bed': return !!request.bed;
      case 'assignment': return !!request.assignment;
    }
  });
  readonly title = computed(() => this.t(`spatial.editor.${this.kind()}.${this.editing() ? 'edit' : 'create'}`));
  readonly context = computed(() => {
    const request = this.request();
    return request.kind === 'bed' || request.kind === 'assignment' ? request.space.name : '';
  });
  readonly hasCode = computed(() => this.kind() === 'location' || this.kind() === 'space');
  readonly code = computed(() => this.codeEdited() ? this.typedCode() : suggestUnitCode(this.name(), this.takenCodes()));
  readonly editedSpace = computed(() => {
    const request = this.request();
    return request.kind === 'space' ? request.space ?? null : null;
  });
  /** Le serveur refuse de retirer les lits d'une chambre ou d'en changer le type : l'écran ne le propose pas. */
  readonly inpatientLocked = computed(() => !!this.editedSpace()?.inpatientProfile);
  readonly inpatientCompatible = computed(() =>
    this.spaceTypes().find((type) => type.code === this.spaceTypeCode())?.inpatientCompatible ?? false);
  readonly parentOptions = computed(() => {
    const request = this.request();
    const excluded = request.kind === 'location' && request.location ? this.descendantsOf(request.location.id) : new Set<string>();
    return this.locations().filter((location) => location.active && !excluded.has(location.id));
  });
  readonly removalBlocked = computed(() => {
    const request = this.request();
    return request.kind === 'bed' && request.bed?.usageStatus === 'OCCUPIED';
  });
  readonly canRemove = computed(() => {
    const request = this.request();
    if (request.kind === 'location') return !!request.location?.active;
    if (request.kind === 'space') return !!request.space?.active;
    return request.kind === 'bed' && !!request.bed;
  });

  readonly errors = computed(() => {
    const kind = this.kind();
    const name = kind === 'bed' || kind === 'assignment' ? '' : this.name().trim() ? '' : 'spatial.editor.validation.nameRequired';
    const bedNumber = kind === 'bed' && !this.name().trim() ? 'spatial.editor.validation.bedNumberRequired' : '';
    const code = this.hasCode() && !isValidPlaceCode(normalizeUnitCode(this.code())) ? 'spatial.editor.validation.codeFormat' : '';
    const spaceType = kind === 'space' && !this.spaceTypeCode() ? 'spatial.editor.validation.spaceTypeRequired' : '';
    const unit = kind === 'assignment' && !this.unitId() ? 'spatial.editor.validation.unitRequired' : '';
    const from = kind === 'assignment' && !this.validFrom() ? 'spatial.editor.validation.fromRequired' : '';
    const order = kind === 'assignment' && this.validFrom() && this.validTo() && this.validTo() <= this.validFrom()
      ? 'spatial.editor.validation.periodOrder' : '';
    return { name, bedNumber, code, spaceType, unit, from, order };
  });
  readonly codeErrorVisible = computed(() => !!this.errors().code && (this.submitted() || this.codeEdited()));
  readonly valid = computed(() => Object.values(this.errors()).every((message) => !message));

  private readonly takenCodes = computed(() => {
    const request = this.request();
    const items = request.kind === 'location'
      ? this.locations().filter((location) => location.id !== request.location?.id)
      : this.spaces().filter((space) => request.kind !== 'space' || space.id !== request.space?.id);
    return new Set(items.map((item) => item.code.toUpperCase()));
  });

  constructor() {
    afterNextRender(() => this.panel()?.nativeElement.querySelector<HTMLElement>(FOCUSABLE_FIELD)?.focus());
  }

  ngOnInit(): void {
    const request = this.request();
    switch (request.kind) {
      case 'location':
        this.name.set(request.location?.name ?? '');
        this.nodeType.set(request.location?.nodeType ?? 'BUILDING');
        this.parentId.set(request.location?.parentId ?? request.parentId ?? '');
        this.presetCode(request.location?.code);
        return;
      case 'space':
        this.name.set(request.space?.name ?? '');
        this.spaceTypeCode.set(request.space?.spaceTypeCode ?? this.spaceTypes()[0]?.code ?? '');
        this.parentId.set(request.space?.locationNodeId ?? request.locationNodeId ?? '');
        this.enableInpatientProfile.set(request.space?.inpatientProfile ?? false);
        this.comfortLevel.set(request.comfortLevel ?? 'STANDARD');
        this.presetCode(request.space?.code);
        return;
      case 'bed':
        this.name.set(request.bed?.bedNumber ?? '');
        return;
      case 'assignment':
        this.unitId.set(request.assignment?.organizationalUnitId ?? this.unitOptions()[0]?.id ?? '');
        this.validFrom.set(toLocalDateTime(request.assignment?.validFrom ?? new Date().toISOString()));
        this.validTo.set(request.assignment?.validTo ? toLocalDateTime(request.assignment.validTo) : '');
    }
  }

  ngOnDestroy(): void {
    if (this.opener?.isConnected) this.opener.focus();
  }

  spaceTypeLabel(type: SpaceTypeEntry): string {
    return this.i18n.locale() === 'en' ? type.nameEn : type.nameFr;
  }

  editCode(value: string): void {
    this.codeEdited.set(true);
    this.typedCode.set(value);
  }

  close(): void {
    if (!this.busy()) this.closed.emit();
  }

  submit(): void {
    this.submitted.set(true);
    if (this.busy() || !this.valid()) {
      afterNextRender(
        () => this.panel()?.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
        { injector: this.injector },
      );
      return;
    }
    this.save.emit(this.saveEvent());
  }

  private saveEvent(): PlaceSaveEvent {
    const request = this.request();
    const code = normalizeUnitCode(this.code());
    const name = this.name().trim();
    switch (request.kind) {
      case 'location':
        return {
          kind: 'location',
          id: request.location?.id ?? null,
          payload: { parentId: this.parentId() || null, code, name, nodeType: this.nodeType() },
        };
      case 'space': {
        const inpatient = this.inpatientCompatible() && this.enableInpatientProfile();
        return {
          kind: 'space',
          id: request.space?.id ?? null,
          payload: { locationNodeId: this.parentId() || null, code, name, spaceTypeCode: this.spaceTypeCode(), enableInpatientProfile: inpatient },
          comfortLevel: inpatient ? this.comfortLevel() : null,
        };
      }
      case 'bed':
        return { kind: 'bed', id: request.bed?.id ?? null, payload: { spaceId: request.space.id, bedNumber: name } };
      case 'assignment':
        return {
          kind: 'assignment',
          id: request.assignment?.id ?? null,
          payload: {
            organizationalUnitId: this.unitId(),
            spaceId: request.space.id,
            validFrom: new Date(this.validFrom()).toISOString(),
            validTo: this.validTo() ? new Date(this.validTo()).toISOString() : null,
          },
        };
    }
  }

  private presetCode(code: string | undefined): void {
    if (!code) return;
    this.typedCode.set(code);
    this.codeEdited.set(true);
  }

  private descendantsOf(locationId: string): Set<string> {
    const result = new Set([locationId]);
    let added = true;
    while (added) {
      added = false;
      for (const location of this.locations()) {
        if (location.parentId && result.has(location.parentId) && !result.has(location.id)) {
          result.add(location.id);
          added = true;
        }
      }
    }
    return result;
  }
}

function toLocalDateTime(value: string): string {
  const date = new Date(value);
  const pad = (part: number) => String(part).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

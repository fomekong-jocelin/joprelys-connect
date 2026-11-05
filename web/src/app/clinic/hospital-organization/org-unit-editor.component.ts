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
import { I18nService } from '../../core/i18n/i18n.service';
import {
  UNIT_LEVELS,
  allowedChildTypes,
  canBeParent,
  requiresParent,
  unitDisplayName,
} from './hospital-organization-hierarchy';
import {
  HospitalServiceCatalogEntry,
  OrganizationalUnit,
  OrganizationalUnitType,
  SaveOrganizationalUnitPayload,
} from './hospital-organization.models';
import { isValidUnitCode, normalizeUnitCode, suggestUnitCode } from './org-unit-code';

export type UnitEditorRequest =
  | { readonly mode: 'create'; readonly unitType: OrganizationalUnitType; readonly parentId: string | null }
  | { readonly mode: 'edit'; readonly unit: OrganizationalUnit };

export interface UnitSaveEvent {
  readonly id: string | null;
  readonly payload: SaveOrganizationalUnitPayload;
}

const FOCUSABLE_FIELD = 'input:not([disabled]), select:not([disabled])';

@Component({
  selector: 'app-org-unit-editor',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './org-unit-editor.component.html',
})
export class OrgUnitEditorComponent implements OnInit, OnDestroy {
  private readonly i18n = inject(I18nService);
  private readonly injector = inject(Injector);
  private readonly opener = inject(DOCUMENT).activeElement as HTMLElement | null;
  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  readonly request = input.required<UnitEditorRequest>();
  readonly units = input.required<readonly OrganizationalUnit[]>();
  readonly serviceCatalog = input.required<readonly HospitalServiceCatalogEntry[]>();
  readonly busy = input(false);
  readonly error = input('');
  readonly save = output<UnitSaveEvent>();
  readonly deactivate = output<OrganizationalUnit>();
  readonly closed = output<void>();

  readonly unitType = signal<OrganizationalUnitType>('SERVICE');
  readonly parentId = signal<string | null>(null);
  readonly name = signal('');
  readonly serviceCatalogCode = signal('');
  private readonly typedCode = signal('');
  private readonly codeEdited = signal(false);
  readonly submitted = signal(false);
  readonly t = (key: string) => this.i18n.t(key);

  readonly editedUnit = computed(() => {
    const request = this.request();
    return request.mode === 'edit' ? request.unit : null;
  });
  readonly levelChoices = computed<readonly OrganizationalUnitType[]>(() => {
    const request = this.request();
    if (request.mode === 'edit') return [request.unit.unitType];
    const parent = this.units().find((unit) => unit.id === request.parentId);
    return parent ? allowedChildTypes(parent.unitType) : UNIT_LEVELS;
  });
  readonly parentOptions = computed(() => this.units().filter((unit) =>
    unit.active && unit.id !== this.editedUnit()?.id && canBeParent(unit.unitType, this.unitType())));
  readonly parentRequired = computed(() => requiresParent(this.unitType()));
  readonly showParentField = computed(() => this.parentRequired() || this.parentOptions().length > 0);
  readonly title = computed(() => this.editedUnit()
    ? this.t('hospitalOrg.editor.edit')
    : this.t(`hospitalOrg.actions.add.${this.unitType()}`));
  readonly code = computed(() => this.codeEdited() ? this.typedCode() : this.suggestedCode());

  readonly nameError = computed(() => {
    const length = this.name().trim().length;
    if (this.unitType() === 'SERVICE') return length === 1 ? 'hospitalOrg.validation.nameTooShort' : '';
    if (length === 0) return 'hospitalOrg.validation.nameRequired';
    return length < 2 ? 'hospitalOrg.validation.nameTooShort' : '';
  });
  readonly catalogError = computed(() =>
    this.unitType() === 'SERVICE' && !this.serviceCatalogCode() ? 'hospitalOrg.validation.serviceTypeRequired' : '');
  readonly parentError = computed(() => {
    if (!this.parentRequired() || this.parentId()) return '';
    return this.parentOptions().length ? 'hospitalOrg.validation.parentRequired' : 'hospitalOrg.validation.noServiceYet';
  });
  readonly codeError = computed(() => isValidUnitCode(this.code()) ? '' : 'hospitalOrg.validation.codeFormat');
  readonly codeErrorVisible = computed(() => !!this.codeError() && (this.submitted() || this.codeEdited()));
  readonly selectedServiceLabel = computed(() => {
    const entry = this.serviceCatalog().find((candidate) => candidate.code === this.serviceCatalogCode());
    return entry ? this.serviceLabel(entry) : '';
  });
  readonly valid = computed(() =>
    !this.nameError() && !this.catalogError() && !this.parentError() && !this.codeError());

  private readonly suggestedCode = computed(() => suggestUnitCode(this.codeSource(), this.takenCodes()));
  private readonly takenCodes = computed(() => new Set(this.units()
    .filter((unit) => unit.id !== this.editedUnit()?.id)
    .map((unit) => unit.code.toUpperCase())));

  constructor() {
    afterNextRender(() => this.panel()?.nativeElement.querySelector<HTMLElement>(FOCUSABLE_FIELD)?.focus());
  }

  ngOnInit(): void {
    const request = this.request();
    if (request.mode === 'edit') {
      const unit = request.unit;
      this.unitType.set(unit.unitType);
      this.parentId.set(unit.parentId);
      this.name.set(unit.name ?? '');
      this.serviceCatalogCode.set(unit.serviceCatalogCode ?? '');
      this.typedCode.set(unit.code);
      this.codeEdited.set(true);
      return;
    }
    const choices = this.levelChoices();
    this.unitType.set(choices.includes(request.unitType) ? request.unitType : choices[0]);
    this.parentId.set(request.parentId);
  }

  ngOnDestroy(): void {
    if (this.opener?.isConnected) this.opener.focus();
  }

  typeLabel(type: OrganizationalUnitType): string {
    return this.t(`hospitalOrg.unitType.${type}`);
  }

  unitLabel(unit: OrganizationalUnit): string {
    return unitDisplayName(unit, this.serviceCatalog(), this.i18n.locale());
  }

  serviceLabel(entry: HospitalServiceCatalogEntry): string {
    return this.i18n.locale() === 'en' ? entry.nameEn : entry.nameFr;
  }

  selectLevel(level: OrganizationalUnitType): void {
    this.unitType.set(level);
    const parent = this.units().find((unit) => unit.id === this.parentId());
    if (parent && !canBeParent(parent.unitType, level)) this.parentId.set(null);
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
      this.focusFirstInvalidField();
      return;
    }
    const type = this.unitType();
    this.save.emit({
      id: this.editedUnit()?.id ?? null,
      payload: {
        code: normalizeUnitCode(this.code()),
        unitType: type,
        parentId: this.parentId() || null,
        name: this.name().trim() || null,
        serviceCatalogCode: type === 'SERVICE' ? this.serviceCatalogCode() : null,
      },
    });
  }

  private codeSource(): string {
    const name = this.name().trim();
    if (name || this.unitType() !== 'SERVICE') return name;
    return this.serviceCatalogCode();
  }

  private focusFirstInvalidField(): void {
    afterNextRender(
      () => this.panel()?.nativeElement.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus(),
      { injector: this.injector },
    );
  }
}

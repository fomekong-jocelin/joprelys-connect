import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, Observable, of, switchMap } from 'rxjs';
import { HospitalOrganizationApiService } from '../clinic/hospital-organization/hospital-organization-api.service';
import { HospitalServiceCatalogEntry, OrganizationalUnit } from '../clinic/hospital-organization/hospital-organization.models';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { StaffMember } from '../clinic/staff/staff.models';
import { BedConfiguration, FacilitySpace, UnitSpaceAssignment } from '../clinic/spatial/spatial-configuration.models';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyDocumentApiService } from '../emergency/document/emergency-document-api.service';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { HospitalizationLocationApiService } from './hospitalization-location-api.service';
import { PatientIdentityStatus } from './patient.models';
import { SpatialApiService } from './spatial-api.service';

@Component({
  selector: 'app-emergency-hospitalization-continuation',
  standalone: true,
  imports: [CommonModule, FormsModule, AlertComponent, ButtonComponent],
  template: `
    <section class="min-w-0 space-y-5 rounded-lg border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] p-4 sm:p-5">
      <header class="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div class="min-w-0">
          <p class="text-[10px] font-extrabold uppercase tracking-[0.16em] text-amber-700 dark:text-amber-300">
            {{ t('patients.hospitalization.emergencyContinuation.eyebrow', 'Continuité urgence') }}
          </p>
          <h3 class="mt-1 break-words font-display text-lg font-extrabold text-[var(--text-primary)]">
            {{ t('patients.hospitalization.emergencyContinuation.title', 'Poursuivre vers l’hospitalisation') }}
          </h3>
          <p class="mt-1 max-w-2xl break-words text-sm leading-6 text-[var(--text-secondary)]">
            {{ t('patients.hospitalization.emergencyContinuation.description', 'Choisissez une unité, un espace qui lui est réellement affecté et un lit disponible.') }}
          </p>
        </div>
        @if (temporaryPatientNumber()) {
          <span class="inline-flex min-h-8 shrink-0 items-center rounded-sm border border-amber-300 bg-amber-50 px-3 text-xs font-bold text-amber-800 dark:border-amber-700 dark:bg-amber-950/30 dark:text-amber-200">
            {{ temporaryPatientNumber() }}
          </span>
        }
      </header>

      @if (identityStatus() === 'PROVISIONAL_URGENCY' || identityStatus() === 'DECLARED') {
        <app-ui-alert tone="warning">
          {{ t('patients.hospitalization.emergencyContinuation.provisionalWarning', 'Identité provisoire : les soins ne sont pas bloqués, mais la régularisation reste requise.') }}
        </app-ui-alert>
      }
      @if (error(); as message) { <app-ui-alert tone="error">{{ message }}</app-ui-alert> }
      @if (documentWarning(); as message) { <app-ui-alert tone="warning">{{ message }}</app-ui-alert> }
      @if (success(); as message) { <app-ui-alert tone="success">{{ message }}</app-ui-alert> }

      @if (!canAdmit()) {
        <app-ui-alert tone="warning">{{ t('patients.hospitalization.emergencyContinuation.admissionForbidden', 'Votre profil ne peut pas créer un séjour hospitalier.') }}</app-ui-alert>
      } @else if (loading()) {
        <p class="text-sm font-semibold text-[var(--text-muted)]">{{ t('common.loading', 'Chargement…') }}</p>
      } @else {
        <form class="grid min-w-0 grid-cols-1 gap-4 lg:grid-cols-2" (submit)="submit($event)">
          <label class="block min-w-0">
            <span class="ui-label">{{ t('patients.hospitalization.service', 'Service / unité') }}</span>
            <select class="ui-select mt-1 w-full min-w-0" name="serviceUnit" [ngModel]="selectedUnitId()" (ngModelChange)="selectUnit($event)" required>
              <option value="">{{ t('patients.hospitalization.selectService', 'Sélectionner un service') }}</option>
              @for (unit of eligibleUnits(); track unit.id) { <option [value]="unit.id">{{ unitLabel(unit) }}</option> }
            </select>
          </label>

          <label class="block min-w-0">
            <span class="ui-label">{{ t('patients.hospitalization.space', 'Espace d’hébergement') }}</span>
            <select class="ui-select mt-1 w-full min-w-0" name="space" [ngModel]="selectedSpaceId()" (ngModelChange)="selectSpace($event)" [disabled]="!selectedUnitId()" required>
              <option value="">{{ t('patients.hospitalization.selectSpace', 'Sélectionner un espace') }}</option>
              @for (space of eligibleSpaces(); track space.id) { <option [value]="space.id">{{ space.name }}</option> }
            </select>
          </label>

          <label class="block min-w-0">
            <span class="ui-label">{{ t('patients.hospitalization.selectBed', 'Lit') }}</span>
            <select class="ui-select mt-1 w-full min-w-0" name="bed" [ngModel]="selectedBedId()" (ngModelChange)="selectedBedId.set($event)" [disabled]="!selectedSpaceId()" required>
              <option value="">{{ t('patients.hospitalization.selectBed', 'Sélectionner un lit') }}</option>
              @for (bed of freeBeds(); track bed.id) { <option [value]="bed.id">{{ bed.bedNumber }}</option> }
            </select>
            @if (selectedSpaceId() && freeBeds().length === 0) {
              <p class="mt-1 text-xs font-semibold text-amber-700 dark:text-amber-300">{{ t('patients.hospitalization.noFreeBed', 'Aucun lit disponible dans cet espace.') }}</p>
            }
          </label>

          <label class="block min-w-0">
            <span class="ui-label">{{ t('patients.hospitalization.responsiblePractitioner', 'Médecin responsable') }}</span>
            <select class="ui-select mt-1 w-full min-w-0" name="responsiblePractitioner" [(ngModel)]="responsiblePractitionerId" required>
              <option value="">{{ t('patients.hospitalization.selectPractitioner', 'Sélectionner un praticien') }}</option>
              @for (member of eligiblePractitioners(); track member.id) { <option [value]="member.id">{{ member.displayName }}</option> }
            </select>
          </label>

          <label class="block min-w-0 lg:col-span-2">
            <span class="ui-label">{{ t('patients.hospitalization.reason', 'Motif d’admission') }}</span>
            <textarea class="ui-textarea mt-1 min-h-24 w-full" name="reason" [(ngModel)]="admissionReason" required></textarea>
          </label>

          <div class="flex flex-col-reverse gap-2 pt-1 sm:flex-row sm:justify-end lg:col-span-2">
            <app-ui-button variant="secondary" type="button" (pressed)="cancelled.emit()" [disabled]="saving()">{{ t('common.cancel', 'Annuler') }}</app-ui-button>
            <app-ui-button variant="primary" type="submit" [disabled]="!canSubmit() || saving()">
              {{ saving() ? t('common.saving', 'Enregistrement…') : t('patients.hospitalization.emergencyContinuation.submit', 'Admettre et sécuriser la continuité') }}
            </app-ui-button>
          </div>
        </form>
      }
    </section>
  `,
})
export class EmergencyHospitalizationContinuationComponent {
  private readonly hospitalizationApi = inject(HospitalizationLocationApiService);
  private readonly spatialApi = inject(SpatialApiService);
  private readonly hospitalOrganizationApi = inject(HospitalOrganizationApiService);
  private readonly staffApi = inject(StaffApiService);
  private readonly documentApi = inject(EmergencyDocumentApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);

  readonly patientId = input.required<string>();
  readonly emergencyId = input.required<string>();
  readonly identityStatus = input<PatientIdentityStatus | undefined>();
  readonly temporaryPatientNumber = input<string | undefined>();
  readonly admitted = output<void>();
  readonly cancelled = output<void>();

  readonly units = signal<OrganizationalUnit[]>([]);
  readonly serviceCatalog = signal<HospitalServiceCatalogEntry[]>([]);
  readonly spaces = signal<FacilitySpace[]>([]);
  readonly assignments = signal<UnitSpaceAssignment[]>([]);
  readonly bedsBySpace = signal<Record<string, BedConfiguration[]>>({});
  readonly staff = signal<StaffMember[]>([]);
  readonly selectedUnitId = signal('');
  readonly selectedSpaceId = signal('');
  readonly selectedBedId = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly documentWarning = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  responsiblePractitionerId = '';
  admissionReason = '';

  readonly eligibleUnits = computed(() => this.units().filter((unit) =>
    unit.active && (unit.unitType === 'SERVICE' || unit.unitType === 'CARE_UNIT')));

  readonly eligibleSpaces = computed(() => {
    const unitId = this.selectedUnitId();
    if (!unitId) return [];
    const allowedSpaceIds = new Set(this.assignments()
      .filter((assignment) => assignment.organizationalUnitId === unitId)
      .map((assignment) => assignment.spaceId));
    return this.spaces().filter((space) => space.active && space.inpatientProfile && allowedSpaceIds.has(space.id));
  });

  readonly freeBeds = computed(() => (this.bedsBySpace()[this.selectedSpaceId()] ?? [])
    .filter((bed) => bed.available && bed.capacityStatus === 'OPEN' && bed.readinessStatus === 'READY'));

  readonly eligiblePractitioners = computed(() => this.staff().filter((member) => {
    const roles = member.role.split(',').map((role) => role.trim());
    return roles.includes('MEDECIN') || roles.includes('ADMIN_CLINIQUE');
  }));

  constructor() {
    if (!this.canAdmit()) {
      this.loading.set(false);
      return;
    }

    const now = new Date().toISOString();
    forkJoin({
      units: this.hospitalOrganizationApi.listUnits(undefined, false),
      serviceCatalog: this.hospitalOrganizationApi.listServiceCatalog(),
      spaces: this.spatialApi.listSpaces(undefined, undefined, false),
      assignments: this.spatialApi.listUnitSpaceAssignments(undefined, { activeAt: now }),
      staff: this.staffApi.list(),
    }).pipe(
      switchMap((data) => {
        this.units.set(data.units);
        this.serviceCatalog.set(data.serviceCatalog);
        this.spaces.set(data.spaces);
        this.assignments.set(data.assignments);
        this.staff.set(data.staff);
        const inpatientSpaces = data.spaces.filter((space) => space.inpatientProfile && space.active);
        if (inpatientSpaces.length === 0) return of({} as Record<string, BedConfiguration[]>);
        const requests: Record<string, Observable<BedConfiguration[]>> = {};
        for (const space of inpatientSpaces) {
          requests[space.id] = this.spatialApi.listBeds(space.id);
        }
        return forkJoin(requests);
      }),
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (beds) => this.bedsBySpace.set(beds),
      error: () => this.error.set(this.t(
        'patients.hospitalization.emergencyContinuation.loadError',
        'Impossible de charger les unités, espaces et lits disponibles.',
      )),
    });
  }

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }

  canAdmit(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_ADMIT');
  }

  selectUnit(unitId: string): void {
    this.selectedUnitId.set(unitId);
    this.selectedSpaceId.set('');
    this.selectedBedId.set('');
  }

  selectSpace(spaceId: string): void {
    this.selectedSpaceId.set(spaceId);
    this.selectedBedId.set('');
  }

  unitLabel(unit: OrganizationalUnit): string {
    if (unit.name) return unit.name;
    const catalog = this.serviceCatalog().find((entry) => entry.code === unit.serviceCatalogCode);
    if (!catalog) return unit.code;
    return this.i18n.currentLanguage() === 'en' ? catalog.nameEn : catalog.nameFr;
  }

  canSubmit(): boolean {
    return Boolean(
      this.canAdmit()
      && this.selectedUnitId()
      && this.selectedSpaceId()
      && this.selectedBedId()
      && this.responsiblePractitionerId
      && this.admissionReason.trim(),
    );
  }

  submit(event: Event): void {
    event.preventDefault();
    if (!this.canSubmit() || this.saving()) return;

    this.saving.set(true);
    this.error.set(null);
    this.documentWarning.set(null);
    this.success.set(null);
    let documentsSecured = true;

    this.hospitalizationApi.admit({
      patientId: this.patientId(),
      serviceUnitId: this.selectedUnitId(),
      spaceId: this.selectedSpaceId(),
      bedId: this.selectedBedId(),
      admissionReason: this.admissionReason.trim(),
      emergencyId: this.emergencyId(),
      responsiblePractitionerId: this.responsiblePractitionerId,
    }).pipe(
      switchMap(() => this.documentApi.generateBundle(this.emergencyId()).pipe(
        catchError(() => {
          documentsSecured = false;
          return of([]);
        }),
      )),
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: () => {
        if (!documentsSecured) {
          this.documentWarning.set(this.t(
            'patients.hospitalization.emergencyContinuation.documentWarning',
            'L’hospitalisation est créée, mais le lot documentaire doit être régénéré depuis le dossier d’urgence.',
          ));
        }
        this.success.set(this.t(
          'patients.hospitalization.emergencyContinuation.success',
          'Hospitalisation créée avec continuité clinique sécurisée.',
        ));
        this.admitted.emit();
      },
      error: (err) => this.error.set(
        err?.error?.detail
        || err?.error?.title
        || this.t(
          'patients.hospitalization.emergencyContinuation.error',
          'La continuité vers l’hospitalisation n’a pas pu être finalisée.',
        ),
      ),
    });
  }
}

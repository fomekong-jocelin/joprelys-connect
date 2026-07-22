import { CommonModule } from '@angular/common';
import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, of, switchMap } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { StaffMember } from '../clinic/staff/staff.models';
import { WardConfiguration } from '../clinic/spatial/spatial-configuration.models';
import { I18nService } from '../core/i18n/i18n.service';
import { EmergencyDocumentApiService } from '../emergency/document/emergency-document-api.service';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { PatientApiService } from './patient-api.service';
import { CreateHospitalizationRequest, PatientIdentityStatus } from './patient.models';
import { SpatialApiService } from './spatial-api.service';

interface FreeBedOption {
  readonly id: string;
  readonly roomNumber: string;
  readonly bedNumber: string;
}

@Component({
  selector: 'app-emergency-hospitalization-continuation',
  standalone: true,
  imports: [CommonModule, FormsModule, AlertComponent, ButtonComponent],
  template: `
    <section class="border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] rounded-lg p-5 space-y-5">
      <header class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div class="flex items-start gap-3">
          <div class="mt-0.5 inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-[var(--brand-warning-border)] bg-[var(--app-surface)] text-amber-700 dark:text-amber-300">
            <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m0 3.75h.008v.008H12v-.008ZM10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0Z" />
            </svg>
          </div>
          <div>
            <p class="text-[10px] font-extrabold uppercase tracking-[0.16em] text-amber-700 dark:text-amber-300">
              {{ t('patients.hospitalization.emergencyContinuation.eyebrow', 'Continuité urgence') }}
            </p>
            <h3 class="mt-1 font-display text-lg font-extrabold text-[var(--text-primary)]">
              {{ t('patients.hospitalization.emergencyContinuation.title', 'Poursuivre vers l’hospitalisation') }}
            </h3>
            <p class="mt-1 max-w-2xl text-sm leading-6 text-[var(--text-secondary)]">
              {{ t('patients.hospitalization.emergencyContinuation.description', 'L’urgence sera reliée au séjour, à la visite clinique, aux documents vérifiables et à la chronologie du DPU.') }}
            </p>
          </div>
        </div>
        @if (temporaryPatientNumber()) {
          <span class="inline-flex min-h-8 items-center rounded-sm border border-amber-300 bg-amber-50 px-3 text-xs font-bold text-amber-800 dark:border-amber-700 dark:bg-amber-950/30 dark:text-amber-200">
            {{ temporaryPatientNumber() }}
          </span>
        }
      </header>

      @if (identityStatus() === 'PROVISIONAL_URGENCY' || identityStatus() === 'DECLARED') {
        <app-ui-alert tone="warning">
          {{ t('patients.hospitalization.emergencyContinuation.provisionalWarning', 'Identité provisoire : les soins ne sont pas bloqués. Les documents et les éléments financiers restent marqués à régulariser jusqu’à la validation de l’identité.') }}
        </app-ui-alert>
      }

      @if (error(); as message) {
        <app-ui-alert tone="error">{{ message }}</app-ui-alert>
      }
      @if (documentWarning(); as message) {
        <app-ui-alert tone="warning">{{ message }}</app-ui-alert>
      }
      @if (success(); as message) {
        <app-ui-alert tone="success">{{ message }}</app-ui-alert>
      }

      @if (!canAdmit()) {
        <app-ui-alert tone="warning">
          {{ t('patients.hospitalization.emergencyContinuation.admissionForbidden', 'Votre profil peut consulter le contexte d’urgence, mais ne peut pas créer un séjour hospitalier.') }}
        </app-ui-alert>
      } @else if (loading()) {
        <p class="text-sm font-semibold text-[var(--text-muted)]">
          {{ t('common.loading', 'Chargement…') }}
        </p>
      } @else {
        <form class="grid grid-cols-1 gap-4 lg:grid-cols-2" (submit)="submit($event)">
          <div class="space-y-1.5">
            <label for="emergency-hospitalization-service" class="ui-label">
              {{ t('patients.hospitalization.service', 'Service') }}
            </label>
            <select
              id="emergency-hospitalization-service"
              name="service"
              class="ui-select"
              [ngModel]="selectedWardId()"
              (ngModelChange)="selectWard($event)"
              required
            >
              <option value="">{{ t('spatial.selectWardPlaceholder', 'Sélectionner un service') }}</option>
              @for (ward of eligibleWards(); track ward.id) {
                <option [value]="ward.id">{{ ward.name }}</option>
              }
            </select>
          </div>

          <div class="space-y-1.5">
            <label for="emergency-hospitalization-bed" class="ui-label">
              {{ t('patients.hospitalization.selectBed', 'Lit') }}
            </label>
            <select
              id="emergency-hospitalization-bed"
              name="bed"
              class="ui-select"
              [ngModel]="selectedBedId()"
              (ngModelChange)="selectedBedId.set($event)"
              [disabled]="!selectedWardId()"
              required
            >
              <option value="">{{ t('patients.hospitalization.selectBed', 'Sélectionner un lit') }}</option>
              @for (bed of freeBeds(); track bed.id) {
                <option [value]="bed.id">{{ bed.roomNumber }} — {{ bed.bedNumber }}</option>
              }
            </select>
            @if (selectedWardId() && freeBeds().length === 0) {
              <p class="text-xs font-semibold text-amber-700 dark:text-amber-300">
                {{ t('patients.hospitalization.noFreeBed', 'Aucun lit libre dans ce service.') }}
              </p>
            }
          </div>

          <div class="space-y-1.5">
            <label for="emergency-hospitalization-practitioner" class="ui-label">
              {{ t('patients.hospitalization.responsiblePractitioner', 'Médecin responsable') }}
            </label>
            <select
              id="emergency-hospitalization-practitioner"
              name="responsiblePractitioner"
              class="ui-select"
              [(ngModel)]="responsiblePractitionerId"
              required
            >
              <option value="">{{ t('patients.hospitalization.selectPractitioner', 'Sélectionner un praticien') }}</option>
              @for (member of eligiblePractitioners(); track member.id) {
                <option [value]="member.id">{{ member.displayName }}</option>
              }
            </select>
          </div>

          <div class="space-y-1.5">
            <label for="emergency-hospitalization-reason" class="ui-label">
              {{ t('patients.hospitalization.reason', 'Motif d’admission') }}
            </label>
            <textarea
              id="emergency-hospitalization-reason"
              name="reason"
              class="ui-textarea min-h-24"
              [(ngModel)]="admissionReason"
              required
            ></textarea>
          </div>

          <div class="flex flex-col-reverse gap-2 pt-1 sm:flex-row sm:justify-end lg:col-span-2">
            <app-ui-button variant="secondary" type="button" (pressed)="cancelled.emit()" [disabled]="saving()">
              {{ t('common.cancel', 'Annuler') }}
            </app-ui-button>
            <app-ui-button variant="primary" type="submit" [disabled]="!canSubmit() || saving()">
              {{ saving()
                ? t('common.saving', 'Enregistrement…')
                : t('patients.hospitalization.emergencyContinuation.submit', 'Admettre et sécuriser la continuité') }}
            </app-ui-button>
          </div>
        </form>
      }
    </section>
  `,
})
export class EmergencyHospitalizationContinuationComponent {
  private readonly patientApi = inject(PatientApiService);
  private readonly spatialApi = inject(SpatialApiService);
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

  readonly wards = signal<WardConfiguration[]>([]);
  readonly staff = signal<StaffMember[]>([]);
  readonly selectedWardId = signal('');
  readonly selectedBedId = signal('');
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly documentWarning = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  responsiblePractitionerId = '';
  admissionReason = '';

  readonly eligibleWards = computed(() => this.wards().filter((ward) => ward.allowsRooms));
  readonly eligiblePractitioners = computed(() => this.staff().filter((member) => {
    const roles = member.role.split(',').map((role) => role.trim());
    return roles.includes('MEDECIN') || roles.includes('ADMIN_CLINIQUE');
  }));
  readonly freeBeds = computed<FreeBedOption[]>(() => {
    const ward = this.eligibleWards().find((item) => item.id === this.selectedWardId());
    if (!ward) return [];
    return ward.rooms.flatMap((room) => room.beds
      .filter((bed) => bed.status === 'FREE')
      .map((bed) => ({
        id: bed.id,
        roomNumber: room.roomNumber,
        bedNumber: bed.bedNumber,
      })));
  });

  constructor() {
    forkJoin({
      spatial: this.spatialApi.getConfiguration(),
      staff: this.staffApi.list(),
    }).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: ({ spatial, staff }) => {
        this.wards.set([...spatial.wards]);
        this.staff.set(staff);
        const firstWard = spatial.wards.find((ward) => ward.allowsRooms);
        if (firstWard) this.selectedWardId.set(firstWard.id);
      },
      error: () => this.error.set(this.t(
        'patients.hospitalization.emergencyContinuation.loadError',
        'Impossible de charger les lits et les praticiens disponibles.',
      )),
    });
  }

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }

  canAdmit(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_ADMIT');
  }

  selectWard(wardId: string): void {
    this.selectedWardId.set(wardId);
    this.selectedBedId.set('');
  }

  canSubmit(): boolean {
    return Boolean(
      this.canAdmit()
      && this.selectedWardId()
      && this.selectedBedId()
      && this.responsiblePractitionerId
      && this.admissionReason.trim(),
    );
  }

  submit(event: Event): void {
    event.preventDefault();
    if (!this.canSubmit() || this.saving()) return;

    const ward = this.eligibleWards().find((item) => item.id === this.selectedWardId());
    const bed = this.freeBeds().find((item) => item.id === this.selectedBedId());
    if (!ward || !bed) return;

    this.saving.set(true);
    this.error.set(null);
    this.documentWarning.set(null);
    this.success.set(null);
    let documentsSecured = true;

    const request = {
      patientId: this.patientId(),
      serviceName: ward.name,
      roomNumber: bed.roomNumber,
      bedNumber: bed.bedNumber,
      admissionReason: this.admissionReason.trim(),
      emergencyId: this.emergencyId(),
      responsiblePractitionerId: this.responsiblePractitionerId,
    } as unknown as CreateHospitalizationRequest;

    this.patientApi.admitPatient(request).pipe(
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

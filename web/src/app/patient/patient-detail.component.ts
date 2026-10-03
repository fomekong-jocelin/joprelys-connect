import { ToastService } from '../shared/ui/toast.service';
import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterOutlet } from '@angular/router';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { VisitApiService } from '../visit/visit-api.service';
import { Visit } from '../visit/visit.models';
import { ActivePatientService } from './active-patient.service';
import { PatientApiService } from './patient-api.service';
import { Patient, PatientAllergy } from './patient.models';
import { PatientRecordNavigationComponent } from './detail/patient-record-navigation.component';
import { PatientVisitAdmissionDialogComponent } from './detail/patient-visit-admission-dialog.component';

@Component({
  selector: 'app-patient-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    AppShellComponent,
    AlertComponent,
    ButtonComponent,
    CardComponent,
    PatientRecordNavigationComponent,
    PatientVisitAdmissionDialogComponent,
  ],
  template: `
    <app-shell>
      <div class="app-container py-5 sm:py-8">
        @if (loading()) {
          <div class="py-12 text-center">
            <div class="inline-block h-8 w-8 animate-spin rounded-full border-4 border-indigo-200 border-t-indigo-600"></div>
            <p class="mt-2 text-sm font-bold text-[var(--text-muted)]">{{ i18n.t('patient.detail.loading') }}</p>
          </div>
        } @else if (error(); as err) {
          <app-ui-card>
            <app-ui-alert tone="error" class="mb-6">{{ err }}</app-ui-alert>
            <div class="flex justify-end">
              <app-ui-button variant="secondary" (pressed)="goBack()">{{ i18n.t('common.back') }}</app-ui-button>
            </div>
          </app-ui-card>
        } @else if (consentRequiredPatient(); as consentPatient) {
          <app-ui-card>
            <div class="mx-auto flex max-w-md flex-col items-center gap-6 py-8 text-center">
              <div class="rounded-md border border-rose-100 bg-rose-50 p-4 text-rose-600 dark:border-rose-900/30 dark:bg-rose-950/20 dark:text-rose-400">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="h-12 w-12" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z" />
                </svg>
              </div>

              <div>
                <h3 class="font-display text-xl font-black text-[var(--text-primary)]">{{ i18n.t('patient.consent.title') }}</h3>
                <p class="mt-1 text-xs text-[var(--text-muted)]">
                  {{ i18n.t('patient.consent.dpuLabel') }}
                  <span class="font-mono font-bold text-[var(--brand-info-text)] dark:text-indigo-400">{{ consentPatient.globalPatientNumber }}</span>
                </p>
                <p class="mt-4 text-sm leading-relaxed text-[var(--text-secondary)]">{{ i18n.t('patient.consent.noAccessMessage') }}</p>
              </div>

              @if (emergencyError(); as emergencyErrorMessage) {
                <app-ui-alert tone="error" class="w-full text-left">{{ emergencyErrorMessage }}</app-ui-alert>
              }

              <div class="flex w-full flex-col gap-4 border-t border-[var(--app-border)]/80 pt-6">
                <div class="w-full text-left">
                  <label for="emergency-reason" class="ui-label mb-2 block">{{ i18n.t('patient.consent.emergencyJustificationLabel') }}</label>
                  <textarea
                    id="emergency-reason"
                    class="ui-input min-h-[80px] w-full"
                    [placeholder]="i18n.t('patient.consent.emergencyReasonPlaceholder')"
                    [value]="emergencyReason()"
                    (input)="emergencyReason.set($any($event.target).value)"
                  ></textarea>
                </div>

                <div class="grid w-full grid-cols-2 gap-3">
                  <app-ui-button variant="secondary" (pressed)="goBack()">{{ i18n.t('common.cancel') }}</app-ui-button>
                  <app-ui-button
                    variant="primary"
                    [disabled]="!emergencyReason().trim() || emergencyLoading()"
                    (pressed)="triggerEmergencyAccess()"
                  >
                    {{ emergencyLoading() ? i18n.t('patient.consent.activating') : i18n.t('patient.consent.breakGlassButton') }}
                  </app-ui-button>
                </div>
              </div>
            </div>
          </app-ui-card>
        } @else if (patient(); as p) {
          <div class="space-y-4 sm:space-y-5">
            <!-- Only the essential patient context and immediate action stay above the fold. -->
            <section class="ui-card p-4 sm:p-5">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div class="min-w-0">
                  <div class="flex flex-wrap items-center gap-2.5">
                    <h2 class="truncate text-xl font-black text-[var(--text-primary)]">{{ p.fullName }}</h2>
                    <span class="inline-flex items-center rounded-md bg-[var(--brand-success-subtle)] px-2 py-0.5 text-[10px] font-extrabold uppercase tracking-wider text-[var(--brand-success-text)] dark:bg-green-950/30 dark:text-green-300">
                      {{ p.status }}
                    </span>
                  </div>
                  <p class="mt-1.5 flex flex-wrap gap-x-3 gap-y-1 text-xs text-[var(--text-muted)]">
                    <span>{{ i18n.t('patient.detail.dpuLabel') }} <strong class="font-mono font-extrabold text-[var(--brand-info-text)] dark:text-indigo-400">{{ p.globalPatientNumber }}</strong></span>
                    <span>{{ i18n.t('patient.detail.establishmentLabel') }} <strong class="font-mono font-bold text-[var(--text-secondary)]">{{ p.localPatientNumber }}</strong></span>
                  </p>
                </div>

                <!-- Mobile: primary clinical action first, then secondary actions. Desktop: compact right-aligned group. -->
                <div
                  class="grid w-full grid-cols-2 gap-2 lg:flex lg:w-auto lg:items-center lg:justify-end"
                  data-testid="patient-header-actions"
                >
                  @if (canStartConsultation()) {
                    <app-ui-button
                      variant="primary"
                      (pressed)="goToConsultation()"
                      class="order-1 col-span-2 w-full text-xs lg:order-3 lg:col-span-1 lg:w-auto"
                      data-testid="patient-primary-action"
                    >
                      {{ i18n.t('patient.detail.startConsultation') }}
                    </app-ui-button>
                  } @else if (canAdmit()) {
                    <app-ui-button
                      variant="primary"
                      (pressed)="openModal()"
                      class="order-1 col-span-2 w-full text-xs lg:order-3 lg:col-span-1 lg:w-auto"
                      data-testid="patient-primary-action"
                    >
                      {{ i18n.t('patient.detail.openVisit') }}
                    </app-ui-button>
                  }

                  @if (canDownloadSummary()) {
                    <button
                      type="button"
                      (click)="downloadSummaryPdf()"
                      class="order-2 flex min-h-11 items-center justify-center gap-1.5 whitespace-nowrap rounded-md border border-[var(--app-border)] px-3 py-2.5 text-xs font-bold text-[var(--text-secondary)] shadow-xs transition-colors hover:bg-[var(--app-surface-muted)] lg:px-4"
                      [title]="i18n.t('patient.detail.downloadSummaryTitle')"
                      data-testid="patient-summary-action"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="h-4 w-4 shrink-0 text-brand-primary" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5M16.5 12L12 16.5m0 0L7.5 12m4.5 4.5V3" />
                      </svg>
                      <span>{{ i18n.t('patient.detail.downloadSummaryLabel') }}</span>
                    </button>
                  }

                  <app-ui-button
                    variant="secondary"
                    (pressed)="goBack()"
                    class="order-3 text-xs lg:order-1"
                    [class.col-span-2]="!canDownloadSummary()"
                    data-testid="patient-back-action"
                  >
                    {{ i18n.t('common.back') }}
                  </app-ui-button>
                </div>
              </div>
            </section>

            <!-- Safety-critical information is never hidden behind progressive disclosure. -->
            @if (p.emergencyAccessActive) {
              <div class="flex items-center gap-3 rounded-md border border-rose-200 bg-rose-50 p-3 text-sm font-bold text-rose-800 dark:border-rose-800/40 dark:bg-rose-950/20 dark:text-rose-300 sm:p-4">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="h-5 w-5 shrink-0" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <span>{{ i18n.t('patient.detail.breakGlassActive') }}</span>
              </div>
            }

            @if (criticalAllergies().length > 0) {
              <div class="flex items-center gap-3 rounded-md border border-red-200 bg-[var(--brand-danger-subtle)] p-3 text-sm font-bold text-red-800 dark:border-red-800/40 dark:text-[var(--brand-danger-text)] sm:p-4">
                <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2.5" stroke="currentColor" class="h-5 w-5 shrink-0" aria-hidden="true">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div class="min-w-0">
                  <span>{{ i18n.t('patient.detail.criticalAllergiesWarning') }}</span>
                  <span class="ml-1 font-extrabold">{{ criticalAllergiesSubstances() }}</span>
                </div>
              </div>
            }

            <!-- One navigation level at a time: mobile selector / desktop sidebar + one content pane. -->
            <div class="grid min-w-0 gap-4 md:grid-cols-[220px_minmax(0,1fr)] lg:grid-cols-[240px_minmax(0,1fr)] lg:gap-6">
              <app-patient-record-navigation
                class="min-w-0 md:self-start"
                [patientId]="p.id"
                [canViewConsultations]="canViewClinicalData()"
                [canViewLabOrders]="canViewLabOrders()"
                [canViewHospitalizations]="canViewHospitalizations()"
                [canViewAudit]="canViewAudit()"
              />

              <main class="min-w-0">
                <router-outlet></router-outlet>
              </main>
            </div>
          </div>
        }
      </div>
    </app-shell>

    @if (showVisitModal() && patient(); as modalPatient) {
      <app-patient-visit-admission-dialog
        [patientId]="modalPatient.id"
        (cancelled)="closeModal()"
        (created)="onVisitCreated()"
      />
    }
  `,
})
export class PatientDetailComponent implements OnInit, OnDestroy {
  readonly patientInput = input<Patient | null>(null, { alias: 'patient' });
  readonly back = output<void>();

  private readonly visitApi = inject(VisitApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly patientApi = inject(PatientApiService);
  private readonly activePatientService = inject(ActivePatientService);
  private readonly rbacApi = inject(RbacApiService);
  readonly i18n = inject(I18nService);
  private readonly toasts = inject(ToastService);

  readonly loadedPatient = signal<Patient | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly consentRequiredPatient = signal<Patient | null>(null);
  readonly emergencyReason = signal('');
  readonly emergencyLoading = signal(false);
  readonly emergencyError = signal<string | null>(null);

  readonly patient = computed(() => this.patientInput() ?? this.loadedPatient());
  readonly showVisitModal = signal(false);
  readonly activeVisit = signal<Visit | null>(null);
  readonly criticalAllergies = signal<PatientAllergy[]>([]);
  readonly criticalAllergiesSubstances = computed(() => this.criticalAllergies().map(allergy => allergy.substance).join(', '));

  readonly canAdmit = computed(() => this.rbacApi.hasPermission('VISIT_CREATE') && this.activeVisit() === null);
  readonly canStartConsultation = computed(() => this.rbacApi.hasPermission('CLINICAL_WRITE') && this.activeVisit() !== null);
  readonly canViewClinicalData = computed(() => this.rbacApi.hasPermission('CLINICAL_READ'));
  readonly canViewLabOrders = computed(() => this.rbacApi.hasPermission('LAB_ORDER_READ'));
  readonly canViewHospitalizations = computed(() => this.rbacApi.hasPermission('HOSPITALIZATION_READ'));
  readonly canViewAudit = computed(() => this.rbacApi.hasPermission('AUDIT_READ'));
  readonly canDownloadSummary = computed(() => this.rbacApi.hasPermission('CLINICAL_READ'));

  ngOnInit(): void {
    const statePatient = window.history.state?.patient as Patient | undefined;
    if (statePatient) {
      this.loadedPatient.set(statePatient);
      this.initializeDetails();
    }

    this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        if (!this.patientInput()) {
          this.loadPatient(id);
        } else {
          this.initializeDetails();
        }
      } else if (this.patientInput()) {
        this.initializeDetails();
      }
    });
  }

  ngOnDestroy(): void {
    this.activePatientService.patient.set(null);
  }

  downloadSummaryPdf(): void {
    const currentPatient = this.patient();
    if (!currentPatient) return;

    this.patientApi.downloadSummaryPdf(currentPatient.id).subscribe({
      next: blob => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `patient-summary-${currentPatient.id}.pdf`;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        window.URL.revokeObjectURL(url);
      },
      error: err => {
        console.error('Error downloading summary pdf', err);
        alert(this.i18n.t('patient.detail.downloadPdfError'));
      },
    });
  }

  loadPatient(id: string): void {
    if (!this.patient()) {
      this.loading.set(true);
    }
    this.error.set(null);
    this.consentRequiredPatient.set(null);

    this.patientApi.getById(id).subscribe({
      next: fullPatient => {
        this.loadedPatient.set(fullPatient);
        this.loading.set(false);
        this.initializeDetails();
      },
      error: err => {
        this.loading.set(false);
        if (err?.status === 403) {
          if (!this.patient()) {
            this.loadedPatient.set({
              id,
              fullName: err.error?.fullName || this.i18n.t('patient.fallback.name'),
              globalPatientNumber: err.error?.globalPatientNumber || this.i18n.t('common.unknown'),
              localPatientNumber: this.i18n.t('common.unknown'),
              gender: this.i18n.t('common.unknown'),
              birthDate: '',
              phone: '',
              city: '',
              status: 'INACTIVE',
              organizationId: '',
              createdAt: '',
              updatedAt: '',
            });
          }
          this.consentRequiredPatient.set(this.patient());
        } else {
          this.error.set(err.error?.detail || err.error?.title || this.i18n.t('patient.detail.loadError'));
        }
      },
    });
  }

  initializeDetails(): void {
    const currentPatient = this.patient();
    if (!currentPatient) return;

    this.activePatientService.patient.set(currentPatient);

    if (this.rbacApi.hasPermission('VISIT_READ')) {
      this.visitApi.getActiveVisits().subscribe({
        next: visits => this.activeVisit.set(visits.find(visit => visit.patientId === currentPatient.id) ?? null),
        error: () => {},
      });
    } else {
      this.activeVisit.set(null);
    }

    this.patientApi.getAllergies(currentPatient.id).subscribe({
      next: allergies => {
        this.criticalAllergies.set(allergies.filter(allergy =>
          allergy.status === 'ACTIVE' && (allergy.severity === 'CRITICAL' || allergy.severity === 'HIGH'),
        ));
      },
      error: () => {},
    });
  }

  triggerEmergencyAccess(): void {
    const currentPatient = this.consentRequiredPatient();
    if (!currentPatient || !this.emergencyReason().trim()) return;

    this.emergencyLoading.set(true);
    this.emergencyError.set(null);

    this.patientApi.triggerEmergencyAccess(currentPatient.id, this.emergencyReason().trim()).subscribe({
      next: () => {
        this.emergencyLoading.set(false);
        this.consentRequiredPatient.set(null);
        this.emergencyReason.set('');
        this.loadPatient(currentPatient.id);
      },
      error: err => {
        this.emergencyLoading.set(false);
        this.emergencyError.set(err.error?.detail || err.error?.title || this.i18n.t('patient.consent.emergencyAccessError'));
      },
    });
  }

  goBack(): void {
    this.back.emit();
    this.router.navigate(['/patients']);
  }

  goToConsultation(): void {
    const visit = this.activeVisit();
    if (visit) {
      this.router.navigate(['/clinic/consultation', visit.id]);
    }
  }

  openModal(): void {
    this.showVisitModal.set(true);
  }

  closeModal(): void {
    this.showVisitModal.set(false);
  }

  onVisitCreated(): void {
    this.showVisitModal.set(false);
    this.toasts.show(`${this.i18n.t('admission.toast.visitCreated')} ${this.patient()?.fullName ?? ''}. ${this.i18n.t('admission.toast.nextStepVitals')}`);
    this.initializeDetails();
  }
}

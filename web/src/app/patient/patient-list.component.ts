import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { AdmissionCompleted, UnifiedAdmissionComponent } from '../admission/unified-admission.component';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { PatientApiService } from './patient-api.service';
import { Patient } from './patient.models';

@Component({
  selector: 'app-patient-list',
  templateUrl: './patient-list.component.html',
  imports: [
    AlertComponent,
    AppShellComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    UnifiedAdmissionComponent,
  ],
})
export class PatientListComponent implements OnInit {
  private readonly api = inject(PatientApiService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly list = signal<Patient[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly searchQuery = signal('');
  readonly appliedSearchQuery = signal('');
  readonly selectedPatient = signal<Patient | null>(null);
  readonly showCreateForm = signal(false);
  readonly consentRequiredPatient = signal<Patient | null>(null);
  readonly emergencyReason = signal('');
  readonly emergencyLoading = signal(false);
  readonly emergencyError = signal<string | null>(null);

  readonly pageTitle = computed(() => this.i18n.t('patients.title'));
  readonly pageSubtitle = computed(() => this.i18n.t('patients.subtitle'));
  readonly searchPlaceholder = computed(() => this.i18n.t('patients.searchPlaceholder'));
  readonly createLabel = computed(() => this.i18n.t('admission.title'));
  readonly loadingLabel = computed(() => this.i18n.t('common.loading'));
  readonly emptyLabel = computed(() => this.i18n.t('patients.empty'));
  readonly hasSearchQuery = computed(() => this.searchQuery().trim().length > 0);
  readonly listContextLabel = computed(() => {
    if (!this.appliedSearchQuery()) {
      return this.i18n.t('patients.listHeading');
    }

    const count = this.list().length;
    const resultLabel = this.i18n.t(count === 1 ? 'patients.resultOne' : 'patients.resultsMany');
    return `${count} ${resultLabel}`;
  });

  ngOnInit(): void {
    this.load();
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  load(): void {
    const query = this.searchQuery().trim();
    this.appliedSearchQuery.set(query);
    this.loading.set(true);
    this.error.set(null);
    this.api.list(query).pipe(
      finalize(() => this.loading.set(false))
    ).subscribe({
      next: (patients) => this.list.set(patients),
      error: () => this.error.set(this.i18n.t('patients.loadError')),
    });
  }

  onSearchInput(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  clearSearch(): void {
    if (!this.hasSearchQuery() && !this.appliedSearchQuery()) {
      return;
    }
    this.searchQuery.set('');
    this.load();
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((visible) => !visible);
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
  }

  onAdmissionCompleted(result: AdmissionCompleted): void {
    this.showCreateForm.set(false);
    const route = result.carePath === 'EMERGENCY'
      ? ['/clinic/emergencies']
      : ['/patients', result.patientId];
    void this.router.navigate(route);
  }

  viewDetail(patient: Patient): void {
    void this.router.navigate(['/patients', patient.id], { state: { patient } });
  }

  closeDetail(): void {
    this.selectedPatient.set(null);
    this.consentRequiredPatient.set(null);
  }

  triggerEmergencyAccess(): void {
    const patient = this.consentRequiredPatient();
    const reason = this.emergencyReason().trim();
    if (!patient || !reason || this.emergencyLoading()) return;

    this.emergencyLoading.set(true);
    this.emergencyError.set(null);
    this.api.triggerEmergencyAccess(patient.id, reason).pipe(
      finalize(() => this.emergencyLoading.set(false))
    ).subscribe({
      next: () => {
        this.consentRequiredPatient.set(null);
        this.emergencyReason.set('');
        this.viewDetail(patient);
      },
      error: (err) => this.emergencyError.set(
        err.error?.detail || err.error?.title || this.t('patient.consent.emergencyAccessError')
      ),
    });
  }

  patientDisplayName(patient: Patient): string {
    return patient.displayName || patient.fullName || patient.temporaryPatientNumber || patient.globalPatientNumber;
  }
}

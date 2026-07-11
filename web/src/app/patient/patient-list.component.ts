import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AdmissionCompleted, UnifiedAdmissionComponent } from '../admission/unified-admission.component';
import { I18nService } from '../core/i18n/i18n.service';
import { AlertComponent } from '../shared/ui/alert.component';
import { AppShellComponent } from '../shared/layout/app-shell.component';
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
  readonly selectedPatient = signal<Patient | null>(null);
  readonly showCreateForm = signal(false);

  readonly consentRequiredPatient = signal<Patient | null>(null);
  readonly emergencyReason = signal('');
  readonly emergencyLoading = signal(false);
  readonly emergencyError = signal<string | null>(null);

  readonly pageTitle = computed(() => this.i18n.t('patients.title'));
  readonly pageSubtitle = computed(() => this.i18n.t('patients.subtitle'));
  readonly backLabel = computed(() => this.i18n.t('common.back'));
  readonly searchPlaceholder = computed(() => this.i18n.t('patients.searchPlaceholder'));
  readonly createLabel = computed(() => this.i18n.locale() === 'en' ? 'New admission' : 'Nouvelle admission');
  readonly loadingLabel = computed(() => this.i18n.t('common.loading'));
  readonly emptyLabel = computed(() => this.i18n.t('patients.empty'));

  ngOnInit(): void {
    this.load();
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list(this.searchQuery()).subscribe({
      next: (res) => {
        this.list.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('patients.loadError'));
        this.loading.set(false);
      },
    });
  }

  onSearchInput(event: Event): void {
    this.searchQuery.set((event.target as HTMLInputElement).value);
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((visible) => !visible);
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
  }

  onAdmissionCompleted(result: AdmissionCompleted): void {
    this.showCreateForm.set(false);
    if (result.carePath === 'EMERGENCY') {
      void this.router.navigate(['/clinic/emergencies']);
      return;
    }
    void this.router.navigate(['/patients', result.patientId]);
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
    if (!patient || !this.emergencyReason().trim()) return;

    this.emergencyLoading.set(true);
    this.emergencyError.set(null);

    this.api.triggerEmergencyAccess(patient.id, this.emergencyReason().trim()).subscribe({
      next: () => {
        this.emergencyLoading.set(false);
        this.consentRequiredPatient.set(null);
        this.emergencyReason.set('');
        this.viewDetail(patient);
      },
      error: (err) => {
        this.emergencyLoading.set(false);
        this.emergencyError.set(err.error?.detail || err.error?.title || this.t('patient.consent.emergencyAccessError'));
      },
    });
  }

  patientDisplayName(patient: Patient): string {
    const extended = patient as Patient & { displayName?: string; temporaryPatientNumber?: string };
    return extended.displayName || patient.fullName || extended.temporaryPatientNumber || patient.globalPatientNumber;
  }
}

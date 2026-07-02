import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { CardComponent } from '../shared/ui/card.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { PatientFormComponent, PatientFormLabels } from './patient-form.component';
import { PatientDetailComponent } from './patient-detail.component';
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
    PatientFormComponent,
    PatientDetailComponent,
    PageHeaderComponent,
  ],
})
export class PatientListComponent implements OnInit {
  private readonly api = inject(PatientApiService);
  private readonly i18n = inject(I18nService);

  readonly list = signal<Patient[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly searchQuery = signal('');
  readonly selectedPatient = signal<Patient | null>(null);

  // STORY-0803 — Consent Blocking & Break-Glass Signals
  readonly consentRequiredPatient = signal<Patient | null>(null);
  readonly emergencyReason = signal('');
  readonly emergencyLoading = signal(false);
  readonly emergencyError = signal<string | null>(null);

  // Form Signals
  readonly fullName = signal('');
  readonly gender = signal('');
  readonly birthDate = signal('');
  readonly phone = signal('');
  readonly city = signal('');
  readonly district = signal('');
  readonly address = signal('');
  readonly emergencyContactName = signal('');
  readonly emergencyContactPhone = signal('');
  readonly allergies = signal('');
  readonly medicalHistory = signal('');
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly showCreateForm = signal(false);

  // Translation Signals
  readonly pageTitle = computed(() => this.i18n.t('patients.title'));
  readonly pageSubtitle = computed(() => this.i18n.t('patients.subtitle'));
  readonly backLabel = computed(() => this.i18n.t('common.back'));
  readonly searchPlaceholder = computed(() => this.i18n.t('patients.searchPlaceholder'));
  readonly createLabel = computed(() => this.i18n.t('patients.create'));
  readonly loadingLabel = computed(() => this.i18n.t('common.loading'));
  readonly emptyLabel = computed(() => this.i18n.t('patients.empty'));

  readonly formLabels = computed<PatientFormLabels>(() => ({
    title: this.i18n.t('patients.createTitle'),
    fullName: this.i18n.t('patients.fullName'),
    fullNamePlaceholder: this.i18n.t('patients.fullNamePlaceholder'),
    gender: this.i18n.t('patients.gender'),
    genderPlaceholder: this.i18n.t('patients.genderPlaceholder'),
    genderMale: this.i18n.t('patients.genderMale'),
    genderFemale: this.i18n.t('patients.genderFemale'),
    birthDate: this.i18n.t('patients.birthDate'),
    phone: this.i18n.t('patients.phone'),
    phonePlaceholder: this.i18n.t('patients.phonePlaceholder'),
    city: this.i18n.t('patients.city'),
    cityPlaceholder: this.i18n.t('patients.cityPlaceholder'),
    district: this.i18n.t('patients.district'),
    districtPlaceholder: this.i18n.t('patients.districtPlaceholder'),
    address: this.i18n.t('patients.address'),
    addressPlaceholder: this.i18n.t('patients.addressPlaceholder'),
    emergencyContactName: this.i18n.t('patients.emergencyContactName'),
    emergencyContactNamePlaceholder: this.i18n.t('patients.emergencyContactNamePlaceholder'),
    emergencyContactPhone: this.i18n.t('patients.emergencyContactPhone'),
    emergencyContactPhonePlaceholder: this.i18n.t('patients.emergencyContactPhonePlaceholder'),
    emergencyContact: this.i18n.t('patients.emergencyContact'),
    allergies: this.i18n.t('patients.allergies'),
    allergiesPlaceholder: this.i18n.t('patients.allergiesPlaceholder'),
    medicalHistory: this.i18n.t('patients.medicalHistory'),
    medicalHistoryPlaceholder: this.i18n.t('patients.medicalHistoryPlaceholder'),
    cancel: this.i18n.t('common.cancel'),
    save: this.i18n.t('common.save'),
    saving: this.i18n.t('common.saving'),
  }));

  ngOnInit(): void {
    this.load();
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
    const value = (event.target as HTMLInputElement).value;
    this.searchQuery.set(value);
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((visible) => !visible);
    if (!this.showCreateForm()) {
      this.resetForm();
    }
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.resetForm();
  }

  viewDetail(patient: Patient): void {
    this.error.set(null);
    this.selectedPatient.set(null);
    this.consentRequiredPatient.set(null);
    this.loading.set(true);

    this.api.getById(patient.id).subscribe({
      next: (fullPatient) => {
        this.selectedPatient.set(fullPatient);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        if (err && err.status === 403) {
          this.consentRequiredPatient.set(patient);
        } else {
          this.error.set(err.error?.detail || err.error?.title || "Impossible de charger le dossier patient.");
        }
      }
    });
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
        this.emergencyError.set(err.error?.detail || err.error?.title || "Erreur lors du déclenchement de l'accès d'urgence.");
      }
    });
  }

  submit(): void {
    this.formError.set(null);
    if (
      !this.fullName() ||
      !this.gender() ||
      !this.birthDate() ||
      !this.phone() ||
      !this.city()
    ) {
      this.formError.set(this.i18n.t('patients.requiredFields'));
      return;
    }

    this.formLoading.set(true);
    this.api
      .create({
        fullName: this.fullName(),
        gender: this.gender(),
        birthDate: this.birthDate(),
        phone: this.phone(),
        city: this.city(),
        district: this.district() || undefined,
        address: this.address() || undefined,
        emergencyContactName: this.emergencyContactName() || undefined,
        emergencyContactPhone: this.emergencyContactPhone() || undefined,
        allergies: this.allergies() || undefined,
        medicalHistory: this.medicalHistory() || undefined,
      })
      .subscribe({
        next: (res) => {
          this.list.update((items) => [...items, res]);
          this.formLoading.set(false);
          this.showCreateForm.set(false);
          this.resetForm();
          // Directly open detail of the newly created patient
          this.viewDetail(res);
        },
        error: (err) => {
          let errorMsg = this.i18n.t('patients.saveError');
          if (err && err.status === 401) {
            errorMsg = this.i18n.t('common.error.unauthorized');
          } else if (err && err.status === 403) {
            errorMsg = this.i18n.t('common.error.forbidden');
          } else if (err && err.status >= 500) {
            errorMsg = this.i18n.t('common.error.server');
          } else if (err && err.error && err.error.detail) {
            errorMsg = err.error.detail;
          }
          this.formError.set(errorMsg);
          this.formLoading.set(false);
        },
      });
  }

  resetForm(): void {
    this.fullName.set('');
    this.gender.set('');
    this.birthDate.set('');
    this.phone.set('');
    this.city.set('');
    this.district.set('');
    this.address.set('');
    this.emergencyContactName.set('');
    this.emergencyContactPhone.set('');
    this.allergies.set('');
    this.medicalHistory.set('');
    this.formError.set(null);
  }
}

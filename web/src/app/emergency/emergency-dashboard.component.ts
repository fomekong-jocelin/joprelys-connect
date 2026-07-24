import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, HostListener, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AdmissionCompleted, UnifiedAdmissionComponent } from '../admission/unified-admission.component';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { EmergencyApiService } from './emergency-api.service';
import { EmergencyDocumentsPanelComponent } from './document/emergency-documents-panel.component';
import { EmergencyRecord } from './emergency.models';
import { EmergencyMedicoLegalPanelComponent } from './medico-legal/emergency-medico-legal-panel.component';
import { EmergencyTriagePanelComponent } from './triage/emergency-triage-panel.component';

type EmergencyDetailTab = 'OVERVIEW' | 'IDENTITY' | 'CARE' | 'LEGAL' | 'DOCUMENTS';

@Component({
  selector: 'app-emergency-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    DatePipe,
    AppShellComponent,
    PageHeaderComponent,
    ButtonComponent,
    AlertComponent,
    EmptyStateComponent,
    UnifiedAdmissionComponent,
    EmergencyDocumentsPanelComponent,
    EmergencyMedicoLegalPanelComponent,
    EmergencyTriagePanelComponent,
  ],
  templateUrl: './emergency-dashboard.component.html',
})
export class EmergencyDashboardComponent implements OnInit {
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  readonly emergencies = signal<EmergencyRecord[]>([]);
  readonly isLoading = signal(false);
  readonly isProcessing = signal(false);
  readonly isDetailLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly detailError = signal<string | null>(null);

  readonly isAdmissionModalOpen = signal(false);
  readonly selectedEmergency = signal<EmergencyRecord | null>(null);
  readonly isDrawerOpen = signal(false);
  readonly detailTab = signal<EmergencyDetailTab>('OVERVIEW');

  readonly canCreateEmergency = computed(() => this.rbacApi.hasPermission('EMERGENCY_WRITE'));
  readonly canCreatePatient = computed(() => this.rbacApi.hasPermission('PATIENT_WRITE'));

  careForm!: FormGroup;

  readonly isStabilizeModalOpen = signal(false);
  readonly stabilizeOrientation = signal<string>('ADMISSION');

  ngOnInit(): void {
    this.initCareForm();
    this.loadEmergencies();
  }

  @HostListener('document:keydown.escape')
  handleEscape(): void {
    if (this.isStabilizeModalOpen()) {
      this.closeStabilizeModal();
      return;
    }
    if (this.isAdmissionModalOpen()) {
      this.closeAdmissionModal();
      return;
    }
    if (this.isDrawerOpen()) {
      this.closeDrawer();
    }
  }

  loadEmergencies(): void {
    this.isLoading.set(true);
    this.emergencyApi.getActive().subscribe({
      next: data => {
        this.emergencies.set(data);
        this.isLoading.set(false);
        const selected = this.selectedEmergency();
        if (!selected) return;
        const updated = data.find(item => item.id === selected.id);
        if (updated) this.selectedEmergency.set(updated);
        else this.closeDrawer();
      },
      error: () => {
        this.error.set(this.t('emergency.error.load'));
        this.isLoading.set(false);
      },
    });
  }

  openAdmissionModal(): void {
    if (!this.canCreateEmergency()) {
      this.isAdmissionModalOpen.set(false);
      return;
    }
    this.isAdmissionModalOpen.set(true);
  }

  closeAdmissionModal(): void {
    this.isAdmissionModalOpen.set(false);
  }

  onAdmissionCompleted(result: AdmissionCompleted): void {
    this.closeAdmissionModal();
    if (result.carePath === 'EMERGENCY') {
      this.loadEmergencies();
      return;
    }
    void this.router.navigate(['/patients', result.patientId]);
  }

  openDrawer(record: EmergencyRecord): void {
    this.selectedEmergency.set(record);
    this.detailTab.set('OVERVIEW');
    this.detailError.set(null);
    this.initCareForm();
    this.isDrawerOpen.set(true);
    this.isDetailLoading.set(true);

    this.emergencyApi.getById(record.id).subscribe({
      next: detail => {
        this.selectedEmergency.set(detail);
        this.isDetailLoading.set(false);
      },
      error: () => {
        this.detailError.set(this.ui('detailsError'));
        this.isDetailLoading.set(false);
      },
    });
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedEmergency.set(null);
    this.detailError.set(null);
  }

  setDetailTab(tab: EmergencyDetailTab): void {
    this.detailTab.set(tab);
  }

  onSubmitCare(): void {
    const record = this.selectedEmergency();
    if (!record || this.careForm.invalid) {
      this.careForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    this.emergencyApi.addResuscitationLog(record.id, this.careForm.value).subscribe({
      next: () => {
        this.careForm.get('description')?.reset();
        this.careForm.get('quantity')?.reset();
        this.isProcessing.set(false);
        this.reloadSelectedEmergency(record.id);
      },
      error: () => {
        this.error.set(this.t('emergency.error.addCare'));
        this.isProcessing.set(false);
      },
    });
  }

  openStabilizeModal(): void {
    this.isStabilizeModalOpen.set(true);
  }

  closeStabilizeModal(): void {
    this.isStabilizeModalOpen.set(false);
  }

  onSubmitStabilize(): void {
    const record = this.selectedEmergency();
    if (!record) return;

    const orientation = this.stabilizeOrientation();
    this.isProcessing.set(true);
    this.emergencyApi.stabilize(record.id, orientation).subscribe({
      next: () => {
        this.isProcessing.set(false);
        this.closeStabilizeModal();
        this.closeDrawer();
        if (orientation === 'ADMISSION' || orientation === 'OR_DIRECT') {
          this.continueToHospitalization(record);
          return;
        }
        this.loadEmergencies();
      },
      error: () => {
        this.error.set(this.t('emergency.error.stabilize'));
        this.isProcessing.set(false);
      },
    });
  }

  continueToHospitalization(record: EmergencyRecord): void {
    this.closeDrawer();
    void this.router.navigate(
      ['/patients', record.patientId, 'hospitalizations'],
      { queryParams: { emergencyId: record.id } },
    );
  }

  openReconciliation(record: EmergencyRecord): void {
    this.closeDrawer();
    void this.router.navigate(
      ['/clinic/patient-reconciliation'],
      { queryParams: { patientId: record.patientId, emergencyId: record.id } },
    );
  }

  patientDisplayName(record: EmergencyRecord): string {
    return record.patientName
      || record.temporaryPatientNumber
      || record.globalPatientNumber
      || this.ui('unknown');
  }

  patientPrimaryNumber(record: EmergencyRecord): string {
    return record.temporaryPatientNumber || record.globalPatientNumber || record.localPatientNumber;
  }

  isProvisional(record: EmergencyRecord): boolean {
    return record.identityStatus === 'PROVISIONAL_URGENCY' || Boolean(record.temporaryPatientNumber);
  }

  arrivalLabel(mode: string): string {
    return this.t(`emergency.detail.arrival.${mode.toLowerCase()}`, mode);
  }

  triageLabel(level: string): string {
    return this.t(`emergency.detail.triage.${level.toLowerCase()}`, level);
  }

  hemodynamicLabel(status: string): string {
    return this.t(`emergency.detail.hemodynamic.${status.toLowerCase()}`, status);
  }

  identityStatusLabel(status?: string): string {
    return status
      ? this.t(`emergency.detail.identityStatus.${status.toLowerCase()}`, status)
      : this.ui('unknown');
  }

  confidenceLabel(confidence?: string): string {
    return confidence
      ? this.t(`emergency.detail.confidence.${confidence.toLowerCase()}`, confidence)
      : this.ui('unknown');
  }

  valueOrUnknown(value?: string | null): string {
    return value?.trim() || this.ui('unknown');
  }

  ui(key: string): string {
    return this.t(`emergency.detail.${key}`);
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  private initCareForm(): void {
    this.careForm = this.fb.group({
      actionType: ['VASCULAR_ACCESS', [Validators.required]],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      quantity: [null],
      unit: [''],
    });

    this.careForm.get('actionType')?.valueChanges.subscribe(type => {
      const quantity = this.careForm.get('quantity');
      if (type === 'FLUID_BOLUS') {
        quantity?.setValidators([Validators.required, Validators.min(1)]);
        this.careForm.patchValue({ unit: 'ml' }, { emitEvent: false });
      } else if (type === 'MEDICATION') {
        quantity?.setValidators([Validators.required, Validators.min(0.01)]);
        this.careForm.patchValue({ unit: 'mg' }, { emitEvent: false });
      } else {
        quantity?.clearValidators();
        quantity?.setValue(null, { emitEvent: false });
        this.careForm.patchValue({ unit: '' }, { emitEvent: false });
      }
      quantity?.updateValueAndValidity({ emitEvent: false });
    });
  }

  private reloadSelectedEmergency(emergencyId: string): void {
    this.emergencyApi.getById(emergencyId).subscribe({
      next: detail => {
        this.selectedEmergency.set(detail);
        this.emergencies.update(items => items.map(item => item.id === detail.id ? detail : item));
      },
      error: () => this.detailError.set(this.ui('detailsError')),
    });
  }
}

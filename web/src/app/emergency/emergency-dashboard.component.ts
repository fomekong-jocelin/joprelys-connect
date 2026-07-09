import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { EmergencyApiService } from './emergency-api.service';
import { PatientApiService } from '../patient/patient-api.service';
import { EmergencyRecord, ResuscitationLog } from './emergency.models';
import { Patient } from '../patient/patient.models';
import { I18nService } from '../core/i18n/i18n.service';

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
  ],
  templateUrl: './emergency-dashboard.component.html',
})
export class EmergencyDashboardComponent implements OnInit {
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly patientApi = inject(PatientApiService);
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  emergencies = signal<EmergencyRecord[]>([]);
  patients = signal<Patient[]>([]);
  isLoading = signal(false);
  isProcessing = signal(false);
  error = signal<string | null>(null);

  // New Admission Modal
  isAdmissionModalOpen = signal(false);
  admissionForm!: FormGroup;

  // Drawer for patient details
  selectedEmergency = signal<EmergencyRecord | null>(null);
  isDrawerOpen = signal(false);

  // Add Care Form (inside drawer)
  careForm!: FormGroup;

  // Stabilize Modal
  isStabilizeModalOpen = signal(false);
  stabilizeOrientation = signal<string>('ADMISSION');

  ngOnInit(): void {
    this.initForms();
    this.loadEmergencies();
    this.loadPatients();
  }

  private initForms(): void {
    this.admissionForm = this.fb.group({
      patientId: ['', [Validators.required]],
      arrivalMode: ['AMBULANCE', [Validators.required]],
      triageLevel: ['RED', [Validators.required]],
      hemodynamicStatus: ['SHOCK', [Validators.required]],
      chiefComplaint: ['', [Validators.required]],
      initialBpSystolic: [null, [Validators.min(30), Validators.max(300)]],
      initialBpDiastolic: [null, [Validators.min(20), Validators.max(200)]],
      initialHr: [null, [Validators.min(20), Validators.max(250)]],
      initialTemp: [null, [Validators.min(30), Validators.max(45)]],
    });

    this.careForm = this.fb.group({
      actionType: ['VASCULAR_ACCESS', [Validators.required]],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      quantity: [null],
      unit: [''],
    });

    // Auto-complete unit, set dynamic validators, and clear quantity based on action type
    this.careForm.get('actionType')?.valueChanges.subscribe(type => {
      let unit = '';
      const qtyControl = this.careForm.get('quantity');

      if (type === 'FLUID_BOLUS') {
        unit = 'ml';
        qtyControl?.setValidators([Validators.required, Validators.min(1)]);
      } else if (type === 'MEDICATION') {
        unit = 'mg';
        qtyControl?.setValidators([Validators.required, Validators.min(0.01)]);
      } else {
        qtyControl?.clearValidators();
        qtyControl?.setValue(null);
      }
      qtyControl?.updateValueAndValidity();
      this.careForm.patchValue({ unit });
    });
  }

  loadEmergencies(): void {
    this.isLoading.set(true);
    this.emergencyApi.getActive().subscribe({
      next: (data) => {
        this.emergencies.set(data);
        this.isLoading.set(false);
        // Refresh selected emergency if drawer is open
        const selected = this.selectedEmergency();
        if (selected) {
          const updated = data.find(x => x.id === selected.id);
          if (updated) {
            this.selectedEmergency.set(updated);
          } else {
            this.closeDrawer();
          }
        }
      },
      error: () => {
        this.error.set(this.t('emergency.error.load'));
        this.isLoading.set(false);
      },
    });
  }

  private loadPatients(): void {
    this.patientApi.list().subscribe({
      next: (data) => this.patients.set(data),
      error: () => {},
    });
  }

  openAdmissionModal(): void {
    this.initForms();
    this.isAdmissionModalOpen.set(true);
  }

  closeAdmissionModal(): void {
    this.isAdmissionModalOpen.set(false);
  }

  onSubmitAdmission(): void {
    if (this.admissionForm.invalid) {
      this.admissionForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const dto = this.admissionForm.value;

    this.emergencyApi.create(dto).subscribe({
      next: () => {
        this.loadEmergencies();
        this.closeAdmissionModal();
        this.isProcessing.set(false);
      },
      error: (err) => {
        const msg = err.error?.message || this.t('emergency.error.create');
        this.error.set(msg);
        this.isProcessing.set(false);
      },
    });
  }

  openDrawer(record: EmergencyRecord): void {
    this.selectedEmergency.set(record);
    this.initForms();
    this.isDrawerOpen.set(true);
  }

  closeDrawer(): void {
    this.isDrawerOpen.set(false);
    this.selectedEmergency.set(null);
  }

  onSubmitCare(): void {
    const record = this.selectedEmergency();
    if (!record || this.careForm.invalid) {
      this.careForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const dto = this.careForm.value;

    this.emergencyApi.addResuscitationLog(record.id, dto).subscribe({
      next: () => {
        this.loadEmergencies(); // will reload selected log and update drawer
        this.careForm.get('description')?.reset();
        this.careForm.get('quantity')?.reset();
        this.isProcessing.set(false);
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

    this.isProcessing.set(true);
    this.emergencyApi.stabilize(record.id, this.stabilizeOrientation()).subscribe({
      next: () => {
        this.loadEmergencies();
        this.closeStabilizeModal();
        this.closeDrawer();
        this.isProcessing.set(false);
      },
      error: () => {
        this.error.set(this.t('emergency.error.stabilize'));
        this.isProcessing.set(false);
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}

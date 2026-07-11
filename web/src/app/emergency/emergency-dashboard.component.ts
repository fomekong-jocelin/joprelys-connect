import { CommonModule, DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AdmissionCompleted, UnifiedAdmissionComponent } from '../admission/unified-admission.component';
import { I18nService } from '../core/i18n/i18n.service';
import { AlertComponent } from '../shared/ui/alert.component';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { EmergencyApiService } from './emergency-api.service';
import { EmergencyRecord } from './emergency.models';

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
  ],
  templateUrl: './emergency-dashboard.component.html',
})
export class EmergencyDashboardComponent implements OnInit {
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  readonly i18n = inject(I18nService);

  emergencies = signal<EmergencyRecord[]>([]);
  isLoading = signal(false);
  isProcessing = signal(false);
  error = signal<string | null>(null);

  isAdmissionModalOpen = signal(false);

  selectedEmergency = signal<EmergencyRecord | null>(null);
  isDrawerOpen = signal(false);

  careForm!: FormGroup;

  isStabilizeModalOpen = signal(false);
  stabilizeOrientation = signal<string>('ADMISSION');

  ngOnInit(): void {
    this.initCareForm();
    this.loadEmergencies();
  }

  private initCareForm(): void {
    this.careForm = this.fb.group({
      actionType: ['VASCULAR_ACCESS', [Validators.required]],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      quantity: [null],
      unit: [''],
    });

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

  openAdmissionModal(): void {
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
    this.initCareForm();
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
        this.loadEmergencies();
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

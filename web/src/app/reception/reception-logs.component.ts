import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { ReceptionApiService } from './reception-api.service';
import { PatientApiService } from '../patient/patient-api.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { ReceptionLog } from './reception.models';
import { Patient } from '../patient/patient.models';
import { StaffMember } from '../clinic/staff/staff.models';
import { I18nService } from '../core/i18n/i18n.service';

@Component({
  selector: 'app-reception-logs',
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
  templateUrl: './reception-logs.component.html',
})
export class ReceptionLogsComponent implements OnInit {
  private readonly receptionApi = inject(ReceptionApiService);
  private readonly patientApi = inject(PatientApiService);
  private readonly staffApi = inject(StaffApiService);
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  logs = signal<ReceptionLog[]>([]);
  patients = signal<Patient[]>([]);
  staff = signal<StaffMember[]>([]);
  isLoading = signal(false);
  isProcessing = signal(false);
  error = signal<string | null>(null);

  // Modal control
  isModalOpen = signal(false);
  logForm!: FormGroup;

  // Filter
  filterType = signal<string>('ALL');

  filteredLogs = computed(() => {
    const type = this.filterType();
    const allLogs = this.logs();
    if (type === 'ALL') {
      return allLogs;
    }
    return allLogs.filter(l => l.logType === type);
  });

  ngOnInit(): void {
    this.initForm();
    this.loadLogs();
    this.loadDropdowns();
  }

  private initForm(): void {
    this.logForm = this.fb.group({
      logType: ['VISITOR', [Validators.required]],
      firstName: ['', [Validators.required, Validators.maxLength(100)]],
      lastName: ['', [Validators.required, Validators.maxLength(100)]],
      idDocumentType: ['CNI'],
      idDocumentNumber: [''],
      targetPatientId: [''],
      targetStaffId: [''],
      reason: [''],
    });

    // Reset targets on type change
    this.logForm.get('logType')?.valueChanges.subscribe((type) => {
      this.logForm.patchValue({
        targetPatientId: '',
        targetStaffId: '',
        idDocumentType: type === 'PATIENT' ? '' : 'CNI',
        idDocumentNumber: '',
      });
    });
  }

  loadLogs(): void {
    this.isLoading.set(true);
    this.receptionApi.getAll().subscribe({
      next: (data) => {
        this.logs.set(data.sort((a, b) => b.arrivalAt.localeCompare(a.arrivalAt)));
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('reception.error.loadLogs'));
        this.isLoading.set(false);
      },
    });
  }

  private loadDropdowns(): void {
    this.patientApi.list().subscribe({
      next: (data) => this.patients.set(data),
      error: () => {},
    });

    this.staffApi.list().subscribe({
      next: (data) => this.staff.set(data),
      error: () => {},
    });
  }

  openModal(): void {
    this.initForm();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
  }

  onSubmit(): void {
    if (this.logForm.invalid) {
      this.logForm.markAllAsTouched();
      return;
    }

    this.isProcessing.set(true);
    const formValue = this.logForm.value;

    // Clean up empty strings
    const dto = {
      ...formValue,
      idDocumentType: formValue.idDocumentType || undefined,
      idDocumentNumber: formValue.idDocumentNumber || undefined,
      targetPatientId: formValue.targetPatientId || undefined,
      targetStaffId: formValue.targetStaffId || undefined,
      reason: formValue.reason || undefined,
    };

    this.receptionApi.create(dto).subscribe({
      next: () => {
        this.loadLogs();
        this.closeModal();
        this.isProcessing.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('reception.error.create'));
        this.isProcessing.set(false);
      },
    });
  }

  recordDeparture(log: ReceptionLog): void {
    this.isProcessing.set(true);
    this.receptionApi.markDeparture(log.id).subscribe({
      next: () => {
        this.loadLogs();
        this.isProcessing.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('reception.error.departure'));
        this.isProcessing.set(false);
      },
    });
  }

  getPatientName(patientId?: string): string {
    if (!patientId) return '';
    const p = this.patients().find(x => x.id === patientId);
    return p ? p.fullName : '';
  }

  getStaffName(staffId?: string): string {
    if (!staffId) return '';
    const s = this.staff().find(x => x.id === staffId);
    return s ? s.displayName : '';
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}

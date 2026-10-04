import { ConfirmationDialogComponent } from '../shared/ui/confirmation-dialog.component';
import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalOperatingReport, SurgicalImplant, HospitalPractitioner } from './hospitalization-workflow.models';
@Component({
  selector: 'app-hospitalization-operating-report-panel', standalone: true, imports: [CommonModule, FormsModule, ConfirmationDialogComponent],
  templateUrl: './hospitalization-operating-report-panel.component.html',
})
export class HospitalizationOperatingReportPanelComponent implements OnChanges {
  @Input({required:true}) hospitalizationId!: string;
  @Input() canModify = false;
  @Input() staffList: HospitalPractitioner[] = [];
  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback?: string) => this.i18n.t(key, fallback);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly pendingValidation = signal<string | null>(null);
  readonly operatingReports = signal<HospitalOperatingReport[]>([]);
  readonly showAddReport = signal(false);
  procedureName = '';
  procedureDescription = '';
  preOperativeDiagnosis = '';
  postOperativeDiagnosis = '';
  anesthesiaType = 'GÉNÉRALE';
  anesthesiaDescription = '';
  kSurgeonValue = 0.0;
  kAnesthesistValue = 0.0;
  kBlocValue = 0.0;
  surgeonId = '';
  anesthetistId = '';
  readonly implantsInForm = signal<SurgicalImplant[]>([]);

  newImplantName = '';
  newImplantLot = '';
  newImplantQty = 1;
  newImplantPrice = 0.0;
  newImplantManufacturer = '';


  anesthesiaLabelKey(type: string): string {
    const keys: Record<string, string> = { 'GÉNÉRALE': 'general', 'LOCORÉGIONALE': 'regional', 'LOCALE': 'local', 'SÉDATION': 'sedation', 'AUTRE': 'other' };
    return 'patients.hospitalization.workflow.' + (keys[type] ?? 'unspecified');
  }
  ngOnChanges(): void { if (this.hospitalizationId) this.load(); }
  load(): void {
    this.loading.set(true); this.error.set(null);
    this.patientApi.getOperatingReports(this.hospitalizationId).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => this.operatingReports.set(data),
      error: () => this.error.set(this.t('patients.hospitalization.workflow.loadError')),
    });
  }
  addImplantToList(): void {
    if (!this.newImplantName.trim()) return;
    this.implantsInForm.update((list) => [...list, {
      implantName: this.newImplantName.trim(),
      lotNumber: this.newImplantLot.trim(),
      quantity: this.newImplantQty,
      unitPrice: this.newImplantPrice,
      manufacturer: this.newImplantManufacturer.trim(),
    }]);
    this.newImplantName = '';
    this.newImplantLot = '';
    this.newImplantQty = 1;
    this.newImplantPrice = 0.0;
    this.newImplantManufacturer = '';
  }

  removeImplantFromList(index: number): void {
    this.implantsInForm.update((list) => list.filter((_, itemIndex) => itemIndex !== index));
  }

  saveOperatingReport(event: Event): void {
    event.preventDefault();
    const active = { id: this.hospitalizationId };
    if (this.saving() || !active || !this.canModify || !this.procedureName.trim()) return;

    this.saving.set(true); this.error.set(null);
    this.patientApi.createOperatingReport(active.id, {
      procedureName: this.procedureName.trim(),
      procedureDescription: this.procedureDescription.trim(),
      preOperativeDiagnosis: this.preOperativeDiagnosis.trim(),
      postOperativeDiagnosis: this.postOperativeDiagnosis.trim(),
      anesthesiaType: this.anesthesiaType.trim(),
      anesthesiaDescription: this.anesthesiaDescription.trim(),
      kSurgeonValue: this.kSurgeonValue,
      kAnesthesistValue: this.kAnesthesistValue,
      kBlocValue: this.kBlocValue,
      surgeonId: this.surgeonId || null,
      anesthetistId: this.anesthetistId || null,
      implants: this.implantsInForm(),
    }).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.showAddReport.set(false);
        this.procedureName = '';
        this.procedureDescription = '';
        this.preOperativeDiagnosis = '';
        this.postOperativeDiagnosis = '';
        this.anesthesiaType = 'GÉNÉRALE';
        this.anesthesiaDescription = '';
        this.kSurgeonValue = 0.0;
        this.kAnesthesistValue = 0.0;
        this.kBlocValue = 0.0;
        this.surgeonId = '';
        this.anesthetistId = '';
        this.implantsInForm.set([]);
        this.load();
      },
      error: (error) => {
        this.error.set(this.t('patients.hospitalization.workflow.saveError'));
      },
    });
  }

  validateReport(reportId: string): void {
    if (!this.canModify || this.saving()) return;
    this.pendingValidation.set(reportId);
  }

  confirmValidation(): void {
    const reportId = this.pendingValidation();
    if (!reportId || !this.canModify || this.saving()) return;
    this.saving.set(true); this.error.set(null);
    this.patientApi.validateOperatingReport(reportId).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.pendingValidation.set(null);
        const active = { id: this.hospitalizationId };
        if (active) this.load();
      },
      error: (error) => {
        this.error.set(this.t('patients.hospitalization.workflow.saveError'));
      },
    });
  }
}

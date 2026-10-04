import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalConsent, HospitalPractitioner } from './hospitalization-workflow.models';
@Component({
  selector: 'app-hospitalization-consent-panel', standalone: true, imports: [CommonModule, FormsModule],
  templateUrl: './hospitalization-consent-panel.component.html',
})
export class HospitalizationConsentPanelComponent implements OnChanges {
  @Input({required:true}) hospitalizationId!: string;
  @Input() canModify = false;
  private readonly patientApi = inject(PatientApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback?: string) => this.i18n.t(key, fallback);
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly consents = signal<HospitalConsent[]>([]);
  readonly showAddConsent = signal(false);
  consentType = 'ANESTHESIA';
  patientSignaturePresent = false;
  witnessName = '';
  consentFile: File | null = null;


  ngOnChanges(): void { if (this.hospitalizationId) this.load(); }
  load(): void {
    this.loading.set(true); this.error.set(null);
    this.patientApi.getConsents(this.hospitalizationId).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: data => this.consents.set(data),
      error: () => this.error.set(this.t('patients.hospitalization.workflow.loadError')),
    });
  }
  onConsentFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.consentFile = input.files && input.files.length > 0 ? input.files[0] : null;
  }

  saveConsent(event: Event): void {
    event.preventDefault();
    const active = { id: this.hospitalizationId };
    if (this.saving() || !active || !this.canModify) return;

    const formData = new FormData();
    formData.append('consentType', this.consentType);
    formData.append('patientSignaturePresent', String(this.patientSignaturePresent));
    formData.append('witnessName', this.witnessName.trim());
    if (this.consentFile) formData.append('file', this.consentFile);

    this.saving.set(true); this.error.set(null);
    this.patientApi.addConsent(active.id, formData).pipe(finalize(() => this.saving.set(false))).subscribe({
      next: () => {
        this.showAddConsent.set(false);
        this.consentFile = null;
        this.witnessName = '';
        this.patientSignaturePresent = false;
        this.load();
      },
      error: (error) => {
        this.error.set(this.t('patients.hospitalization.workflow.saveError'));
      },
    });
  }

  downloadConsentFile(documentId: string): void {
    window.open(`/api/documents/${documentId}/download`, '_blank');
  }


}

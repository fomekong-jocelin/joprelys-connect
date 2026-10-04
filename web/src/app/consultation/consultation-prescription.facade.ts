import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { Injectable, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, Validators } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { ConsultationApiService } from './consultation-api.service';
import { ConsultationFeedbackStore } from './consultation-feedback.store';
import { Prescription } from './consultation.models';

@Injectable()
export class ConsultationPrescriptionFacade {
  private readonly formBuilder = inject(FormBuilder);
  private readonly rbac = inject(RbacApiService);
  canWrite(): boolean { return this.rbac.hasPermission('PRESCRIPTION_WRITE'); }
  canSign(): boolean { return this.rbac.hasPermission('PRESCRIPTION_SIGN'); }
  private readonly consultationApi = inject(ConsultationApiService);
  private readonly i18n = inject(I18nService);
  private readonly feedback = inject(ConsultationFeedbackStore);

  readonly current = signal<Prescription | null>(null);
  readonly items: FormArray = this.formBuilder.array([]);

  load(consultationId: string): void {
    let hasPrescription = false;
    this.consultationApi.getPrescription(consultationId).subscribe({
      next: (prescription) => {
        hasPrescription = true;
        this.current.set(prescription);
        this.items.clear();
        prescription.items.forEach((item) => {
          const isReadonly = prescription.status !== 'DRAFT' || !this.canWrite();
          this.items.push(
            this.formBuilder.group({
              drugName: [{ value: item.drugName, disabled: isReadonly }, Validators.required],
              dosage: [{ value: item.dosage, disabled: isReadonly }, Validators.required],
              posology: [{ value: item.posology || '', disabled: isReadonly }],
              duration: [{ value: item.duration || '', disabled: isReadonly }],
              quantity: [{ value: item.quantity || '', disabled: isReadonly }],
              instructions: [{ value: item.instructions || '', disabled: isReadonly }],
              form: [{ value: item.form || '', disabled: isReadonly }],
              route: [{ value: item.route || '', disabled: isReadonly }],
              frequency: [{ value: item.frequency || '', disabled: isReadonly }],
              substitutionAllowed: [
                { value: item.substitutionAllowed !== false, disabled: isReadonly },
              ],
            }),
          );
        });
      },
      error: () => {
        this.current.set(null);
        this.items.clear();
      },
      complete: () => {
        if (hasPrescription) return;
        this.current.set(null);
        this.items.clear();
      },
    });
  }

  setCurrent(prescription: Prescription): void {
    this.current.set(prescription);
  }

  addLine(): void {
    if (!this.canWrite()) return;
    if (this.current()?.status !== undefined && this.current()?.status !== 'DRAFT') return;
    this.items.push(
      this.formBuilder.group({
        drugName: ['', Validators.required],
        dosage: ['', Validators.required],
        posology: [''],
        duration: [''],
        quantity: [''],
        instructions: [''],
        form: [''],
        route: [''],
        frequency: [''],
        substitutionAllowed: [true],
      }),
    );
  }

  removeLine(index: number): void {
    if (!this.canWrite()) return;
    if (this.current()?.status !== undefined && this.current()?.status !== 'DRAFT') return;
    this.items.removeAt(index);
  }

  finalize(): void {
    if (!this.canSign()) return;
    const prescription = this.current();
    if (!prescription) return;
    this.feedback.isLoading.set(true);
    this.consultationApi.finalizePrescription(prescription.id).subscribe({
      next: () => {
        this.feedback.isLoading.set(false);
        this.feedback.successMessage.set(this.i18n.t('consultation.prescription.finalizedSuccess'));
        this.load(prescription.consultationId);
      },
      error: (error) => {
        this.feedback.isLoading.set(false);
        this.feedback.errorMessage.set(
          error.error?.detail || this.i18n.t('consultation.errors.finalizePrescription'),
        );
      },
    });
  }

  confirmAndCancel(): void {
    if (!this.canSign()) return;
    const prescription = this.current();
    if (!prescription) return;
    if (!confirm(this.i18n.t('consultation.prescription.confirmCancel'))) return;

    this.feedback.isLoading.set(true);
    this.consultationApi.cancelPrescription(prescription.id).subscribe({
      next: () => {
        this.feedback.isLoading.set(false);
        this.feedback.successMessage.set(this.i18n.t('consultation.prescription.cancelledSuccess'));
        this.load(prescription.consultationId);
      },
      error: (error) => {
        this.feedback.isLoading.set(false);
        this.feedback.errorMessage.set(
          error.error?.detail || this.i18n.t('consultation.errors.cancelPrescription'),
        );
      },
    });
  }

  downloadPdf(): void {
    const prescription = this.current();
    if (!prescription?.documentId) return;
    this.feedback.isLoading.set(true);
    this.consultationApi.downloadDocumentById(prescription.documentId).subscribe({
      next: (blob) => {
        this.feedback.isLoading.set(false);
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `ordonnance-${prescription.prescriptionNumber}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.feedback.isLoading.set(false);
        this.feedback.errorMessage.set(this.i18n.t('consultation.errors.downloadPrescription'));
      },
    });
  }

  statusLabel(status?: string | null): string {
    if (!status) return '';
    const keyByStatus: Record<string, string> = {
      DRAFT: 'consultation.prescription.statusDraft',
      ACTIVE: 'consultation.prescription.statusActive',
      CANCELLED: 'consultation.prescription.statusCancelled',
      EXPIRED: 'consultation.prescription.statusExpired',
    };
    return this.i18n.t(keyByStatus[status] ?? 'consultation.prescription.statusUnknown');
  }

  statusClasses(status?: string | null): string {
    if (status === 'ACTIVE') {
      return 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    }
    if (status === 'DRAFT') {
      return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
    }
    return 'border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)]';
  }
}

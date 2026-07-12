import { Component, inject, OnInit, signal, viewChild } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { PatientReconciliationApiService } from './patient-reconciliation-api.service';
import { PatientReconciliationDecisionComponent } from './patient-reconciliation-decision.component';
import {
  PatientReconciliationCandidate,
  PatientReconciliationDecisionDto,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';
import { PatientReconciliationQueueComponent } from './patient-reconciliation-queue.component';

@Component({
  selector: 'app-patient-reconciliation-page',
  standalone: true,
  imports: [
    AppShellComponent,
    AlertComponent,
    PatientReconciliationQueueComponent,
    PatientReconciliationDecisionComponent,
  ],
  templateUrl: './patient-reconciliation-page.component.html',
})
export class PatientReconciliationPageComponent implements OnInit {
  private readonly api = inject(PatientReconciliationApiService);
  private readonly apiErrors = inject(ApiErrorI18nService);
  private readonly i18n = inject(I18nService);

  private readonly decisionForm = viewChild(PatientReconciliationDecisionComponent);

  readonly queue = signal<PatientReconciliationQueueItem[]>([]);
  readonly selectedPatient = signal<PatientReconciliationQueueItem | null>(null);
  readonly candidates = signal<PatientReconciliationCandidate[]>([]);
  readonly loadingQueue = signal(false);
  readonly loadingCandidates = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  ngOnInit(): void {
    this.loadQueue();
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  selectPatient(patient: PatientReconciliationQueueItem): void {
    if (this.saving()) return;
    this.selectedPatient.set(patient);
    this.candidates.set([]);
    this.error.set(null);
    this.success.set(null);
    this.loadCandidates(patient.patientId);
  }

  submitDecision(dto: PatientReconciliationDecisionDto): void {
    const patient = this.selectedPatient();
    if (!patient || this.saving()) return;

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.decide(patient.patientId, dto, this.createIdempotencyKey()).pipe(
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: (result) => {
        this.success.set(this.t(`patientReconciliation.success.${result.decision.toLowerCase()}`));
        this.decisionForm()?.reset();
        this.selectedPatient.set(null);
        this.candidates.set([]);
        this.loadQueue();
      },
      error: (error) => this.error.set(this.apiErrors.message(
        error,
        'patientReconciliation.error',
        'patientReconciliation.error.save',
      )),
    });
  }

  refresh(): void {
    if (!this.loadingQueue() && !this.saving()) {
      this.loadQueue();
    }
  }

  private loadQueue(): void {
    this.loadingQueue.set(true);
    this.error.set(null);
    this.api.getQueue().pipe(
      finalize(() => this.loadingQueue.set(false)),
    ).subscribe({
      next: (items) => this.queue.set(items),
      error: (error) => this.error.set(this.apiErrors.message(
        error,
        'patientReconciliation.error',
        'patientReconciliation.error.loadQueue',
      )),
    });
  }

  private loadCandidates(patientId: string): void {
    this.loadingCandidates.set(true);
    this.api.getCandidates(patientId).pipe(
      finalize(() => this.loadingCandidates.set(false)),
    ).subscribe({
      next: (items) => this.candidates.set(items),
      error: (error) => this.error.set(this.apiErrors.message(
        error,
        'patientReconciliation.error',
        'patientReconciliation.error.loadCandidates',
      )),
    });
  }

  private createIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `patient-reconciliation-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}

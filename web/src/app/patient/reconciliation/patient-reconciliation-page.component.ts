import { Component, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { finalize, Subscription } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { PatientReconciliationApiService } from './patient-reconciliation-api.service';
import { PatientReconciliationDecisionComponent } from './patient-reconciliation-decision.component';
import {
  PatientReconciliationCandidate,
  PatientReconciliationDecisionDto,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';
import { PatientReconciliationQueueComponent } from './patient-reconciliation-queue.component';

interface PendingDecisionSubmission {
  fingerprint: string;
  idempotencyKey: string;
}

@Component({
  selector: 'app-patient-reconciliation-page',
  standalone: true,
  imports: [
    AlertComponent,
    PatientReconciliationQueueComponent,
    PatientReconciliationDecisionComponent,
  ],
  templateUrl: './patient-reconciliation-page.component.html',
})
export class PatientReconciliationPageComponent implements OnInit, OnDestroy {
  private readonly api = inject(PatientReconciliationApiService);
  private readonly apiErrors = inject(ApiErrorI18nService);
  private readonly i18n = inject(I18nService);

  private readonly decisionForm = viewChild(PatientReconciliationDecisionComponent);
  private candidateRequest: Subscription | null = null;
  private candidateRequestVersion = 0;
  private pendingDecisionSubmission: PendingDecisionSubmission | null = null;

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

  ngOnDestroy(): void {
    this.cancelCandidateRequest();
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  selectPatient(patient: PatientReconciliationQueueItem): void {
    if (this.saving()) return;

    this.cancelCandidateRequest();
    this.selectedPatient.set(patient);
    this.candidates.set([]);
    this.error.set(null);
    this.success.set(null);
    this.loadCandidates(patient.patientId);
  }

  submitDecision(dto: PatientReconciliationDecisionDto): void {
    const patient = this.selectedPatient();
    if (!patient || this.saving()) return;

    const fingerprint = this.createDecisionFingerprint(patient.patientId, dto);
    const idempotencyKey = this.resolveIdempotencyKey(fingerprint);

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.decide(patient.patientId, dto, idempotencyKey).pipe(
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: (result) => {
        this.pendingDecisionSubmission = null;
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
    const requestVersion = ++this.candidateRequestVersion;
    this.loadingCandidates.set(true);

    this.candidateRequest = this.api.getCandidates(patientId).pipe(
      finalize(() => {
        if (requestVersion === this.candidateRequestVersion) {
          this.loadingCandidates.set(false);
        }
      }),
    ).subscribe({
      next: (items) => {
        if (this.isCurrentCandidateRequest(patientId, requestVersion)) {
          this.candidates.set(items);
        }
      },
      error: (error) => {
        if (this.isCurrentCandidateRequest(patientId, requestVersion)) {
          this.error.set(this.apiErrors.message(
            error,
            'patientReconciliation.error',
            'patientReconciliation.error.loadCandidates',
          ));
        }
      },
    });
  }

  private cancelCandidateRequest(): void {
    this.candidateRequestVersion += 1;
    this.candidateRequest?.unsubscribe();
    this.candidateRequest = null;
    this.loadingCandidates.set(false);
  }

  private isCurrentCandidateRequest(patientId: string, requestVersion: number): boolean {
    return requestVersion === this.candidateRequestVersion
      && this.selectedPatient()?.patientId === patientId;
  }

  private resolveIdempotencyKey(fingerprint: string): string {
    if (this.pendingDecisionSubmission?.fingerprint === fingerprint) {
      return this.pendingDecisionSubmission.idempotencyKey;
    }

    const idempotencyKey = this.createIdempotencyKey();
    this.pendingDecisionSubmission = { fingerprint, idempotencyKey };
    return idempotencyKey;
  }

  private createDecisionFingerprint(
    patientId: string,
    dto: PatientReconciliationDecisionDto,
  ): string {
    return JSON.stringify({
      patientId,
      decision: dto.decision,
      candidatePatientId: dto.candidatePatientId ?? null,
      evidenceSourceType: dto.evidenceSourceType,
      evidenceReference: dto.evidenceReference?.trim() || null,
      justification: dto.justification.trim(),
    });
  }

  private createIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `patient-reconciliation-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}

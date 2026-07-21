import { Component, inject, OnDestroy, OnInit, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyApiService } from '../../emergency/emergency-api.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { PatientReconciliationApiService } from './patient-reconciliation-api.service';
import { PatientReconciliationCorrectionComponent } from './patient-reconciliation-correction.component';
import { PatientReconciliationDecisionComponent } from './patient-reconciliation-decision.component';
import { PatientReconciliationHistoryComponent } from './patient-reconciliation-history.component';
import {
  PatientReconciliationCandidate,
  PatientReconciliationCorrectionDto,
  PatientReconciliationDecisionDto,
  PatientReconciliationDecisionResult,
  PatientReconciliationEvent,
  PatientReconciliationQueueItem,
} from './patient-reconciliation.models';
import { PatientReconciliationQueueComponent } from './patient-reconciliation-queue.component';

type SubmissionKind = 'decision' | 'correction';

interface PendingSubmission {
  fingerprint: string;
  idempotencyKey: string;
}

interface CompletedJourney {
  result: PatientReconciliationDecisionResult;
  emergencyId: string | null;
  loadingEmergency: boolean;
}

@Component({
  selector: 'app-patient-reconciliation-page',
  standalone: true,
  imports: [
    AppShellComponent,
    AlertComponent,
    PatientReconciliationQueueComponent,
    PatientReconciliationDecisionComponent,
    PatientReconciliationHistoryComponent,
    PatientReconciliationCorrectionComponent,
  ],
  templateUrl: './patient-reconciliation-page.component.html',
})
export class PatientReconciliationPageComponent implements OnInit, OnDestroy {
  private readonly api = inject(PatientReconciliationApiService);
  private readonly emergencyApi = inject(EmergencyApiService);
  private readonly apiErrors = inject(ApiErrorI18nService);
  private readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  private readonly decisionForm = viewChild(PatientReconciliationDecisionComponent);
  private readonly correctionForm = viewChild(PatientReconciliationCorrectionComponent);
  private readonly pendingSubmissions = new Map<SubmissionKind, PendingSubmission>();
  private readonly requestedPatientId = this.route.snapshot.queryParamMap.get('patientId');
  private readonly requestedEmergencyId = this.route.snapshot.queryParamMap.get('emergencyId');

  private candidateRequest: Subscription | null = null;
  private candidateRequestVersion = 0;
  private historyRequest: Subscription | null = null;
  private historyRequestVersion = 0;

  readonly queue = signal<PatientReconciliationQueueItem[]>([]);
  readonly selectedPatient = signal<PatientReconciliationQueueItem | null>(null);
  readonly candidates = signal<PatientReconciliationCandidate[]>([]);
  readonly history = signal<PatientReconciliationEvent[]>([]);
  readonly completedJourney = signal<CompletedJourney | null>(null);
  readonly loadingQueue = signal(false);
  readonly loadingCandidates = signal(false);
  readonly loadingHistory = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  ngOnInit(): void {
    this.loadQueue();
  }

  ngOnDestroy(): void {
    this.cancelSelectionRequests();
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  selectPatient(patient: PatientReconciliationQueueItem): void {
    if (this.saving()) return;

    this.cancelSelectionRequests();
    this.completedJourney.set(null);
    this.selectedPatient.set(patient);
    this.candidates.set([]);
    this.history.set([]);
    this.error.set(null);
    this.success.set(null);

    this.loadHistory(patient.patientId);
    if (this.requiresCandidates(patient)) {
      this.loadCandidates(patient.patientId);
    }
  }

  submitDecision(dto: PatientReconciliationDecisionDto): void {
    const patient = this.selectedPatient();
    if (!patient || patient.terminal || this.saving()) return;

    const fingerprint = this.createSubmissionFingerprint(patient.patientId, dto);
    const idempotencyKey = this.resolveIdempotencyKey('decision', fingerprint);
    this.startSaving();

    this.api.decide(patient.patientId, dto, idempotencyKey).pipe(
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: (result) => {
        this.pendingSubmissions.delete('decision');
        this.decisionForm()?.reset();
        this.completeSuccessfulOperation(result);
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  submitCorrection(dto: PatientReconciliationCorrectionDto): void {
    const patient = this.selectedPatient();
    if (!patient || !patient.terminal || this.saving()) return;

    const fingerprint = this.createSubmissionFingerprint(patient.patientId, dto);
    const idempotencyKey = this.resolveIdempotencyKey('correction', fingerprint);
    this.startSaving();

    this.api.correct(patient.patientId, dto, idempotencyKey).pipe(
      finalize(() => this.saving.set(false)),
    ).subscribe({
      next: (result) => {
        this.pendingSubmissions.delete('correction');
        this.correctionForm()?.reset();
        this.completeSuccessfulOperation(result);
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  refresh(): void {
    if (this.loadingQueue() || this.saving()) return;

    this.loadQueue();
    const patient = this.selectedPatient();
    if (!patient) return;

    this.cancelSelectionRequests();
    this.loadHistory(patient.patientId);
    if (this.requiresCandidates(patient)) {
      this.loadCandidates(patient.patientId);
    }
  }

  canCorrect(patient: PatientReconciliationQueueItem): boolean {
    return patient.terminal
      && patient.decisionEventId !== null
      && patient.canonicalPatientId !== null
      && patient.canonicalPatientId !== patient.patientId;
  }

  continueToHospitalization(): void {
    const journey = this.completedJourney();
    if (!journey || journey.loadingEmergency || !journey.emergencyId) return;
    void this.router.navigate(
      ['/patients', journey.result.canonicalPatientId, 'hospitalizations'],
      { queryParams: { emergencyId: journey.emergencyId } },
    );
  }

  openCanonicalDpu(): void {
    const journey = this.completedJourney();
    if (!journey) return;
    void this.router.navigate(['/patients', journey.result.canonicalPatientId]);
  }

  dismissCompletedJourney(): void {
    this.completedJourney.set(null);
    this.success.set(null);
  }

  private loadQueue(): void {
    this.loadingQueue.set(true);
    this.error.set(null);
    this.api.getQueue().pipe(
      finalize(() => this.loadingQueue.set(false)),
    ).subscribe({
      next: (items) => {
        this.queue.set(items);
        if (this.requestedPatientId && !this.selectedPatient()) {
          const requested = items.find((item) => item.patientId === this.requestedPatientId);
          if (requested) this.selectPatient(requested);
        }
      },
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
        if (this.isCurrentSelectionRequest(patientId, requestVersion, this.candidateRequestVersion)) {
          this.candidates.set(items);
        }
      },
      error: (error) => {
        if (this.isCurrentSelectionRequest(patientId, requestVersion, this.candidateRequestVersion)) {
          this.error.set(this.apiErrors.message(
            error,
            'patientReconciliation.error',
            'patientReconciliation.error.loadCandidates',
          ));
        }
      },
    });
  }

  private loadHistory(patientId: string): void {
    const requestVersion = ++this.historyRequestVersion;
    this.loadingHistory.set(true);

    this.historyRequest = this.api.getHistory(patientId).pipe(
      finalize(() => {
        if (requestVersion === this.historyRequestVersion) {
          this.loadingHistory.set(false);
        }
      }),
    ).subscribe({
      next: (events) => {
        if (this.isCurrentSelectionRequest(patientId, requestVersion, this.historyRequestVersion)) {
          this.history.set(events);
        }
      },
      error: (error) => {
        if (this.isCurrentSelectionRequest(patientId, requestVersion, this.historyRequestVersion)) {
          this.error.set(this.apiErrors.message(
            error,
            'patientReconciliation.error',
            'patientReconciliation.error.loadHistory',
          ));
        }
      },
    });
  }

  private cancelSelectionRequests(): void {
    this.candidateRequestVersion += 1;
    this.historyRequestVersion += 1;
    this.candidateRequest?.unsubscribe();
    this.historyRequest?.unsubscribe();
    this.candidateRequest = null;
    this.historyRequest = null;
    this.loadingCandidates.set(false);
    this.loadingHistory.set(false);
  }

  private isCurrentSelectionRequest(
    patientId: string,
    requestVersion: number,
    currentVersion: number,
  ): boolean {
    return requestVersion === currentVersion
      && this.selectedPatient()?.patientId === patientId;
  }

  private requiresCandidates(patient: PatientReconciliationQueueItem): boolean {
    return !patient.terminal || this.canCorrect(patient);
  }

  private startSaving(): void {
    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);
  }

  private handleSaveError(error: unknown): void {
    this.error.set(this.apiErrors.message(
      error,
      'patientReconciliation.error',
      'patientReconciliation.error.save',
    ));
  }

  private completeSuccessfulOperation(result: PatientReconciliationDecisionResult): void {
    this.success.set(this.t(
      `patientReconciliation.success.${result.decision.toLowerCase()}`,
      'Décision de rapprochement enregistrée.',
    ));
    this.cancelSelectionRequests();
    this.selectedPatient.set(null);
    this.candidates.set([]);
    this.history.set([]);
    this.completedJourney.set({
      result,
      emergencyId: this.requestedEmergencyId,
      loadingEmergency: !this.requestedEmergencyId,
    });
    this.loadQueue();

    if (!this.requestedEmergencyId) {
      this.resolveEmergencyForJourney(result);
    }
  }

  private resolveEmergencyForJourney(result: PatientReconciliationDecisionResult): void {
    this.emergencyApi.getPatientEmergencies(result.sourcePatientId).subscribe({
      next: (emergencies) => {
        const emergency = [...emergencies]
          .sort((left, right) => right.createdAt.localeCompare(left.createdAt))[0];
        this.completedJourney.set({
          result,
          emergencyId: emergency?.id ?? null,
          loadingEmergency: false,
        });
      },
      error: () => this.completedJourney.set({
        result,
        emergencyId: null,
        loadingEmergency: false,
      }),
    });
  }

  private resolveIdempotencyKey(kind: SubmissionKind, fingerprint: string): string {
    const pendingSubmission = this.pendingSubmissions.get(kind);
    if (pendingSubmission?.fingerprint === fingerprint) {
      return pendingSubmission.idempotencyKey;
    }

    const idempotencyKey = this.createIdempotencyKey();
    this.pendingSubmissions.set(kind, { fingerprint, idempotencyKey });
    return idempotencyKey;
  }

  private createSubmissionFingerprint(patientId: string, payload: object): string {
    return JSON.stringify({ patientId, ...payload });
  }

  private createIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    return `patient-reconciliation-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }
}

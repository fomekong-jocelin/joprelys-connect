import { CommonModule } from '@angular/common';
import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  inject,
  signal,
} from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { AiAssistantInputComponent } from './ai-assistant-input.component';
import { AiDraftMergeService } from './ai-draft-merge.service';
import { AiDraftPreviewComponent } from './ai-draft-preview.component';
import {
  AiConsultationApiService,
  AiConsultationDraft,
  AiField,
  AiSessionResponse,
} from './ai-consultation-api.service';
import {
  ClinicalCaptureCorrection,
  ClinicalCaptureReviewComponent,
} from './clinical-capture-review.component';
import {
  ClassicVoiceCapture,
  ClassicVoiceRecorderService,
} from './classic-voice-recorder.service';
import { LinkedEvidenceNotePanelComponent } from './linked-evidence-note-panel.component';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';
import { RealtimeVoiceControllerComponent } from './realtime-voice-controller.component';

export type { AiConsultationDraft } from './ai-consultation-api.service';

type AiWorkflowStage =
  | 'PREPARE'
  | 'CAPTURE_REALTIME'
  | 'CAPTURE_DICTATION'
  | 'TRANSCRIPT_REVIEW'
  | 'REPORT_REVIEW'
  | 'FORM_READY';

@Component({
  selector: 'app-voice-assistant-panel',
  standalone: true,
  imports: [
    CommonModule,
    AiAssistantInputComponent,
    AiDraftPreviewComponent,
    ClinicalCaptureReviewComponent,
    LinkedEvidenceNotePanelComponent,
    RealtimeVoiceControllerComponent,
  ],
  templateUrl: './voice-assistant-panel.component.html',
})
export class VoiceAssistantPanelComponent implements OnInit, OnDestroy {
  private readonly api = inject(AiConsultationApiService);
  private readonly intakeApi = inject(RealtimeClinicalIntakeApiService);
  private readonly voiceRecorder = inject(ClassicVoiceRecorderService);
  private readonly draftMerge = inject(AiDraftMergeService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() currentDraft: Record<string, unknown> | null = null;
  @Output() readonly applyDraft = new EventEmitter<AiConsultationDraft>();
  @Output() readonly formReadyChange = new EventEmitter<boolean>();

  readonly stage = signal<AiWorkflowStage>('PREPARE');
  readonly session = signal<AiSessionResponse | null>(null);
  readonly captureEntries = signal<RealtimeClinicalIntakeAck[]>([]);
  readonly busy = signal(false);
  readonly recording = signal(false);
  readonly realtimeActive = signal(false);
  readonly audioLevel = signal(0);
  readonly errorMessage = signal('');
  readonly vitalsWarning = signal('');
  readonly mediaRecorderSupported = this.voiceRecorder.supported;

  private sessionBaseDraft: AiConsultationDraft = {};
  private destroyed = false;

  ngOnInit(): void {
    if (!this.visitId) return;
    this.loadRecoverableCapture(true);
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    this.voiceRecorder.dispose();
  }

  startRealtime(): void {
    if (!this.visitId || this.busy()) return;
    this.errorMessage.set('');
    this.stage.set('CAPTURE_REALTIME');
    this.formReadyChange.emit(false);
  }

  startDictation(): void {
    if (!this.visitId || this.busy() || !this.mediaRecorderSupported) return;
    this.errorMessage.set('');
    this.stage.set('CAPTURE_DICTATION');
    this.formReadyChange.emit(false);
  }

  openManualForm(): void {
    if (this.busy() || this.recording() || this.realtimeActive()) return;
    this.stage.set('FORM_READY');
    this.formReadyChange.emit(true);
  }

  finishRealtime(): void {
    this.realtimeActive.set(false);
    this.loadRecoverableCapture(false, () => this.stage.set('TRANSCRIPT_REVIEW'));
  }

  finishDictation(): void {
    if (this.recording()) {
      this.voiceRecorder.stop();
      return;
    }
    if (this.busy()) return;
    this.loadRecoverableCapture(false, () => this.stage.set('TRANSCRIPT_REVIEW'));
  }

  resumeCapture(): void {
    if (this.busy()) return;
    this.startRealtime();
  }

  backToTranscript(): void {
    if (this.busy()) return;
    this.session.set(null);
    this.stage.set('TRANSCRIPT_REVIEW');
  }

  toggleRecording(): void {
    if (this.busy() || !this.mediaRecorderSupported) return;
    if (this.recording()) {
      this.voiceRecorder.stop();
      return;
    }
    void this.startRecording();
  }

  correctCapture(request: ClinicalCaptureCorrection): void {
    if (this.busy() || !request.transcript.trim()) return;
    this.startBusy();
    this.intakeApi.correct(this.visitId, request.id, request.transcript.trim()).subscribe({
      next: corrected => {
        this.upsertCapture(corrected);
        this.busy.set(false);
      },
      error: error => this.handleError(
        error,
        this.i18n.t(
          'consultation.ai.realtimeCorrectionFailed',
          'La correction n’a pas pu être enregistrée. La transcription précédente reste conservée.',
        ),
      ),
    });
  }

  generateReport(): void {
    if (this.busy() || this.captureEntries().length === 0) return;
    this.startBusy();
    this.sessionBaseDraft = this.sanitizedCurrentDraft();
    this.api.rebuildCapture(this.visitId, this.sessionBaseDraft).subscribe({
      next: response => {
        this.session.set(response);
        this.busy.set(false);
        this.stage.set('REPORT_REVIEW');
        if (!this.hasDraftContent()) {
          this.errorMessage.set(this.i18n.t(
            'consultation.ai.reportEmpty',
            'La transcription est conservée, mais aucun élément clinique suffisamment fondé n’a pu être structuré. Relisez le transcript ou complétez l’enregistrement.',
          ));
        }
      },
      error: error => this.handleError(
        error,
        this.i18n.t(
          'consultation.ai.reportGenerationFailed',
          'Le compte rendu n’a pas pu être généré. La transcription complète reste sauvegardée.',
        ),
      ),
    });
  }

  hasDraftContent(): boolean {
    const draft = this.session()?.draft;
    return !!draft && Object.entries(draft).some(([field, value]) =>
      field !== 'vitals' && typeof value === 'string' && !!value.trim());
  }

  applyCurrentDraft(): void {
    const draft = this.session()?.draft;
    if (!draft || !this.hasDraftContent()) return;

    const current = this.currentDraft ?? {};
    const plan = this.draftMerge.plan(
      { baseDraft: this.cloneDraft(this.sessionBaseDraft), draft: this.cloneDraft(draft) },
      current,
    );
    const safeDraft: AiConsultationDraft = { ...plan.textPatch };

    if (draft.prescription?.trim()) {
      const currentPrescription = Array.isArray(current['prescription']) ? current['prescription'] : [];
      safeDraft.prescription = JSON.stringify([...currentPrescription, ...plan.prescriptionAdds]);
    }
    if (draft.labOrders?.trim()) {
      const currentExams = Array.isArray(current['exams'])
        ? current['exams'].filter((item): item is string => typeof item === 'string' && !!item.trim())
        : [];
      safeDraft.labOrders = JSON.stringify([...currentExams, ...plan.labAdds]);
    }

    if (plan.conflicts.length > 0) {
      this.errorMessage.set(this.i18n.t(
        'consultation.ai.safeMergeConflict',
        'Des saisies plus récentes du médecin ont été conservées. Vérifiez les champs signalés avant l’enregistrement.',
      ));
    }
    if (plan.vitalsProposal) {
      this.vitalsWarning.set(this.i18n.t(
        'consultation.ai.vitalsRequireDedicatedValidation',
        'Les constantes détectées restent à confirmer dans le bloc Constantes.',
      ));
    }

    this.applyDraft.emit(safeDraft);
    this.stage.set('FORM_READY');
    this.formReadyChange.emit(true);
  }

  applyFinalReviewPatch(patch: AiConsultationDraft): void {
    if (!patch || Object.keys(patch).length === 0) return;
    this.session.update(current => current ? {
      ...current,
      draft: { ...current.draft, ...patch },
    } : current);
  }

  dismissVitalsWarning(): void {
    this.vitalsWarning.set('');
  }

  handleRealtimeError(message: string): void {
    if (!message.trim()) return;
    this.errorMessage.set(message.trim());
  }

  latestDictationEntries(): RealtimeClinicalIntakeAck[] {
    return this.captureEntries().slice(-6);
  }

  formatCaptureTime(value: string): string {
    const timestamp = Date.parse(value);
    return Number.isFinite(timestamp)
      ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : '';
  }

  private async startRecording(): Promise<void> {
    if (this.busy() || this.recording() || this.stage() !== 'CAPTURE_DICTATION') return;
    try {
      this.errorMessage.set('');
      await this.voiceRecorder.start(
        capture => this.handleClassicCapture(capture),
        level => this.audioLevel.set(level),
      );
      this.recording.set(true);
    } catch {
      this.errorMessage.set(this.i18n.t('consultation.ai.errorMicrophoneUnavailable'));
      this.voiceRecorder.dispose();
    }
  }

  private handleClassicCapture(capture: ClassicVoiceCapture): void {
    this.recording.set(false);
    if (capture.audio.size === 0) {
      this.errorMessage.set(this.i18n.t('consultation.ai.errorEmptyRecording'));
      return;
    }
    this.startBusy();
    this.intakeApi.captureDictation(this.visitId, capture.audio).subscribe({
      next: entry => {
        this.upsertCapture(entry);
        this.busy.set(false);
      },
      error: error => this.handleError(
        error,
        this.i18n.t(
          'consultation.ai.errorTranscribeDictation',
          'La dictée n’a pas pu être transcrite. Aucun élément déjà sauvegardé n’a été supprimé.',
        ),
      ),
    });
  }

  private loadRecoverableCapture(initial: boolean, afterLoad?: () => void): void {
    const visitId = this.visitId;
    this.intakeApi.list(visitId).subscribe({
      next: entries => {
        if (this.destroyed || visitId !== this.visitId) return;
        this.captureEntries.set([...entries].sort((a, b) => a.sequence - b.sequence));
        if (initial && entries.length > 0) {
          this.stage.set('TRANSCRIPT_REVIEW');
          this.formReadyChange.emit(false);
        }
        afterLoad?.();
      },
      error: error => {
        if (!initial) {
          this.handleError(
            error,
            this.i18n.t(
              'consultation.ai.captureLoadFailed',
              'La transcription sauvegardée n’a pas pu être rechargée. Ne poursuivez pas avant d’avoir rétabli l’accès aux données.',
            ),
          );
        }
      },
    });
  }

  private upsertCapture(entry: RealtimeClinicalIntakeAck): void {
    this.captureEntries.update(entries => {
      const found = entries.findIndex(item => item.id === entry.id);
      const next = found >= 0
        ? entries.map((item, index) => index === found ? entry : item)
        : [...entries, entry];
      return next.sort((a, b) => a.sequence - b.sequence);
    });
  }

  private sanitizedCurrentDraft(): AiConsultationDraft {
    const fields: AiField[] = [
      'symptoms',
      'clinicalExam',
      'suspectedDiagnosis',
      'diagnosis',
      'finalDiagnosis',
      'conclusion',
      'advice',
      'followUp',
    ];
    const result: AiConsultationDraft = {};
    for (const field of fields) {
      const value = this.currentDraft?.[field];
      if (typeof value === 'string' && value.trim()) result[field] = value.trim();
    }
    const prescription = this.currentDraft?.['prescription'];
    if (Array.isArray(prescription) && prescription.length > 0) {
      result.prescription = JSON.stringify(prescription);
    }
    const exams = this.currentDraft?.['exams'];
    if (Array.isArray(exams) && exams.length > 0) {
      result.labOrders = JSON.stringify(
        exams.filter(value => typeof value === 'string' && value.trim()),
      );
    }
    const vitals = this.currentDraft?.['vitals'];
    if (vitals && typeof vitals === 'object' && Object.keys(vitals).length > 0) {
      result.vitals = JSON.stringify(vitals);
    }
    return result;
  }

  private cloneDraft(draft: AiConsultationDraft): AiConsultationDraft {
    return { ...draft };
  }

  private startBusy(): void {
    this.busy.set(true);
    this.errorMessage.set('');
  }

  private handleError(
    error: { status?: number; error?: { error?: { message?: string }; detail?: string; title?: string } },
    fallback: string,
  ): void {
    this.busy.set(false);
    this.recording.set(false);
    const detail = error.error?.error?.message || error.error?.detail || error.error?.title;
    this.errorMessage.set(detail && !detail.startsWith('AI_') ? detail : fallback);
  }
}

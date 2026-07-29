import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import {
  RealtimeClinicalIntakeAck,
  RealtimeClinicalIntakeApiService,
} from './realtime-clinical-intake-api.service';

export interface ClinicalCaptureCorrection {
  id: string;
  transcript: string;
}

@Component({
  selector: 'app-clinical-capture-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="mx-auto w-full max-w-4xl py-2">
      <header class="flex flex-col gap-3 border-b border-[var(--app-border)] pb-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p class="text-[10px] font-bold uppercase tracking-[0.14em] text-[var(--brand-primary)]">
            {{ i18n.t('consultation.ai.transcriptStepEyebrow', 'Étape 2 · Relecture') }}
          </p>
          <h3 class="mt-1 text-lg font-extrabold text-[var(--text-primary)]">
            {{ i18n.t('consultation.ai.transcriptStepTitle', 'Relisez la transcription avant le compte rendu') }}
          </h3>
          <p class="mt-1 max-w-2xl text-xs leading-5 text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.transcriptStepHelp', 'Corrigez ou écartez les anciens passages qui ne doivent plus participer au compte rendu, puis générez la synthèse.') }}
          </p>
        </div>
        <div class="shrink-0 text-right text-xs text-[var(--text-muted)]">
          <p class="font-bold text-[var(--text-primary)]">{{ entries.length }} {{ i18n.t('consultation.ai.savedSegments', 'passage(s) sauvegardé(s)') }}</p>
          @if (reviewCount() > 0) {
            <p class="mt-1 text-[var(--brand-warning-text)]">{{ reviewCount() }} {{ i18n.t('consultation.ai.segmentsToVerify', 'à vérifier') }}</p>
          }
          @if (entries.length > 0 && !confirmDeleteAll()) {
            <button
              type="button"
              class="mt-2 text-[11px] font-bold text-[var(--brand-danger-text)] hover:underline disabled:opacity-50"
              [disabled]="working()"
              (click)="confirmDeleteAll.set(true)"
            >
              {{ i18n.t('consultation.ai.deleteAllTranscripts', 'Supprimer tous les passages') }}
            </button>
          }
        </div>
      </header>

      @if (actionError()) {
        <div class="mt-3 border-l-2 border-[var(--brand-danger-border)] py-1.5 pl-3 text-xs font-semibold text-[var(--brand-danger-text)]" role="alert">
          {{ actionError() }}
        </div>
      }

      @if (confirmDeleteAll()) {
        <div class="mt-3 flex flex-col gap-3 rounded-[var(--radius-brand-sm)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 sm:flex-row sm:items-center sm:justify-between">
          <p class="text-xs font-semibold leading-5 text-[var(--brand-danger-text)]">
            {{ i18n.t('consultation.ai.deleteAllTranscriptsConfirm', 'Tous ces passages seront retirés du prochain compte rendu. Cette action reste tracée.') }}
          </p>
          <div class="flex shrink-0 gap-2">
            <button type="button" class="ui-button ui-button-danger min-h-9" [disabled]="working()" (click)="requestDeleteAll()">
              {{ i18n.t('consultation.ai.confirmDeleteAllTranscripts', 'Tout supprimer') }}
            </button>
            <button type="button" class="ui-button ui-button-secondary min-h-9" [disabled]="working()" (click)="confirmDeleteAll.set(false)">
              {{ i18n.t('common.cancel', 'Annuler') }}
            </button>
          </div>
        </div>
      }

      <div class="mt-4 max-h-[52vh] space-y-2 overflow-y-auto pr-1">
        @for (entry of entries; track entry.id) {
          <article class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-3">
            <div class="flex items-start gap-3">
              <time class="shrink-0 pt-0.5 text-[10px] tabular-nums text-[var(--text-muted)]">
                {{ formatTime(entry.receivedAt) }}
              </time>
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-1.5">
                  @if (entry.reviewRequired) {
                    <span class="rounded-[var(--radius-brand-xs)] border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] px-1.5 py-0.5 text-[9px] font-bold text-[var(--brand-warning-text)]">
                      {{ i18n.t('consultation.ai.transcriptToVerify', 'À vérifier') }}
                    </span>
                  }
                  @if (entry.correctionCount > 0) {
                    <span class="rounded-[var(--radius-brand-xs)] border border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] px-1.5 py-0.5 text-[9px] font-bold text-[var(--brand-success-text)]">
                      {{ i18n.t('consultation.ai.transcriptCorrected', 'Corrigé') }}
                    </span>
                  }
                </div>

                @if (editingId() === entry.id) {
                  <textarea
                    rows="4"
                    class="ui-textarea mt-2 w-full resize-y"
                    [value]="editingText()"
                    (input)="editingText.set(($any($event.target)).value)"
                  ></textarea>
                  <div class="mt-2 flex gap-2">
                    <button type="button" class="ui-button ui-button-primary min-h-10" [disabled]="working() || !editingText().trim()" (click)="saveCorrection(entry.id)">
                      {{ i18n.t('consultation.ai.saveCorrection', 'Enregistrer la correction') }}
                    </button>
                    <button type="button" class="ui-button ui-button-secondary min-h-10" [disabled]="working()" (click)="cancelCorrection()">
                      {{ i18n.t('common.cancel', 'Annuler') }}
                    </button>
                  </div>
                } @else if (confirmDeleteId() === entry.id) {
                  <p class="mt-1 whitespace-pre-wrap text-sm leading-6 text-[var(--text-primary)]">{{ entry.transcript }}</p>
                  <div class="mt-2 flex flex-wrap items-center gap-2">
                    <span class="text-xs font-semibold text-[var(--brand-danger-text)]">
                      {{ i18n.t('consultation.ai.deleteTranscriptConfirm', 'Retirer ce passage du prochain compte rendu ?') }}
                    </span>
                    <button type="button" class="ui-button ui-button-danger min-h-9" [disabled]="working()" (click)="requestDelete(entry)">
                      {{ i18n.t('common.delete', 'Supprimer') }}
                    </button>
                    <button type="button" class="ui-button ui-button-secondary min-h-9" [disabled]="working()" (click)="confirmDeleteId.set(null)">
                      {{ i18n.t('common.cancel', 'Annuler') }}
                    </button>
                  </div>
                } @else {
                  <p class="whitespace-pre-wrap text-sm leading-6 text-[var(--text-primary)]">{{ entry.transcript }}</p>
                  <div class="mt-1 flex items-center gap-4">
                    <button type="button" class="min-h-8 text-xs font-bold text-[var(--brand-primary)] hover:underline" [disabled]="working()" (click)="edit(entry)">
                      {{ i18n.t('consultation.ai.correctTranscript', 'Corriger') }}
                    </button>
                    <button type="button" class="min-h-8 text-xs font-bold text-[var(--brand-danger-text)] hover:underline" [disabled]="working()" (click)="confirmDeleteId.set(entry.id)">
                      {{ i18n.t('consultation.ai.deleteTranscript', 'Supprimer') }}
                    </button>
                  </div>
                }
              </div>
            </div>
          </article>
        } @empty {
          <div class="rounded-[var(--radius-brand-sm)] border border-dashed border-[var(--app-border)] px-4 py-8 text-center">
            <p class="text-sm font-semibold text-[var(--text-secondary)]">{{ i18n.t('consultation.ai.noTranscriptRemaining', 'Aucun passage conservé pour ce compte rendu.') }}</p>
            <p class="mt-1 text-xs text-[var(--text-muted)]">{{ i18n.t('consultation.ai.noTranscriptRemainingHelp', 'Reprenez l’enregistrement pour ajouter de nouveaux éléments.') }}</p>
          </div>
        }
      </div>

      <footer class="mt-5 flex flex-col-reverse gap-2 border-t border-[var(--app-border)] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <button type="button" class="ui-button ui-button-secondary w-full sm:w-auto" [disabled]="working()" (click)="resume.emit()">
          {{ i18n.t('consultation.ai.resumeRecording', 'Reprendre l’enregistrement') }}
        </button>
        <button type="button" class="ui-button ui-button-primary w-full sm:w-auto" [disabled]="working() || entries.length === 0" (click)="generate.emit()">
          {{ working() ? i18n.t('consultation.ai.generatingReport', 'Génération en cours…') : i18n.t('consultation.ai.generateReport', 'Générer le compte rendu') }}
        </button>
      </footer>
    </section>
  `,
})
export class ClinicalCaptureReviewComponent {
  private readonly intakeApi = inject(RealtimeClinicalIntakeApiService);
  readonly i18n = inject(I18nService);

  @Input() entries: RealtimeClinicalIntakeAck[] = [];
  @Input() busy = false;
  @Output() readonly corrected = new EventEmitter<ClinicalCaptureCorrection>();
  @Output() readonly resume = new EventEmitter<void>();
  @Output() readonly generate = new EventEmitter<void>();

  readonly editingId = signal<string | null>(null);
  readonly editingText = signal('');
  readonly confirmDeleteId = signal<string | null>(null);
  readonly confirmDeleteAll = signal(false);
  readonly deleting = signal(false);
  readonly actionError = signal('');

  working(): boolean {
    return this.busy || this.deleting();
  }

  reviewCount(): number {
    return this.entries.filter(entry => entry.reviewRequired).length;
  }

  edit(entry: RealtimeClinicalIntakeAck): void {
    if (this.working()) return;
    this.confirmDeleteId.set(null);
    this.editingId.set(entry.id);
    this.editingText.set(entry.transcript);
  }

  cancelCorrection(): void {
    this.editingId.set(null);
    this.editingText.set('');
  }

  saveCorrection(id: string): void {
    const transcript = this.editingText().trim();
    if (!transcript || this.working()) return;
    this.corrected.emit({ id, transcript });
    this.cancelCorrection();
  }

  requestDelete(entry: RealtimeClinicalIntakeAck): void {
    if (this.working()) return;
    this.actionError.set('');
    this.deleting.set(true);
    this.intakeApi.discard(entry.visitId, entry.id).subscribe({
      next: () => {
        this.entries = this.entries.filter(item => item.id !== entry.id);
        this.confirmDeleteId.set(null);
        this.deleting.set(false);
      },
      error: () => {
        this.deleting.set(false);
        this.actionError.set(this.i18n.t(
          'consultation.ai.deleteTranscriptFailed',
          'Ce passage n’a pas pu être supprimé. Il reste conservé et sera toujours pris en compte.',
        ));
      },
    });
  }

  requestDeleteAll(): void {
    if (this.working() || this.entries.length === 0) return;
    const visitId = this.entries[0]?.visitId;
    if (!visitId) return;
    this.actionError.set('');
    this.deleting.set(true);
    this.intakeApi.discardAll(visitId).subscribe({
      next: () => {
        this.entries = [];
        this.confirmDeleteAll.set(false);
        this.confirmDeleteId.set(null);
        this.deleting.set(false);
      },
      error: () => {
        this.deleting.set(false);
        this.actionError.set(this.i18n.t(
          'consultation.ai.deleteAllTranscriptsFailed',
          'Les passages n’ont pas pu être supprimés. Ils restent conservés et seront toujours pris en compte.',
        ));
      },
    });
  }

  formatTime(value: string): string {
    const timestamp = Date.parse(value);
    return Number.isFinite(timestamp)
      ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : '';
  }
}

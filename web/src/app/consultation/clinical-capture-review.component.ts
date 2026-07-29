import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { RealtimeClinicalIntakeAck } from './realtime-clinical-intake-api.service';

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
            {{ i18n.t('consultation.ai.transcriptStepHelp', 'Aucune information n’est appliquée au dossier à ce stade. Corrigez uniquement les passages nécessaires, puis générez le compte rendu.') }}
          </p>
        </div>
        <div class="shrink-0 text-right text-xs text-[var(--text-muted)]">
          <p class="font-bold text-[var(--text-primary)]">{{ entries.length }} {{ i18n.t('consultation.ai.savedSegments', 'passage(s) sauvegardé(s)') }}</p>
          @if (reviewCount() > 0) {
            <p class="mt-1 text-[var(--brand-warning-text)]">{{ reviewCount() }} {{ i18n.t('consultation.ai.segmentsToVerify', 'à vérifier') }}</p>
          }
        </div>
      </header>

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
                    <button type="button" class="ui-button ui-button-primary min-h-10" [disabled]="busy || !editingText().trim()" (click)="saveCorrection(entry.id)">
                      {{ i18n.t('consultation.ai.saveCorrection', 'Enregistrer la correction') }}
                    </button>
                    <button type="button" class="ui-button ui-button-secondary min-h-10" [disabled]="busy" (click)="cancelCorrection()">
                      {{ i18n.t('common.cancel', 'Annuler') }}
                    </button>
                  </div>
                } @else {
                  <p class="whitespace-pre-wrap text-sm leading-6 text-[var(--text-primary)]">{{ entry.transcript }}</p>
                  <button type="button" class="mt-1 min-h-8 text-xs font-bold text-[var(--brand-primary)] hover:underline" [disabled]="busy" (click)="edit(entry)">
                    {{ i18n.t('consultation.ai.correctTranscript', 'Corriger') }}
                  </button>
                }
              </div>
            </div>
          </article>
        }
      </div>

      <footer class="mt-5 flex flex-col-reverse gap-2 border-t border-[var(--app-border)] pt-4 sm:flex-row sm:items-center sm:justify-between">
        <button type="button" class="ui-button ui-button-secondary w-full sm:w-auto" [disabled]="busy" (click)="resume.emit()">
          {{ i18n.t('consultation.ai.resumeRecording', 'Reprendre l’enregistrement') }}
        </button>
        <button type="button" class="ui-button ui-button-primary w-full sm:w-auto" [disabled]="busy || entries.length === 0" (click)="generate.emit()">
          {{ busy ? i18n.t('consultation.ai.generatingReport', 'Génération en cours…') : i18n.t('consultation.ai.generateReport', 'Générer le compte rendu') }}
        </button>
      </footer>
    </section>
  `,
})
export class ClinicalCaptureReviewComponent {
  readonly i18n = inject(I18nService);

  @Input() entries: readonly RealtimeClinicalIntakeAck[] = [];
  @Input() busy = false;
  @Output() readonly corrected = new EventEmitter<ClinicalCaptureCorrection>();
  @Output() readonly resume = new EventEmitter<void>();
  @Output() readonly generate = new EventEmitter<void>();

  readonly editingId = signal<string | null>(null);
  readonly editingText = signal('');

  reviewCount(): number {
    return this.entries.filter(entry => entry.reviewRequired).length;
  }

  edit(entry: RealtimeClinicalIntakeAck): void {
    if (this.busy) return;
    this.editingId.set(entry.id);
    this.editingText.set(entry.transcript);
  }

  cancelCorrection(): void {
    this.editingId.set(null);
    this.editingText.set('');
  }

  saveCorrection(id: string): void {
    const transcript = this.editingText().trim();
    if (!transcript || this.busy) return;
    this.corrected.emit({ id, transcript });
    this.cancelCorrection();
  }

  formatTime(value: string): string {
    const timestamp = Date.parse(value);
    return Number.isFinite(timestamp)
      ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      : '';
  }
}

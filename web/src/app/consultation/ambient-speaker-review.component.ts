import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  EventEmitter,
  Input,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  inject,
  signal,
} from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AmbientSpeakerReviewService,
  AmbientTranscriptItem,
} from './ambient-speaker-review.service';

const NON_CLINICAL_FILLERS = new Set([
  'oh',
  'hello',
  'bonjour',
  'salut',
  'merci',
  'hmm',
  'hum',
  'euh',
]);

@Component({
  selector: 'app-ambient-speaker-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (shouldRender()) {
      <section class="rounded-[6px] border border-amber-200 bg-amber-50/60 p-3 shadow-sm dark:border-amber-900 dark:bg-amber-950/15">
        @if (error()) {
          <p class="text-xs font-bold text-rose-800 dark:text-rose-200">{{ error() }}</p>
        } @else {
          <div>
            <p class="text-sm font-black text-[var(--text-primary)]">
              {{ reviewItems().length === 1
                ? i18n.t('consultation.ai.speakerOnePhrase', 'Une phrase doit être vérifiée')
                : i18n.t('consultation.ai.speakerSeveralPhrases', 'Quelques phrases doivent être vérifiées') }}
            </p>
            <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.speakerSimpleHelp', 'Joprelys n’est pas assez sûr de qui a parlé. Confirmez seulement les phrases que vous reconnaissez.') }}
            </p>
          </div>

          <div class="mt-3 space-y-3">
            @for (item of reviewItems(); track item.id) {
              <article class="rounded-[5px] border border-amber-200 bg-white p-3 dark:border-amber-900 dark:bg-slate-950">
                <div class="text-[10px] font-bold text-[var(--text-muted)]">
                  {{ formatOffset(item.startOffsetMs) }}
                </div>
                <p class="mt-1 text-sm leading-5 text-[var(--text-primary)]">“{{ item.text }}”</p>
                <p class="mt-3 text-xs font-black text-[var(--text-primary)]">
                  {{ i18n.t('consultation.ai.whoSaidThis', 'Qui a dit cette phrase ?') }}
                </p>
                <div class="mt-2 grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    (click)="assign(item, 'DOCTOR')"
                    [disabled]="updatingItemId() === item.id"
                    class="min-h-10 rounded-[4px] border border-cyan-300 bg-cyan-50 px-3 text-xs font-black text-cyan-900 hover:bg-cyan-100 disabled:opacity-50 dark:border-cyan-800 dark:bg-cyan-950/30 dark:text-cyan-100"
                  >
                    {{ i18n.t('consultation.ai.assignDoctorSpeaker', 'Médecin') }}
                  </button>
                  <button
                    type="button"
                    (click)="assign(item, 'PATIENT')"
                    [disabled]="updatingItemId() === item.id"
                    class="min-h-10 rounded-[4px] border border-emerald-300 bg-emerald-50 px-3 text-xs font-black text-emerald-900 hover:bg-emerald-100 disabled:opacity-50 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-100"
                  >
                    {{ i18n.t('consultation.ai.assignPatientSpeaker', 'Patient') }}
                  </button>
                </div>
              </article>
            }
          </div>
        }
      </section>
    }
  `,
})
export class AmbientSpeakerReviewComponent implements OnChanges, OnDestroy {
  private readonly api = inject(AmbientSpeakerReviewService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Output() readonly ambiguityChange = new EventEmitter<number>();

  readonly items = signal<AmbientTranscriptItem[]>([]);
  readonly loading = signal(false);
  readonly updatingItemId = signal<string | null>(null);
  readonly error = signal('');
  private readonly subscriptions = new Subscription();

  constructor() {
    this.subscriptions.add(interval(5000).subscribe(() => {
      if (!this.loading() && !this.updatingItemId() && this.visitId.trim()) this.load(true);
    }));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visitId']) {
      this.items.set([]);
      this.error.set('');
      this.emitAmbiguityCount();
      if (this.visitId.trim()) this.load();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  shouldRender(): boolean {
    return !!this.error() || this.reviewItems().length > 0;
  }

  reviewItems(): AmbientTranscriptItem[] {
    return this.items()
      .filter(item => item.speakerType === 'UNSPECIFIED' && this.requiresHumanReview(item.text))
      .sort((a, b) => a.startOffsetMs - b.startOffsetMs || a.sequence - b.sequence);
  }

  load(silent = false): void {
    const visitId = this.visitId.trim();
    if (!visitId || this.loading()) return;
    this.loading.set(true);
    if (!silent) this.error.set('');
    this.api.transcript(visitId).subscribe({
      next: ledger => {
        this.items.set([...(ledger.items ?? [])]);
        this.loading.set(false);
        this.emitAmbiguityCount();
      },
      error: () => {
        this.loading.set(false);
        if (!silent) {
          this.error.set(this.i18n.t(
            'consultation.ai.speakerReviewLoadFailedSimple',
            'La vérification des locuteurs est momentanément indisponible. Aucune attribution n’a été modifiée.',
          ));
        }
      },
    });
  }

  assign(item: AmbientTranscriptItem, speaker: 'DOCTOR' | 'PATIENT'): void {
    const visitId = this.visitId.trim();
    if (!visitId || this.updatingItemId()) return;
    this.updatingItemId.set(item.id);
    this.error.set('');
    this.api.assignSpeaker(visitId, item, speaker).subscribe({
      next: corrected => {
        const next = this.items().filter(current => current.id !== item.id);
        next.push(corrected);
        this.items.set(next);
        this.updatingItemId.set(null);
        this.emitAmbiguityCount();
      },
      error: error => {
        this.updatingItemId.set(null);
        if (this.isStaleConflict(error)) {
          this.error.set(this.i18n.t(
            'consultation.ai.speakerReviewStaleSimple',
            'Cette phrase a changé pendant votre vérification. Joprelys recharge la version actuelle.',
          ));
          this.load(true);
          return;
        }
        this.error.set(this.i18n.t(
          'consultation.ai.speakerReviewSaveFailedSimple',
          'La confirmation n’a pas été enregistrée. La phrase originale reste inchangée.',
        ));
      },
    });
  }

  formatOffset(milliseconds: number): string {
    const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  private requiresHumanReview(text: string): boolean {
    const normalized = text
      .trim()
      .toLowerCase()
      .replace(/[.!?,;:]+$/g, '')
      .replace(/\s+/g, ' ');
    if (!normalized) return false;
    return !NON_CLINICAL_FILLERS.has(normalized);
  }

  private emitAmbiguityCount(): void {
    this.ambiguityChange.emit(this.reviewItems().length);
  }

  private isStaleConflict(error: unknown): boolean {
    if (!(error instanceof HttpErrorResponse) || error.status !== 409) return false;
    const payload = error.error;
    const detail = payload && typeof payload === 'object'
      ? (payload as { detail?: unknown; title?: unknown }).detail
        ?? (payload as { detail?: unknown; title?: unknown }).title
      : payload;
    return typeof detail === 'string'
      && [
        'AI_AMBIENT_TRANSCRIPT_ITEM_SUPERSEDED',
        'AI_AMBIENT_TRANSCRIPT_CORRECTION_CONFLICT',
        'AI_AMBIENT_CORRECTION_ID_REUSED',
      ].includes(detail);
  }
}

import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnChanges, OnDestroy, SimpleChanges, inject, signal } from '@angular/core';
import { Subscription, interval } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AmbientSpeakerReviewService,
  AmbientSpeakerType,
  AmbientTranscriptItem,
} from './ambient-speaker-review.service';

@Component({
  selector: 'app-ambient-speaker-review',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="border-t border-[var(--app-border)] pt-3">
      <div class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div class="flex items-center gap-2">
            <p class="text-xs font-black text-[var(--text-primary)]">
              {{ i18n.t('consultation.ai.speakerReviewTitle', 'Locuteurs à confirmer') }}
            </p>
            <span
              class="rounded-[4px] border px-2 py-0.5 text-[10px] font-black"
              [ngClass]="unknownCount() > 0
                ? 'border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900 dark:bg-amber-950/20 dark:text-amber-200'
                : 'border-emerald-200 bg-emerald-50 text-emerald-800 dark:border-emerald-900 dark:bg-emerald-950/20 dark:text-emerald-200'"
            >
              {{ unknownCount() }} {{ i18n.t('consultation.ai.speakerReviewPending', 'à confirmer') }}
            </span>
          </div>
          <p class="mt-1 text-[11px] leading-4 text-[var(--text-muted)]">
            {{ i18n.t(
              'consultation.ai.speakerReviewHelp',
              'Attribuez seulement ce que vous reconnaissez. Joprelys ne transforme jamais automatiquement les autres voix en patient.'
            ) }}
          </p>
        </div>
        <div class="flex gap-2">
          <button
            type="button"
            (click)="showAssigned.update(value => !value)"
            class="min-h-9 rounded-[5px] border border-slate-300 bg-white px-3 text-[10px] font-bold text-slate-700 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200"
          >
            {{ showAssigned()
              ? i18n.t('consultation.ai.hideAssignedSpeakers', 'Masquer attribués')
              : i18n.t('consultation.ai.showAssignedSpeakers', 'Revoir les attribués') }}
          </button>
          <button
            type="button"
            (click)="load()"
            [disabled]="loading() || !!updatingItemId()"
            class="min-h-9 rounded-[5px] border border-cyan-300 bg-cyan-50 px-3 text-[10px] font-black text-cyan-800 hover:bg-cyan-100 disabled:opacity-50 dark:border-cyan-800 dark:bg-cyan-950/30 dark:text-cyan-200"
          >
            {{ i18n.t('consultation.ai.refreshSpeakerReview', 'Actualiser') }}
          </button>
        </div>
      </div>

      @if (error()) {
        <p class="mt-2 rounded-[4px] border border-rose-200 bg-rose-50 px-2.5 py-2 text-[10px] font-bold text-rose-800 dark:border-rose-900 dark:bg-rose-950/20 dark:text-rose-200">
          {{ error() }}
        </p>
      }

      @if (loading() && !items().length) {
        <p class="mt-3 text-[11px] text-[var(--text-muted)]">
          {{ i18n.t('consultation.ai.loadingSpeakerReview', 'Chargement du transcript ambient…') }}
        </p>
      } @else if (!visibleItems().length) {
        <p class="mt-3 rounded-[4px] bg-[var(--app-surface-muted)] px-3 py-2 text-[11px] text-[var(--text-muted)]">
          {{ unknownCount() === 0
            ? i18n.t('consultation.ai.noUnknownSpeakers', 'Aucun locuteur non attribué dans le transcript effectif.')
            : i18n.t('consultation.ai.noVisibleSpeakers', 'Aucun segment à afficher.') }}
        </p>
      } @else {
        <div class="mt-3 max-h-72 space-y-2 overflow-y-auto pr-1">
          @for (item of visibleItems(); track item.id) {
            <article class="rounded-[5px] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-2.5">
              <div class="flex flex-wrap items-center gap-2 text-[10px] font-bold text-[var(--text-muted)]">
                <span [ngClass]="speakerBadgeClasses(item.speakerType)">
                  {{ speakerLabel(item) }}
                </span>
                <span>{{ formatOffset(item.startOffsetMs) }} – {{ formatOffset(item.endOffsetMs) }}</span>
                @if (item.speakerLabel && item.speakerType === 'UNSPECIFIED') {
                  <span class="rounded-[3px] border border-slate-300 px-1.5 py-0.5 dark:border-slate-700">
                    source {{ item.speakerLabel }}
                  </span>
                }
              </div>
              <p class="mt-1.5 text-xs leading-5 text-[var(--text-primary)]">{{ item.text }}</p>
              <div class="mt-2 grid grid-cols-2 gap-2 sm:flex">
                <button
                  type="button"
                  (click)="assign(item, 'DOCTOR')"
                  [disabled]="updatingItemId() === item.id || item.speakerType === 'DOCTOR'"
                  class="min-h-9 rounded-[4px] border px-3 text-[10px] font-black disabled:cursor-default disabled:opacity-60"
                  [ngClass]="item.speakerType === 'DOCTOR'
                    ? 'border-cyan-300 bg-cyan-100 text-cyan-900 dark:border-cyan-800 dark:bg-cyan-950/40 dark:text-cyan-100'
                    : 'border-slate-300 bg-white text-slate-700 hover:border-cyan-300 hover:bg-cyan-50 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200'"
                >
                  {{ i18n.t('consultation.ai.assignDoctorSpeaker', 'Médecin') }}
                </button>
                <button
                  type="button"
                  (click)="assign(item, 'PATIENT')"
                  [disabled]="updatingItemId() === item.id || item.speakerType === 'PATIENT'"
                  class="min-h-9 rounded-[4px] border px-3 text-[10px] font-black disabled:cursor-default disabled:opacity-60"
                  [ngClass]="item.speakerType === 'PATIENT'
                    ? 'border-emerald-300 bg-emerald-100 text-emerald-900 dark:border-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-100'
                    : 'border-slate-300 bg-white text-slate-700 hover:border-emerald-300 hover:bg-emerald-50 dark:border-slate-700 dark:bg-slate-950 dark:text-slate-200'"
                >
                  {{ i18n.t('consultation.ai.assignPatientSpeaker', 'Patient') }}
                </button>
              </div>
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class AmbientSpeakerReviewComponent implements OnChanges, OnDestroy {
  private readonly api = inject(AmbientSpeakerReviewService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';

  readonly items = signal<AmbientTranscriptItem[]>([]);
  readonly loading = signal(false);
  readonly updatingItemId = signal<string | null>(null);
  readonly showAssigned = signal(false);
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
      if (this.visitId.trim()) this.load();
    }
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  unknownCount(): number {
    return this.items().filter(item => item.speakerType === 'UNSPECIFIED').length;
  }

  visibleItems(): AmbientTranscriptItem[] {
    return this.items()
      .filter(item => this.showAssigned() || item.speakerType === 'UNSPECIFIED')
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
      },
      error: () => {
        this.loading.set(false);
        if (!silent) {
          this.error.set(this.i18n.t(
            'consultation.ai.speakerReviewLoadFailed',
            'Le transcript ambient n’a pas pu être chargé. Aucune attribution n’a été modifiée.',
          ));
        }
      },
    });
  }

  assign(item: AmbientTranscriptItem, speaker: 'DOCTOR' | 'PATIENT'): void {
    const visitId = this.visitId.trim();
    if (!visitId || this.updatingItemId() || item.speakerType === speaker) return;
    this.updatingItemId.set(item.id);
    this.error.set('');
    this.api.assignSpeaker(visitId, item, speaker).subscribe({
      next: corrected => {
        const next = this.items().filter(current => current.id !== item.id);
        next.push(corrected);
        this.items.set(next);
        this.updatingItemId.set(null);
      },
      error: error => {
        this.updatingItemId.set(null);
        if (this.isStaleConflict(error)) {
          this.error.set(this.i18n.t(
            'consultation.ai.speakerReviewStale',
            'Ce segment a changé pendant votre revue. Le transcript a été actualisé ; vérifiez la version courante avant de décider.',
          ));
          this.load(true);
          return;
        }
        this.error.set(this.i18n.t(
          'consultation.ai.speakerReviewSaveFailed',
          'L’attribution n’a pas été enregistrée. Le transcript original reste inchangé.',
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

  speakerLabel(item: AmbientTranscriptItem): string {
    if (item.speakerType === 'DOCTOR') return this.i18n.t('consultation.ai.speakerDoctor', 'Médecin');
    if (item.speakerType === 'PATIENT') return this.i18n.t('consultation.ai.speakerPatient', 'Patient');
    return this.i18n.t('consultation.ai.speakerUnknown', 'Non attribué');
  }

  speakerBadgeClasses(speaker: AmbientSpeakerType): string {
    if (speaker === 'DOCTOR') {
      return 'rounded-[3px] bg-cyan-100 px-1.5 py-0.5 text-cyan-800 dark:bg-cyan-950/40 dark:text-cyan-200';
    }
    if (speaker === 'PATIENT') {
      return 'rounded-[3px] bg-emerald-100 px-1.5 py-0.5 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200';
    }
    return 'rounded-[3px] bg-amber-100 px-1.5 py-0.5 text-amber-800 dark:bg-amber-950/40 dark:text-amber-200';
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

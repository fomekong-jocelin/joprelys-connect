import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AmbientNoteApiService,
  AmbientNoteRevision,
  AmbientNoteStatement,
  AmbientNoteTemplate,
  AmbientTranscriptItem,
} from './ambient-note-api.service';

@Component({
  selector: 'app-ambient-note-review',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (enabled && visitId) {
      <section class="rounded-[8px] border border-slate-200 bg-white p-4 shadow-sm dark:border-slate-800 dark:bg-slate-950">
        <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <div class="flex items-center gap-2">
              <span class="inline-flex h-8 w-8 items-center justify-center rounded-[5px] bg-cyan-50 text-cyan-700 dark:bg-cyan-950/40 dark:text-cyan-200">N</span>
              <div>
                <h3 class="text-sm font-black text-[var(--text-primary)]">
                  {{ i18n.t('consultation.ai.ambientNoteTitle', 'Note clinique ambient sourcée') }}
                </h3>
                <p class="mt-0.5 text-[11px] text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.ambientNoteHelp', 'Chaque phrase doit être reliée au transcript final. Les sections critiques exigent une preuve attribuée au médecin.') }}
                </p>
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-2 sm:flex-row sm:items-center">
            <select [(ngModel)]="template"
              class="min-h-10 rounded-[5px] border border-slate-300 bg-white px-3 text-xs font-bold text-[var(--text-primary)] dark:border-slate-700 dark:bg-slate-900">
              <option value="SOAP">SOAP</option>
              <option value="APSO">APSO</option>
              <option value="MULTI_SECTION">Multi-sections</option>
            </select>
            <button type="button" (click)="generate()" [disabled]="busy()"
              class="min-h-10 rounded-[5px] bg-cyan-700 px-4 text-xs font-black text-white shadow-sm hover:bg-cyan-800 disabled:cursor-not-allowed disabled:opacity-50">
              {{ busy()
                ? i18n.t('consultation.ai.ambientNoteGenerating', 'Génération sécurisée…')
                : note()
                  ? i18n.t('consultation.ai.ambientNoteUpdate', 'Mettre à jour la note')
                  : i18n.t('consultation.ai.ambientNoteGenerate', 'Générer la note') }}
            </button>
          </div>
        </div>

        @if (error()) {
          <div class="mt-3 rounded-[5px] border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-bold text-rose-800 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-100">
            {{ error() }}
          </div>
        }

        @if (note(); as currentNote) {
          <div class="mt-4 flex flex-wrap items-center gap-2 text-[10px] font-bold uppercase tracking-wide">
            <span class="rounded-[4px] border px-2 py-1" [ngClass]="statusClasses(currentNote.status)">
              {{ statusLabel(currentNote.status) }}
            </span>
            <span class="rounded-[4px] border border-slate-200 px-2 py-1 text-[var(--text-muted)] dark:border-slate-700">
              Révision {{ currentNote.revision }} · {{ currentNote.template }}
            </span>
            <span class="rounded-[4px] border border-slate-200 px-2 py-1 text-[var(--text-muted)] dark:border-slate-700">
              Transcript jusqu’au segment #{{ currentNote.transcriptMaxSequence }}
            </span>
          </div>

          @if (currentNote.status === 'GENERATED') {
            <div class="mt-3 rounded-[5px] border border-amber-200 bg-amber-50 px-3 py-2 text-[11px] font-bold text-amber-900 dark:border-amber-900 dark:bg-amber-950/25 dark:text-amber-100">
              {{ i18n.t('consultation.ai.ambientNotePendingReview', 'Proposition IA non validée : elle ne doit pas être considérée comme une note clinique approuvée avant votre décision.') }}
            </div>
          }

          @if (!currentNote.statements.length) {
            <div class="mt-4 rounded-[5px] border border-slate-200 bg-slate-50 px-3 py-3 text-xs text-[var(--text-muted)] dark:border-slate-800 dark:bg-slate-900/60">
              {{ i18n.t('consultation.ai.ambientNoteEmpty', 'Aucune affirmation suffisamment sourcée n’a été retenue dans le transcript final.') }}
            </div>
          } @else {
            <div class="mt-4 space-y-4">
              @for (section of orderedSections(currentNote); track section) {
                <div>
                  <div class="mb-2 flex items-center gap-2 border-b border-slate-200 pb-1.5 dark:border-slate-800">
                    <h4 class="text-[11px] font-black uppercase tracking-wider text-[var(--text-primary)]">{{ sectionLabel(section) }}</h4>
                    @if (isCriticalSection(currentNote, section)) {
                      <span class="rounded-[3px] bg-rose-50 px-1.5 py-0.5 text-[9px] font-black uppercase text-rose-700 dark:bg-rose-950/30 dark:text-rose-200">preuve médecin</span>
                    }
                  </div>

                  <div class="space-y-2">
                    @for (statement of statementsFor(currentNote, section); track statement.id) {
                      <article class="rounded-[5px] border border-slate-200 bg-slate-50/60 px-3 py-2.5 dark:border-slate-800 dark:bg-slate-900/40">
                        <p class="text-sm leading-6 text-[var(--text-primary)]">{{ statement.text }}</p>
                        <button type="button" (click)="toggleEvidence(statement.id)"
                          class="mt-2 inline-flex items-center gap-1 text-[10px] font-black uppercase tracking-wide text-cyan-700 hover:underline dark:text-cyan-300">
                          {{ expandedStatementId() === statement.id
                            ? i18n.t('consultation.ai.hideEvidence', 'Masquer les preuves')
                            : i18n.t('consultation.ai.showEvidence', 'Voir les preuves') }}
                          · {{ statement.evidenceItemIds.length }}
                        </button>

                        @if (expandedStatementId() === statement.id) {
                          <div class="mt-2 space-y-1.5 border-l-2 border-cyan-300 pl-3 dark:border-cyan-800">
                            @for (evidence of evidenceFor(statement); track evidence.id) {
                              <div class="rounded-[4px] bg-white px-2.5 py-2 text-[11px] shadow-sm dark:bg-slate-950">
                                <div class="flex flex-wrap items-center gap-2 font-bold text-[var(--text-muted)]">
                                  <span [ngClass]="speakerClasses(evidence.speakerType)">{{ speakerLabel(evidence.speakerType) }}</span>
                                  <span>{{ formatOffset(evidence.startOffsetMs) }} – {{ formatOffset(evidence.endOffsetMs) }}</span>
                                  <span>#{{ evidence.sequence }}</span>
                                </div>
                                <p class="mt-1 leading-5 text-[var(--text-primary)]">{{ evidence.text }}</p>
                              </div>
                            } @empty {
                              <div class="text-[11px] font-bold text-rose-700 dark:text-rose-300">
                                {{ i18n.t('consultation.ai.evidenceUnavailable', 'Preuve indisponible dans le ledger effectif — ne validez pas cette note.') }}
                              </div>
                            }
                          </div>
                        }
                      </article>
                    }
                  </div>
                </div>
              }
            </div>
          }

          @if (currentNote.status === 'GENERATED') {
            <div class="mt-5 flex flex-col gap-2 border-t border-slate-200 pt-4 sm:flex-row sm:justify-end dark:border-slate-800">
              <button type="button" (click)="decide('REJECT')" [disabled]="busy()"
                class="min-h-10 rounded-[5px] border border-rose-300 bg-white px-4 text-xs font-black text-rose-700 hover:bg-rose-50 disabled:opacity-50 dark:border-rose-800 dark:bg-slate-950 dark:text-rose-300">
                {{ i18n.t('consultation.ai.rejectAmbientNote', 'Rejeter cette note') }}
              </button>
              <button type="button" (click)="decide('ACCEPT')" [disabled]="busy() || hasMissingEvidence(currentNote)"
                class="min-h-10 rounded-[5px] bg-emerald-700 px-4 text-xs font-black text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-50">
                {{ i18n.t('consultation.ai.acceptAmbientNote', 'Valider cette note sourcée') }}
              </button>
            </div>
          }
        } @else if (!busy()) {
          <div class="mt-4 rounded-[5px] border border-dashed border-slate-300 px-4 py-5 text-center text-xs text-[var(--text-muted)] dark:border-slate-700">
            {{ i18n.t('consultation.ai.ambientNoteNoRevision', 'Le transcript ambient est conservé séparément. Générez une note lorsque vous souhaitez obtenir une synthèse sourcée.') }}
          </div>
        }
      </section>
    }
  `,
})
export class AmbientNoteReviewComponent implements OnChanges {
  private readonly api = inject(AmbientNoteApiService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';
  @Input() enabled = false;

  readonly note = signal<AmbientNoteRevision | null>(null);
  readonly transcript = signal<AmbientTranscriptItem[]>([]);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly expandedStatementId = signal<string | null>(null);

  template: AmbientNoteTemplate = 'SOAP';

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['visitId'] || changes['enabled']) && this.enabled && this.visitId) {
      this.load();
    }
  }

  load(): void {
    if (!this.visitId || !this.enabled || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    forkJoin({
      note: this.api.latestNote(this.visitId),
      transcript: this.api.transcript(this.visitId),
    }).pipe(finalize(() => this.busy.set(false))).subscribe({
      next: ({ note, transcript }) => {
        this.note.set(note);
        this.transcript.set(transcript.items ?? []);
        if (note?.template) this.template = note.template;
      },
      error: error => this.error.set(this.errorMessage(error)),
    });
  }

  generate(): void {
    if (!this.visitId || this.busy()) return;
    this.busy.set(true);
    this.error.set(null);
    this.api.generateNote(this.visitId, this.template)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: note => {
          this.note.set(note);
          this.expandedStatementId.set(null);
          this.api.transcript(this.visitId).subscribe({
            next: transcript => this.transcript.set(transcript.items ?? []),
          });
        },
        error: error => this.error.set(this.errorMessage(error)),
      });
  }

  decide(decision: 'ACCEPT' | 'REJECT'): void {
    const current = this.note();
    if (!current || current.status !== 'GENERATED' || this.busy()) return;
    if (decision === 'ACCEPT' && this.hasMissingEvidence(current)) {
      this.error.set(this.i18n.t(
        'consultation.ai.ambientNoteEvidenceMissing',
        'Validation bloquée : au moins une preuve de la note n’est plus disponible dans le transcript effectif.',
      ));
      return;
    }
    this.busy.set(true);
    this.error.set(null);
    this.api.decideNote(this.visitId, current.id, decision)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: note => this.note.set(note),
        error: error => this.error.set(this.errorMessage(error)),
      });
  }

  toggleEvidence(statementId: string): void {
    this.expandedStatementId.update(current => current === statementId ? null : statementId);
  }

  evidenceFor(statement: AmbientNoteStatement): AmbientTranscriptItem[] {
    const byId = new Map(this.transcript().map(item => [item.id, item]));
    return statement.evidenceItemIds
      .map(id => byId.get(id))
      .filter((item): item is AmbientTranscriptItem => !!item)
      .sort((a, b) => a.startOffsetMs - b.startOffsetMs || a.sequence - b.sequence);
  }

  hasMissingEvidence(note: AmbientNoteRevision): boolean {
    const ids = new Set(this.transcript().map(item => item.id));
    return note.statements.some(statement => statement.evidenceItemIds.some(id => !ids.has(id)));
  }

  orderedSections(note: AmbientNoteRevision): string[] {
    const templateOrder: Record<AmbientNoteTemplate, string[]> = {
      SOAP: ['SUBJECTIVE', 'OBJECTIVE', 'ASSESSMENT', 'PLAN'],
      APSO: ['ASSESSMENT', 'PLAN', 'SUBJECTIVE', 'OBJECTIVE'],
      MULTI_SECTION: ['CHIEF_COMPLAINT', 'HISTORY', 'EXAM', 'VITALS', 'ASSESSMENT', 'PLAN', 'MEDICATIONS', 'ORDERS', 'FOLLOW_UP'],
    };
    const present = new Set(note.statements.map(statement => statement.section));
    return templateOrder[note.template].filter(section => present.has(section));
  }

  statementsFor(note: AmbientNoteRevision, section: string): AmbientNoteStatement[] {
    return note.statements
      .filter(statement => statement.section === section)
      .sort((a, b) => a.order - b.order);
  }

  isCriticalSection(note: AmbientNoteRevision, section: string): boolean {
    return note.statements.some(statement => statement.section === section && statement.critical);
  }

  formatOffset(milliseconds: number): string {
    const totalSeconds = Math.max(0, Math.floor(milliseconds / 1000));
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  }

  sectionLabel(section: string): string {
    const labels: Record<string, string> = {
      SUBJECTIVE: 'Subjectif',
      OBJECTIVE: 'Objectif',
      ASSESSMENT: 'Évaluation',
      PLAN: 'Plan',
      CHIEF_COMPLAINT: 'Motif',
      HISTORY: 'Histoire',
      EXAM: 'Examen',
      VITALS: 'Constantes',
      MEDICATIONS: 'Médicaments',
      ORDERS: 'Examens / prescriptions',
      FOLLOW_UP: 'Suivi',
    };
    return labels[section] ?? section;
  }

  speakerLabel(speaker: AmbientTranscriptItem['speakerType']): string {
    if (speaker === 'DOCTOR') return 'Médecin';
    if (speaker === 'PATIENT') return 'Patient';
    return 'Locuteur non attribué';
  }

  speakerClasses(speaker: AmbientTranscriptItem['speakerType']): string {
    if (speaker === 'DOCTOR') return 'rounded-[3px] bg-cyan-50 px-1.5 py-0.5 text-cyan-800 dark:bg-cyan-950/30 dark:text-cyan-200';
    if (speaker === 'PATIENT') return 'rounded-[3px] bg-emerald-50 px-1.5 py-0.5 text-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-200';
    return 'rounded-[3px] bg-amber-50 px-1.5 py-0.5 text-amber-800 dark:bg-amber-950/30 dark:text-amber-200';
  }

  statusLabel(status: AmbientNoteRevision['status']): string {
    if (status === 'ACCEPTED') return 'Validée';
    if (status === 'REJECTED') return 'Rejetée';
    return 'À vérifier';
  }

  statusClasses(status: AmbientNoteRevision['status']): string {
    if (status === 'ACCEPTED') return 'border-emerald-300 bg-emerald-50 text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/30 dark:text-emerald-200';
    if (status === 'REJECTED') return 'border-rose-300 bg-rose-50 text-rose-800 dark:border-rose-800 dark:bg-rose-950/30 dark:text-rose-200';
    return 'border-amber-300 bg-amber-50 text-amber-800 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200';
  }

  private errorMessage(error: unknown): string {
    const payload = error && typeof error === 'object' ? (error as { error?: unknown }).error : null;
    const detail = payload && typeof payload === 'object'
      ? (payload as { detail?: unknown }).detail
      : null;
    const reason = typeof detail === 'string' ? detail : '';
    const messages: Record<string, string> = {
      AI_AMBIENT_TRANSCRIPT_EMPTY: 'Le transcript ambient final est vide. Aucun contenu ne sera inventé.',
      AI_AMBIENT_NOTE_UNGROUNDED: 'La note proposée contenait au moins une affirmation non prouvable. Joprelys a bloqué toute la génération.',
      AI_AMBIENT_NOTE_CRITICAL_REQUIRES_DOCTOR_EVIDENCE: 'Une section critique n’était pas soutenue par une parole attribuée au médecin. Attribution/correction requise.',
      AI_AMBIENT_NOTE_OUTPUT_INVALID: 'La sortie IA ne respecte pas le contrat clinique strict et a été rejetée.',
    };
    return messages[reason] ?? this.i18n.t(
      'consultation.ai.ambientNoteError',
      'La note n’a pas été produite de façon suffisamment sûre. Le transcript original reste intact et auditable.',
    );
  }
}

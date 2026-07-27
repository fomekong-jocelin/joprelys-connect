import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, Input, OnChanges, SimpleChanges, computed, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import {
  ClinicalLinkedEvidence,
  ClinicalNoteEntry,
  ClinicalNoteProjection,
  ClinicalNoteProjectionApiService,
  ClinicalNoteSectionCode,
  ClinicalNoteValidation,
} from './clinical-note-projection-api.service';

@Component({
  selector: 'app-linked-evidence-note-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="ui-card overflow-hidden">
      <header class="border-b border-[var(--app-border)] bg-[var(--app-surface-muted)] px-4 py-3 sm:px-5 sm:py-4">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-sm font-bold text-[var(--text-primary)]">
                {{ i18n.t('consultation.linkedEvidence.title') }}
              </h2>
              @if (currentValidation()) {
                <span class="rounded-[var(--radius-brand-xs)] border border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] px-2 py-0.5 text-[10px] font-bold text-[var(--brand-success-text)]">
                  {{ i18n.t('consultation.linkedEvidence.validated') }}
                </span>
              } @else if (projection()) {
                <span class="rounded-[var(--radius-brand-xs)] border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] px-2 py-0.5 text-[10px] font-bold text-[var(--brand-warning-text)]">
                  {{ i18n.t('consultation.linkedEvidence.reviewRequired') }}
                </span>
              }
            </div>
            <p class="mt-1 max-w-2xl text-xs leading-5 text-[var(--text-muted)]">
              {{ i18n.t('consultation.linkedEvidence.subtitle') }}
            </p>
          </div>

          <button
            type="button"
            class="ui-button ui-button-secondary w-full shrink-0 sm:w-auto"
            [disabled]="loading() || validating()"
            (click)="refresh()"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            {{ i18n.t('consultation.linkedEvidence.refresh') }}
          </button>
        </div>
      </header>

      <div class="p-4 sm:p-5">
        @if (loading()) {
          <div class="flex min-h-28 items-center justify-center" role="status">
            <div class="text-center">
              <span class="inline-block h-6 w-6 animate-spin rounded-full border-2 border-[var(--app-border)] border-t-[var(--brand-primary)]"></span>
              <p class="mt-2 text-xs font-medium text-[var(--text-muted)]">
                {{ i18n.t('consultation.linkedEvidence.loading') }}
              </p>
            </div>
          </div>
        } @else if (loadErrorKey()) {
          <div class="rounded-[var(--radius-brand-sm)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 text-sm text-[var(--brand-danger-text)]" role="alert">
            <p class="font-semibold">{{ i18n.t(loadErrorKey()) }}</p>
            <p class="mt-1 text-xs leading-5 opacity-90">{{ i18n.t('consultation.linkedEvidence.loadErrorHelp') }}</p>
          </div>
        } @else if (projection(); as note) {
          @if (validationNoticeKey()) {
            <div
              class="mb-4 rounded-[var(--radius-brand-sm)] border p-3 text-sm"
              [ngClass]="validationNoticeClasses()"
              role="status"
            >
              <p class="font-semibold">{{ i18n.t(validationNoticeKey()) }}</p>
              @if (validationNoticeKey() === 'consultation.linkedEvidence.stale') {
                <p class="mt-1 text-xs leading-5">{{ i18n.t('consultation.linkedEvidence.staleHelp') }}</p>
              }
            </div>
          }

          @if (note.sections.length === 0) {
            <div class="rounded-[var(--radius-brand-sm)] border border-dashed border-[var(--app-border)] bg-[var(--app-surface-muted)] p-5 text-center">
              <p class="text-sm font-semibold text-[var(--text-secondary)]">
                {{ i18n.t('consultation.linkedEvidence.empty') }}
              </p>
              <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
                {{ i18n.t('consultation.linkedEvidence.emptyHelp') }}
              </p>
            </div>
          } @else {
            <div class="space-y-4">
              @for (section of note.sections; track section.code) {
                <section>
                  <div class="mb-2 flex items-center justify-between gap-3">
                    <h3 class="text-xs font-bold uppercase tracking-wide text-[var(--text-secondary)]">
                      {{ sectionLabel(section.code) }}
                    </h3>
                    <span class="text-[10px] font-semibold text-[var(--text-muted)]">
                      {{ section.entries.length }}
                    </span>
                  </div>

                  <div class="space-y-2">
                    @for (entry of section.entries; track entry.factId) {
                      <article class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 sm:p-4">
                        <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                          <div class="min-w-0">
                            <div class="flex flex-wrap items-center gap-1.5">
                              <p class="text-sm font-semibold leading-5 text-[var(--text-primary)]">
                                {{ entryTitle(entry) }}
                              </p>
                              @if (entry.polarity !== 'POSITIVE') {
                                <span class="rounded-[var(--radius-brand-xs)] border px-1.5 py-0.5 text-[10px] font-bold" [ngClass]="polarityClasses(entry.polarity)">
                                  {{ polarityLabel(entry.polarity) }}
                                </span>
                              }
                            </div>

                            @if (entryValue(entry)) {
                              <p class="mt-1 text-sm leading-5 text-[var(--text-secondary)]">
                                {{ entryValue(entry) }}
                              </p>
                            }

                            <div class="mt-2 flex flex-wrap gap-1.5">
                              <span class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-muted)]">
                                {{ authorityLabel(entry.authority) }}
                              </span>
                              @if (entry.temporalityText) {
                                <span class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-muted)]">
                                  {{ entry.temporalityText }}
                                </span>
                              }
                              @if (entry.laterality && entry.laterality !== 'UNSPECIFIED') {
                                <span class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-muted)]">
                                  {{ lateralityLabel(entry.laterality) }}
                                </span>
                              }
                              @if (entry.frequencyText) {
                                <span class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-muted)]">
                                  {{ entry.frequencyText }}
                                </span>
                              }
                              @if (entry.routeText) {
                                <span class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-2 py-0.5 text-[10px] font-semibold text-[var(--text-muted)]">
                                  {{ entry.routeText }}
                                </span>
                              }
                            </div>
                          </div>

                          <button
                            type="button"
                            class="ui-link inline-flex shrink-0 items-center gap-1.5 self-start text-xs"
                            (click)="toggleEvidence(entry.factId)"
                            [attr.aria-expanded]="isEvidenceOpen(entry.factId)"
                          >
                            <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                            </svg>
                            {{ i18n.t(isEvidenceOpen(entry.factId)
                              ? 'consultation.linkedEvidence.hideEvidence'
                              : 'consultation.linkedEvidence.showEvidence') }}
                            <span class="text-[10px] text-[var(--text-muted)]">({{ entry.evidence.length }})</span>
                          </button>
                        </div>

                        @if (isEvidenceOpen(entry.factId)) {
                          <div class="mt-3 space-y-2 border-t border-[var(--app-border)] pt-3">
                            @for (evidence of entry.evidence; track evidence.transcriptItemId + ':' + evidence.quoteStartChar) {
                              <div class="rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-3">
                                <div class="flex flex-wrap items-center justify-between gap-2">
                                  <div class="flex flex-wrap items-center gap-2 text-[10px] font-bold text-[var(--text-secondary)]">
                                    <span>{{ speakerLabel(evidence) }}</span>
                                    <span aria-hidden="true">•</span>
                                    <span>{{ formatTimestamp(evidence.startOffsetMs) }}–{{ formatTimestamp(evidence.endOffsetMs) }}</span>
                                  </div>
                                  @if (evidence.primarySupport) {
                                    <span class="rounded-[var(--radius-brand-xs)] border border-[var(--brand-primary-border)] bg-[var(--app-surface)] px-1.5 py-0.5 text-[9px] font-bold text-[var(--brand-primary)]">
                                      {{ i18n.t('consultation.linkedEvidence.primaryEvidence') }}
                                    </span>
                                  }
                                </div>
                                <blockquote class="mt-2 text-sm italic leading-6 text-[var(--text-primary)]">
                                  “{{ evidence.quoteText }}”
                                </blockquote>
                              </div>
                            }
                            <p class="text-[10px] leading-4 text-[var(--text-muted)]">
                              {{ i18n.t('consultation.linkedEvidence.audioRetentionNotice') }}
                            </p>
                          </div>
                        }
                      </article>
                    }
                  </div>
                </section>
              }
            </div>

            <footer class="mt-5 border-t border-[var(--app-border)] pt-4">
              <div class="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                <div class="min-w-0 text-xs leading-5 text-[var(--text-muted)]">
                  <p>
                    {{ i18n.t('consultation.linkedEvidence.version') }}
                    <span class="font-mono font-semibold text-[var(--text-secondary)]">{{ shortVersion(note.projectionVersion) }}</span>
                  </p>
                  <p>{{ i18n.t('consultation.linkedEvidence.validationSafety') }}</p>
                  @if (currentValidation(); as validation) {
                    <p class="mt-1 font-semibold text-[var(--brand-success-text)]">
                      {{ i18n.t('consultation.linkedEvidence.validatedAt') }} {{ formatDate(validation.validatedAt) }}
                    </p>
                  }
                </div>

                @if (!currentValidation() && note.sections.length > 0) {
                  <button
                    type="button"
                    class="ui-button ui-button-primary w-full sm:w-auto"
                    [disabled]="validating() || loading()"
                    (click)="validateCurrentProjection()"
                  >
                    @if (validating()) {
                      <span class="inline-block h-4 w-4 animate-spin rounded-full border-2 border-[var(--text-inverse)]/40 border-t-[var(--text-inverse)]"></span>
                      {{ i18n.t('consultation.linkedEvidence.validating') }}
                    } @else {
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
                      </svg>
                      {{ i18n.t('consultation.linkedEvidence.validate') }}
                    }
                  </button>
                }
              </div>
            </footer>
          }
        }
      </div>
    </section>
  `,
})
export class LinkedEvidenceNotePanelComponent implements OnChanges {
  private readonly api = inject(ClinicalNoteProjectionApiService);
  readonly i18n = inject(I18nService);

  @Input({ required: true }) visitId = '';

  readonly loading = signal(false);
  readonly validating = signal(false);
  readonly projection = signal<ClinicalNoteProjection | null>(null);
  readonly validations = signal<ClinicalNoteValidation[]>([]);
  readonly loadErrorKey = signal('');
  readonly validationNoticeKey = signal('');
  readonly openEvidenceFactId = signal<string | null>(null);

  private loadGeneration = 0;

  readonly currentValidation = computed(() => {
    const note = this.projection();
    if (!note) return null;
    return this.validations().find(item => item.projectionVersion === note.projectionVersion) ?? null;
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['visitId']) return;
    this.loadGeneration += 1;
    this.loading.set(false);
    this.validating.set(false);
    this.projection.set(null);
    this.validations.set([]);
    this.openEvidenceFactId.set(null);
    this.validationNoticeKey.set('');
    this.loadErrorKey.set('');
    this.refresh();
  }

  refresh(): void {
    const visitId = this.visitId.trim();
    if (!visitId) return;
    const generation = ++this.loadGeneration;
    this.loading.set(true);
    this.loadErrorKey.set('');

    this.api.getProjection(visitId).subscribe({
      next: note => {
        if (!this.isCurrentLoad(visitId, generation)) return;
        this.projection.set(note);
        this.loading.set(false);
        this.loadValidationHistory(visitId, generation);
      },
      error: error => {
        if (!this.isCurrentLoad(visitId, generation)) return;
        this.loading.set(false);
        this.projection.set(null);
        this.loadErrorKey.set(this.projectionErrorKey(error));
      },
    });
  }

  validateCurrentProjection(): void {
    const visitId = this.visitId.trim();
    const note = this.projection();
    if (!visitId || !note || note.sections.length === 0 || this.validating()) return;

    const reviewedVersion = note.projectionVersion;
    this.validating.set(true);
    this.validationNoticeKey.set('');
    this.api.validateProjection(visitId, this.uuid(), reviewedVersion).subscribe({
      next: validation => {
        if (visitId !== this.visitId.trim()) return;
        this.validating.set(false);
        if (this.projection()?.projectionVersion !== reviewedVersion) return;
        this.validations.update(current => [validation, ...current]);
        this.validationNoticeKey.set('consultation.linkedEvidence.validationSuccess');
      },
      error: error => {
        if (visitId !== this.visitId.trim()) return;
        this.validating.set(false);
        const reason = this.backendReason(error);
        if (error instanceof HttpErrorResponse
          && error.status === 409
          && reason === 'AI_CLINICAL_NOTE_PROJECTION_STALE') {
          this.validationNoticeKey.set('consultation.linkedEvidence.stale');
          this.refresh();
          return;
        }
        this.validationNoticeKey.set('consultation.linkedEvidence.validationFailed');
      },
    });
  }

  toggleEvidence(factId: string): void {
    this.openEvidenceFactId.update(current => current === factId ? null : factId);
  }

  isEvidenceOpen(factId: string): boolean {
    return this.openEvidenceFactId() === factId;
  }

  sectionLabel(code: ClinicalNoteSectionCode): string {
    const key: Record<ClinicalNoteSectionCode, string> = {
      HISTORY_OF_PRESENT_ILLNESS: 'consultation.linkedEvidence.section.hpi',
      MEDICAL_HISTORY: 'consultation.linkedEvidence.section.history',
      ALLERGIES: 'consultation.linkedEvidence.section.allergies',
      VITALS: 'consultation.linkedEvidence.section.vitals',
      ASSESSMENT: 'consultation.linkedEvidence.section.assessment',
      MEDICATIONS: 'consultation.linkedEvidence.section.medications',
      ORDERS: 'consultation.linkedEvidence.section.orders',
      PLAN: 'consultation.linkedEvidence.section.plan',
    };
    return this.i18n.t(key[code]);
  }

  entryTitle(entry: ClinicalNoteEntry): string {
    const concept = entry.conceptText?.trim();
    if (concept) return concept;
    const keyByType: Record<string, string> = {
      SYMPTOM: 'consultation.linkedEvidence.fact.symptom',
      HISTORY: 'consultation.linkedEvidence.fact.history',
      ALLERGY: 'consultation.linkedEvidence.fact.allergy',
      VITAL: 'consultation.linkedEvidence.fact.vital',
      ASSESSMENT: 'consultation.linkedEvidence.fact.assessment',
      MEDICATION: 'consultation.linkedEvidence.fact.medication',
      ORDER: 'consultation.linkedEvidence.fact.order',
      PLAN: 'consultation.linkedEvidence.fact.plan',
    };
    return this.i18n.t(keyByType[entry.factType] ?? 'consultation.linkedEvidence.fact.other');
  }

  entryValue(entry: ClinicalNoteEntry): string {
    const values = [entry.valuePrimary, entry.valueSecondary]
      .map(value => value?.trim())
      .filter((value): value is string => !!value);
    if (!values.length) return '';
    const core = values.join(' / ');
    return entry.unitCode?.trim() ? `${core} ${entry.unitCode.trim()}` : core;
  }

  authorityLabel(authority: string): string {
    const keyByAuthority: Record<string, string> = {
      PATIENT_REPORTED: 'consultation.linkedEvidence.authority.patient',
      CLINICIAN_OBSERVED: 'consultation.linkedEvidence.authority.observed',
      CLINICIAN_DECISION: 'consultation.linkedEvidence.authority.decision',
    };
    return this.i18n.t(keyByAuthority[authority] ?? 'consultation.linkedEvidence.authority.unknown');
  }

  polarityLabel(polarity: string): string {
    if (polarity === 'NEGATIVE') return this.i18n.t('consultation.linkedEvidence.polarity.negative');
    if (polarity === 'UNCERTAIN') return this.i18n.t('consultation.linkedEvidence.polarity.uncertain');
    return this.i18n.t('consultation.linkedEvidence.polarity.positive');
  }

  polarityClasses(polarity: string): string {
    if (polarity === 'NEGATIVE') {
      return 'border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)]';
    }
    return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
  }

  lateralityLabel(laterality: string): string {
    const keyByLaterality: Record<string, string> = {
      LEFT: 'consultation.linkedEvidence.laterality.left',
      RIGHT: 'consultation.linkedEvidence.laterality.right',
      BILATERAL: 'consultation.linkedEvidence.laterality.bilateral',
    };
    return this.i18n.t(keyByLaterality[laterality] ?? 'consultation.linkedEvidence.laterality.unspecified');
  }

  speakerLabel(evidence: ClinicalLinkedEvidence): string {
    const explicit = evidence.speakerLabel?.trim();
    if (explicit) return explicit;
    if (evidence.speakerType === 'DOCTOR') return this.i18n.t('consultation.linkedEvidence.speaker.doctor');
    if (evidence.speakerType === 'PATIENT') return this.i18n.t('consultation.linkedEvidence.speaker.patient');
    return this.i18n.t('consultation.linkedEvidence.speaker.unspecified');
  }

  formatTimestamp(offsetMs: number): string {
    const safeMs = Number.isFinite(offsetMs) && offsetMs > 0 ? Math.round(offsetMs) : 0;
    const totalSeconds = Math.floor(safeMs / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;
    if (hours > 0) {
      return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
    }
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  formatDate(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;
    return new Intl.DateTimeFormat(this.i18n.currentLanguage(), {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(date);
  }

  shortVersion(version: string): string {
    const separator = version.indexOf(':');
    const hash = separator >= 0 ? version.slice(separator + 1) : version;
    return hash.length > 10 ? hash.slice(0, 10) : hash;
  }

  validationNoticeClasses(): string {
    if (this.validationNoticeKey() === 'consultation.linkedEvidence.validationSuccess') {
      return 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
    }
    return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
  }

  private loadValidationHistory(visitId: string, generation: number): void {
    this.api.getValidationHistory(visitId).subscribe({
      next: history => {
        if (this.isCurrentLoad(visitId, generation)) this.validations.set(history.validations ?? []);
      },
      error: () => {
        if (this.isCurrentLoad(visitId, generation)) this.validations.set([]);
      },
    });
  }

  private isCurrentLoad(visitId: string, generation: number): boolean {
    return generation === this.loadGeneration && visitId === this.visitId.trim();
  }

  private projectionErrorKey(error: unknown): string {
    const reason = this.backendReason(error);
    if (reason === 'AI_CLINICAL_NOTE_EVIDENCE_STALE'
      || reason === 'AI_CLINICAL_NOTE_PROJECTION_STALE') {
      return 'consultation.linkedEvidence.sourceChanged';
    }
    if (error instanceof HttpErrorResponse && error.status === 403) {
      return 'consultation.linkedEvidence.forbidden';
    }
    return 'consultation.linkedEvidence.loadFailed';
  }

  private backendReason(error: unknown): string {
    if (!(error instanceof HttpErrorResponse)) return '';
    const payload = error.error;
    if (typeof payload === 'string') return payload;
    if (payload && typeof payload === 'object') {
      const detail = (payload as { detail?: unknown; title?: unknown }).detail
        ?? (payload as { detail?: unknown; title?: unknown }).title;
      return typeof detail === 'string' ? detail : '';
    }
    return '';
  }

  private uuid(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    if (typeof crypto !== 'undefined' && typeof crypto.getRandomValues === 'function') {
      const bytes = crypto.getRandomValues(new Uint8Array(16));
      bytes[6] = (bytes[6] & 0x0f) | 0x40;
      bytes[8] = (bytes[8] & 0x3f) | 0x80;
      const hex = Array.from(bytes, value => value.toString(16).padStart(2, '0')).join('');
      return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
    }
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, token => {
      const random = Math.floor(Math.random() * 16);
      const value = token === 'x' ? random : (random & 0x3) | 0x8;
      return value.toString(16);
    });
  }
}

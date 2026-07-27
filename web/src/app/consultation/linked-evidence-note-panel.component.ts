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
  templateUrl: './linked-evidence-note-panel.component.html',
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
      bytes[6] = ((bytes[6] ?? 0) & 0x0f) | 0x40;
      bytes[8] = ((bytes[8] ?? 0) & 0x3f) | 0x80;
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

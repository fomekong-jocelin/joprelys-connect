import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiConsultationApiService,
  AiConsultationDraft,
  AiField,
  AiFinalReviewResponse,
} from './ai-consultation-api.service';
import {
  AiProposalDecisionRequest,
  AiProposalPanelComponent,
} from './ai-proposal-panel.component';

@Component({
  selector: 'app-ai-draft-preview',
  standalone: true,
  imports: [CommonModule, AiProposalPanelComponent],
  template: `
    @if (entries().length > 0) {
      <section class="ui-card-subtle space-y-3 p-3 sm:p-4">
        <div>
          <p class="ui-label">{{ i18n.t('consultation.ai.acceptedDraft') }}</p>
          <p class="mt-1 text-[10px] leading-4 text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.acceptedDraftGovernanceHelp') }}
          </p>
        </div>

        <div class="grid grid-cols-1 gap-2 sm:grid-cols-2">
          @for (entry of entries(); track entry.key) {
            <div class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3">
              <p class="ui-label">{{ fieldLabel(entry.key) }}</p>
              <p class="mt-1 line-clamp-5 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">
                {{ formatValue(entry.key, entry.value) }}
              </p>
            </div>
          }
        </div>

        @if (reviewError()) {
          <div class="rounded-[var(--radius-brand-sm)] border border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] p-3 text-xs font-semibold text-[var(--brand-warning-text)]" role="status">
            {{ reviewError() }}
          </div>
        }

        @if (finalReview()?.status === 'NO_CHANGES') {
          <div class="rounded-[var(--radius-brand-sm)] border border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] p-3 text-xs font-semibold text-[var(--brand-success-text)]" role="status">
            {{ i18n.t('consultation.ai.finalReviewNoChanges', 'Revue approfondie terminée : aucune modification sûre n’est nécessaire.') }}
          </div>
        }

        @if (finalReview(); as review) {
          @if (review.revision.proposals.length > 0) {
            <div class="space-y-2">
              <div class="rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-3">
                <p class="text-xs font-bold text-[var(--text-primary)]">
                  {{ i18n.t('consultation.ai.finalReviewTitle', 'Revue clinique approfondie') }}
                </p>
                <p class="mt-1 text-[10px] leading-4 text-[var(--text-muted)]">
                  {{ i18n.t('consultation.ai.finalReviewHelp', 'Ces propositions proviennent d’un second passage IA. Rien n’est appliqué sans votre validation explicite.') }}
                  @if (review.model) { · {{ review.model }} }
                </p>
              </div>
              <app-ai-proposal-panel
                [revisions]="[review.revision]"
                [disabled]="reviewing() || !canApply"
                (decided)="decideFinalReview($event)"
              />
            </div>
          }
        }

        <div class="flex flex-col gap-2 sm:flex-row sm:flex-wrap">
          <button
            type="button"
            (click)="startFinalReview()"
            [disabled]="!canApply || reviewing() || !visitId || finalReview()?.status === 'PENDING'"
            class="ui-button ui-button-secondary w-full sm:w-auto"
          >
            {{ reviewing()
              ? i18n.t('consultation.ai.finalReviewRunning', 'Revue approfondie en cours…')
              : i18n.t('consultation.ai.finalReviewAction', 'Faire une dernière revue IA') }}
          </button>

          <button
            type="button"
            (click)="apply.emit()"
            [disabled]="!canApply || finalReview()?.status === 'PENDING'"
            class="ui-button ui-button-primary w-full sm:w-auto"
          >
            {{ i18n.t('consultation.ai.apply') }}
          </button>
        </div>
      </section>
    }
  `,
})
export class AiDraftPreviewComponent {
  private readonly api = inject(AiConsultationApiService);
  readonly i18n = inject(I18nService);

  @Input() visitId = '';
  @Input() draft: AiConsultationDraft = {};
  @Input() canApply = true;
  @Output() readonly apply = new EventEmitter<void>();
  @Output() readonly applyPatch = new EventEmitter<AiConsultationDraft>();

  readonly finalReview = signal<AiFinalReviewResponse | null>(null);
  readonly reviewing = signal(false);
  readonly reviewError = signal('');

  entries(): Array<{ key: AiField; value: string }> {
    const diagnosis = this.draft.diagnosis?.trim();
    return (Object.entries(this.draft) as Array<[AiField, string]>)
      .filter(([key, value]) => {
        if (!value?.trim()) return false;
        if (key === 'finalDiagnosis' && diagnosis && value.trim() === diagnosis) return false;
        return true;
      })
      .map(([key, value]) => ({ key, value }));
  }

  startFinalReview(): void {
    if (!this.visitId || !this.canApply || this.reviewing()) return;
    this.reviewing.set(true);
    this.reviewError.set('');
    this.api.createFinalReview(this.visitId).subscribe({
      next: review => {
        this.finalReview.set(review);
        this.reviewing.set(false);
      },
      error: () => {
        this.reviewing.set(false);
        this.reviewError.set(this.i18n.t(
          'consultation.ai.finalReviewError',
          'La revue approfondie n’a pas pu être exécutée. Le brouillon actuel reste inchangé.',
        ));
      },
    });
  }

  decideFinalReview(request: AiProposalDecisionRequest): void {
    const review = this.finalReview();
    if (!review || this.reviewing()) return;
    this.reviewing.set(true);
    this.reviewError.set('');
    const operation = request.scope === 'PROPOSAL' && request.proposalId
      ? this.api.decideFinalReviewProposal(
          this.visitId,
          review.reviewId,
          request.proposalId,
          request.decision,
        )
      : this.api.decideFinalReview(this.visitId, review.reviewId, request.decision);
    operation.subscribe({
      next: updated => {
        this.finalReview.set(updated);
        this.reviewing.set(false);
        if (updated.status === 'DECIDED' && Object.keys(updated.acceptedPatch).length > 0) {
          this.applyPatch.emit(updated.acceptedPatch);
        }
      },
      error: () => {
        this.reviewing.set(false);
        this.reviewError.set(this.i18n.t(
          'consultation.ai.finalReviewDecisionError',
          'La décision n’a pas pu être enregistrée. Aucune modification n’a été appliquée.',
        ));
      },
    });
  }

  formatValue(field: AiField, value: string): string {
    if (!['prescription', 'labOrders', 'vitals'].includes(field)) return value;
    try {
      const parsed = JSON.parse(value);
      if (field === 'prescription' && Array.isArray(parsed)) {
        return parsed
          .map((line: Record<string, unknown>) =>
            [line['drugName'], line['dosage'], line['frequency'], line['duration']]
              .filter(Boolean)
              .join(' · '),
          )
          .join('\n');
      }
      if (field === 'labOrders' && Array.isArray(parsed)) {
        return parsed.join(', ');
      }
      if (field === 'vitals' && parsed && typeof parsed === 'object') {
        return Object.entries(parsed as Record<string, unknown>)
          .map(([key, item]) => `${this.vitalLabel(key)}: ${this.vitalValue(key, item)}`)
          .join(' · ');
      }
    } catch {
      return value;
    }
    return value;
  }

  fieldLabel(field: AiField): string {
    const labels: Record<AiField, string> = {
      symptoms: this.i18n.t('consultation.symptoms.label'),
      clinicalExam: this.i18n.t('consultation.clinicalExam.label'),
      suspectedDiagnosis: this.i18n.t('consultation.suspectedDiagnosis.label'),
      diagnosis: this.i18n.t('consultation.diagnosis.label'),
      finalDiagnosis: this.i18n.t('consultation.finalDiagnosis.label'),
      conclusion: this.i18n.t('consultation.conclusion.label'),
      advice: this.i18n.t('consultation.advice.label'),
      followUp: this.i18n.t('consultation.followUp.label'),
      prescription: this.i18n.t('consultation.ai.field.prescription'),
      labOrders: this.i18n.t('consultation.ai.field.labOrders'),
      vitals: this.i18n.t('consultation.ai.field.vitals'),
    };
    return labels[field];
  }

  private vitalLabel(key: string): string {
    return this.i18n.t(`consultation.ai.vital.${key}`, key);
  }

  private vitalValue(key: string, value: unknown): string {
    const unit = this.i18n.t(`consultation.ai.vitalUnit.${key}`, '');
    return `${String(value)}${unit ? ` ${unit}` : ''}`;
  }
}

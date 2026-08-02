import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { formatClinicalValue } from './ai-clinical-value-formatter';
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
      <section class="pt-4">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div class="min-w-0">
            <p class="text-xs font-bold uppercase tracking-wide text-[var(--text-muted)]">
              {{ i18n.t('consultation.ai.acceptedDraft', 'Brouillon prêt') }}
            </p>
            <p class="mt-0.5 text-xs leading-5 text-[var(--text-secondary)]">
              {{ entries().length }} {{ i18n.t('consultation.ai.draftSectionsReady', 'élément(s) prêt(s) à vérifier') }}
            </p>
          </div>

          <div class="flex flex-col gap-2 sm:flex-row">
            <button
              type="button"
              (click)="apply.emit()"
              [disabled]="!canApply || finalReview()?.status === 'PENDING'"
              class="ui-button ui-button-primary w-full sm:w-auto"
            >
              {{ applyLabel || i18n.t('consultation.ai.apply') }}
            </button>
            <button
              type="button"
              (click)="startFinalReview()"
              [disabled]="!canApply || !canFinalReview || reviewing() || !visitId || finalReview()?.status === 'PENDING'"
              class="ui-button ui-button-secondary w-full sm:w-auto"
            >
              {{ reviewing()
                ? i18n.t('consultation.ai.finalReviewRunning', 'Revue en cours…')
                : i18n.t('consultation.ai.finalReviewAction', 'Dernière revue IA') }}
            </button>
          </div>
        </div>

        @if (reviewError()) {
          <div class="mt-3 border-l-2 border-[var(--brand-warning-border)] py-1.5 pl-3 text-xs font-semibold text-[var(--brand-warning-text)]" role="status">
            {{ reviewError() }}
          </div>
        }

        @if (finalReview()?.status === 'NO_CHANGES') {
          <p class="mt-3 text-xs font-semibold text-[var(--brand-success-text)]" role="status">
            {{ i18n.t('consultation.ai.finalReviewNoChanges', 'Revue approfondie terminée : aucune modification sûre n’est nécessaire.') }}
          </p>
        }

        @if (finalReview(); as review) {
          @if (review.revision.proposals.length > 0) {
            <div class="mt-3 border-t border-[var(--app-border)] pt-3">
              <div class="mb-2 flex items-center justify-between gap-3">
                <p class="text-xs font-bold text-[var(--text-primary)]">
                  {{ i18n.t('consultation.ai.finalReviewTitle', 'Revue clinique approfondie') }}
                </p>
                @if (review.model) {
                  <span class="text-[9px] font-medium text-[var(--text-muted)]">{{ review.model }}</span>
                }
              </div>
              <p class="mb-3 text-[10px] leading-4 text-[var(--text-muted)]">
                {{ i18n.t('consultation.ai.finalReviewHelp', 'Les propositions restent sous votre contrôle et ne sont jamais appliquées automatiquement.') }}
              </p>
              <app-ai-proposal-panel
                [revisions]="[review.revision]"
                [disabled]="reviewing() || !canApply || !canFinalReview"
                (decided)="decideFinalReview($event)"
              />
            </div>
          }
        }

        <div class="mt-4 divide-y divide-[var(--app-border)] border-y border-[var(--app-border)]">
          @for (entry of entries(); track entry.key) {
            <div class="grid gap-1 py-3 sm:grid-cols-[11rem_1fr] sm:gap-4">
              <p class="text-[10px] font-bold uppercase tracking-wide text-[var(--text-muted)]">
                {{ fieldLabel(entry.key) }}
              </p>
              <p class="whitespace-pre-wrap text-sm leading-6 text-[var(--text-primary)]">
                {{ formatValue(entry.key, entry.value) }}
              </p>
            </div>
          }
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
  @Input() canFinalReview = true;
  @Input() applyLabel = '';
  @Output() readonly apply = new EventEmitter<void>();
  @Output() readonly applyPatch = new EventEmitter<AiConsultationDraft>();

  readonly finalReview = signal<AiFinalReviewResponse | null>(null);
  readonly reviewing = signal(false);
  readonly reviewError = signal('');

  entries(): Array<{ key: AiField; value: string }> {
    return (Object.entries(this.draft) as Array<[AiField, string]>)
      .filter(([key, value]) => {
        if (!value?.trim()) return false;
        if (key === 'vitals') return false;
        return true;
      })
      .map(([key, value]) => ({ key, value }));
  }

  startFinalReview(): void {
    if (!this.visitId || !this.canApply || !this.canFinalReview || this.reviewing()) return;
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
    if (!review || !this.canFinalReview || this.reviewing()) return;
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
    return formatClinicalValue(
      field,
      value,
      this.i18n.currentLanguage(),
      (key, item) => `${this.vitalLabel(key)}: ${this.vitalValue(key, item)}`,
    );
  }

  fieldLabel(field: AiField): string {
    const labels: Record<AiField, string> = {
      symptoms: this.i18n.t('consultation.symptoms.label'),
      clinicalExam: this.i18n.t('consultation.clinicalExam.label'),
      diagnosis: this.i18n.t('consultation.diagnosis.label'),
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

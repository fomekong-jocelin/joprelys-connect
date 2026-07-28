import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { AiDecision, AiField, AiFieldProposal, AiRevision } from './ai-consultation-api.service';

export interface AiProposalDecisionRequest {
  scope: 'PROPOSAL' | 'REVISION';
  revisionId: string;
  proposalId?: string;
  decision: AiDecision;
}

@Component({
  selector: 'app-ai-proposal-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (allPendingRevisions().length > 0) {
      <section class="ui-card-subtle space-y-3 border-[var(--brand-primary-border)] p-4">
        <header class="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="ui-label text-[var(--brand-primary)]">
              {{ i18n.t('consultation.ai.revisionTitle', 'Modifications à valider') }}
            </p>
            <p class="mt-0.5 text-xs leading-5 text-[var(--text-secondary)]">
              {{ totalPendingProposalsCount() }} {{ i18n.t('consultation.ai.pendingProposalsHelp', "proposition(s) en attente sur l'ensemble de votre dictée.") }}
            </p>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <button
              type="button"
              (click)="acceptAllPending()"
              [disabled]="disabled"
              class="ui-button ui-button-primary min-h-11 w-full sm:w-auto font-bold shadow-xs"
            >
              <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" />
              </svg>
              {{ i18n.t('consultation.ai.acceptAllGlobal', 'Tout valider (' + totalPendingProposalsCount() + ' en 1 clic)') }}
            </button>
            <button
              type="button"
              (click)="rejectAllPending()"
              [disabled]="disabled"
              class="ui-button ui-button-secondary min-h-11 w-full sm:w-auto"
            >
              {{ i18n.t('consultation.ai.rejectAllGlobal', 'Tout rejeter') }}
            </button>
          </div>
        </header>

        <div class="space-y-4">
          @for (revision of allPendingRevisions(); track revision.id) {
            <div class="space-y-2 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3">
              <div class="flex items-center justify-between">
                <span class="text-[10px] font-bold text-[var(--text-muted)]">Révision #{{ revision.sequence }}</span>
                <span class="text-[10px] font-semibold text-[var(--brand-primary)]">{{ pendingCount(revision) }} champ(s)</span>
              </div>

              @for (proposal of revision.proposals; track proposal.id) {
                <article
                  class="rounded-[var(--radius-brand-md)] border bg-[var(--app-surface)] p-3"
                  [ngClass]="proposal.status === 'PENDING'
                    ? 'border-[var(--brand-primary-border)]'
                    : proposal.status === 'ACCEPTED'
                      ? 'border-[var(--brand-success-muted)] opacity-80'
                      : 'border-[var(--app-border)] opacity-65'"
                >
                  <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <p class="text-xs font-bold text-[var(--text-primary)]">
                        {{ fieldLabel(proposal.field) }}
                      </p>
                      <p class="mt-1 text-[10px] leading-4 text-[var(--text-muted)]">{{ proposal.reason }}</p>
                    </div>
                    <div class="flex items-center gap-2">
                      <span class="rounded-[var(--radius-brand-xs)] border border-[var(--app-border)] px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider text-[var(--text-muted)]">
                        {{ uncertaintyLabel(proposal.uncertainty) }}
                      </span>
                      @if (proposal.status !== 'PENDING') {
                        <span
                          class="rounded-[var(--radius-brand-xs)] border px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider"
                          [ngClass]="proposal.status === 'ACCEPTED'
                            ? 'border-[var(--brand-success-muted)] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]'
                            : 'border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)]'"
                        >
                          {{ proposal.status === 'ACCEPTED'
                            ? i18n.t('consultation.ai.proposalAccepted')
                            : i18n.t('consultation.ai.proposalRejected') }}
                        </span>
                      }
                    </div>
                  </div>

                  <div class="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                    <div class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-2.5">
                      <p class="ui-label">{{ i18n.t('consultation.ai.previousValue') }}</p>
                      <p class="mt-1 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">
                        {{ proposal.previousValue
                          ? formatValue(proposal.field, proposal.previousValue)
                          : i18n.t('consultation.ai.emptyValue') }}
                      </p>
                    </div>
                    <div class="rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-2.5">
                      <p class="ui-label text-[var(--brand-primary)]">{{ i18n.t('consultation.ai.proposedValue') }}</p>
                      <p class="mt-1 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">
                        {{ proposal.operation === 'CLEAR'
                          ? i18n.t('consultation.ai.clearValue')
                          : formatValue(proposal.field, proposal.proposedValue || '') }}
                      </p>
                    </div>
                  </div>

                  @if (proposal.status === 'PENDING') {
                    <div class="mt-3 flex flex-col gap-2 sm:flex-row">
                      <button
                        type="button"
                        (click)="decideProposal(revision.id, proposal.id, 'ACCEPT')"
                        [disabled]="disabled"
                        class="ui-button ui-button-primary min-h-11 w-full sm:w-auto"
                      >
                        {{ i18n.t('consultation.ai.acceptProposal') }}
                      </button>
                      <button
                        type="button"
                        (click)="decideProposal(revision.id, proposal.id, 'REJECT')"
                        [disabled]="disabled"
                        class="ui-button ui-button-secondary min-h-11 w-full sm:w-auto"
                      >
                        {{ i18n.t('consultation.ai.rejectProposal') }}
                      </button>
                    </div>
                  }
                </article>
              }
            </div>
          }
        </div>
      </section>
    }

    @if (decidedRevisions().length > 0) {
      <details class="ui-card-muted">
        <summary class="cursor-pointer px-3 py-2.5 text-xs font-semibold text-[var(--text-secondary)]">
          {{ i18n.t('consultation.ai.revisionHistory') }} ({{ decidedRevisions().length }})
        </summary>
        <div class="space-y-2 border-t border-[var(--app-border)] p-3">
          @for (revision of decidedRevisions(); track revision.id) {
            <article class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 text-xs">
              <p class="font-semibold text-[var(--text-primary)]">
                {{ i18n.t('consultation.ai.revisionLabel') }} #{{ revision.sequence }}
              </p>
              <p class="mt-1 text-[var(--text-muted)]">
                {{ acceptedCount(revision) }} {{ i18n.t('consultation.ai.acceptedCount') }} ·
                {{ rejectedCount(revision) }} {{ i18n.t('consultation.ai.rejectedCount') }}
              </p>
            </article>
          }
        </div>
      </details>
    }
  `,
})
export class AiProposalPanelComponent {
  readonly i18n = inject(I18nService);

  @Input() revisions: readonly AiRevision[] = [];
  @Input() disabled = false;
  @Output() readonly decided = new EventEmitter<AiProposalDecisionRequest>();

  pendingRevision(): AiRevision | null {
    return [...this.revisions].reverse().find((revision) => revision.status === 'PENDING') ?? null;
  }

  allPendingRevisions(): AiRevision[] {
    return this.revisions.filter((revision) => revision.status === 'PENDING');
  }

  totalPendingProposalsCount(): number {
    return this.allPendingRevisions().reduce(
      (acc, revision) => acc + this.pendingCount(revision),
      0,
    );
  }

  acceptAllPending(): void {
    if (this.disabled) return;
    for (const revision of this.allPendingRevisions()) {
      this.decided.emit({ scope: 'REVISION', revisionId: revision.id, decision: 'ACCEPT' });
    }
  }

  rejectAllPending(): void {
    if (this.disabled) return;
    for (const revision of this.allPendingRevisions()) {
      this.decided.emit({ scope: 'REVISION', revisionId: revision.id, decision: 'REJECT' });
    }
  }

  decidedRevisions(): AiRevision[] {
    return this.revisions
      .filter((revision) => revision.status === 'DECIDED')
      .slice(-5)
      .reverse();
  }

  pendingCount(revision: AiRevision): number {
    return revision.proposals.filter((proposal) => proposal.status === 'PENDING').length;
  }

  acceptedCount(revision: AiRevision): number {
    return revision.proposals.filter((proposal) => proposal.status === 'ACCEPTED').length;
  }

  rejectedCount(revision: AiRevision): number {
    return revision.proposals.filter((proposal) => proposal.status === 'REJECTED').length;
  }

  decideProposal(revisionId: string, proposalId: string, decision: AiDecision): void {
    if (this.disabled) return;
    this.decided.emit({ scope: 'PROPOSAL', revisionId, proposalId, decision });
  }

  decideRevision(revisionId: string, decision: AiDecision): void {
    if (this.disabled) return;
    this.decided.emit({ scope: 'REVISION', revisionId, decision });
  }

  uncertaintyLabel(uncertainty: AiFieldProposal['uncertainty']): string {
    const labels: Record<AiFieldProposal['uncertainty'], string> = {
      LOW: this.i18n.t('consultation.ai.uncertaintyLow'),
      MEDIUM: this.i18n.t('consultation.ai.uncertaintyMedium'),
      HIGH: this.i18n.t('consultation.ai.uncertaintyHigh'),
      UNKNOWN: this.i18n.t('consultation.ai.uncertaintyUnknown'),
    };
    return labels[uncertainty];
  }

  formatValue(field: AiField, value: string): string {
    if (!['prescription', 'labOrders', 'vitals'].includes(field)) return value;
    try {
      const parsed = JSON.parse(value);
      if (field === 'prescription' && Array.isArray(parsed)) {
        return parsed
          .map((line: Record<string, unknown>) =>
            [line['drugName'], line['dosage'], line['frequency'], line['duration'], line['route']]
              .filter(Boolean)
              .join(' · '),
          )
          .join('\n');
      }
      if (field === 'labOrders' && Array.isArray(parsed)) return parsed.join(', ');
      if (field === 'vitals' && parsed && typeof parsed === 'object') {
        return Object.entries(parsed as Record<string, unknown>)
          .map(([key, item]) => `${this.vitalLabel(key)}: ${item}`)
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
}

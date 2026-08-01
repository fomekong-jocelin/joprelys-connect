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
      <section class="border-l-2 border-[var(--brand-primary-border)] py-1 pl-3 sm:pl-4">
        <header class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p class="text-[10px] font-bold uppercase tracking-wide text-[var(--brand-primary)]">
              {{ i18n.t('consultation.ai.revisionTitle', 'Modifications à valider') }}
            </p>
            <p class="mt-1 text-xs leading-5 text-[var(--text-secondary)]">
              {{ totalPendingProposalsCount() }} {{ i18n.t('consultation.ai.pendingProposalsHelp', 'proposition(s) à vérifier.') }}
            </p>
          </div>

          <div class="flex flex-col gap-2 sm:flex-row">
            <button
              type="button"
              (click)="acceptAllPending()"
              [disabled]="disabled"
              class="ui-button ui-button-primary min-h-10 w-full sm:w-auto"
            >
              {{ i18n.t('consultation.ai.acceptAllGlobal', 'Tout valider') }}
            </button>
            <button
              type="button"
              (click)="rejectAllPending()"
              [disabled]="disabled"
              class="ui-button ui-button-secondary min-h-10 w-full sm:w-auto"
            >
              {{ i18n.t('consultation.ai.rejectAllGlobal', 'Tout rejeter') }}
            </button>
          </div>
        </header>

        <div class="mt-3 divide-y divide-[var(--app-border)]">
          @for (revision of allPendingRevisions(); track revision.id) {
            @for (proposal of revision.proposals; track proposal.id) {
              <article class="py-3" [class.opacity-60]="proposal.status !== 'PENDING'">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="text-xs font-bold text-[var(--text-primary)]">{{ fieldLabel(proposal.field) }}</p>
                    @if (proposal.reason) {
                      <p class="mt-0.5 text-[10px] leading-4 text-[var(--text-muted)]">{{ proposal.reason }}</p>
                    }
                  </div>
                  <span class="shrink-0 text-[9px] font-bold uppercase tracking-wide text-[var(--text-muted)]">
                    {{ uncertaintyLabel(proposal.uncertainty) }}
                  </span>
                </div>

                <div class="mt-2 grid gap-2 sm:grid-cols-2 sm:gap-4">
                  <div>
                    <p class="text-[9px] font-bold uppercase tracking-wide text-[var(--text-muted)]">
                      {{ i18n.t('consultation.ai.previousValue') }}
                    </p>
                    <p class="mt-1 whitespace-pre-wrap text-xs leading-5 text-[var(--text-secondary)]">
                      {{ proposal.previousValue
                        ? formatValue(proposal.field, proposal.previousValue)
                        : i18n.t('consultation.ai.emptyValue') }}
                    </p>
                  </div>
                  <div class="border-l-2 border-[var(--brand-primary-border)] pl-3">
                    <p class="text-[9px] font-bold uppercase tracking-wide text-[var(--brand-primary)]">
                      {{ i18n.t('consultation.ai.proposedValue') }}
                    </p>
                    <p class="mt-1 whitespace-pre-wrap text-xs font-medium leading-5 text-[var(--text-primary)]">
                      {{ proposal.operation === 'CLEAR'
                        ? i18n.t('consultation.ai.clearValue')
                        : formatValue(proposal.field, proposal.proposedValue || '') }}
                    </p>
                  </div>
                </div>

                @if (proposal.status === 'PENDING') {
                  <div class="mt-2 flex gap-3">
                    <button
                      type="button"
                      (click)="decideProposal(revision.id, proposal.id, 'ACCEPT')"
                      [disabled]="disabled"
                      class="text-xs font-bold text-[var(--brand-primary)] hover:underline disabled:opacity-50"
                    >
                      {{ i18n.t('consultation.ai.acceptProposal') }}
                    </button>
                    <button
                      type="button"
                      (click)="decideProposal(revision.id, proposal.id, 'REJECT')"
                      [disabled]="disabled"
                      class="text-xs font-semibold text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:underline disabled:opacity-50"
                    >
                      {{ i18n.t('consultation.ai.rejectProposal') }}
                    </button>
                  </div>
                } @else {
                  <p class="mt-2 text-[10px] font-semibold text-[var(--text-muted)]">
                    {{ proposal.status === 'ACCEPTED'
                      ? i18n.t('consultation.ai.proposalAccepted')
                      : i18n.t('consultation.ai.proposalRejected') }}
                  </p>
                }
              </article>
            }
          }
        </div>
      </section>
    }

    @if (decidedRevisions().length > 0) {
      <details class="mt-3 border-t border-[var(--app-border)] pt-1">
        <summary class="cursor-pointer py-2 text-xs font-medium text-[var(--text-muted)] hover:text-[var(--text-primary)]">
          {{ i18n.t('consultation.ai.revisionHistory') }} ({{ decidedRevisions().length }})
        </summary>
        <div class="divide-y divide-[var(--app-border)]">
          @for (revision of decidedRevisions(); track revision.id) {
            <div class="flex items-center justify-between gap-3 py-2 text-xs">
              <span class="font-semibold text-[var(--text-primary)]">{{ i18n.t('consultation.ai.revisionLabel') }} #{{ revision.sequence }}</span>
              <span class="text-[var(--text-muted)]">
                {{ acceptedCount(revision) }} {{ i18n.t('consultation.ai.acceptedCount') }} ·
                {{ rejectedCount(revision) }} {{ i18n.t('consultation.ai.rejectedCount') }}
              </span>
            </div>
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
            [
              line['drugName'],
              line['dosage'],
              line['form'],
              line['posology'],
              line['frequency'],
              line['route'],
              line['duration'],
              line['quantity'],
              line['instructions'],
            ]
              .filter(item => typeof item === 'string' && !!item.trim())
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
}

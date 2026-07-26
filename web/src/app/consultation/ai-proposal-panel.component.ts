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
    @if (pendingRevision(); as revision) {
      <section
        class="space-y-3 rounded-[6px] border border-violet-300 bg-violet-50/60 p-4 dark:border-violet-800 dark:bg-violet-950/20"
      >
        <header class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p
              class="text-[10px] font-bold uppercase tracking-wider text-violet-700 dark:text-violet-300"
            >
              {{ i18n.t('consultation.ai.revisionTitle', 'Modifications à valider') }}
            </p>
            <p class="mt-1 text-xs leading-5 text-violet-900 dark:text-violet-100">
              {{
                i18n.t(
                  'consultation.ai.revisionGovernanceHelp',
                  'Le copilote propose uniquement. Les données restent inchangées jusqu’à votre validation explicite.'
                )
              }}
            </p>
          </div>
          <span
            class="w-fit rounded-[3px] border border-violet-300 px-2 py-1 text-[10px] font-semibold text-violet-800 dark:border-violet-700 dark:text-violet-200"
          >
            #{{ revision.sequence }} · {{ pendingCount(revision) }}
          </span>
        </header>

        <div class="space-y-3">
          @for (proposal of revision.proposals; track proposal.id) {
            <article
              class="rounded-[6px] border bg-[var(--app-surface)] p-3"
              [ngClass]="
                proposal.status === 'PENDING'
                  ? 'border-violet-200 dark:border-violet-800'
                  : proposal.status === 'ACCEPTED'
                    ? 'border-emerald-200 opacity-75 dark:border-emerald-800'
                    : 'border-slate-200 opacity-60 dark:border-slate-700'
              "
            >
              <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <p class="text-xs font-bold text-[var(--text-primary)]">
                    {{ fieldLabel(proposal.field) }}
                  </p>
                  <p class="mt-1 text-[10px] text-[var(--text-muted)]">{{ proposal.reason }}</p>
                </div>
                <div class="flex items-center gap-2">
                  <span
                    class="rounded-[3px] border border-[var(--app-border)] px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider text-[var(--text-muted)]"
                  >
                    {{ uncertaintyLabel(proposal.uncertainty) }}
                  </span>
                  @if (proposal.status !== 'PENDING') {
                    <span
                      class="rounded-[3px] px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider"
                      [ngClass]="
                        proposal.status === 'ACCEPTED'
                          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200'
                          : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-200'
                      "
                    >
                      {{
                        proposal.status === 'ACCEPTED'
                          ? i18n.t('consultation.ai.proposalAccepted', 'Acceptée')
                          : i18n.t('consultation.ai.proposalRejected', 'Rejetée')
                      }}
                    </span>
                  }
                </div>
              </div>

              <div class="mt-3 grid grid-cols-1 gap-2 sm:grid-cols-2">
                <div
                  class="rounded-[4px] border border-rose-100 bg-rose-50/50 p-2.5 dark:border-rose-950 dark:bg-rose-950/20"
                >
                  <p
                    class="text-[9px] font-bold uppercase tracking-wider text-rose-700 dark:text-rose-300"
                  >
                    {{ i18n.t('consultation.ai.previousValue', 'Avant') }}
                  </p>
                  <p class="mt-1 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">
                    {{
                      proposal.previousValue
                        ? formatValue(proposal.field, proposal.previousValue)
                        : i18n.t('consultation.ai.emptyValue', 'Vide')
                    }}
                  </p>
                </div>
                <div
                  class="rounded-[4px] border border-emerald-100 bg-emerald-50/50 p-2.5 dark:border-emerald-950 dark:bg-emerald-950/20"
                >
                  <p
                    class="text-[9px] font-bold uppercase tracking-wider text-emerald-700 dark:text-emerald-300"
                  >
                    {{ i18n.t('consultation.ai.proposedValue', 'Après') }}
                  </p>
                  <p class="mt-1 whitespace-pre-wrap text-xs leading-5 text-[var(--text-primary)]">
                    {{
                      proposal.operation === 'CLEAR'
                        ? i18n.t('consultation.ai.clearValue', 'Supprimer cette valeur')
                        : formatValue(proposal.field, proposal.proposedValue || '')
                    }}
                  </p>
                </div>
              </div>

              @if (proposal.status === 'PENDING') {
                <div class="mt-3 flex flex-col gap-2 sm:flex-row">
                  <button
                    type="button"
                    (click)="decideProposal(revision.id, proposal.id, 'ACCEPT')"
                    [disabled]="disabled"
                    class="inline-flex min-h-11 w-full items-center justify-center rounded-[4px] bg-emerald-600 px-3 py-2 text-xs font-bold text-white hover:bg-emerald-700 disabled:opacity-50 sm:w-auto"
                  >
                    {{ i18n.t('consultation.ai.acceptProposal', 'Accepter ce champ') }}
                  </button>
                  <button
                    type="button"
                    (click)="decideProposal(revision.id, proposal.id, 'REJECT')"
                    [disabled]="disabled"
                    class="inline-flex min-h-11 w-full items-center justify-center rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 text-xs font-bold text-[var(--text-secondary)] hover:bg-[var(--app-surface-muted)] disabled:opacity-50 sm:w-auto"
                  >
                    {{ i18n.t('consultation.ai.rejectProposal', 'Rejeter ce champ') }}
                  </button>
                </div>
              }
            </article>
          }
        </div>

        @if (pendingCount(revision) > 1) {
          <div
            class="flex flex-col gap-2 border-t border-violet-200 pt-3 sm:flex-row dark:border-violet-800"
          >
            <button
              type="button"
              (click)="decideRevision(revision.id, 'ACCEPT')"
              [disabled]="disabled"
              class="inline-flex min-h-11 w-full items-center justify-center rounded-[4px] bg-violet-700 px-4 py-2 text-xs font-bold text-white hover:bg-violet-800 disabled:opacity-50 sm:w-auto"
            >
              {{ i18n.t('consultation.ai.acceptAll', 'Tout accepter') }}
            </button>
            <button
              type="button"
              (click)="decideRevision(revision.id, 'REJECT')"
              [disabled]="disabled"
              class="inline-flex min-h-11 w-full items-center justify-center rounded-[4px] border border-violet-300 bg-[var(--app-surface)] px-4 py-2 text-xs font-bold text-violet-800 hover:bg-violet-100 disabled:opacity-50 sm:w-auto dark:border-violet-700 dark:text-violet-200 dark:hover:bg-violet-950/40"
            >
              {{ i18n.t('consultation.ai.rejectAll', 'Tout rejeter') }}
            </button>
          </div>
        }
      </section>
    }

    @if (decidedRevisions().length > 0) {
      <details
        class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/20"
      >
        <summary
          class="cursor-pointer px-3 py-2.5 text-xs font-semibold text-[var(--text-secondary)]"
        >
          {{ i18n.t('consultation.ai.revisionHistory', 'Historique des révisions') }}
          ({{ decidedRevisions().length }})
        </summary>
        <div class="space-y-2 border-t border-[var(--app-border)] p-3">
          @for (revision of decidedRevisions(); track revision.id) {
            <article
              class="rounded-[4px] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 text-xs"
            >
              <p class="font-semibold text-[var(--text-primary)]">
                {{ i18n.t('consultation.ai.revisionLabel', 'Révision') }} #{{ revision.sequence }}
              </p>
              <p class="mt-1 text-[var(--text-muted)]">
                {{ acceptedCount(revision) }}
                {{ i18n.t('consultation.ai.acceptedCount', 'acceptée(s)') }} ·
                {{ rejectedCount(revision) }}
                {{ i18n.t('consultation.ai.rejectedCount', 'rejetée(s)') }}
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
      LOW: this.i18n.t('consultation.ai.uncertaintyLow', 'Confiance élevée'),
      MEDIUM: this.i18n.t('consultation.ai.uncertaintyMedium', 'À vérifier'),
      HIGH: this.i18n.t('consultation.ai.uncertaintyHigh', 'Incertitude forte'),
      UNKNOWN: this.i18n.t('consultation.ai.uncertaintyUnknown', 'Non évaluée'),
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
          .map(([key, item]) => `${key}: ${item}`)
          .join(' · ');
      }
    } catch {
      return value;
    }
    return value;
  }

  fieldLabel(field: AiField): string {
    const labels: Record<AiField, string> = {
      symptoms: this.i18n.t('consultation.symptoms.label', 'Symptômes'),
      clinicalExam: this.i18n.t('consultation.clinicalExam.label', 'Examen clinique'),
      suspectedDiagnosis: this.i18n.t(
        'consultation.suspectedDiagnosis.label',
        'Hypothèse diagnostique',
      ),
      diagnosis: this.i18n.t('consultation.diagnosis.label', 'Diagnostic'),
      finalDiagnosis: this.i18n.t('consultation.finalDiagnosis.label', 'Diagnostic final'),
      conclusion: this.i18n.t('consultation.conclusion.label', 'Conclusion'),
      advice: this.i18n.t('consultation.advice.label', 'Conseils au patient'),
      followUp: this.i18n.t('consultation.followUp.label', 'Suivi recommandé'),
      prescription: this.i18n.t('consultation.ai.field.prescription', 'Ordonnance'),
      labOrders: this.i18n.t('consultation.ai.field.labOrders', 'Examens biologiques'),
      vitals: this.i18n.t('consultation.ai.field.vitals', 'Constantes vitales'),
    };
    return labels[field];
  }
}

import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConversationMessage } from './ai-consultation-api.service';

@Component({
  selector: 'app-ai-conversation-thread',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section
      class="rounded-[6px] border border-[var(--app-border)] bg-[var(--app-surface-muted)]/30"
      aria-live="polite"
    >
      <header class="flex items-center justify-between border-b border-[var(--app-border)] px-3 py-2.5">
        <div>
          <p class="text-xs font-bold text-[var(--text-primary)]">
            {{ i18n.t('consultation.ai.conversationTitle', 'Conversation avec l’assistant') }}
          </p>
          <p class="text-[10px] text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.conversationHelp', 'Les échanges restent liés à cette session de consultation.') }}
          </p>
        </div>
        <span class="text-[10px] font-semibold text-[var(--text-muted)]">
          {{ messages.length }}
        </span>
      </header>

      <div class="max-h-80 space-y-3 overflow-y-auto p-3">
        @for (message of messages; track message.id) {
          <article
            class="flex"
            [ngClass]="message.role === 'USER' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[88%] rounded-[6px] border px-3 py-2.5 shadow-xs"
              [ngClass]="message.role === 'USER'
                ? 'border-cyan-200 bg-cyan-50 text-cyan-950 dark:border-cyan-900 dark:bg-cyan-950/30 dark:text-cyan-100'
                : message.needsClarification
                  ? 'border-amber-200 bg-amber-50 text-amber-950 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-100'
                  : 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)]'"
            >
              <div class="mb-1 flex items-center justify-between gap-3 text-[10px] font-semibold uppercase tracking-wider opacity-70">
                <span>
                  {{ message.role === 'USER'
                    ? i18n.t('consultation.ai.doctorRole', 'Médecin')
                    : i18n.t('consultation.ai.assistantRole', 'Assistant') }}
                </span>
                <time [attr.datetime]="message.createdAt">
                  {{ message.createdAt | date:'HH:mm' }}
                </time>
              </div>
              <p class="whitespace-pre-wrap text-xs leading-5">{{ message.content }}</p>
              @if (message.needsClarification) {
                <span class="mt-2 inline-flex rounded-[3px] border border-amber-300 px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-wider dark:border-amber-700">
                  {{ i18n.t('consultation.ai.clarificationNeeded', 'Précision attendue') }}
                </span>
              }
            </div>
          </article>
        } @empty {
          <p class="py-5 text-center text-xs text-[var(--text-muted)]">
            {{ i18n.t('consultation.ai.noConversation', 'Aucun échange pour le moment.') }}
          </p>
        }
      </div>
    </section>
  `,
})
export class AiConversationThreadComponent {
  readonly i18n = inject(I18nService);

  @Input() messages: readonly AiConversationMessage[] = [];
}

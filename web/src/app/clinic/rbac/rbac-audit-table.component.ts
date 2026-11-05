import { Component, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppDatePipe } from '../../shared/formatting/app-date.pipe';
import { RbacAuditEntry } from './rbac.models';

/** Onglet « Historique » : qui a changé quels droits, et quand. */
@Component({
  selector: 'app-rbac-audit-table',
  standalone: true,
  imports: [AppDatePipe],
  template: `
    <section class="ui-card min-w-0 overflow-hidden" aria-labelledby="rbac-audit-title">
      <div class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--app-border)] p-4">
        <div class="min-w-0">
          <h2 id="rbac-audit-title" class="text-base font-bold text-[var(--text-primary)]">{{ t('rbac.audit.title') }}</h2>
          <p class="text-sm text-[var(--text-secondary)]">{{ t('rbac.audit.help') }}</p>
        </div>
        <button type="button" class="ui-button ui-button-secondary" [disabled]="loading()" (click)="refresh.emit()">{{ t('common.refresh') }}</button>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full text-left text-sm">
          <caption class="sr-only">{{ t('rbac.audit.title') }}</caption>
          <thead class="bg-[var(--app-surface-muted)] text-xs text-[var(--text-secondary)]">
            <tr>
              <th scope="col" class="px-4 py-3 font-semibold">{{ t('rbac.audit.date') }}</th>
              <th scope="col" class="px-4 py-3 font-semibold">{{ t('rbac.audit.action') }}</th>
              <th scope="col" class="px-4 py-3 font-semibold">{{ t('rbac.audit.details') }}</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-[var(--app-border)]">
            @for (entry of entries(); track entry.id) {
              <tr>
                <td class="whitespace-nowrap px-4 py-3 text-[var(--text-secondary)]">{{ entry.createdAt | appDate: 'dateTime' }}</td>
                <td class="px-4 py-3 font-semibold text-[var(--text-primary)]">{{ actionLabel(entry.action) }}</td>
                <td class="px-4 py-3 text-[var(--text-secondary)]">{{ entry.details || '—' }}</td>
              </tr>
            } @empty {
              <tr>
                <td colspan="3" class="px-4 py-10 text-center text-[var(--text-muted)]">{{ t('rbac.audit.empty') }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </section>
  `,
})
export class RbacAuditTableComponent {
  private readonly i18n = inject(I18nService);

  readonly entries = input.required<readonly RbacAuditEntry[]>();
  readonly loading = input(false);
  readonly refresh = output<void>();

  readonly t = (key: string) => this.i18n.t(key);

  /** Une action inconnue reste lisible telle quelle plutôt que de disparaître. */
  actionLabel(action: string): string {
    return this.i18n.t(`rbac.audit.actions.${action}`, action);
  }
}

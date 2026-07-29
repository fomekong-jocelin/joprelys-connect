import { Component, inject, OnInit, signal } from '@angular/core';
import { AuditApiService } from '../../audit/audit-api.service';
import { AuditLog } from '../../audit/audit.models';
import { RbacApiService } from '../../clinic/rbac/rbac-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientDetailComponent } from '../patient-detail.component';

@Component({
  selector: 'app-patient-audit-trail-tab',
  standalone: true,
  template: `
    <div class="min-w-0 space-y-3 animate-fade-in text-xs text-[var(--text-secondary)]">
      @if (canViewAudit()) {
        <!-- The mobile section selector already names this page; keep the duplicate heading desktop-only. -->
        <h3 class="mb-4 hidden text-xs font-black uppercase tracking-wider text-[var(--text-muted)] md:block">
          {{ i18n.t('patients.auditLogsTitle') }}
        </h3>

        @if (isLoadingAudit()) {
          <div class="py-12 text-center">
            <div class="inline-block h-6 w-6 animate-spin rounded-full border-2 border-indigo-200 border-t-indigo-600"></div>
            <p class="mt-2 text-xs font-bold text-[var(--text-muted)]">{{ i18n.t('patients.auditLogsLoading') }}</p>
          </div>
        } @else if (auditLogs().length === 0) {
          <div class="rounded-md border border-dashed border-[var(--app-border)]/80 p-8 text-center">
            <p class="text-sm font-semibold text-[var(--text-muted)]">{{ i18n.t('patients.auditLogsEmpty') }}</p>
          </div>
        } @else {
          <div class="grid min-w-0 gap-3" data-testid="patient-audit-list">
            @for (log of auditLogs(); track log.id) {
              <article class="ui-card min-w-0 overflow-hidden" [attr.data-audit-id]="log.id">
                <button
                  type="button"
                  class="grid min-h-14 w-full grid-cols-[auto_minmax(0,1fr)_auto] items-start gap-3 p-3 text-left sm:p-4"
                  [attr.aria-expanded]="isExpanded(log.id)"
                  (click)="toggleLog(log.id)"
                  data-testid="patient-audit-toggle"
                >
                  <span
                    [class]="log.status === 'SUCCESS'
                      ? 'mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-green-50 text-[var(--brand-success-text)] dark:bg-green-950/20'
                      : 'mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-rose-50 text-rose-600 dark:bg-rose-950/20 dark:text-rose-400'"
                    [attr.aria-label]="log.status"
                  >
                    @if (log.status === 'SUCCESS') {
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                      </svg>
                    } @else {
                      <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" aria-hidden="true">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                      </svg>
                    }
                  </span>

                  <span class="min-w-0">
                    <span class="block break-words text-sm font-extrabold leading-5 text-[var(--text-primary)]" data-testid="patient-audit-title">
                      {{ compactTitle(log) }}
                    </span>
                    <span class="mt-1.5 flex min-w-0 flex-wrap items-center gap-x-2 gap-y-1 text-[11px] leading-4 text-[var(--text-muted)]">
                      <span class="min-w-0 break-words">
                        {{ i18n.t('patients.auditLogsUser') }}
                        <strong class="font-extrabold text-[var(--brand-info-text)]">{{ log.actorName || 'Système' }}</strong>
                      </span>
                      <span aria-hidden="true">·</span>
                      <time class="whitespace-nowrap" [attr.datetime]="log.createdAt">{{ formatDateTime(log.createdAt) }}</time>
                    </span>
                  </span>

                  <svg
                    class="mt-2 h-4 w-4 shrink-0 text-[var(--text-muted)] transition-transform"
                    [class.rotate-180]="isExpanded(log.id)"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    aria-hidden="true"
                  >
                    <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
                  </svg>
                </button>

                @if (isExpanded(log.id)) {
                  <div class="border-t border-[var(--divider-subtle)] bg-[var(--app-surface-muted)] p-3 sm:p-4 animate-fade-in" data-testid="patient-audit-details">
                    <dl class="grid min-w-0 gap-3">
                      <div class="flex min-w-0 items-baseline gap-2 whitespace-nowrap" data-testid="patient-audit-action-row">
                        <dt class="ui-label shrink-0">{{ i18n.t('patients.auditLogsAction') }}</dt>
                        <dd class="min-w-0 font-mono text-xs font-bold text-[var(--text-primary)]">{{ log.action }}</dd>
                      </div>

                      <div class="flex min-w-0 items-baseline gap-2 whitespace-nowrap" data-testid="patient-audit-ip-row">
                        <dt class="ui-label shrink-0">{{ i18n.t('patients.auditLogsIp') }}</dt>
                        <dd class="min-w-0 font-mono text-xs text-[var(--text-primary)]">{{ log.ipAddress || '—' }}</dd>
                      </div>

                      <div class="min-w-0">
                        <dd class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] px-3 py-2 font-mono text-[11px] leading-4 text-[var(--text-secondary)] [overflow-wrap:anywhere]">
                          {{ log.resourceType }} · {{ log.resourceId }}
                        </dd>
                      </div>

                      @if (log.userAgent) {
                        <div class="min-w-0">
                          <dd class="break-words text-[11px] leading-4 text-[var(--text-muted)] [overflow-wrap:anywhere]">{{ log.userAgent }}</dd>
                        </div>
                      }
                    </dl>
                  </div>
                }
              </article>
            }
          </div>
        }
      }
    </div>
  `,
})
export class PatientAuditTrailTabComponent implements OnInit {
  readonly parent = inject(PatientDetailComponent);
  private readonly auditApi = inject(AuditApiService);
  private readonly rbacApi = inject(RbacApiService);
  readonly i18n = inject(I18nService);

  readonly auditLogs = signal<AuditLog[]>([]);
  readonly isLoadingAudit = signal(false);
  readonly expandedLogIds = signal<ReadonlySet<string>>(new Set<string>());

  ngOnInit(): void {
    this.loadAudit();
  }

  loadAudit(): void {
    if (!this.canViewAudit()) {
      return;
    }
    const patient = this.parent.patient();
    if (!patient) return;

    this.isLoadingAudit.set(true);
    this.auditApi.getPatientLogs(patient.id).subscribe({
      next: data => {
        this.auditLogs.set(data);
        this.isLoadingAudit.set(false);
      },
      error: () => {
        this.isLoadingAudit.set(false);
      },
    });
  }

  toggleLog(logId: string): void {
    this.expandedLogIds.update(current => {
      const next = new Set(current);
      if (next.has(logId)) {
        next.delete(logId);
      } else {
        next.add(logId);
      }
      return next;
    });
  }

  isExpanded(logId: string): boolean {
    return this.expandedLogIds().has(logId);
  }

  compactTitle(log: AuditLog): string {
    return this.i18n.t(`patient.audit.action.${log.action}`, log.reason || log.action);
  }

  formatDateTime(value: string): string {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value;

    const locale = this.i18n.locale() === 'en' ? 'en-GB' : 'fr-FR';
    return new Intl.DateTimeFormat(locale, {
      dateStyle: 'short',
      timeStyle: 'short',
    }).format(date);
  }

  canViewAudit(): boolean {
    return this.rbacApi.hasPermission('AUDIT_READ');
  }
}

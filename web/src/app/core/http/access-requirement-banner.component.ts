import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '../i18n/i18n.service';
import { AccessRequirementNoticeService } from './access-requirement-notice.service';

@Component({
  selector: 'app-access-requirement-banner',
  standalone: true,
  template: `
    @if (notices.notice(); as notice) {
      <div class="fixed inset-x-0 top-3 z-[300] flex justify-center px-3 pointer-events-none" role="alert">
        <div class="pointer-events-auto flex w-full max-w-3xl flex-col gap-3 rounded-[var(--radius-brand-md)] border border-[var(--brand-warning-border)] bg-[var(--app-surface)] px-4 py-3 shadow-lg sm:flex-row sm:items-center sm:justify-between">
          <div class="min-w-0">
            <p class="text-sm font-bold text-[var(--text-primary)]">
              {{ notice.code === 'SCOPE_REQUIRED'
                ? i18n.t('access.remediation.scopeTitle', 'Périmètre patient non partagé')
                : i18n.t('access.remediation.consentTitle', 'Autorisation du patient requise') }}
            </p>
            <p class="mt-0.5 text-xs leading-5 text-[var(--text-secondary)]">
              {{ notice.message || fallbackMessage(notice.code) }}
            </p>
          </div>
          <div class="flex shrink-0 items-center gap-2">
            <button
              type="button"
              class="ui-button ui-button-primary min-h-10"
              (click)="requestAccess(notice.requiredScope)"
            >
              {{ i18n.t('access.remediation.requestAccess', 'Demander l’accès') }}
            </button>
            <button
              type="button"
              class="ui-button ui-button-secondary min-h-10"
              (click)="notices.clear()"
              [attr.aria-label]="i18n.t('common.dismiss', 'Fermer')"
            >
              {{ i18n.t('common.close', 'Fermer') }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class AccessRequirementBannerComponent {
  readonly notices = inject(AccessRequirementNoticeService);
  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  fallbackMessage(code: 'CONSENT_REQUIRED' | 'SCOPE_REQUIRED'): string {
    return code === 'CONSENT_REQUIRED'
      ? this.i18n.t(
          'access.remediation.consentMessage',
          'Le patient n’a pas encore autorisé l’accès à cette partie de son dossier.',
        )
      : this.i18n.t(
          'access.remediation.scopeMessage',
          'L’accès existe, mais le périmètre demandé n’a pas été partagé.',
        );
  }

  requestAccess(requiredScope?: string): void {
    this.notices.clear();
    void this.router.navigate(['/clinic/access-request'], {
      queryParams: {
        scope: requiredScope || undefined,
        reason: requiredScope
          ? this.i18n.t('access.remediation.scopeReason', 'Accès requis au périmètre {scope}').replace('{scope}', requiredScope)
          : this.i18n.t('access.remediation.recordReason', 'Accès au dossier patient requis'),
      },
    });
  }
}

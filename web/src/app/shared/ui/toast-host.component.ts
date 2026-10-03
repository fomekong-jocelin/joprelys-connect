import { Component, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { ToastService, ToastTone } from './toast.service';

/** Zone d'affichage des notifications, montée une seule fois à la racine de l'application. */
@Component({
  selector: 'app-toast-host',
  standalone: true,
  template: `
    <div class="pointer-events-none fixed inset-x-4 bottom-4 z-[70] flex flex-col items-end gap-2 sm:inset-x-auto sm:right-6" aria-live="polite">
      @for (toast of toasts.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-[6px] border p-3 text-sm font-semibold shadow-lg"
          [class]="toneClass(toast.tone)"
          [attr.role]="toast.tone === 'error' ? 'alert' : 'status'"
        >
          <span class="min-w-0 flex-1">{{ toast.message }}</span>
          <button
            type="button"
            class="shrink-0 cursor-pointer text-xs font-bold underline hover:no-underline"
            [attr.aria-label]="i18n.t('common.close', 'Fermer')"
            (click)="toasts.dismiss(toast.id)"
          >
            ✕
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastHostComponent {
  readonly toasts = inject(ToastService);
  readonly i18n = inject(I18nService);

  toneClass(tone: ToastTone): string {
    switch (tone) {
      case 'success':
        return 'border-[var(--brand-success-border,var(--app-border))] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
      case 'error':
        return 'border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]';
      default:
        return 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)]';
    }
  }
}

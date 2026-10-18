import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { I18nService } from '../../core/i18n/i18n.service';
import { ToastService, ToastTone } from './toast.service';
import { IconComponent, UiIconName } from './icon.component';

/** Zone d'affichage des notifications, montée une seule fois à la racine de l'application. */
@Component({
  selector: 'app-toast-host',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    <div
      class="pointer-events-none fixed inset-x-4 bottom-4 z-[85] flex flex-col items-end gap-2.5 sm:inset-x-auto sm:right-6"
      aria-live="polite"
    >
      @for (toast of toasts.toasts(); track toast.id) {
        <div
          class="pointer-events-auto flex w-full max-w-sm items-start gap-3 border p-3.5 text-xs font-semibold shadow-[var(--shadow-panel)] rounded-[var(--radius-brand-md)] transition-all duration-200"
          [class]="toneClass(toast.tone)"
          [attr.role]="toast.tone === 'error' ? 'alert' : 'status'"
        >
          <span class="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center text-sm">
            <app-ui-icon [name]="iconForTone(toast.tone)" />
          </span>
          <span class="min-w-0 flex-1 leading-5">{{ toast.message }}</span>
          <button
            type="button"
            class="flex h-5 w-5 shrink-0 cursor-pointer items-center justify-center rounded-[var(--radius-brand-xs)] text-current opacity-70 transition-opacity hover:opacity-100"
            [attr.aria-label]="i18n.t('common.close', 'Fermer')"
            (click)="toasts.dismiss(toast.id)"
          >
            <app-ui-icon name="x-mark" />
          </button>
        </div>
      }
    </div>
  `,
})
export class ToastHostComponent {
  readonly toasts = inject(ToastService);
  readonly i18n = inject(I18nService);

  iconForTone(tone: ToastTone): UiIconName {
    switch (tone) {
      case 'success':
        return 'check';
      case 'error':
        return 'x-mark';
      case 'warning':
        return 'exclamation-triangle';
      case 'info':
      default:
        return 'information-circle';
    }
  }

  toneClass(tone: ToastTone): string {
    switch (tone) {
      case 'success':
        return 'border-[var(--brand-success-border,var(--app-border))] bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)]';
      case 'error':
        return 'border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]';
      case 'warning':
        return 'border-[var(--brand-warning-border)] bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)]';
      case 'info':
      default:
        return 'border-[var(--app-border)] bg-[var(--app-surface)] text-[var(--text-primary)]';
    }
  }
}

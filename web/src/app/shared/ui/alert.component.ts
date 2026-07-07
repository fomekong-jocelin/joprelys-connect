import { Component, input } from '@angular/core';

export type AlertTone = 'error' | 'info' | 'success' | 'warning';

@Component({
  selector: 'app-ui-alert',
  standalone: true,
  template: `
    <div class="rounded-[var(--radius-brand-md)] border px-4 py-3 text-sm font-semibold flex items-start gap-2"
      [style.background]="toneStyle().bg"
      [style.border-color]="toneStyle().border"
      [style.color]="toneStyle().color"
      role="alert"
    >
      <ng-content></ng-content>
    </div>
  `,
})
export class AlertComponent {
  readonly tone = input<AlertTone>('info');

  toneStyle(): { bg: string; border: string; color: string } {
    switch (this.tone()) {
      case 'error':
        return {
          bg: 'var(--brand-danger-subtle)',
          border: 'var(--brand-danger-border)',
          color: 'var(--brand-danger-text)',
        };
      case 'success':
        return {
          bg: 'var(--brand-success-subtle)',
          border: 'color-mix(in srgb, var(--brand-success-muted) 80%, transparent)',
          color: 'var(--brand-success-text)',
        };
      case 'warning':
        return {
          bg: 'var(--brand-warning-subtle)',
          border: 'var(--brand-warning-border)',
          color: 'var(--brand-warning-text)',
        };
      case 'info':
      default:
        return {
          bg: 'var(--brand-info-subtle)',
          border: 'var(--brand-primary-border)',
          color: 'var(--brand-info-text)',
        };
    }
  }
}

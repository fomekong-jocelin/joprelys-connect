import { Component, input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-extrabold"
      [style.background]="active() ? 'var(--brand-success-subtle)' : 'var(--brand-danger-subtle)'"
      [style.color]="active() ? 'var(--brand-success-text)' : 'var(--brand-danger-text)'"
      [style.border]="active()
        ? '1px solid color-mix(in srgb, var(--brand-success-muted) 80%, transparent)'
        : '1px solid var(--brand-danger-border)'"
    >
      <span
        class="h-1.5 w-1.5 rounded-full"
        [style.background]="active() ? 'var(--brand-success)' : 'var(--brand-danger)'"
      ></span>
      {{ label() }}
    </span>
  `,
})
export class StatusBadgeComponent {
  readonly active = input.required<boolean>();
  readonly label = input.required<string>();
}

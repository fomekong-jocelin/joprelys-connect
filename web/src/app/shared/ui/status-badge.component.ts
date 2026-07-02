import { Component, input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: `
    <span
      class="inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-extrabold"
      [class]="active() ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'"
    >
      <span class="h-1.5 w-1.5 rounded-full" [class]="active() ? 'bg-green-600' : 'bg-red-600'"></span>
      {{ label() }}
    </span>
  `,
})
export class StatusBadgeComponent {
  readonly active = input.required<boolean>();
  readonly label = input.required<string>();
}

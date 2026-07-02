import { Component, input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  template: `
    <div class="ui-card-muted px-6 py-10 text-center">
      <p class="text-sm font-medium" style="color: var(--text-secondary)">{{ message() }}</p>
    </div>
  `,
})
export class EmptyStateComponent {
  readonly message = input.required<string>();
}

import { Component, input } from '@angular/core';

@Component({
  selector: 'app-ui-alert',
  standalone: true,
  template: `
    <div class="rounded-lg border px-4 py-3 text-sm font-semibold" [class]="toneClass()" role="alert">
      <ng-content></ng-content>
    </div>
  `,
})
export class AlertComponent {
  readonly tone = input<'error' | 'info'>('info');

  toneClass(): string {
    return this.tone() === 'error'
      ? 'border-red-100 bg-red-50 text-red-700'
      : 'border-cyan-100 bg-cyan-50 text-cyan-800';
  }
}

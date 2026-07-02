import { Component, input } from '@angular/core';

@Component({
  selector: 'app-ui-card',
  standalone: true,
  template: `
    <section [class]="'ui-card p-6 ' + class()">
      @if (title()) {
        <h3 class="ui-title text-lg mb-4">{{ title() }}</h3>
      }
      <ng-content></ng-content>
    </section>
  `
})
export class CardComponent {
  readonly title = input<string | null>(null);
  readonly class = input<string>('');
}

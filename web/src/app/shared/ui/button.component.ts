import { Component, input, output } from '@angular/core';

export type ButtonVariant = 'primary' | 'secondary' | 'link' | 'danger';
export type ButtonType = 'button' | 'submit';

@Component({
  selector: 'app-ui-button',
  standalone: true,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled()"
      [class]="buttonClass()"
      (click)="pressed.emit()"
    >
      <ng-content></ng-content>
    </button>
  `,
})
export class ButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly type = input<ButtonType>('button');
  readonly disabled = input(false);
  readonly class = input('');
  readonly pressed = output<void>();

  buttonClass(): string {
    const variants: Record<ButtonVariant, string> = {
      primary: 'ui-button ui-button-primary',
      secondary: 'ui-button ui-button-secondary',
      link: 'ui-link text-sm',
      danger: 'ui-link text-sm text-red-600 hover:text-red-700',
    };
    return `${variants[this.variant()]} ${this.class()}`;
  }
}

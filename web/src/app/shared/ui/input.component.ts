import { Component, input, model } from '@angular/core';

@Component({
  selector: 'app-ui-input',
  standalone: true,
  template: `
    <div class="space-y-1.5 w-full">
      @if (label()) {
        <label class="ui-label" [for]="inputId">
          {{ label() }} @if (required()) {
            <span style="color:var(--brand-danger);" aria-hidden="true">*</span>
          }
        </label>
      }
      <input
        [id]="inputId"
        [attr.aria-invalid]="error() ? 'true' : null"
        [attr.aria-describedby]="error() ? inputId + '-error' : null"
        [style.border-color]="error() ? 'var(--brand-danger)' : null"
        [type]="type()"
        [placeholder]="placeholder()"
        [value]="value()"
        (input)="value.set($any($event.target).value)"
        class="ui-input"
        [required]="required()"
        [attr.aria-required]="required() ? 'true' : null"
      />
      @if (error()) {
        <p [id]="inputId + '-error'" class="text-xs text-[var(--brand-danger-text)]" role="alert">{{ error() }}</p>
      }
    </div>
  `
})
export class InputComponent {
  private static nextId = 0;
  readonly inputId = `ui-input-${InputComponent.nextId++}`;
  readonly error = input<string | null>(null);
  readonly label = input<string | null>(null);
  readonly placeholder = input<string>('');
  readonly type = input<string>('text');
  readonly required = input<boolean>(false);
  readonly value = model<string>('');
}

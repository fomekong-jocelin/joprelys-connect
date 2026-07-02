import { Component, input, model } from '@angular/core';

@Component({
  selector: 'app-ui-input',
  standalone: true,
  template: `
    <div class="space-y-1.5 w-full">
      @if (label()) {
        <label class="ui-label">
          {{ label() }} @if (required()) { <span class="text-red-500">*</span> }
        </label>
      }
      <input 
        [type]="type()" 
        [placeholder]="placeholder()"
        [value]="value()"
        (input)="value.set($any($event.target).value)"
        class="ui-input"
        [required]="required()"
      />
    </div>
  `
})
export class InputComponent {
  readonly label = input<string | null>(null);
  readonly placeholder = input<string>('');
  readonly type = input<string>('text');
  readonly required = input<boolean>(false);
  readonly value = model<string>('');
}

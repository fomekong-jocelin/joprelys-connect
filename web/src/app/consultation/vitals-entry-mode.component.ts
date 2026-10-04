import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';

export type VitalsEntryMode = 'manual' | 'dictation' | 'realtime';

@Component({
  selector: 'app-vitals-entry-mode',
  standalone: true,
  imports: [CommonModule],
  template: `
    <fieldset [disabled]="disabled" class="min-w-0">
      <legend class="sr-only">{{ i18n.t('vitals.entry.chooseMode') }}</legend>
      <div class="grid gap-2 sm:grid-cols-3">
        @for (mode of modes; track mode) {
          <label
            class="flex cursor-pointer items-start gap-2 rounded-[var(--radius-brand-md)] border p-3 focus-within:outline-2 focus-within:outline-offset-2 focus-within:outline-[var(--brand-primary)]"
            [ngClass]="
              selected === mode
                ? 'border-[var(--brand-primary)] bg-[var(--brand-primary-subtle)]'
                : 'border-[var(--app-border)] bg-[var(--app-surface)]'
            "
            [class.opacity-50]="disabled || (mode === 'dictation' && !dictationSupported)"
          >
            <input
              type="radio"
              name="vitals-entry-mode"
              [value]="mode"
              [checked]="selected === mode"
              [disabled]="disabled || (mode === 'dictation' && !dictationSupported)"
              (change)="choose(mode)"
              class="mt-0.5 h-4 w-4 shrink-0 accent-[var(--brand-primary)]"
            />
            <span class="min-w-0">
              <span class="block text-xs font-bold text-[var(--text-primary)]">{{
                i18n.t('vitals.entry.' + mode + '.title')
              }}</span>
              <span class="mt-1 block text-xs leading-4 text-[var(--text-secondary)]">{{
                i18n.t('vitals.entry.' + mode + '.summary')
              }}</span>
            </span>
          </label>
        }
      </div>
      @if (!dictationSupported) {
        <p class="mt-2 text-xs text-[var(--brand-warning-text)]">
          {{ i18n.t('vitals.entry.dictationUnavailable') }}
        </p>
      }
    </fieldset>
  `,
})
export class VitalsEntryModeComponent {
  readonly i18n = inject(I18nService);
  @Input() selected: VitalsEntryMode = 'manual';
  @Input() disabled = false;
  @Input() dictationSupported = true;
  @Output() readonly selectedChange = new EventEmitter<VitalsEntryMode>();
  readonly modes: readonly VitalsEntryMode[] = ['manual', 'dictation', 'realtime'];

  choose(mode: VitalsEntryMode): void {
    if (this.disabled || (mode === 'dictation' && !this.dictationSupported)) return;
    this.selectedChange.emit(mode);
  }
}

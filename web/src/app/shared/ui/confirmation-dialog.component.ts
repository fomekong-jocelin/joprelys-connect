import {
  Component,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnChanges,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-confirmation-dialog',
  standalone: true,
  imports: [IconComponent],
  template: `
    @if (visible) {
      <div class="fixed inset-0 z-[70] flex items-center justify-center p-4" (click)="requestCancel()">
        <div class="absolute inset-0 bg-[var(--overlay-bg)]" aria-hidden="true"></div>
        <section
          #dialogPanel
          class="relative w-full max-w-md border border-[var(--app-border)] bg-[var(--app-surface)] p-5 shadow-[var(--shadow-panel)] rounded-[var(--radius-brand-md)]"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="confirmation-dialog-title"
          aria-describedby="confirmation-dialog-message"
          [attr.aria-busy]="busy"
          tabindex="-1"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-start gap-3">
            <span class="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)] bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)]">
              <app-ui-icon name="information-circle" />
            </span>
            <div class="min-w-0 flex-1">
              <h2 id="confirmation-dialog-title" class="text-sm font-bold text-[var(--text-primary)]">{{ title }}</h2>
              <p id="confirmation-dialog-message" class="mt-2 text-xs leading-5 text-[var(--text-secondary)]">{{ message }}</p>
            </div>
          </div>
          <div class="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <button #cancelButton type="button" class="ui-button ui-button-secondary" [disabled]="busy" (click)="requestCancel()">
              {{ cancelLabel }}
            </button>
            <button type="button" class="ui-button ui-button-danger confirmation-dialog__confirm" [disabled]="busy" (click)="requestConfirm()">
              @if (busy) {
                <span class="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-current border-r-transparent" aria-hidden="true"></span>
              } @else {
                <app-ui-icon name="x-mark" />
              }
              {{ confirmLabel }}
            </button>
          </div>
        </section>
      </div>
    }
  `,
})
export class ConfirmationDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() title = '';
  @Input() message = '';
  @Input() confirmLabel = '';
  @Input() cancelLabel = '';
  @Input() busy = false;

  @Output() confirmed = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  @ViewChild('dialogPanel') private dialogPanel?: ElementRef<HTMLElement>;
  private previouslyFocusedElement?: HTMLElement;

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['visible']) return;
    if (this.visible) {
      if (typeof document !== 'undefined' && document.activeElement instanceof HTMLElement) {
        this.previouslyFocusedElement = document.activeElement;
      }
      queueMicrotask(() => this.dialogPanel?.nativeElement.focus());
      return;
    }
    queueMicrotask(() => this.previouslyFocusedElement?.focus());
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.visible) this.requestCancel();
  }

  requestConfirm(): void {
    if (!this.busy) this.confirmed.emit();
  }

  requestCancel(): void {
    if (!this.busy) this.cancelled.emit();
  }
}

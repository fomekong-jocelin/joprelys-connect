import {
  Component,
  ElementRef,
  HostListener,
  ViewChild,
  effect,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { DialogService, DialogTone } from './dialog.service';
import { IconComponent } from './icon.component';

@Component({
  selector: 'app-dialog-host',
  standalone: true,
  imports: [CommonModule, IconComponent],
  template: `
    @if (dialog.activeDialog(); as d) {
      <div
        class="fixed inset-0 z-[90] flex items-center justify-center p-4"
        (click)="cancel()"
      >
        <!-- Arrière-plan assombri avec flou subtil -->
        <div class="absolute inset-0 bg-[var(--overlay-bg,rgba(0,0,0,0.45))] backdrop-blur-[1px]" aria-hidden="true"></div>

        <!-- Boîte modale accessible conforme Design System Joprelys -->
        <section
          #dialogPanel
          class="relative w-full max-w-md border border-[var(--app-border)] bg-[var(--app-surface)] p-6 shadow-[var(--shadow-panel)] rounded-[var(--radius-brand-md)]"
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="app-dialog-title"
          aria-describedby="app-dialog-message"
          tabindex="-1"
          (click)="$event.stopPropagation()"
        >
          <div class="flex items-start gap-3.5">
            <span
              class="mt-0.5 flex h-10 w-10 shrink-0 items-center justify-center rounded-[var(--radius-brand-sm)] border text-base"
              [class]="toneBadgeClass(d.tone)"
            >
              <app-ui-icon [name]="d.icon" />
            </span>
            <div class="min-w-0 flex-1">
              <h2 id="app-dialog-title" class="text-sm font-bold text-[var(--text-primary)]">
                {{ d.title }}
              </h2>
              <p id="app-dialog-message" class="mt-2 text-xs leading-5 text-[var(--text-secondary)] whitespace-pre-line">
                {{ d.message }}
              </p>
            </div>
          </div>

          <div class="mt-6 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            @if (!d.isAlert) {
              <button
                type="button"
                class="ui-button ui-button-secondary app-dialog__cancel"
                (click)="cancel()"
              >
                {{ d.cancelLabel }}
              </button>
            }
            <button
              #confirmButton
              type="button"
              class="ui-button app-dialog__confirm"
              [class]="confirmButtonClass(d.tone)"
              (click)="confirm()"
            >
              {{ d.confirmLabel }}
            </button>
          </div>
        </section>
      </div>
    }
  `,
})
export class DialogHostComponent {
  readonly dialog = inject(DialogService);

  @ViewChild('confirmButton') private confirmButton?: ElementRef<HTMLButtonElement>;
  private previouslyFocusedElement?: HTMLElement;

  constructor() {
    effect(() => {
      const active = this.dialog.activeDialog();
      if (active) {
        if (typeof document !== 'undefined' && document.activeElement instanceof HTMLElement) {
          this.previouslyFocusedElement = document.activeElement;
        }
        queueMicrotask(() => this.confirmButton?.nativeElement.focus());
      } else {
        queueMicrotask(() => this.previouslyFocusedElement?.focus());
      }
    });
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.dialog.activeDialog()) {
      this.cancel();
    }
  }

  confirm(): void {
    this.dialog.close(true);
  }

  cancel(): void {
    this.dialog.close(false);
  }

  toneBadgeClass(tone: DialogTone): string {
    switch (tone) {
      case 'warning':
        return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] border-[var(--brand-warning-border)]';
      case 'danger':
        return 'bg-[var(--brand-danger-subtle)] text-[var(--brand-danger-text)] border-[var(--brand-danger-border)]';
      case 'primary':
      case 'info':
      default:
        return 'bg-[var(--brand-info-subtle)] text-[var(--brand-info-text)] border-[var(--brand-primary-border)]';
    }
  }

  confirmButtonClass(tone: DialogTone): string {
    switch (tone) {
      case 'warning':
        return 'ui-button-warning';
      case 'danger':
        return 'ui-button-danger';
      case 'primary':
      case 'info':
      default:
        return 'ui-button-primary';
    }
  }
}

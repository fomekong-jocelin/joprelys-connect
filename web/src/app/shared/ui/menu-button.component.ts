import { DOCUMENT } from '@angular/common';
import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  inject,
  input,
  output,
  signal,
  viewChildren,
} from '@angular/core';
import { IconComponent, UiIconName } from './icon.component';

export interface MenuButtonItem {
  readonly id: string;
  readonly label: string;
  readonly description?: string;
}

let nextMenuId = 0;

/**
 * Bouton qui déplie une courte liste d'actions décrites (motif « disclosure » accessible).
 * Échap ou un clic à l'extérieur referme la liste ; les flèches parcourent les choix.
 */
@Component({
  selector: 'app-menu-button',
  standalone: true,
  imports: [IconComponent],
  host: {
    class: 'relative block w-full sm:w-auto',
    '(document:click)': 'onDocumentClick($event)',
    '(keydown.escape)': 'close(true)',
  },
  template: `
    <button
      #trigger
      type="button"
      class="ui-button w-full sm:w-auto"
      [class]="buttonClass()"
      [disabled]="disabled()"
      [attr.aria-expanded]="open()"
      [attr.aria-controls]="menuId"
      (click)="toggle()"
    >
      @if (icon(); as iconName) {
        <app-ui-icon [name]="iconName" aria-hidden="true" />
      }
      {{ label() }}
      <span class="inline-flex transition-transform" [class.rotate-180]="open()" aria-hidden="true">
        <app-ui-icon name="chevron-down" />
      </span>
    </button>
    @if (open()) {
      <ul
        [id]="menuId"
        class="absolute z-40 mt-2 w-max min-w-full max-w-[min(20rem,calc(100vw-2rem))] overflow-hidden rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] py-1 shadow-[var(--shadow-panel)]"
        [class.right-0]="align() === 'end'"
        [class.left-0]="align() === 'start'"
        (keydown.arrowDown)="moveFocus(1, $event)"
        (keydown.arrowUp)="moveFocus(-1, $event)"
      >
        @for (item of items(); track item.id) {
          <li>
            <button
              #itemButton
              type="button"
              class="flex min-h-12 w-full flex-col items-start gap-0.5 px-4 py-2.5 text-left transition-colors hover:bg-[var(--app-surface-muted)] focus-visible:bg-[var(--app-surface-muted)] focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[var(--brand-primary)]"
              (click)="choose(item)"
            >
              <span class="text-sm font-semibold text-[var(--text-primary)]">{{ item.label }}</span>
              @if (item.description) {
                <span class="text-xs leading-5 text-[var(--text-secondary)]">{{ item.description }}</span>
              }
            </button>
          </li>
        }
      </ul>
    }
  `,
})
export class MenuButtonComponent {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly injector = inject(Injector);
  private readonly document = inject(DOCUMENT);
  private readonly itemButtons = viewChildren<ElementRef<HTMLButtonElement>>('itemButton');

  readonly label = input.required<string>();
  readonly items = input.required<readonly MenuButtonItem[]>();
  readonly buttonClass = input('ui-button-primary');
  readonly icon = input<UiIconName | null>('plus');
  /** Bord du bouton auquel la liste s'aligne. */
  readonly align = input<'start' | 'end'>('end');
  readonly disabled = input(false);
  readonly selected = output<string>();

  readonly open = signal(false);
  readonly menuId = `app-menu-${nextMenuId++}`;

  toggle(): void {
    if (this.open()) {
      this.close(false);
      return;
    }
    this.open.set(true);
    afterNextRender(() => this.itemButtons()[0]?.nativeElement.focus(), { injector: this.injector });
  }

  choose(item: MenuButtonItem): void {
    this.open.set(false);
    this.selected.emit(item.id);
  }

  close(restoreFocus: boolean): void {
    if (!this.open()) return;
    this.open.set(false);
    if (restoreFocus) this.host.nativeElement.querySelector<HTMLButtonElement>('button')?.focus();
  }

  moveFocus(step: number, event: Event): void {
    event.preventDefault();
    const buttons = this.itemButtons().map((ref) => ref.nativeElement);
    const current = buttons.indexOf(this.document.activeElement as HTMLButtonElement);
    buttons[(current + step + buttons.length) % buttons.length]?.focus();
  }

  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) this.close(false);
  }
}

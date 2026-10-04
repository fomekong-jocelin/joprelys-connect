import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { I18nService } from '../core/i18n/i18n.service';
import { IconComponent, UiIconName } from '../shared/ui/icon.component';

export type ConsultationEntryMode = 'manual' | 'dictation' | 'conversation';

@Component({
  selector: 'app-consultation-entry-mode',
  standalone: true,
  imports: [CommonModule, IconComponent],
  templateUrl: './consultation-entry-mode.component.html',
})
export class ConsultationEntryModeComponent {
  readonly i18n = inject(I18nService);
  @Input() busy = false;
  @Input() visitAvailable = false;
  @Input() dictationSupported = true;
  @Output() readonly start = new EventEmitter<ConsultationEntryMode>();

  readonly selected = signal<ConsultationEntryMode>('conversation');
  readonly modes: readonly { id: ConsultationEntryMode; icon: UiIconName }[] = [
    { id: 'manual', icon: 'document-text' },
    { id: 'dictation', icon: 'microphone' },
    { id: 'conversation', icon: 'users' },
  ];
  readonly steps = ['choose', 'capture', 'review'];

  select(mode: ConsultationEntryMode): void {
    if (!this.busy && (mode !== 'dictation' || this.dictationSupported)) this.selected.set(mode);
  }

  startSelected(): void {
    if (this.busy || !this.visitAvailable) return;
    if (this.selected() === 'dictation' && !this.dictationSupported) return;
    this.start.emit(this.selected());
  }
}

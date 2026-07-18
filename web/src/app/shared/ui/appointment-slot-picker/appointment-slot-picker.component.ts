import { Component, computed, input, output } from '@angular/core';
import { AppointmentSlot } from '../../../patient/portal/appointments/patient-appointments.models';

export interface AppointmentSlotPickerLabels {
  empty: string;
  select: string;
}

interface SlotDayGroup {
  key: string;
  label: string;
  slots: AppointmentSlot[];
}

@Component({
  selector: 'app-appointment-slot-picker',
  standalone: true,
  template: `
    @if (groups().length === 0) {
      <div class="ui-empty-state py-8 text-center text-sm text-[var(--text-secondary)]">
        {{ labels().empty }}
      </div>
    } @else {
      <div class="space-y-4">
        @for (group of groups(); track group.key) {
          <section class="border border-[var(--app-border)] bg-[var(--app-surface)] p-3 rounded-[var(--radius-brand-md)]">
            <h3 class="text-xs font-black uppercase tracking-wide text-[var(--text-muted)]">{{ group.label }}</h3>
            <div class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4">
              @for (slot of group.slots; track slot.startAt) {
                <button
                  type="button"
                  class="ui-button min-h-10 justify-center text-xs"
                  [class.ui-button-primary]="selectedStartAt() === slot.startAt"
                  [class.ui-button-secondary]="selectedStartAt() !== slot.startAt"
                  [attr.aria-pressed]="selectedStartAt() === slot.startAt"
                  [attr.aria-label]="labels().select + ' ' + timeLabel(slot.startAt)"
                  (click)="slotSelected.emit(slot)"
                >
                  {{ timeLabel(slot.startAt) }}
                </button>
              }
            </div>
          </section>
        }
      </div>
    }
  `,
})
export class AppointmentSlotPickerComponent {
  readonly slots = input<AppointmentSlot[]>([]);
  readonly selectedStartAt = input<string | null>(null);
  readonly locale = input<'fr' | 'en'>('fr');
  readonly labels = input.required<AppointmentSlotPickerLabels>();
  readonly slotSelected = output<AppointmentSlot>();

  readonly groups = computed<SlotDayGroup[]>(() => {
    const formatter = new Intl.DateTimeFormat(this.locale() === 'fr' ? 'fr-FR' : 'en-GB', {
      weekday: 'long',
      day: '2-digit',
      month: 'long',
    });
    const grouped = new Map<string, AppointmentSlot[]>();
    for (const slot of this.slots()) {
      const date = new Date(slot.startAt);
      const key = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
      grouped.set(key, [...(grouped.get(key) ?? []), slot]);
    }
    return [...grouped.entries()].map(([key, slots]) => ({
      key,
      label: formatter.format(new Date(slots[0].startAt)),
      slots,
    }));
  });

  timeLabel(value: string): string {
    return new Intl.DateTimeFormat(this.locale() === 'fr' ? 'fr-FR' : 'en-GB', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }
}

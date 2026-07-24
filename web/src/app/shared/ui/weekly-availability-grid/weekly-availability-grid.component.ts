import { Component, computed, input, output } from '@angular/core';
import { IconComponent } from '../icon.component';

const DEFAULT_START_HOUR = 8;
const DEFAULT_END_HOUR = 18;
const MIN_START_HOUR = 0;
const MAX_END_HOUR = 24;
const HOUR_ROW_HEIGHT = 48;
const SELECTION_STEP_MINUTES = 30;
const DEFAULT_SELECTION_DURATION_MINUTES = 60;

/** Vue minimale d'une règle hebdomadaire affichée dans le calendrier. */
export interface WeeklyAvailabilityRuleView {
  readonly id: string;
  /** Jour de la semaine ISO-8601 : 1 = lundi … 7 = dimanche. */
  readonly weekday: number;
  /** Heure de début au format `HH:mm`. */
  readonly startTime: string;
  /** Heure de fin au format `HH:mm`. */
  readonly endTime: string;
  readonly active: boolean;
  /** Facultatifs pour rester compatible avec les anciens usages du composant. */
  readonly validFrom?: string;
  readonly validTo?: string | null;
}

export interface WeeklyAvailabilityExceptionView {
  readonly id: string;
  readonly startAt: string;
  readonly endAt: string;
  readonly reason?: string | null;
}

/** Libellés injectés par le parent : le composant reste sans dépendance i18n. */
export interface WeeklyAvailabilityGridLabels {
  readonly weekdays: readonly string[];
  readonly emptyDay: string;
  readonly inactive: string;
  readonly selectDay: string;
  readonly editRule: string;
  readonly deactivateRule: string;
  readonly available?: string;
  readonly unavailable?: string;
  readonly clickToAdd?: string;
}

export interface WeeklyAvailabilityRangeSelection {
  readonly weekday: number;
  readonly date: Date;
  readonly validFrom: string;
  readonly startTime: string;
  readonly endTime: string;
}

interface WeeklyCalendarDay {
  readonly weekday: number;
  readonly label: string;
  readonly date: Date;
  readonly dateKey: string;
  readonly today: boolean;
  readonly rules: readonly WeeklyAvailabilityRuleView[];
  readonly exceptions: readonly WeeklyAvailabilityExceptionView[];
}

interface CalendarBlock {
  readonly id: string;
  readonly kind: 'availability' | 'exception';
  readonly top: number;
  readonly height: number;
  readonly title: string;
  readonly subtitle: string;
  readonly rule?: WeeklyAvailabilityRuleView;
  readonly exception?: WeeklyAvailabilityExceptionView;
}

/**
 * Calendrier hebdomadaire de disponibilité, volontairement sans bibliothèque externe.
 *
 * - lecture type Teams/Outlook : jours en colonnes, heures en lignes ;
 * - règles récurrentes actives positionnées dans leur contexte horaire ;
 * - indisponibilités superposées sur la semaine réellement affichée ;
 * - clic sur une zone vide => proposition d'une plage d'une heure, arrondie à 30 min ;
 * - tous les styles utilisent les tokens de DESIGN.md, rayon <= 8 px.
 */
@Component({
  selector: 'app-weekly-availability-grid',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div class="space-y-3">
      <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-[11px] font-semibold text-[var(--text-muted)]">
        <span class="inline-flex items-center gap-1.5">
          <span class="h-2.5 w-2.5 rounded-[var(--radius-brand-xs)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)]"></span>
          {{ labels().available || 'Available' }}
        </span>
        <span class="inline-flex items-center gap-1.5">
          <span class="h-2.5 w-2.5 rounded-[var(--radius-brand-xs)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)]"></span>
          {{ labels().unavailable || 'Unavailable' }}
        </span>
        @if (labels().clickToAdd) {
          <span class="ml-auto hidden sm:inline">{{ labels().clickToAdd }}</span>
        }
      </div>

      <div class="overflow-x-auto rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)]">
        <div class="min-w-[980px]">
          <!-- En-têtes des jours -->
          <div class="grid grid-cols-[64px_repeat(7,minmax(0,1fr))] border-b border-[var(--app-border)] bg-[var(--app-surface-muted)]">
            <div class="border-r border-[var(--app-border)]"></div>
            @for (day of days(); track day.dateKey) {
              <button
                type="button"
                class="min-h-14 border-r border-[var(--app-border)] px-2 py-2 text-center transition-colors last:border-r-0 hover:bg-[var(--brand-primary-subtle)]"
                [class.bg-[var(--brand-primary-subtle)]]="day.today"
                [attr.aria-pressed]="selectedWeekday() === day.weekday"
                [title]="labels().selectDay"
                (click)="weekdaySelected.emit(day.weekday)"
              >
                <span class="block text-[10px] font-black uppercase tracking-wider text-[var(--text-muted)]">{{ day.label }}</span>
                <span class="mt-0.5 block text-sm font-extrabold text-[var(--text-primary)]">{{ day.date.getDate() }}</span>
              </button>
            }
          </div>

          <!-- Corps horaire -->
          <div class="grid grid-cols-[64px_repeat(7,minmax(0,1fr))]">
            <div class="relative border-r border-[var(--app-border)] bg-[var(--app-surface-muted)]" [style.height.px]="calendarHeight()">
              @for (hour of hourLabels(); track hour) {
                <div
                  class="absolute left-0 right-0 border-t border-[var(--divider-subtle)] pr-2 pt-1 text-right text-[10px] font-semibold text-[var(--text-muted)]"
                  [style.top.px]="$index * rowHeight"
                >
                  {{ formatHour(hour) }}
                </div>
              }
            </div>

            @for (day of days(); track day.dateKey) {
              <div
                class="relative cursor-crosshair border-r border-[var(--app-border)] last:border-r-0"
                [class.bg-[var(--brand-primary-subtle)]]="day.today"
                [style.height.px]="calendarHeight()"
                [attr.aria-label]="day.label"
                (click)="selectRange($event, day)"
              >
                @for (hour of hourLabels(); track hour) {
                  <div
                    class="pointer-events-none absolute left-0 right-0 border-t border-[var(--divider-subtle)]"
                    [style.top.px]="$index * rowHeight"
                  ></div>
                }

                @for (block of blocksForDay(day); track block.id) {
                  @if (block.kind === 'availability' && block.rule; as rule) {
                    <article
                      data-calendar-block
                      class="group absolute left-1 right-1 z-10 overflow-hidden rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-1.5 shadow-[var(--shadow-panel-subtle)]"
                      [style.top.px]="block.top"
                      [style.height.px]="block.height"
                      (click)="$event.stopPropagation()"
                    >
                      <button
                        type="button"
                        class="block w-full pr-5 text-left"
                        [title]="labels().editRule"
                        (click)="$event.stopPropagation(); ruleSelected.emit(rule)"
                      >
                        <span class="block text-[11px] font-black text-[var(--brand-primary)]">{{ block.title }}</span>
                        <span class="mt-0.5 block truncate text-[10px] font-semibold text-[var(--text-secondary)]">{{ block.subtitle }}</span>
                      </button>
                      <button
                        type="button"
                        class="absolute right-1 top-1 rounded-[var(--radius-brand-xs)] p-0.5 text-[var(--text-muted)] opacity-0 transition-opacity hover:text-[var(--brand-danger-text)] focus:opacity-100 group-hover:opacity-100"
                        [title]="labels().deactivateRule"
                        [attr.aria-label]="labels().deactivateRule"
                        (click)="$event.stopPropagation(); ruleDeactivateRequested.emit(rule)"
                      >
                        <app-ui-icon name="x-mark" />
                      </button>
                    </article>
                  } @else if (block.exception; as exception) {
                    <article
                      data-calendar-block
                      class="absolute left-1 right-1 z-20 overflow-hidden rounded-[var(--radius-brand-sm)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-1.5"
                      [style.top.px]="block.top"
                      [style.height.px]="block.height"
                      (click)="$event.stopPropagation()"
                    >
                      <span class="block text-[11px] font-black text-[var(--brand-danger-text)]">{{ block.title }}</span>
                      @if (block.subtitle) {
                        <span class="mt-0.5 block truncate text-[10px] font-semibold text-[var(--text-secondary)]">{{ block.subtitle }}</span>
                      }
                    </article>
                  }
                }
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
})
export class WeeklyAvailabilityGridComponent {
  readonly rules = input.required<readonly WeeklyAvailabilityRuleView[]>();
  readonly exceptions = input<readonly WeeklyAvailabilityExceptionView[]>([]);
  readonly weekStart = input<Date>(startOfIsoWeek(new Date()));
  readonly selectedWeekday = input<number | null>(null);
  readonly labels = input.required<WeeklyAvailabilityGridLabels>();

  readonly weekdaySelected = output<number>();
  readonly rangeSelected = output<WeeklyAvailabilityRangeSelection>();
  readonly ruleSelected = output<WeeklyAvailabilityRuleView>();
  readonly ruleDeactivateRequested = output<WeeklyAvailabilityRuleView>();

  readonly rowHeight = HOUR_ROW_HEIGHT;

  readonly days = computed<readonly WeeklyCalendarDay[]>(() => {
    const start = startOfIsoWeek(this.weekStart());
    const names = this.labels().weekdays;
    const rules = this.rules();
    const exceptions = this.exceptions();
    const todayKey = toLocalDateKey(new Date());

    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + index);
      const dateKey = toLocalDateKey(date);
      const weekday = index + 1;
      return {
        weekday,
        label: names[index] ?? '',
        date,
        dateKey,
        today: dateKey === todayKey,
        rules: rules
          .filter((rule) => isRuleApplicable(rule, weekday, dateKey))
          .slice()
          .sort((left, right) => left.startTime.localeCompare(right.startTime)),
        exceptions: exceptions.filter((exception) => overlapsLocalDay(exception, date)),
      };
    });
  });

  readonly calendarStartHour = computed(() => {
    const starts: number[] = [DEFAULT_START_HOUR * 60];
    for (const day of this.days()) {
      for (const rule of day.rules) {
        const minutes = parseTimeToMinutes(rule.startTime);
        if (minutes !== null) starts.push(minutes);
      }
      for (const exception of day.exceptions) {
        const start = new Date(exception.startAt);
        if (!Number.isNaN(start.getTime())) starts.push(start.getHours() * 60 + start.getMinutes());
      }
    }
    return Math.max(MIN_START_HOUR, Math.floor(Math.min(...starts) / 60));
  });

  readonly calendarEndHour = computed(() => {
    const ends: number[] = [DEFAULT_END_HOUR * 60];
    for (const day of this.days()) {
      for (const rule of day.rules) {
        const minutes = parseTimeToMinutes(rule.endTime);
        if (minutes !== null) ends.push(minutes);
      }
      for (const exception of day.exceptions) {
        const end = new Date(exception.endAt);
        if (!Number.isNaN(end.getTime())) ends.push(end.getHours() * 60 + end.getMinutes());
      }
    }
    return Math.min(MAX_END_HOUR, Math.max(this.calendarStartHour() + 1, Math.ceil(Math.max(...ends) / 60)));
  });

  readonly hourLabels = computed(() =>
    Array.from({ length: this.calendarEndHour() - this.calendarStartHour() + 1 }, (_, index) => this.calendarStartHour() + index));

  readonly calendarHeight = computed(() => (this.calendarEndHour() - this.calendarStartHour()) * HOUR_ROW_HEIGHT);

  blocksForDay(day: WeeklyCalendarDay): readonly CalendarBlock[] {
    const blocks: CalendarBlock[] = [];
    for (const rule of day.rules) {
      const start = parseTimeToMinutes(rule.startTime);
      const end = parseTimeToMinutes(rule.endTime);
      if (start === null || end === null || end <= start) continue;
      const position = this.positionForMinutes(start, end);
      if (!position) continue;
      blocks.push({
        id: `rule-${rule.id}-${day.dateKey}`,
        kind: 'availability',
        ...position,
        title: `${rule.startTime} – ${rule.endTime}`,
        subtitle: this.labels().available || '',
        rule,
      });
    }

    for (const exception of day.exceptions) {
      const dayStart = new Date(day.date.getFullYear(), day.date.getMonth(), day.date.getDate());
      const dayEnd = new Date(dayStart.getFullYear(), dayStart.getMonth(), dayStart.getDate() + 1);
      const start = new Date(exception.startAt);
      const end = new Date(exception.endAt);
      if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) continue;
      const visibleStart = start.getTime() < dayStart.getTime() ? dayStart : start;
      const visibleEnd = end.getTime() > dayEnd.getTime() ? dayEnd : end;
      const startMinutes = visibleStart.getHours() * 60 + visibleStart.getMinutes();
      const endMinutes = visibleEnd.getTime() === dayEnd.getTime()
        ? 24 * 60
        : visibleEnd.getHours() * 60 + visibleEnd.getMinutes();
      const position = this.positionForMinutes(startMinutes, endMinutes);
      if (!position) continue;
      blocks.push({
        id: `exception-${exception.id}-${day.dateKey}`,
        kind: 'exception',
        ...position,
        title: this.labels().unavailable || '',
        subtitle: exception.reason ?? '',
        exception,
      });
    }

    return blocks;
  }

  selectRange(event: MouseEvent, day: WeeklyCalendarDay): void {
    const target = event.target as HTMLElement | null;
    if (target?.closest('[data-calendar-block]')) return;

    const host = event.currentTarget as HTMLElement | null;
    if (!host) return;
    const rect = host.getBoundingClientRect();
    if (rect.height <= 0) return;

    const relativeY = Math.max(0, Math.min(event.clientY - rect.top, rect.height - 1));
    const minutesFromStart = Math.floor(
      ((relativeY / HOUR_ROW_HEIGHT) * 60) / SELECTION_STEP_MINUTES,
    ) * SELECTION_STEP_MINUTES;
    const startMinutes = Math.min(
      this.calendarEndHour() * 60 - SELECTION_STEP_MINUTES,
      this.calendarStartHour() * 60 + minutesFromStart,
    );
    const endMinutes = Math.min(
      this.calendarEndHour() * 60,
      startMinutes + DEFAULT_SELECTION_DURATION_MINUTES,
    );

    this.weekdaySelected.emit(day.weekday);
    this.rangeSelected.emit({
      weekday: day.weekday,
      date: new Date(day.date),
      validFrom: day.dateKey,
      startTime: formatMinutes(startMinutes),
      endTime: formatMinutes(endMinutes),
    });
  }

  formatHour(hour: number): string {
    return `${`${hour}`.padStart(2, '0')}:00`;
  }

  private positionForMinutes(startMinutes: number, endMinutes: number): Pick<CalendarBlock, 'top' | 'height'> | null {
    const visibleStart = this.calendarStartHour() * 60;
    const visibleEnd = this.calendarEndHour() * 60;
    const clippedStart = Math.max(startMinutes, visibleStart);
    const clippedEnd = Math.min(endMinutes, visibleEnd);
    if (clippedEnd <= clippedStart) return null;

    const top = ((clippedStart - visibleStart) / 60) * HOUR_ROW_HEIGHT;
    const rawHeight = ((clippedEnd - clippedStart) / 60) * HOUR_ROW_HEIGHT;
    return { top, height: Math.max(24, rawHeight) };
  }
}

function isRuleApplicable(rule: WeeklyAvailabilityRuleView, weekday: number, dateKey: string): boolean {
  if (!rule.active || rule.weekday !== weekday) return false;
  if (rule.validFrom && rule.validFrom > dateKey) return false;
  if (rule.validTo && rule.validTo < dateKey) return false;
  return true;
}

function overlapsLocalDay(exception: WeeklyAvailabilityExceptionView, date: Date): boolean {
  const start = new Date(exception.startAt).getTime();
  const end = new Date(exception.endAt).getTime();
  if (!Number.isFinite(start) || !Number.isFinite(end) || end <= start) return false;
  const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
  const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1).getTime();
  return start < dayEnd && end > dayStart;
}

function startOfIsoWeek(value: Date): Date {
  const date = new Date(value.getFullYear(), value.getMonth(), value.getDate());
  const offset = (date.getDay() + 6) % 7;
  date.setDate(date.getDate() - offset);
  return date;
}

function toLocalDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function parseTimeToMinutes(value: string): number | null {
  const match = /^(\d{2}):(\d{2})/.exec(value);
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

function formatMinutes(totalMinutes: number): string {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${`${hours}`.padStart(2, '0')}:${`${minutes}`.padStart(2, '0')}`;
}

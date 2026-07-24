import { Component, computed, input, output } from '@angular/core';

const HOUR_ROW_HEIGHT = 48;
const DAY_MINUTES = 24 * 60;
const SELECTION_STEP_MINUTES = 30;

export interface WeeklyAvailabilityRuleView {
  readonly id: string;
  readonly weekday: number;
  readonly startTime: string;
  readonly endTime: string;
  readonly active: boolean;
  readonly validFrom?: string;
  readonly validTo?: string | null;
}

export interface WeeklyAvailabilityExceptionView {
  readonly id: string;
  readonly startAt: string;
  readonly endAt: string;
  readonly reason?: string | null;
}

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

interface CalendarDay {
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
  readonly top: number;
  readonly height: number;
  readonly title: string;
  readonly subtitle: string;
  readonly rule?: WeeklyAvailabilityRuleView;
  readonly exception?: WeeklyAvailabilityExceptionView;
}

/** Calendrier hebdomadaire type agenda, sans dépendance externe. */
@Component({
  selector: 'app-weekly-availability-grid',
  standalone: true,
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

      <div class="max-h-[640px] overflow-auto rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)]">
        <div class="min-w-[980px]">
          <div class="sticky top-0 z-30 grid grid-cols-[64px_repeat(7,minmax(0,1fr))] border-b border-[var(--app-border)] bg-[var(--app-surface-muted)]">
            <div class="border-r border-[var(--app-border)]"></div>
            @for (day of days(); track day.dateKey) {
              <button
                type="button"
                class="min-h-14 border-r border-[var(--app-border)] px-2 py-2 text-center transition-colors last:border-r-0 hover:bg-[var(--brand-primary-subtle)]"
                [style.background-color]="day.today ? 'var(--brand-primary-subtle)' : null"
                [attr.aria-pressed]="selectedWeekday() === day.weekday"
                [title]="labels().selectDay"
                (click)="weekdaySelected.emit(day.weekday)"
              >
                <span class="block text-[10px] font-black uppercase tracking-wider text-[var(--text-muted)]">{{ day.label }}</span>
                <span class="mt-0.5 block text-sm font-extrabold text-[var(--text-primary)]">{{ day.date.getDate() }}</span>
              </button>
            }
          </div>

          <div class="grid grid-cols-[64px_repeat(7,minmax(0,1fr))]">
            <div class="relative border-r border-[var(--app-border)] bg-[var(--app-surface-muted)]" [style.height.px]="calendarHeight">
              @for (hour of hours; track hour) {
                <div class="absolute left-0 right-0 border-t border-[var(--divider-subtle)] pr-2 pt-1 text-right text-[10px] font-semibold text-[var(--text-muted)]" [style.top.px]="$index * rowHeight">
                  {{ formatHour(hour) }}
                </div>
              }
            </div>

            @for (day of days(); track day.dateKey) {
              <div
                class="relative cursor-crosshair border-r border-[var(--app-border)] last:border-r-0"
                [style.background-color]="day.today ? 'var(--brand-primary-subtle)' : null"
                [style.height.px]="calendarHeight"
                [attr.aria-label]="day.label"
                (click)="selectRange($event, day)"
              >
                @for (hour of hours; track hour) {
                  <div class="pointer-events-none absolute left-0 right-0 border-t border-[var(--divider-subtle)]" [style.top.px]="$index * rowHeight"></div>
                }

                @for (block of blocksForDay(day); track block.id) {
                  @if (block.rule; as rule) {
                    <button
                      data-calendar-block
                      type="button"
                      class="absolute left-1 right-1 z-10 overflow-hidden rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-1.5 text-left shadow-[var(--shadow-panel-subtle)]"
                      [style.top.px]="block.top"
                      [style.height.px]="block.height"
                      [title]="labels().editRule"
                      (click)="$event.stopPropagation(); ruleSelected.emit(rule)"
                    >
                      <span class="block text-[11px] font-black text-[var(--brand-primary)]">{{ block.title }}</span>
                      <span class="mt-0.5 block truncate text-[10px] font-semibold text-[var(--text-secondary)]">{{ block.subtitle }}</span>
                    </button>
                  } @else if (block.exception) {
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
  readonly hours = Array.from({ length: 25 }, (_, index) => index);
  readonly calendarHeight = 24 * HOUR_ROW_HEIGHT;

  readonly days = computed<readonly CalendarDay[]>(() => {
    const start = startOfIsoWeek(this.weekStart());
    const todayKey = toLocalDateKey(new Date());
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + index);
      const dateKey = toLocalDateKey(date);
      const weekday = index + 1;
      return {
        weekday,
        label: this.labels().weekdays[index] ?? '',
        date,
        dateKey,
        today: dateKey === todayKey,
        rules: this.rules()
          .filter((rule) => isRuleApplicable(rule, weekday, dateKey))
          .slice()
          .sort((left, right) => left.startTime.localeCompare(right.startTime)),
        exceptions: this.exceptions().filter((exception) => overlapsLocalDay(exception, date)),
      };
    });
  });

  blocksForDay(day: CalendarDay): readonly CalendarBlock[] {
    const blocks: CalendarBlock[] = [];
    for (const rule of day.rules) {
      const start = parseTimeToMinutes(rule.startTime);
      const end = parseTimeToMinutes(rule.endTime);
      if (start === null || end === null || end <= start) continue;
      blocks.push({
        id: `rule-${rule.id}-${day.dateKey}`,
        top: minutesToPixels(start),
        height: Math.max(24, minutesToPixels(end - start)),
        title: `${rule.startTime} – ${rule.endTime}`,
        subtitle: this.labels().available || '',
        rule,
      });
    }

    const dayStart = new Date(day.date.getFullYear(), day.date.getMonth(), day.date.getDate());
    const dayEnd = new Date(dayStart.getFullYear(), dayStart.getMonth(), dayStart.getDate() + 1);
    for (const exception of day.exceptions) {
      const start = new Date(exception.startAt);
      const end = new Date(exception.endAt);
      if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) continue;
      const visibleStart = start.getTime() < dayStart.getTime() ? dayStart : start;
      const visibleEnd = end.getTime() > dayEnd.getTime() ? dayEnd : end;
      const startMinutes = visibleStart.getHours() * 60 + visibleStart.getMinutes();
      const endMinutes = visibleEnd.getTime() === dayEnd.getTime() ? DAY_MINUTES : visibleEnd.getHours() * 60 + visibleEnd.getMinutes();
      blocks.push({
        id: `exception-${exception.id}-${day.dateKey}`,
        top: minutesToPixels(startMinutes),
        height: Math.max(24, minutesToPixels(endMinutes - startMinutes)),
        title: this.labels().unavailable || '',
        subtitle: exception.reason ?? '',
        exception,
      });
    }
    return blocks;
  }

  selectRange(event: MouseEvent, day: CalendarDay): void {
    const target = event.target as HTMLElement | null;
    if (target?.closest('[data-calendar-block]')) return;
    const host = event.currentTarget as HTMLElement | null;
    if (!host) return;
    const rect = host.getBoundingClientRect();
    if (rect.height <= 0) return;

    const relativeY = Math.max(0, Math.min(event.clientY - rect.top, rect.height - 1));
    const rawMinutes = (relativeY / rect.height) * DAY_MINUTES;
    const startMinutes = Math.min(DAY_MINUTES - SELECTION_STEP_MINUTES,
      Math.floor(rawMinutes / SELECTION_STEP_MINUTES) * SELECTION_STEP_MINUTES);
    const endMinutes = Math.min(DAY_MINUTES, startMinutes + 60);

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
}

function isRuleApplicable(rule: WeeklyAvailabilityRuleView, weekday: number, dateKey: string): boolean {
  return rule.active
    && rule.weekday === weekday
    && (!rule.validFrom || rule.validFrom <= dateKey)
    && (!rule.validTo || rule.validTo >= dateKey);
}

function overlapsLocalDay(exception: WeeklyAvailabilityExceptionView, date: Date): boolean {
  const start = new Date(exception.startAt).getTime();
  const end = new Date(exception.endAt).getTime();
  const dayStart = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
  const dayEnd = new Date(date.getFullYear(), date.getMonth(), date.getDate() + 1).getTime();
  return Number.isFinite(start) && Number.isFinite(end) && start < dayEnd && end > dayStart;
}

function startOfIsoWeek(value: Date): Date {
  const date = new Date(value.getFullYear(), value.getMonth(), value.getDate());
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  return date;
}

function toLocalDateKey(date: Date): string {
  return `${date.getFullYear()}-${`${date.getMonth() + 1}`.padStart(2, '0')}-${`${date.getDate()}`.padStart(2, '0')}`;
}

function parseTimeToMinutes(value: string): number | null {
  const match = /^(\d{2}):(\d{2})/.exec(value);
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

function minutesToPixels(minutes: number): number {
  return (minutes / 60) * HOUR_ROW_HEIGHT;
}

function formatMinutes(totalMinutes: number): string {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${`${hours}`.padStart(2, '0')}:${`${minutes}`.padStart(2, '0')}`;
}

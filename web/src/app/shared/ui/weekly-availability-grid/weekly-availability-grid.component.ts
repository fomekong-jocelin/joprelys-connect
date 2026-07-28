import { Component, computed, input, output, signal } from '@angular/core';

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

/** Calendrier hebdomadaire type agenda, sans dépendance externe et 100% mobile-first. */
@Component({
  selector: 'app-weekly-availability-grid',
  standalone: true,
  host: {
    class: 'block w-full min-w-0 max-w-full',
  },
  template: `
    <div class="space-y-4 w-full min-w-0 max-w-full">
      <div class="flex flex-wrap items-center justify-between gap-x-4 gap-y-2 text-[11px] font-semibold text-[var(--text-muted)]">
        <div class="flex items-center gap-3">
          <span class="inline-flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-[var(--radius-brand-xs)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)]"></span>
            {{ labels().available || 'Disponible' }}
          </span>
          <span class="inline-flex items-center gap-1.5">
            <span class="h-2.5 w-2.5 rounded-[var(--radius-brand-xs)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)]"></span>
            {{ labels().unavailable || 'Indisponible' }}
          </span>
        </div>
        @if (labels().clickToAdd) {
          <span class="hidden sm:inline">{{ labels().clickToAdd }}</span>
        }
      </div>

      <!-- VUE MOBILE (< 768px) : Onglets jour par jour + timeline fluide sans overflow viewport -->
      <div class="block md:hidden w-full min-w-0 max-w-full">
        <!-- Bandeau d'onglets pour les 7 jours de la semaine -->
        <div class="flex w-full min-w-0 max-w-full overflow-x-auto rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-1 gap-1">
          @for (day of days(); track day.dateKey) {
            <button
              type="button"
              class="flex flex-1 shrink-0 min-w-[62px] sm:min-w-[70px] flex-col items-center justify-center rounded-[var(--radius-brand-md)] py-2 px-1 text-center transition-colors border"
              [class.bg-[var(--brand-primary)]]="selectedMobileWeekday() === day.weekday"
              [class.text-white]="selectedMobileWeekday() === day.weekday"
              [class.border-transparent]="selectedMobileWeekday() === day.weekday"
              [class.bg-[var(--app-surface)]]="selectedMobileWeekday() !== day.weekday && day.today"
              [class.border-[var(--brand-primary-border)]]="selectedMobileWeekday() !== day.weekday && day.today"
              [class.border-transparent]="selectedMobileWeekday() !== day.weekday && !day.today"
              (click)="selectMobileDay(day.weekday)"
            >
              <span class="text-[10px] font-black uppercase tracking-wider" [class.text-[var(--brand-primary)]]="selectedMobileWeekday() !== day.weekday && day.today" [class.text-[var(--text-muted)]]="selectedMobileWeekday() !== day.weekday && !day.today">{{ day.label.substring(0, 3) }}</span>
              <span class="text-sm font-extrabold" [class.text-[var(--text-primary)]]="selectedMobileWeekday() !== day.weekday">{{ day.date.getDate() }}</span>
              @if (day.rules.length > 0) {
                <span class="mt-0.5 h-1.5 w-1.5 rounded-full" [class.bg-white]="selectedMobileWeekday() === day.weekday" [class.bg-[var(--brand-primary)]]="selectedMobileWeekday() !== day.weekday"></span>
              }
            </button>
          }
        </div>

        <!-- Détail du jour sélectionné sur mobile -->
        @if (selectedMobileDay(); as currentDay) {
          <div class="mt-3 w-full min-w-0 max-w-full rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)] p-3 sm:p-4 space-y-3">
            <div class="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--divider-subtle)] pb-3">
              <div class="min-w-0">
                <h3 class="text-sm font-extrabold text-[var(--text-primary)] truncate">
                  {{ currentDay.label }} {{ currentDay.date.getDate() }}
                </h3>
                <p class="text-xs text-[var(--text-muted)] truncate">
                  {{ currentDay.rules.length }} {{ labels().available || 'plage(s)' }}
                </p>
              </div>
              <button
                type="button"
                class="ui-button ui-button-primary min-h-[38px] text-xs py-1.5 px-3 shrink-0"
                (click)="addRuleForDay(currentDay)"
              >
                + Ajouter une plage
              </button>
            </div>

            <!-- Liste des plages du jour -->
            @if (currentDay.rules.length === 0 && currentDay.exceptions.length === 0) {
              <div class="py-6 text-center text-xs text-[var(--text-muted)] bg-[var(--app-surface-muted)] rounded-[var(--radius-brand-md)]">
                {{ labels().emptyDay || 'Aucune plage configurée pour ce jour' }}
              </div>
            } @else {
              <div class="space-y-2 min-w-0">
                @for (rule of currentDay.rules; track rule.id) {
                  <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-2 rounded-[var(--radius-brand-md)] border border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] p-3 min-w-0">
                    <button type="button" class="text-left flex-1 min-w-0" (click)="ruleSelected.emit(rule)">
                      <span class="block text-sm font-black text-[var(--brand-primary)] truncate">{{ rule.startTime }} – {{ rule.endTime }}</span>
                      <span class="block text-xs text-[var(--text-secondary)] truncate">{{ labels().available || 'Disponible' }}</span>
                    </button>
                    <div class="flex items-center gap-2 shrink-0 self-end sm:self-auto">
                      <button
                        type="button"
                        class="ui-button ui-button-secondary text-xs px-2.5 py-1.5 min-h-[36px]"
                        (click)="ruleSelected.emit(rule)"
                      >
                        Éditer
                      </button>
                      <button
                        type="button"
                        class="ui-button ui-button-danger text-xs px-2.5 py-1.5 min-h-[36px]"
                        (click)="ruleDeactivateRequested.emit(rule)"
                      >
                        Désactiver
                      </button>
                    </div>
                  </div>
                }

                @for (exception of currentDay.exceptions; track exception.id) {
                  <div class="rounded-[var(--radius-brand-md)] border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 min-w-0">
                    <span class="block text-xs font-black text-[var(--brand-danger-text)] truncate">{{ labels().unavailable || 'Indisponible' }}</span>
                    <span class="block text-xs text-[var(--text-secondary)] truncate">
                      {{ formatExceptionTimes(exception) }}
                    </span>
                    @if (exception.reason) {
                      <span class="mt-1 block text-xs italic text-[var(--text-muted)] break-words">{{ exception.reason }}</span>
                    }
                  </div>
                }
              </div>
            }

            <!-- Créneaux rapides de la journée -->
            <div class="pt-2 min-w-0">
              <h4 class="mb-2 text-xs font-bold uppercase tracking-wider text-[var(--text-muted)]">Créer une plage rapide</h4>
              <div class="grid grid-cols-2 sm:grid-cols-4 gap-2 min-w-0">
                <button
                  type="button"
                  class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] py-2 px-1 text-center text-[11px] sm:text-xs font-semibold text-[var(--text-primary)] hover:border-[var(--brand-primary)] hover:bg-[var(--brand-primary-subtle)] truncate min-h-[38px]"
                  (click)="addSlotForDay(currentDay, '08:00', '12:00')"
                >
                  Matin (08h - 12h)
                </button>
                <button
                  type="button"
                  class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] py-2 px-1 text-center text-[11px] sm:text-xs font-semibold text-[var(--text-primary)] hover:border-[var(--brand-primary)] hover:bg-[var(--brand-primary-subtle)] truncate min-h-[38px]"
                  (click)="addSlotForDay(currentDay, '14:00', '18:00')"
                >
                  A-M (14h - 18h)
                </button>
                <button
                  type="button"
                  class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] py-2 px-1 text-center text-[11px] sm:text-xs font-semibold text-[var(--text-primary)] hover:border-[var(--brand-primary)] hover:bg-[var(--brand-primary-subtle)] truncate min-h-[38px]"
                  (click)="addSlotForDay(currentDay, '08:00', '17:00')"
                >
                  Jour (08h - 17h)
                </button>
                <button
                  type="button"
                  class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] py-2 px-1 text-center text-[11px] sm:text-xs font-semibold text-[var(--text-primary)] hover:border-[var(--brand-primary)] hover:bg-[var(--brand-primary-subtle)] truncate min-h-[38px]"
                  (click)="addRuleForDay(currentDay)"
                >
                  Personnalisé...
                </button>
              </div>
            </div>
          </div>
        }
      </div>

      <!-- VUE DESKTOP (>= 768px) : Grille 7 jours type agenda -->
      <div class="hidden md:block max-h-[640px] overflow-auto rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] bg-[var(--app-surface)]">
        <div class="w-full">
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

  readonly activeMobileDay = signal<number>(1);

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

  readonly selectedMobileWeekday = computed(() => this.selectedWeekday() ?? this.activeMobileDay());
  readonly selectedMobileDay = computed(() =>
    this.days().find((d) => d.weekday === this.selectedMobileWeekday()) ?? this.days()[0] ?? null);

  selectMobileDay(weekday: number): void {
    this.activeMobileDay.set(weekday);
    this.weekdaySelected.emit(weekday);
  }

  addRuleForDay(day: CalendarDay): void {
    this.rangeSelected.emit({
      weekday: day.weekday,
      date: new Date(day.date),
      validFrom: day.dateKey,
      startTime: '08:00',
      endTime: '17:00',
    });
  }

  addSlotForDay(day: CalendarDay, startTime: string, endTime: string): void {
    this.rangeSelected.emit({
      weekday: day.weekday,
      date: new Date(day.date),
      validFrom: day.dateKey,
      startTime,
      endTime,
    });
  }

  formatExceptionTimes(exception: WeeklyAvailabilityExceptionView): string {
    const start = new Date(exception.startAt);
    const end = new Date(exception.endAt);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return '';
    return `${start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} → ${end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  }

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

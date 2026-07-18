import { Component, computed, input, output } from '@angular/core';
import { IconComponent } from '../icon.component';

/**
 * Vue minimale d'une règle hebdomadaire affichée dans la grille.
 * Structurellement compatible avec `AvailabilityRule` (clinic/availability).
 */
export interface WeeklyAvailabilityRuleView {
  readonly id: string;
  /** Jour de la semaine ISO-8601 : 1 = lundi … 7 = dimanche. */
  readonly weekday: number;
  /** Heure de début au format `HH:mm`. */
  readonly startTime: string;
  /** Heure de fin au format `HH:mm`. */
  readonly endTime: string;
  readonly active: boolean;
}

/** Libellés injectés par le parent : le composant reste sans dépendance i18n. */
export interface WeeklyAvailabilityGridLabels {
  /** Noms courts des jours (index 0 = lundi). */
  readonly weekdays: readonly string[];
  readonly emptyDay: string;
  readonly inactive: string;
  readonly selectDay: string;
  readonly editRule: string;
  readonly deactivateRule: string;
}

interface WeeklyAvailabilityColumn {
  readonly weekday: number;
  readonly label: string;
  readonly rules: readonly WeeklyAvailabilityRuleView[];
}

/**
 * Grille hebdomadaire (lundi → dimanche) des plages de disponibilité.
 * Composant purement présentationnel : aucune couleur codée en dur (tokens
 * `--app-surface`, `--app-border`, `--brand-primary`, `--text-*` ⇒ light/dark
 * automatiques), rayon ≤ 8px (`--radius-brand-*`), ombre `var(--shadow-panel)`.
 * Les couleurs conditionnelles passent par des bindings de style (les classes
 * Tailwind arbitraires conditionnelles ne sont pas générées par le scanner).
 */
@Component({
  selector: 'app-weekly-availability-grid',
  standalone: true,
  imports: [IconComponent],
  template: `
    <div class="overflow-x-auto">
      <div class="grid min-w-[760px] grid-cols-7 gap-2">
        @for (column of columns(); track column.weekday) {
          <section
            class="flex flex-col rounded-[var(--radius-brand-md)] border bg-[var(--app-surface)] p-2 transition-colors"
            [style.border-color]="selectedWeekday() === column.weekday ? 'var(--brand-primary)' : 'var(--app-border)'"
            [attr.aria-label]="column.label"
          >
            <button
              type="button"
              class="flex w-full items-center justify-between gap-1 rounded-[var(--radius-brand-sm)] px-2 py-1.5 text-xs font-bold uppercase tracking-wide text-[var(--text-secondary)] transition-colors hover:text-[var(--brand-primary)]"
              [attr.aria-pressed]="selectedWeekday() === column.weekday"
              [title]="labels().selectDay"
              (click)="weekdaySelected.emit(column.weekday)"
            >
              <span>{{ column.label }}</span>
              <span class="text-[var(--text-muted)]" aria-hidden="true"><app-ui-icon name="plus" /></span>
            </button>

            <div class="mt-2 flex flex-1 flex-col gap-1.5">
              @if (column.rules.length === 0) {
                <p class="px-1 py-2 text-center text-[11px] leading-4 text-[var(--text-muted)]">
                  {{ labels().emptyDay }}
                </p>
              } @else {
                @for (rule of column.rules; track rule.id) {
                  <article
                    class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] p-1.5 shadow-[var(--shadow-panel)]"
                    [class.opacity-50]="!rule.active"
                  >
                    <button
                      type="button"
                      class="block w-full text-left text-xs font-semibold text-[var(--text-primary)] transition-colors hover:text-[var(--brand-primary)]"
                      [title]="labels().editRule"
                      (click)="ruleSelected.emit(rule)"
                    >
                      {{ rule.startTime }} – {{ rule.endTime }}
                    </button>
                    @if (!rule.active) {
                      <p class="mt-1 text-[10px] font-semibold uppercase text-[var(--text-muted)]">
                        {{ labels().inactive }}
                      </p>
                    } @else {
                      <div class="mt-1 flex justify-end">
                        <button
                          type="button"
                          class="rounded-[var(--radius-brand-sm)] p-0.5 text-[var(--text-muted)] transition-colors hover:text-[var(--brand-danger-text)]"
                          [title]="labels().deactivateRule"
                          [attr.aria-label]="labels().deactivateRule"
                          (click)="ruleDeactivateRequested.emit(rule)"
                        >
                          <app-ui-icon name="x-mark" />
                        </button>
                      </div>
                    }
                  </article>
                }
              }
            </div>
          </section>
        }
      </div>
    </div>
  `,
})
export class WeeklyAvailabilityGridComponent {
  readonly rules = input.required<readonly WeeklyAvailabilityRuleView[]>();
  readonly selectedWeekday = input<number | null>(null);
  readonly labels = input.required<WeeklyAvailabilityGridLabels>();

  readonly weekdaySelected = output<number>();
  readonly ruleSelected = output<WeeklyAvailabilityRuleView>();
  readonly ruleDeactivateRequested = output<WeeklyAvailabilityRuleView>();

  readonly columns = computed<readonly WeeklyAvailabilityColumn[]>(() => {
    const names = this.labels().weekdays;
    const rules = this.rules();
    return Array.from({ length: 7 }, (_, index) => {
      const weekday = index + 1;
      return {
        weekday,
        label: names[index] ?? '',
        rules: rules
          .filter((rule) => rule.weekday === weekday)
          .slice()
          .sort((left, right) => left.startTime.localeCompare(right.startTime)),
      };
    });
  });
}

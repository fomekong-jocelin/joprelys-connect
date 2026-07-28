import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { ConfirmationDialogComponent } from '../../shared/ui/confirmation-dialog.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import {
  WeeklyAvailabilityGridComponent,
  WeeklyAvailabilityGridLabels,
  WeeklyAvailabilityRangeSelection,
  WeeklyAvailabilityRuleView,
} from '../../shared/ui/weekly-availability-grid/weekly-availability-grid.component';
import { AvailabilityApiService } from './availability-api.service';
import {
  AvailabilityException,
  AvailabilityRule,
  CreateAvailabilityExceptionRequest,
  UpsertAvailabilityRuleRequest,
} from './availability.models';
import {
  AvailabilityDaySlots,
  computeUpcomingSlots,
  DEFAULT_SLOT_DURATION_MINUTES,
  toLocalDateKey,
} from './availability-slots.util';

const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const;
const EXCEPTIONS_HORIZON_DAYS = 365;

type PendingAction =
  | { readonly kind: 'deactivate-rule'; readonly rule: AvailabilityRule }
  | { readonly kind: 'delete-exception'; readonly exception: AvailabilityException };

interface ApiErrorShape {
  status?: number;
  error?: { code?: string; message?: string; detail?: string };
}

@Component({
  selector: 'app-availability-page',
  standalone: true,
  imports: [
    AlertComponent,
    AppShellComponent,
    ButtonComponent,
    CardComponent,
    ConfirmationDialogComponent,
    DatePipe,
    PageHeaderComponent,
    WeeklyAvailabilityGridComponent,
  ],
  templateUrl: './availability-page.component.html',
})
export class AvailabilityPageComponent implements OnInit {
  private readonly api = inject(AvailabilityApiService);
  private readonly i18n = inject(I18nService);

  readonly rules = signal<AvailabilityRule[]>([]);
  readonly exceptions = signal<AvailabilityException[]>([]);
  readonly loading = signal(false);
  readonly pageError = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly weekStart = signal(startOfIsoWeek(new Date()));
  readonly weekEnd = computed(() => addDays(this.weekStart(), 6));
  readonly isCurrentWeek = computed(() =>
    toLocalDateKey(this.weekStart()) === toLocalDateKey(startOfIsoWeek(new Date())));
  readonly weekLabel = computed(() => this.formatWeekLabel(this.weekStart(), this.weekEnd()));

  readonly rulesExpanded = signal(false);
  readonly exceptionsExpanded = signal(false);
  readonly slotsExpanded = signal(false);

  readonly entryType = signal<'rule' | 'exception'>('rule');
  readonly showUnifiedModal = signal(false);
  readonly showRuleForm = computed(() => this.showUnifiedModal() && this.entryType() === 'rule');
  readonly showExceptionForm = computed(() => this.showUnifiedModal() && this.entryType() === 'exception');

  readonly ruleFormLoading = signal(false);
  readonly ruleFormError = signal<string | null>(null);
  readonly editingRule = signal<AvailabilityRule | null>(null);
  readonly formWeekday = signal<number>(1);
  readonly formStartTime = signal('');
  readonly formEndTime = signal('');
  readonly formValidFrom = signal('');
  readonly formValidTo = signal('');

  readonly exceptionFormLoading = signal(false);
  readonly exceptionFormError = signal<string | null>(null);
  readonly exceptionStart = signal('');
  readonly exceptionEnd = signal('');
  readonly exceptionReason = signal('');

  readonly pendingAction = signal<PendingAction | null>(null);
  readonly pendingActionLoading = signal(false);

  readonly weekdays = WEEKDAYS;
  readonly slotDurationMinutes = DEFAULT_SLOT_DURATION_MINUTES;

  readonly sortedRules = computed(() =>
    [...this.rules()].sort((left, right) =>
      left.weekday - right.weekday || left.startTime.localeCompare(right.startTime)));
  readonly activeRuleCount = computed(() => this.rules().filter((rule) => rule.active).length);
  readonly sortedExceptions = computed(() =>
    [...this.exceptions()].sort((left, right) => left.startAt.localeCompare(right.startAt)));

  readonly modalTitle = computed(() => {
    if (this.editingRule()) return this.t('availability.rules.editTitle');
    return this.t('availability.modal.addTitle');
  });

  readonly ruleFormTitle = computed(() =>
    this.editingRule() ? this.t('availability.rules.editTitle') : this.t('availability.rules.addTitle'));
  readonly ruleSubmitLabel = computed(() =>
    this.editingRule() ? this.t('availability.rules.update') : this.t('availability.rules.create'));

  readonly modalSubmitLabel = computed(() => {
    if (this.entryType() === 'rule') {
      return this.editingRule() ? this.t('availability.rules.update') : this.t('availability.rules.create');
    }
    return this.t('availability.exceptions.create');
  });

  readonly gridLabels = computed<WeeklyAvailabilityGridLabels>(() => ({
    weekdays: WEEKDAYS.map((day) => this.t(`availability.weekdays.${day}`)),
    emptyDay: this.t('availability.rules.noneForDay'),
    inactive: this.t('availability.rules.inactive'),
    selectDay: this.t('availability.rules.selectDay'),
    editRule: this.t('availability.rules.edit'),
    deactivateRule: this.t('availability.rules.deactivate'),
    available: this.t('availability.calendar.available'),
    unavailable: this.t('availability.calendar.unavailable'),
    clickToAdd: this.t('availability.calendar.clickToAdd'),
    addRange: this.t('availability.grid.addRange'),
    edit: this.t('availability.rules.edit'),
    deactivate: this.t('availability.rules.deactivate'),
    quickSlotsTitle: this.t('availability.grid.quickSlotsTitle'),
    morningSlot: this.t('availability.grid.morningSlot'),
    afternoonSlot: this.t('availability.grid.afternoonSlot'),
    fullDaySlot: this.t('availability.grid.fullDaySlot'),
    customSlot: this.t('availability.grid.customSlot'),
    rangesCount: this.t('availability.grid.rangesCount'),
  }));

  readonly slotsPreview = computed<readonly AvailabilityDaySlots[]>(() =>
    computeUpcomingSlots(this.rules(), this.exceptions()));
  readonly totalSlots = computed(() =>
    this.slotsPreview().reduce((total, day) => total + day.slots.length, 0));
  readonly hasAnySlots = computed(() => this.totalSlots() > 0);

  readonly pendingActionTitle = computed(() =>
    this.pendingAction()?.kind === 'deactivate-rule'
      ? this.t('availability.rules.deactivateTitle')
      : this.t('availability.exceptions.deleteTitle'));
  readonly pendingActionMessage = computed(() => {
    const action = this.pendingAction();
    if (!action) return '';
    return action.kind === 'deactivate-rule'
      ? this.t('availability.rules.deactivateMessage')
      : this.t('availability.exceptions.deleteMessage');
  });
  readonly pendingActionConfirmLabel = computed(() =>
    this.pendingAction()?.kind === 'deactivate-rule'
      ? this.t('availability.rules.deactivate')
      : this.t('availability.exceptions.delete'));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const to = new Date(from.getTime() + EXCEPTIONS_HORIZON_DAYS * 24 * 60 * 60 * 1000);
    forkJoin({
      rules: this.api.listRules(),
      exceptions: this.api.listExceptions(from.toISOString(), to.toISOString()),
    }).subscribe({
      next: ({ rules, exceptions }) => {
        this.rules.set(rules);
        this.exceptions.set(exceptions);
        this.loading.set(false);
      },
      error: (error) => {
        this.pageError.set(this.errorMessage(error, this.t('availability.loadError')));
        this.loading.set(false);
      },
    });
  }

  previousWeek(): void {
    this.weekStart.update((start) => addDays(start, -7));
  }

  nextWeek(): void {
    this.weekStart.update((start) => addDays(start, 7));
  }

  goToCurrentWeek(): void {
    this.weekStart.set(startOfIsoWeek(new Date()));
  }

  openUnifiedModal(): void {
    if (this.showUnifiedModal() && !this.editingRule()) {
      this.cancelUnifiedModal();
      return;
    }
    this.resetRuleForm();
    this.resetExceptionForm();
    this.entryType.set('rule');
    const today = new Date();
    this.formWeekday.set(isoWeekday(today));
    this.formValidFrom.set(toLocalDateKey(today));
    this.successMessage.set(null);
    this.showUnifiedModal.set(true);
  }

  toggleRuleForm(): void {
    this.openUnifiedModal();
  }

  toggleExceptionForm(): void {
    if (this.showUnifiedModal() && this.entryType() === 'exception') {
      this.cancelUnifiedModal();
      return;
    }
    this.resetExceptionForm();
    this.entryType.set('exception');
    const today = new Date();
    const startStr = `${toLocalDateKey(today)}T08:00`;
    const endStr = `${toLocalDateKey(today)}T12:00`;
    this.exceptionStart.set(startStr);
    this.exceptionEnd.set(endStr);
    this.successMessage.set(null);
    this.showUnifiedModal.set(true);
  }

  onWeekdaySelected(weekday: number): void {
    this.successMessage.set(null);
    this.editingRule.set(null);
    this.entryType.set('rule');
    this.ruleFormError.set(null);
    this.formWeekday.set(weekday);
    this.formValidFrom.set(toLocalDateKey(dateForWeekday(this.weekStart(), weekday)));
    if (!this.formStartTime()) this.formStartTime.set('08:00');
    if (!this.formEndTime()) this.formEndTime.set('17:00');
    this.showUnifiedModal.set(true);
  }

  onCalendarRangeSelected(selection: WeeklyAvailabilityRangeSelection): void {
    this.editingRule.set(null);
    this.entryType.set('rule');
    this.ruleFormError.set(null);
    this.successMessage.set(null);
    this.formWeekday.set(selection.weekday);
    this.formStartTime.set(selection.startTime);
    this.formEndTime.set(selection.endTime);
    this.formValidFrom.set(selection.validFrom);
    this.formValidTo.set('');
    this.showUnifiedModal.set(true);
  }

  onFormWeekdayChange(weekday: number): void {
    this.formWeekday.set(weekday);
    if (this.editingRule() === null) {
      this.formValidFrom.set(toLocalDateKey(dateForWeekday(this.weekStart(), weekday)));
    }
  }

  onFormValidFromChange(dateStr: string): void {
    this.formValidFrom.set(dateStr);
    if (dateStr && this.editingRule() === null) {
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        const year = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1;
        const day = parseInt(parts[2], 10);
        const parsed = new Date(year, month, day);
        if (!Number.isNaN(parsed.getTime())) {
          this.formWeekday.set(isoWeekday(parsed));
        }
      }
    }
  }

  startEditRule(rule: WeeklyAvailabilityRuleView): void {
    const current = this.rules().find((item) => item.id === rule.id) ?? null;
    if (!current) return;
    this.editingRule.set(current);
    this.entryType.set('rule');
    this.formWeekday.set(current.weekday);
    this.formStartTime.set(current.startTime);
    this.formEndTime.set(current.endTime);
    this.formValidFrom.set(current.validFrom);
    this.formValidTo.set(current.validTo ?? '');
    this.ruleFormError.set(null);
    this.successMessage.set(null);
    this.showUnifiedModal.set(true);
  }

  cancelUnifiedModal(): void {
    this.showUnifiedModal.set(false);
    this.resetRuleForm();
    this.resetExceptionForm();
  }

  cancelRuleForm(): void {
    this.cancelUnifiedModal();
  }

  cancelExceptionForm(): void {
    this.cancelUnifiedModal();
  }

  submitUnifiedForm(): void {
    if (this.entryType() === 'rule') {
      this.submitRuleForm();
    } else {
      this.submitExceptionForm();
    }
  }

  submitRuleForm(): void {
    this.ruleFormError.set(null);
    this.successMessage.set(null);
    if (!this.formStartTime() || !this.formEndTime() || !this.formValidFrom()) {
      this.ruleFormError.set(this.t('availability.form.requiredFields'));
      return;
    }
    if (this.formEndTime() <= this.formStartTime()) {
      this.ruleFormError.set(this.t('availability.form.invalidTimeRange'));
      return;
    }
    if (this.formValidTo() && this.formValidTo() < this.formValidFrom()) {
      this.ruleFormError.set(this.t('availability.form.invalidValidityRange'));
      return;
    }
    const request: UpsertAvailabilityRuleRequest = {
      weekday: this.formWeekday(),
      startTime: this.formStartTime(),
      endTime: this.formEndTime(),
      validFrom: this.formValidFrom(),
      validTo: this.formValidTo() || null,
    };
    const current = this.editingRule();
    this.ruleFormLoading.set(true);
    const call = current ? this.api.updateRule(current.id, request) : this.api.createRule(request);
    call.subscribe({
      next: () => {
        this.ruleFormLoading.set(false);
        this.showUnifiedModal.set(false);
        this.resetRuleForm();
        this.successMessage.set(this.t(current ? 'availability.success.ruleUpdated' : 'availability.success.ruleCreated'));
        this.load();
      },
      error: (error) => {
        this.ruleFormError.set(this.errorMessage(error, this.t('availability.rules.saveError')));
        this.ruleFormLoading.set(false);
      },
    });
  }

  askDeactivateRule(rule: WeeklyAvailabilityRuleView): void {
    const current = this.rules().find((item) => item.id === rule.id) ?? null;
    if (current) {
      this.successMessage.set(null);
      this.pendingAction.set({ kind: 'deactivate-rule', rule: current });
    }
  }

  submitExceptionForm(): void {
    this.exceptionFormError.set(null);
    this.successMessage.set(null);
    if (!this.exceptionStart() || !this.exceptionEnd()) {
      this.exceptionFormError.set(this.t('availability.form.requiredFields'));
      return;
    }
    const startAt = new Date(this.exceptionStart());
    const endAt = new Date(this.exceptionEnd());
    if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime()) || endAt.getTime() <= startAt.getTime()) {
      this.exceptionFormError.set(this.t('availability.form.invalidDateRange'));
      return;
    }
    const request: CreateAvailabilityExceptionRequest = {
      startAt: startAt.toISOString(),
      endAt: endAt.toISOString(),
      reason: this.exceptionReason().trim() || null,
    };
    this.exceptionFormLoading.set(true);
    this.api.createException(request).subscribe({
      next: () => {
        this.exceptionFormLoading.set(false);
        this.showUnifiedModal.set(false);
        this.resetExceptionForm();
        this.successMessage.set(this.t('availability.success.exceptionCreated'));
        this.load();
      },
      error: (error) => {
        this.exceptionFormError.set(this.errorMessage(error, this.t('availability.exceptions.saveError')));
        this.exceptionFormLoading.set(false);
      },
    });
  }

  askDeleteException(exception: AvailabilityException): void {
    this.successMessage.set(null);
    this.pendingAction.set({ kind: 'delete-exception', exception });
  }

  toggleRulesPanel(): void {
    this.rulesExpanded.update((value) => !value);
  }

  toggleExceptionsPanel(): void {
    this.exceptionsExpanded.update((value) => !value);
  }

  toggleSlotsPanel(): void {
    this.slotsExpanded.update((value) => !value);
  }

  confirmPendingAction(): void {
    const action = this.pendingAction();
    if (!action) return;
    this.pendingActionLoading.set(true);
    if (action.kind === 'deactivate-rule') {
      this.api.deactivateRule(action.rule.id).subscribe({
        next: () => this.finishPendingAction('availability.success.ruleDeactivated'),
        error: (error) => this.failPendingAction(error, this.t('availability.rules.saveError')),
      });
    } else {
      this.api.deleteException(action.exception.id).subscribe({
        next: () => this.finishPendingAction('availability.success.exceptionDeleted'),
        error: (error) => this.failPendingAction(error, this.t('availability.exceptions.deleteError')),
      });
    }
  }

  cancelPendingAction(): void {
    this.pendingAction.set(null);
  }

  dayLabel(date: Date): string {
    return this.t(`availability.weekdays.${isoWeekday(date)}`);
  }

  ruleWeekdayLabel(rule: AvailabilityRule): string {
    return this.t(`availability.weekdays.${rule.weekday}`);
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  private formatWeekLabel(start: Date, end: Date): string {
    const locale = this.i18n.locale() === 'en' ? 'en-GB' : 'fr-FR';
    const startFormatter = new Intl.DateTimeFormat(locale, { day: '2-digit', month: 'short' });
    const endFormatter = new Intl.DateTimeFormat(locale, { day: '2-digit', month: 'short', year: 'numeric' });
    return `${this.t('availability.calendar.week')} ${startFormatter.format(start)} – ${endFormatter.format(end)}`;
  }

  private finishPendingAction(successKey: string): void {
    this.pendingActionLoading.set(false);
    this.pendingAction.set(null);
    this.successMessage.set(this.t(successKey));
    this.load();
  }

  private failPendingAction(error: ApiErrorShape, fallback: string): void {
    this.pendingActionLoading.set(false);
    this.pendingAction.set(null);
    this.pageError.set(this.errorMessage(error, fallback));
  }

  private resetRuleForm(): void {
    this.editingRule.set(null);
    this.formWeekday.set(1);
    this.formStartTime.set('');
    this.formEndTime.set('');
    this.formValidFrom.set('');
    this.formValidTo.set('');
    this.ruleFormError.set(null);
    this.ruleFormLoading.set(false);
  }

  private resetExceptionForm(): void {
    this.exceptionStart.set('');
    this.exceptionEnd.set('');
    this.exceptionReason.set('');
    this.exceptionFormError.set(null);
    this.exceptionFormLoading.set(false);
  }

  private errorMessage(error: ApiErrorShape, fallback: string): string {
    if (error.status === 401) return this.t('common.error.unauthorized');
    if (error.status === 403) return this.t('common.error.forbidden');
    if (error.status && error.status >= 500) return this.t('common.error.server');
    const code = error.error?.code;
    if (code) {
      const key = `availability.error.${code}`;
      const translated = this.t(key);
      if (translated !== key) return translated;
    }
    return error.error?.message ?? error.error?.detail ?? fallback;
  }
}

function startOfIsoWeek(value: Date): Date {
  const date = new Date(value.getFullYear(), value.getMonth(), value.getDate());
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  return date;
}

function addDays(value: Date, days: number): Date {
  return new Date(value.getFullYear(), value.getMonth(), value.getDate() + days);
}

function isoWeekday(value: Date): number {
  return ((value.getDay() + 6) % 7) + 1;
}

function dateForWeekday(weekStart: Date, weekday: number): Date {
  return addDays(startOfIsoWeek(weekStart), Math.max(1, Math.min(7, weekday)) - 1);
}

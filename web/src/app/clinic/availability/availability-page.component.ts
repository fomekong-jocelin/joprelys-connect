import { DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { ConfirmationDialogComponent } from '../../shared/ui/confirmation-dialog.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import {
  WeeklyAvailabilityGridComponent,
  WeeklyAvailabilityGridLabels,
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
} from './availability-slots.util';

const WEEKDAYS = [1, 2, 3, 4, 5, 6, 7] as const;
/** Fenêtre de chargement des indisponibilités : du jour courant à +1 an. */
const EXCEPTIONS_HORIZON_DAYS = 365;

type PendingAction =
  | { readonly kind: 'deactivate-rule'; readonly rule: AvailabilityRule }
  | { readonly kind: 'delete-exception'; readonly exception: AvailabilityException };

interface ApiErrorShape {
  status?: number;
  error?: { code?: string; message?: string; detail?: string };
}

/**
 * Page « Mes disponibilités » (STORY-2602) : plages hebdomadaires récurrentes
 * (CRUD + désactivation logique), indisponibilités (création / suppression) et
 * aperçu indicatif des créneaux libres (calcul client, défaut 30 min).
 */
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
    EmptyStateComponent,
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

  // Formulaire de plage hebdomadaire
  readonly showRuleForm = signal(false);
  readonly ruleFormLoading = signal(false);
  readonly ruleFormError = signal<string | null>(null);
  readonly editingRule = signal<AvailabilityRule | null>(null);
  readonly formWeekday = signal<number>(1);
  readonly formStartTime = signal('');
  readonly formEndTime = signal('');
  readonly formValidFrom = signal('');
  readonly formValidTo = signal('');

  // Formulaire d'indisponibilité
  readonly showExceptionForm = signal(false);
  readonly exceptionFormLoading = signal(false);
  readonly exceptionFormError = signal<string | null>(null);
  readonly exceptionStart = signal('');
  readonly exceptionEnd = signal('');
  readonly exceptionReason = signal('');

  // Confirmation partagée (désactivation de plage / suppression d'indisponibilité)
  readonly pendingAction = signal<PendingAction | null>(null);
  readonly pendingActionLoading = signal(false);

  readonly weekdays = WEEKDAYS;
  readonly slotDurationMinutes = DEFAULT_SLOT_DURATION_MINUTES;

  readonly sortedExceptions = computed(() =>
    [...this.exceptions()].sort((left, right) => left.startAt.localeCompare(right.startAt)));

  readonly ruleFormTitle = computed(() =>
    this.editingRule() ? this.t('availability.rules.editTitle') : this.t('availability.rules.addTitle'));
  readonly ruleSubmitLabel = computed(() =>
    this.editingRule() ? this.t('availability.rules.update') : this.t('availability.rules.create'));

  readonly gridLabels = computed<WeeklyAvailabilityGridLabels>(() => ({
    weekdays: WEEKDAYS.map((day) => this.t(`availability.weekdays.${day}`)),
    emptyDay: this.t('availability.rules.noneForDay'),
    inactive: this.t('availability.rules.inactive'),
    selectDay: this.t('availability.rules.selectDay'),
    editRule: this.t('availability.rules.edit'),
    deactivateRule: this.t('availability.rules.deactivate'),
  }));

  readonly slotsPreview = computed<readonly AvailabilityDaySlots[]>(() =>
    computeUpcomingSlots(this.rules(), this.exceptions()));
  readonly hasAnySlots = computed(() => this.slotsPreview().some((day) => day.slots.length > 0));

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

  // ----- Plages hebdomadaires -----

  toggleRuleForm(): void {
    if (this.showRuleForm() && !this.editingRule()) {
      this.cancelRuleForm();
      return;
    }
    this.resetRuleForm();
    this.successMessage.set(null);
    this.showRuleForm.set(true);
  }

  onWeekdaySelected(weekday: number): void {
    this.successMessage.set(null);
    if (!this.showRuleForm() || this.editingRule() !== null) {
      this.resetRuleForm();
      this.showRuleForm.set(true);
    }
    this.formWeekday.set(weekday);
  }

  startEditRule(rule: WeeklyAvailabilityRuleView): void {
    const current = this.rules().find((item) => item.id === rule.id) ?? null;
    if (!current) return;
    this.editingRule.set(current);
    this.formWeekday.set(current.weekday);
    this.formStartTime.set(current.startTime);
    this.formEndTime.set(current.endTime);
    this.formValidFrom.set(current.validFrom);
    this.formValidTo.set(current.validTo ?? '');
    this.ruleFormError.set(null);
    this.successMessage.set(null);
    this.showRuleForm.set(true);
  }

  cancelRuleForm(): void {
    this.showRuleForm.set(false);
    this.resetRuleForm();
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
        this.showRuleForm.set(false);
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

  // ----- Indisponibilités -----

  toggleExceptionForm(): void {
    if (this.showExceptionForm()) {
      this.cancelExceptionForm();
      return;
    }
    this.resetExceptionForm();
    this.successMessage.set(null);
    this.showExceptionForm.set(true);
  }

  cancelExceptionForm(): void {
    this.showExceptionForm.set(false);
    this.resetExceptionForm();
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
        this.showExceptionForm.set(false);
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

  // ----- Confirmation -----

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

  // ----- Utilitaires -----

  dayLabel(date: Date): string {
    const isoWeekday = ((date.getDay() + 6) % 7) + 1;
    return this.t(`availability.weekdays.${isoWeekday}`);
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
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

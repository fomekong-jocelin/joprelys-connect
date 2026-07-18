import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { EmptyStateComponent } from '../../shared/ui/empty-state.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { DoctorAppointmentsApiService } from './doctor-appointments-api.service';
import {
  DoctorAgendaDay,
  DoctorAppointment,
  DoctorAppointmentStatus,
} from './doctor-appointments.models';

const AUTO_REFRESH_MS = 30_000;
const DAYS_PER_WEEK = 7;

@Component({
  selector: 'app-doctor-appointments-page',
  standalone: true,
  imports: [
    AlertComponent,
    AppShellComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    PageHeaderComponent,
  ],
  templateUrl: './doctor-appointments-page.component.html',
})
export class DoctorAppointmentsPageComponent implements OnInit {
  private readonly api = inject(DoctorAppointmentsApiService);
  private readonly i18n = inject(I18nService);
  private readonly destroyRef = inject(DestroyRef);
  private requestSequence = 0;

  readonly weekStart = signal(this.startOfWeek(new Date()));
  readonly appointments = signal<DoctorAppointment[]>([]);
  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly error = signal<string | null>(null);
  readonly lastUpdatedAt = signal<Date | null>(null);

  readonly days = computed<DoctorAgendaDay[]>(() => {
    const grouped = new Map<string, DoctorAppointment[]>();
    for (const appointment of [...this.appointments()].sort((left, right) =>
      left.startAt.localeCompare(right.startAt))) {
      const key = this.localDateKey(new Date(appointment.startAt));
      const current = grouped.get(key) ?? [];
      current.push(appointment);
      grouped.set(key, current);
    }

    return Array.from({ length: DAYS_PER_WEEK }, (_, index) => {
      const date = this.addDays(this.weekStart(), index);
      const key = this.localDateKey(date);
      return { key, date, appointments: grouped.get(key) ?? [] };
    });
  });

  readonly hasAppointments = computed(() => this.appointments().length > 0);
  readonly isCurrentWeek = computed(() =>
    this.localDateKey(this.weekStart()) === this.localDateKey(this.startOfWeek(new Date())));

  ngOnInit(): void {
    this.load(true);
    interval(AUTO_REFRESH_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.load(false));
  }

  previousWeek(): void {
    this.selectWeek(this.addDays(this.weekStart(), -DAYS_PER_WEEK));
  }

  nextWeek(): void {
    this.selectWeek(this.addDays(this.weekStart(), DAYS_PER_WEEK));
  }

  currentWeek(): void {
    this.selectWeek(this.startOfWeek(new Date()));
  }

  load(showLoading = false): void {
    const requestId = ++this.requestSequence;
    const from = this.weekStart();
    const to = this.addDays(from, DAYS_PER_WEEK);

    if (showLoading) {
      this.loading.set(true);
    } else {
      this.refreshing.set(true);
    }
    this.error.set(null);

    this.api.listOwn(from.toISOString(), to.toISOString()).subscribe({
      next: (appointments) => {
        if (requestId !== this.requestSequence) return;
        this.appointments.set(appointments);
        this.lastUpdatedAt.set(new Date());
        this.loading.set(false);
        this.refreshing.set(false);
      },
      error: () => {
        if (requestId !== this.requestSequence) return;
        this.error.set(this.t('doctorAppointments.errors.load'));
        this.loading.set(false);
        this.refreshing.set(false);
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  weekLabel(): string {
    const start = this.weekStart();
    const end = this.addDays(start, DAYS_PER_WEEK - 1);
    const formatter = new Intl.DateTimeFormat(this.locale(), {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
    return `${formatter.format(start)} – ${formatter.format(end)}`;
  }

  dayLabel(date: Date): string {
    return new Intl.DateTimeFormat(this.locale(), {
      weekday: 'long',
      day: '2-digit',
      month: 'short',
    }).format(date);
  }

  timeLabel(value: string): string {
    return new Intl.DateTimeFormat(this.locale(), {
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }

  lastUpdatedLabel(): string {
    const value = this.lastUpdatedAt();
    if (!value) return '';
    const time = new Intl.DateTimeFormat(this.locale(), {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(value);
    return `${this.t('doctorAppointments.lastUpdated')} ${time}`;
  }

  appointmentCountLabel(count: number): string {
    return count === 1
      ? this.t('doctorAppointments.count.one')
      : this.t('doctorAppointments.count.many').replace('{count}', String(count));
  }

  statusLabel(status: DoctorAppointmentStatus): string {
    return this.t(`appointments.status.${status}`);
  }

  statusClass(status: DoctorAppointmentStatus): string {
    switch (status) {
      case 'COMPLETED':
        return 'bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)] border-[var(--brand-success-muted)]';
      case 'NO_SHOW':
        return 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] border-[var(--brand-warning-border)]';
      case 'CANCELLED_BY_PATIENT':
      case 'CANCELLED_BY_CLINIC':
        return 'bg-[var(--app-surface-muted)] text-[var(--text-muted)] border-[var(--app-border)]';
      case 'CONFIRMED':
      default:
        return 'bg-[var(--brand-info-subtle)] text-[var(--brand-info-text)] border-[var(--brand-primary-border)]';
    }
  }

  private selectWeek(value: Date): void {
    this.weekStart.set(this.startOfWeek(value));
    this.appointments.set([]);
    this.load(true);
  }

  private locale(): string {
    return this.i18n.locale() === 'fr' ? 'fr-FR' : 'en-GB';
  }

  private startOfWeek(value: Date): Date {
    const result = new Date(value);
    result.setHours(0, 0, 0, 0);
    const offset = (result.getDay() + 6) % DAYS_PER_WEEK;
    result.setDate(result.getDate() - offset);
    return result;
  }

  private addDays(value: Date, days: number): Date {
    const result = new Date(value);
    result.setDate(result.getDate() + days);
    return result;
  }

  private localDateKey(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}

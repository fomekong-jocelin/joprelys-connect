import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../../core/i18n/i18n.service';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import {
  AppointmentSlotPickerComponent,
  AppointmentSlotPickerLabels,
} from '../../../shared/ui/appointment-slot-picker/appointment-slot-picker.component';
import { ConfirmationDialogComponent } from '../../../shared/ui/confirmation-dialog.component';
import { PatientAppointmentsApiService } from './patient-appointments-api.service';
import {
  ApiErrorEnvelope,
  AppointmentSlot,
  DoctorDirectoryEntry,
  DoctorSpecialtyEntry,
  DoctorUnitEntry,
  PatientAppointment,
} from './patient-appointments.models';

@Component({
  selector: 'app-patient-appointments-page',
  standalone: true,
  imports: [AppShellComponent, FormsModule, AppointmentSlotPickerComponent, ConfirmationDialogComponent],
  template: `
    <app-shell>
      <main class="app-container space-y-6 py-6">
        <header class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-5 shadow-[var(--shadow-panel)]">
          <p class="text-xs font-black uppercase tracking-wider text-[var(--brand-primary)]">{{ i18n.t('appointments.eyebrow') }}</p>
          <h1 class="mt-2 font-display text-2xl font-extrabold text-[var(--text-primary)]">{{ i18n.t('appointments.title') }}</h1>
          <p class="mt-2 max-w-3xl text-sm leading-6 text-[var(--text-secondary)]">{{ i18n.t('appointments.description') }}</p>
        </header>

        @if (notice()) {
          <div class="rounded-[var(--radius-brand-sm)] border border-emerald-300 bg-emerald-50 p-3 text-sm font-semibold text-emerald-800 dark:border-emerald-800 dark:bg-emerald-950/20 dark:text-emerald-300" role="status">{{ notice() }}</div>
        }
        @if (error()) {
          <div class="rounded-[var(--radius-brand-sm)] border border-rose-300 bg-rose-50 p-3 text-sm font-semibold text-rose-800 dark:border-rose-800 dark:bg-rose-950/20 dark:text-rose-300" role="alert">{{ error() }}</div>
        }

        <section class="grid gap-6 xl:grid-cols-[minmax(0,0.9fr)_minmax(0,1.4fr)]">
          <div class="space-y-4">
            <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4">
              <div class="flex items-center justify-between gap-3">
                <div>
                  <h2 class="font-display text-lg font-extrabold text-[var(--text-primary)]">{{ i18n.t('appointments.doctors.title') }}</h2>
                  <p class="mt-1 text-xs text-[var(--text-secondary)]">{{ i18n.t('appointments.doctors.description') }}</p>
                </div>
                <button type="button" class="ui-button ui-button-secondary" [disabled]="loadingDoctors()" (click)="loadDoctors()">
                  {{ i18n.t('appointments.actions.refresh') }}
                </button>
              </div>

              <div class="mt-4 grid gap-3 sm:grid-cols-2">
                <label class="text-xs font-bold text-[var(--text-secondary)]">
                  {{ i18n.t('appointments.filters.specialty') }}
                  <select class="ui-select mt-1 w-full" [(ngModel)]="specialtyCodeFilter" (change)="onFilterChanged()">
                    <option value="">{{ i18n.t('appointments.filters.allSpecialties', 'Toutes les spécialités') }}</option>
                    @for (item of specialtyOptions(); track item.code) {
                      <option [value]="item.code">{{ specialtyName(item) }}</option>
                    }
                  </select>
                </label>

                <label class="text-xs font-bold text-[var(--text-secondary)]">
                  {{ i18n.t('appointments.filters.department') }}
                  <select class="ui-select mt-1 w-full" [(ngModel)]="organizationalUnitIdFilter" (change)="onFilterChanged()">
                    <option value="">{{ i18n.t('appointments.filters.allUnits', 'Toutes les unités') }}</option>
                    @for (item of unitOptions(); track item.id) {
                      <option [value]="item.id">{{ unitName(item) }}</option>
                    }
                  </select>
                </label>
              </div>

              @if (loadingDoctors()) {
                <p class="py-8 text-center text-sm text-[var(--text-secondary)]">{{ i18n.t('appointments.loading.doctors') }}</p>
              } @else if (doctors().length === 0) {
                <p class="py-8 text-center text-sm text-[var(--text-secondary)]">{{ i18n.t('appointments.empty.doctors') }}</p>
              } @else {
                <div class="mt-4 space-y-2">
                  @for (doctor of doctors(); track doctor.doctorId) {
                    <button
                      type="button"
                      class="w-full rounded-[var(--radius-brand-sm)] border p-3 text-left transition"
                      [style.border-color]="selectedDoctor()?.doctorId === doctor.doctorId ? 'var(--brand-primary)' : 'var(--app-border)'"
                      [style.background]="selectedDoctor()?.doctorId === doctor.doctorId ? 'var(--brand-primary-subtle)' : 'var(--app-surface-muted)'"
                      [attr.aria-pressed]="selectedDoctor()?.doctorId === doctor.doctorId"
                      (click)="selectDoctor(doctor)"
                    >
                      <span class="block text-sm font-extrabold text-[var(--text-primary)]">{{ doctor.displayName }}</span>
                      <span class="mt-1 block text-xs text-[var(--text-secondary)]">{{ doctorContext(doctor) }}</span>
                    </button>
                  }
                </div>
              }
            </div>
          </div>

          <div class="space-y-4">
            <section class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4">
              <h2 class="font-display text-lg font-extrabold text-[var(--text-primary)]">{{ i18n.t('appointments.slots.title') }}</h2>
              @if (!selectedDoctor()) {
                <p class="py-10 text-center text-sm text-[var(--text-secondary)]">{{ i18n.t('appointments.slots.selectDoctor') }}</p>
              } @else if (loadingSlots()) {
                <p class="py-10 text-center text-sm text-[var(--text-secondary)]">{{ i18n.t('appointments.loading.slots') }}</p>
              } @else {
                <app-appointment-slot-picker
                  [slots]="slots()"
                  [selectedStartAt]="selectedSlot()?.startAt ?? null"
                  [locale]="i18n.locale()"
                  [labels]="slotLabels()"
                  (slotSelected)="selectedSlot.set($event)"
                />
              }

              @if (selectedSlot()) {
                <div class="mt-5 border-t border-[var(--app-border)] pt-4">
                  <p class="text-sm font-bold text-[var(--text-primary)]">{{ selectedDoctor()!.displayName }} — {{ formatDateTime(selectedSlot()!.startAt) }}</p>
                  <label class="mt-3 block text-xs font-bold text-[var(--text-secondary)]">
                    {{ i18n.t('appointments.booking.reason') }}
                    <textarea class="ui-input mt-1 min-h-20 w-full" maxlength="2000" [(ngModel)]="reason"></textarea>
                  </label>
                  <button type="button" class="ui-button ui-button-primary mt-3 w-full justify-center" [disabled]="booking()" (click)="bookSelectedSlot()">
                    {{ booking() ? i18n.t('appointments.loading.booking') : i18n.t('appointments.actions.book') }}
                  </button>
                </div>
              }
            </section>
          </div>
        </section>

        <section class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4">
          <div class="flex items-center justify-between gap-3">
            <div>
              <h2 class="font-display text-lg font-extrabold text-[var(--text-primary)]">{{ i18n.t('appointments.mine.title') }}</h2>
              <p class="mt-1 text-xs text-[var(--text-secondary)]">{{ i18n.t('appointments.mine.description') }}</p>
            </div>
            <button type="button" class="ui-button ui-button-secondary" [disabled]="loadingAppointments()" (click)="loadAppointments()">
              {{ i18n.t('appointments.actions.refresh') }}
            </button>
          </div>

          @if (loadingAppointments()) {
            <p class="py-8 text-center text-sm text-[var(--text-secondary)]">{{ i18n.t('appointments.loading.mine') }}</p>
          } @else if (appointments().length === 0) {
            <p class="py-8 text-center text-sm text-[var(--text-secondary)]">{{ i18n.t('appointments.empty.mine') }}</p>
          } @else {
            <div class="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              @for (appointment of appointments(); track appointment.id) {
                <article class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-4">
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <h3 class="text-sm font-extrabold text-[var(--text-primary)]">{{ appointment.doctorDisplayName }}</h3>
                      <p class="mt-1 text-xs text-[var(--text-secondary)]">{{ formatDateTime(appointment.startAt) }}</p>
                    </div>
                    <span class="rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] px-2 py-1 text-[10px] font-black uppercase text-[var(--text-secondary)]">{{ statusLabel(appointment.status) }}</span>
                  </div>
                  @if (appointment.reason) {
                    <p class="mt-3 text-xs leading-5 text-[var(--text-secondary)]">{{ appointment.reason }}</p>
                  }
                  @if (appointment.status === 'CONFIRMED') {
                    <button type="button" class="ui-button ui-button-danger mt-4 w-full justify-center" (click)="requestCancellation(appointment)">{{ i18n.t('appointments.actions.cancel') }}</button>
                  }
                </article>
              }
            </div>
          }
        </section>
      </main>

      <app-confirmation-dialog
        [visible]="cancelTarget() !== null"
        [title]="i18n.t('appointments.cancel.title')"
        [message]="i18n.t('appointments.cancel.message')"
        [confirmLabel]="i18n.t('appointments.actions.confirmCancel')"
        [cancelLabel]="i18n.t('appointments.actions.keep')"
        [busy]="cancelling()"
        (confirmed)="confirmCancellation()"
        (cancelled)="cancelTarget.set(null)"
      />
    </app-shell>
  `,
})
export class PatientAppointmentsPageComponent implements OnInit {
  private readonly api = inject(PatientAppointmentsApiService);
  readonly i18n = inject(I18nService);

  readonly directory = signal<DoctorDirectoryEntry[]>([]);
  readonly doctors = computed(() => this.directory().filter((doctor) => {
    const specialtyMatches = !this.specialtyCodeFilter
      || doctor.specialties.some((item) => item.code === this.specialtyCodeFilter);
    const unitMatches = !this.organizationalUnitIdFilter
      || doctor.units.some((item) => item.id === this.organizationalUnitIdFilter);
    return specialtyMatches && unitMatches;
  }));
  readonly specialtyOptions = computed(() => this.uniqueSpecialties(this.directory()));
  readonly unitOptions = computed(() => this.uniqueUnits(this.directory()));
  readonly slots = signal<AppointmentSlot[]>([]);
  readonly appointments = signal<PatientAppointment[]>([]);
  readonly selectedDoctor = signal<DoctorDirectoryEntry | null>(null);
  readonly selectedSlot = signal<AppointmentSlot | null>(null);
  readonly cancelTarget = signal<PatientAppointment | null>(null);
  readonly loadingDoctors = signal(false);
  readonly loadingSlots = signal(false);
  readonly loadingAppointments = signal(false);
  readonly booking = signal(false);
  readonly cancelling = signal(false);
  readonly error = signal('');
  readonly notice = signal('');

  specialtyCodeFilter = '';
  organizationalUnitIdFilter = '';
  reason = '';

  readonly slotLabels = computed<AppointmentSlotPickerLabels>(() => ({
    empty: this.i18n.t('appointments.empty.slots'),
    select: this.i18n.t('appointments.slots.select'),
  }));

  ngOnInit(): void {
    this.loadDoctors();
    this.loadAppointments();
  }

  loadDoctors(): void {
    this.loadingDoctors.set(true);
    this.clearMessages();
    this.api.listDoctors().subscribe({
      next: (doctors) => {
        this.directory.set(doctors);
        this.loadingDoctors.set(false);
        this.onFilterChanged();
      },
      error: (error) => this.handleError(error, 'appointments.errors.loadDoctors', this.loadingDoctors),
    });
  }

  onFilterChanged(): void {
    const selectedId = this.selectedDoctor()?.doctorId;
    if (selectedId && !this.doctors().some((doctor) => doctor.doctorId === selectedId)) {
      this.selectedDoctor.set(null);
      this.selectedSlot.set(null);
      this.slots.set([]);
    }
  }

  selectDoctor(doctor: DoctorDirectoryEntry): void {
    this.selectedDoctor.set(doctor);
    this.selectedSlot.set(null);
    this.loadSlots();
  }

  loadSlots(): void {
    const doctor = this.selectedDoctor();
    if (!doctor) return;
    this.loadingSlots.set(true);
    this.clearMessages();
    const from = new Date();
    const to = new Date(from.getTime() + 14 * 24 * 60 * 60 * 1000);
    this.api.listSlots(doctor.doctorId, from.toISOString(), to.toISOString()).subscribe({
      next: (slots) => {
        this.slots.set(slots);
        this.loadingSlots.set(false);
        if (this.selectedSlot() && !slots.some((slot) => slot.startAt === this.selectedSlot()!.startAt)) {
          this.selectedSlot.set(null);
        }
      },
      error: (error) => this.handleError(error, 'appointments.errors.loadSlots', this.loadingSlots),
    });
  }

  bookSelectedSlot(): void {
    const doctor = this.selectedDoctor();
    const slot = this.selectedSlot();
    if (!doctor || !slot || this.booking()) return;
    this.booking.set(true);
    this.clearMessages();
    this.api.book({ doctorId: doctor.doctorId, startAt: slot.startAt, reason: this.reason.trim() || undefined }).subscribe({
      next: () => {
        this.booking.set(false);
        this.reason = '';
        this.selectedSlot.set(null);
        this.loadAppointments(false);
        this.loadSlots();
        this.notice.set(this.i18n.t('appointments.success.booked'));
      },
      error: (error: HttpErrorResponse) => {
        this.booking.set(false);
        if (this.errorCode(error) === 'SLOT_UNAVAILABLE') this.loadSlots();
        this.error.set(this.errorMessage(error, 'appointments.errors.booking'));
      },
    });
  }

  loadAppointments(clearMessages = true): void {
    this.loadingAppointments.set(true);
    if (clearMessages) this.clearMessages();
    this.api.listOwn().subscribe({
      next: (appointments) => {
        this.appointments.set(appointments);
        this.loadingAppointments.set(false);
      },
      error: (error) => this.handleError(error, 'appointments.errors.loadMine', this.loadingAppointments),
    });
  }

  requestCancellation(appointment: PatientAppointment): void {
    this.cancelTarget.set(appointment);
    this.clearMessages();
  }

  confirmCancellation(): void {
    const appointment = this.cancelTarget();
    if (!appointment || this.cancelling()) return;
    this.cancelling.set(true);
    this.api.cancel(appointment.id).subscribe({
      next: () => {
        this.cancelling.set(false);
        this.cancelTarget.set(null);
        this.loadAppointments(false);
        if (this.selectedDoctor()?.doctorId === appointment.doctorId) this.loadSlots();
        this.notice.set(this.i18n.t('appointments.success.cancelled'));
      },
      error: (error: HttpErrorResponse) => {
        this.cancelling.set(false);
        this.cancelTarget.set(null);
        this.error.set(this.errorMessage(error, 'appointments.errors.cancel'));
      },
    });
  }

  specialtyName(item: DoctorSpecialtyEntry): string {
    return this.i18n.currentLanguage() === 'en' ? item.nameEn : item.nameFr;
  }

  unitName(item: DoctorUnitEntry): string {
    return this.i18n.currentLanguage() === 'en' ? item.nameEn : item.nameFr;
  }

  doctorContext(doctor: DoctorDirectoryEntry): string {
    const specialty = doctor.specialties.find((item) => item.primary) ?? doctor.specialties[0];
    const unit = doctor.units.find((item) => item.primary) ?? doctor.units[0];
    const specialtyLabel = specialty ? this.specialtyName(specialty) : this.i18n.t('appointments.doctors.specialtyUnknown');
    const unitLabel = unit ? this.unitName(unit) : this.i18n.t('appointments.doctors.departmentUnknown');
    return `${specialtyLabel} · ${unitLabel}`;
  }

  statusLabel(status: string): string {
    return this.i18n.t(`appointments.status.${status}`);
  }

  formatDateTime(value: string): string {
    return new Intl.DateTimeFormat(this.i18n.locale() === 'fr' ? 'fr-FR' : 'en-GB', {
      dateStyle: 'medium',
      timeStyle: 'short',
    }).format(new Date(value));
  }

  private uniqueSpecialties(doctors: DoctorDirectoryEntry[]): DoctorSpecialtyEntry[] {
    const byCode = new Map<string, DoctorSpecialtyEntry>();
    doctors.flatMap((doctor) => doctor.specialties).forEach((item) => byCode.set(item.code, item));
    return [...byCode.values()].sort((left, right) => this.specialtyName(left).localeCompare(this.specialtyName(right)));
  }

  private uniqueUnits(doctors: DoctorDirectoryEntry[]): DoctorUnitEntry[] {
    const byId = new Map<string, DoctorUnitEntry>();
    doctors.flatMap((doctor) => doctor.units).forEach((item) => byId.set(item.id, item));
    return [...byId.values()].sort((left, right) => this.unitName(left).localeCompare(this.unitName(right)));
  }

  private clearMessages(): void {
    this.error.set('');
    this.notice.set('');
  }

  private handleError(error: HttpErrorResponse, fallbackKey: string, loadingSignal: { set(value: boolean): void }): void {
    loadingSignal.set(false);
    this.error.set(this.errorMessage(error, fallbackKey));
  }

  private errorMessage(error: HttpErrorResponse, fallbackKey: string): string {
    const code = this.errorCode(error);
    return code ? this.i18n.t(`appointments.errors.codes.${code}`, this.i18n.t(fallbackKey)) : this.i18n.t(fallbackKey);
  }

  private errorCode(error: HttpErrorResponse): string | undefined {
    return (error.error as ApiErrorEnvelope | undefined)?.error?.code;
  }
}

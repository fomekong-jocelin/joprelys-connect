import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, of } from 'rxjs';
import { HospitalOrganizationApiService } from '../../clinic/hospital-organization/hospital-organization-api.service';
import { StaffApiService } from '../../clinic/staff/staff-api.service';
import { StaffMember } from '../../clinic/staff/staff.models';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import { VisitApiService } from '../../visit/visit-api.service';
import { CreateVisitRequest } from '../../visit/visit.models';

@Component({
  selector: 'app-patient-visit-admission-dialog',
  standalone: true,
  imports: [FormsModule, ButtonComponent],
  template: `
    <div class="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/50 p-0 backdrop-blur-xs sm:items-center sm:p-4 animate-fade-in">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="patient-visit-admission-title"
        class="max-h-[92vh] w-full overflow-y-auto rounded-t-md border border-[var(--app-border)]/80 bg-[var(--app-surface)] p-5 shadow-2xl sm:max-w-md sm:rounded-md sm:p-6"
      >
        <div class="mb-5 flex items-start justify-between gap-4">
          <div>
            <h3 id="patient-visit-admission-title" class="font-display text-lg font-bold text-[var(--text-primary)]">
              {{ i18n.t('patient.visit.admitTitle') }}
            </h3>
            <p class="mt-1 text-xs text-[var(--text-muted)]">{{ i18n.t('patient.visit.admitSubtitle') }}</p>
          </div>
          <button
            type="button"
            class="flex min-h-11 min-w-11 items-center justify-center rounded-md text-[var(--text-muted)] hover:bg-[var(--app-surface-muted)] hover:text-[var(--text-secondary)]"
            [attr.aria-label]="i18n.t('common.cancel')"
            (click)="cancel()"
          >
            <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        @if (visitError) {
          <div class="mb-4 rounded-md border border-[var(--brand-danger-border)] bg-[var(--brand-danger-subtle)] p-3 text-xs font-semibold leading-relaxed text-[var(--brand-danger-text)]">
            {{ visitError }}
          </div>
        }

        <div class="space-y-4">
          <div class="space-y-1.5">
            <label class="ui-label">{{ i18n.t('patient.visit.reasonLabel') }} <span class="text-[var(--brand-danger)]">*</span></label>
            <textarea
              [(ngModel)]="visitReason"
              [placeholder]="i18n.t('patient.visit.reasonPlaceholder')"
              class="ui-textarea min-h-[80px] p-3 text-sm transition-colors focus:border-brand-primary"
              [disabled]="isSubmitting()"
            ></textarea>
          </div>

          <div class="space-y-1.5">
            <label class="ui-label">{{ i18n.t('patient.visit.orientationLabel') }} <span class="text-[var(--brand-danger)]">*</span></label>
            <select
              [(ngModel)]="visitOrientation"
              (ngModelChange)="onOrientationChange()"
              class="ui-select transition-colors focus:border-brand-primary"
              [disabled]="isSubmitting()"
            >
              <option value="" disabled>{{ i18n.t('patient.visit.orientationPlaceholder') }}</option>
              <option value="Médecine générale">{{ i18n.t('patient.visit.orientation.general') }}</option>
              <option value="Tri / Urgences">{{ i18n.t('patient.visit.orientation.emergency') }}</option>
              <option value="Pédiatrie">{{ i18n.t('patient.visit.orientation.pediatrics') }}</option>
              <option value="Gynécologie">{{ i18n.t('patient.visit.orientation.gynecology') }}</option>
              <option value="Pharmacie">{{ i18n.t('patient.visit.orientation.pharmacy') }}</option>
              <option value="Autre">{{ i18n.t('common.other') }}</option>
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="ui-label">{{ i18n.t('patient.visit.serviceLabel') }}</label>
            <select
              [(ngModel)]="selectedVisitService"
              (ngModelChange)="onServiceChange($event)"
              class="ui-select transition-colors focus:border-brand-primary"
              [disabled]="isSubmitting()"
            >
              <option value="">{{ i18n.t('patient.visit.servicePlaceholder') }}</option>
              @for (department of getDepartments(); track department) {
                <option [value]="department">{{ department }}</option>
              }
              <option value="Autre">{{ i18n.t('patient.visit.serviceOther') }}</option>
            </select>

            @if (selectedVisitService === 'Autre') {
              <input
                type="text"
                [(ngModel)]="customVisitService"
                (ngModelChange)="onCustomServiceInput($event)"
                [placeholder]="i18n.t('patient.visit.serviceCustomPlaceholder')"
                class="ui-input mt-2 w-full p-3 text-sm transition-colors focus:border-brand-primary"
                [disabled]="isSubmitting()"
              />
            }
          </div>

          <div class="space-y-1.5">
            <label class="ui-label">{{ i18n.t('patient.visit.practitionerLabel') }}</label>
            <select
              [(ngModel)]="visitMainPractitionerId"
              class="ui-select transition-colors focus:border-brand-primary"
              [disabled]="isSubmitting()"
            >
              <option value="">{{ i18n.t('patient.visit.practitionerPlaceholder') }}</option>
              @for (practitioner of getFilteredPractitioners(); track practitioner.id) {
                <option [value]="practitioner.id">{{ practitioner.displayName }}</option>
              }
            </select>
          </div>

          <div class="space-y-1.5">
            <label class="ui-label">{{ i18n.t('patient.visit.arrivalDateLabel') }}</label>
            <input
              type="datetime-local"
              [(ngModel)]="visitArrivalAt"
              class="ui-input w-full p-3 text-sm transition-colors focus:border-brand-primary"
              [disabled]="isSubmitting()"
            />
          </div>
        </div>

        <div class="mt-7 grid grid-cols-2 gap-3">
          <app-ui-button variant="secondary" (pressed)="cancel()" [disabled]="isSubmitting()">
            {{ i18n.t('common.cancel') }}
          </app-ui-button>
          <app-ui-button variant="primary" (pressed)="submitVisit()" [disabled]="isSubmitting() || !visitReason || !visitOrientation">
            {{ isSubmitting() ? i18n.t('common.saving') : i18n.t('patient.visit.submitLabel') }}
          </app-ui-button>
        </div>
      </section>
    </div>
  `,
})
export class PatientVisitAdmissionDialogComponent implements OnInit {
  readonly patientId = input.required<string>();
  readonly cancelled = output<void>();
  readonly created = output<void>();

  readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);
  private readonly staffApi = inject(StaffApiService);
  private readonly hospitalOrgApi = inject(HospitalOrganizationApiService);

  readonly isSubmitting = signal(false);
  readonly staffList = signal<StaffMember[]>([]);
  readonly catalogServices = signal<string[]>([]);

  visitReason = '';
  visitOrientation = '';
  visitService = '';
  selectedVisitService = '';
  customVisitService = '';
  visitMainPractitionerId = '';
  visitArrivalAt = '';
  visitError = '';

  ngOnInit(): void {
    this.staffApi.list().subscribe({
      next: list => this.staffList.set(list.filter(member => member.enabled)),
      error: () => {},
    });

    this.hospitalOrgApi.listServiceCatalog().pipe(
      catchError(() => of([])),
    ).subscribe(entries => {
      if (entries?.length) {
        this.catalogServices.set(entries.map(entry => entry.nameFr || entry.nameEn || entry.code).filter(Boolean));
      }
    });

    const now = new Date();
    const offsetMs = now.getTimezoneOffset() * 60000;
    this.visitArrivalAt = new Date(now.getTime() - offsetMs).toISOString().slice(0, 16);
  }

  cancel(): void {
    if (!this.isSubmitting()) {
      this.cancelled.emit();
    }
  }

  submitVisit(): void {
    if (!this.visitReason || !this.visitOrientation || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    this.visitError = '';

    const request: CreateVisitRequest = {
      patientId: this.patientId(),
      reason: this.visitReason,
      orientation: this.visitOrientation,
      service: this.visitService?.trim() || undefined,
      mainPractitionerId: this.visitMainPractitionerId?.trim() || undefined,
      arrivalAt: this.visitArrivalAt ? new Date(this.visitArrivalAt).toISOString() : undefined,
    };

    this.visitApi.create(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.created.emit();
      },
      error: err => {
        this.isSubmitting.set(false);
        this.visitError = err.error?.detail || err.error?.title || this.i18n.t('patient.visit.openError');
      },
    });
  }

  /** Praticien principal d'une visite : médecin ou infirmier, du service choisi s'il en a. */
  getFilteredPractitioners(): StaffMember[] {
    const hasRole = (member: StaffMember, role: string) =>
      Boolean(member.role?.split(',').map(value => value.trim()).includes(role));
    const isClinician = (member: StaffMember) => hasRole(member, 'MEDECIN') || hasRole(member, 'INFIRMIER');

    const clinicians = this.staffList().filter(isClinician);
    const service = this.visitService;
    if (service) {
      const byService = clinicians.filter(member => member.department?.toLowerCase() === service.toLowerCase());
      if (byService.length > 0) return byService;
    }

    return clinicians;
  }

  onOrientationChange(): void {
    if (this.visitOrientation) {
      const matchedDepartment = this.getDepartments().find(department => department.toLowerCase() === this.visitOrientation.toLowerCase());
      if (matchedDepartment) {
        this.selectedVisitService = matchedDepartment;
        this.visitService = matchedDepartment;
        this.customVisitService = '';
      } else if (this.visitOrientation === 'Tri / Urgences') {
        const emergencyDepartment = this.getDepartments().find(department => department.toLowerCase().includes('urgence'));
        if (emergencyDepartment) {
          this.selectedVisitService = emergencyDepartment;
          this.visitService = emergencyDepartment;
          this.customVisitService = '';
        }
      }
    }
    this.updateAvailablePractitioners();
  }

  onServiceChange(value: string): void {
    this.selectedVisitService = value;
    if (value !== 'Autre') {
      this.visitService = value;
      this.customVisitService = '';
    } else {
      this.visitService = this.customVisitService;
    }
    this.updateAvailablePractitioners();
  }

  onCustomServiceInput(value: string): void {
    this.customVisitService = value;
    this.visitService = value;
    this.updateAvailablePractitioners();
  }

  updateAvailablePractitioners(): void {
    if (!this.getFilteredPractitioners().some(practitioner => practitioner.id === this.visitMainPractitionerId)) {
      this.visitMainPractitionerId = '';
    }
  }

  getDepartments(): string[] {
    const defaults = [
      'Médecine générale',
      'Pédiatrie',
      'Gynécologie',
      'Urgences',
      'Pharmacie',
      'Laboratoire',
      'Cardiologie',
    ];
    const departments = new Set<string>([...defaults, ...this.catalogServices()]);
    this.staffList().forEach(member => {
      if (member.department) departments.add(member.department);
    });
    return Array.from(departments).sort();
  }
}

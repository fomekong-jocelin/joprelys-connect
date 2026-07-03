import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientConsent, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-consents-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-card-subtle p-5 lg:p-6 flex flex-col gap-4">
      <div class="flex flex-col gap-1 border-b border-[var(--app-border)] pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="ui-label">{{ i18n.t('patient.dashboard.consent') }}</p>
          <h3 class="font-display text-xl font-extrabold" style="color: var(--text-primary)">
            {{ i18n.t('patient.consent.title') }}
          </h3>
        </div>
      </div>

      <p class="text-xs text-[var(--text-secondary)] leading-relaxed">
        {{ i18n.t('patient.consent.subtitle') }}
      </p>

      @if (isLoading()) {
        <div class="flex items-center justify-center py-12">
          <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
        </div>
      } @else if (error()) {
        <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
          {{ error() }}
        </div>
      } @else {
        <div class="flex flex-col gap-3">
          @for (consent of consents(); track consent.organizationId) {
            <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4 flex items-center justify-between gap-4">
              <div class="flex items-center gap-3 min-w-0">
                <div class="p-2.5 rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] text-[var(--brand-primary)] shrink-0">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.33l-7.5-5-7.5 5V21" />
                  </svg>
                </div>
                <div class="min-w-0">
                  <h4 class="font-display font-bold text-sm text-[var(--text-primary)] truncate">
                    {{ consent.organizationName }}
                  </h4>
                  @if (consent.isCreator) {
                    <span class="inline-flex items-center gap-1 text-[10px] font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                      <span class="w-1 h-1 rounded-full bg-emerald-500"></span>
                      {{ i18n.t('patient.consent.creator') }}
                    </span>
                  } @else {
                    <span class="text-[10px] font-medium text-[var(--text-muted)]">
                      {{ consent.status === 'ACTIVE' ? i18n.t('patient.consent.active') : i18n.t('patient.consent.revoked') }}
                    </span>
                  }
                </div>
              </div>

              <div class="shrink-0">
                @if (consent.isCreator) {
                  <span class="inline-flex items-center justify-center px-2.5 py-1 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-400">
                    {{ i18n.t('patient.consent.statusActive') }}
                  </span>
                } @else {
                  <button
                    type="button"
                    (click)="toggleConsent(consent)"
                    [attr.aria-label]="i18n.t('patient.consent.toggleAria') + consent.organizationName"
                    [class]="consent.status === 'ACTIVE'
                      ? 'px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 transition-colors cursor-pointer'
                      : 'px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)] transition-colors cursor-pointer'"
                  >
                    {{ consent.status === 'ACTIVE' ? i18n.t('patient.consent.active') : i18n.t('patient.consent.revoked') }}
                  </button>
                }
              </div>
            </div>
          } @empty {
            <div class="flex flex-col items-center justify-center p-8 border border-dashed border-[var(--app-border)] rounded-[var(--radius-brand-md)] text-[var(--text-muted)] bg-[var(--app-surface-muted)]">
              <p class="text-sm font-semibold">{{ i18n.t('patient.consent.empty') }}</p>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class PatientConsentsListComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly consents = signal<PatientConsent[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadConsents();
  }

  loadConsents(): void {
    this.isLoading.set(true);
    this.portalService.getConsents().subscribe({
      next: (data) => {
        this.consents.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('patient.consent.loadError'));
        this.isLoading.set(false);
      }
    });
  }

  toggleConsent(consent: PatientConsent): void {
    const nextStatus = consent.status === 'ACTIVE' ? 'REVOKED' : 'ACTIVE';
    this.portalService.updateConsent(consent.organizationId, nextStatus).subscribe({
      next: () => {
        this.consents.update((list) =>
          list.map((c) =>
            c.organizationId === consent.organizationId
              ? { ...c, status: nextStatus }
              : c
          )
        );
      },
      error: () => {
        alert(this.i18n.t('patient.consent.updateError'));
      }
    });
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { CardComponent } from '../../../shared/ui/card.component';
import { StatusBadgeComponent } from '../../../shared/ui/status-badge.component';
import { PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-profile-page',
  standalone: true,
  imports: [CommonModule, AppShellComponent, PageHeaderComponent, CardComponent, StatusBadgeComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('patient.profile.title')"
        [subtitle]="t('patient.profile.subtitle')"
      />

      <div class="app-container pb-10">
        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
            {{ error() }}
          </div>
        } @else if (patient(); as p) {
          <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <div class="lg:col-span-1 space-y-6">
              <app-ui-card [title]="t('patient.profile.identity')">
                <div class="space-y-3 text-sm">
                  <div>
                    <span class="ui-label">{{ t('patient.profile.fullName') }}</span>
                    <span class="font-semibold block" style="color: var(--text-primary)">{{ p.fullName }}</span>
                  </div>
                  <div>
                    <span class="ui-label">{{ t('patient.profile.globalPatientNumber') }}</span>
                    <span class="font-mono block" style="color: var(--text-secondary)">{{ p.globalPatientNumber }}</span>
                  </div>
                  <div class="grid grid-cols-2 gap-4">
                    <div>
                      <span class="ui-label">{{ t('patient.profile.birthDate') }}</span>
                      <span style="color: var(--text-secondary)">{{ p.birthDate | date:'dd/MM/yyyy' }}</span>
                    </div>
                    <div>
                      <span class="ui-label">{{ t('patient.profile.gender') }}</span>
                      <span style="color: var(--text-secondary)">{{ p.gender }}</span>
                    </div>
                  </div>
                  @if (p.bloodGroup) {
                    <div>
                      <span class="ui-label">{{ t('patient.profile.bloodGroup') }}</span>
                      <app-status-badge [active]="true" [label]="p.bloodGroup" />
                    </div>
                  }
                </div>
              </app-ui-card>

              <app-ui-card [title]="t('patient.profile.contact')">
                <div class="space-y-3 text-sm">
                  <div>
                    <span class="ui-label">{{ t('patient.profile.phone') }}</span>
                    <span style="color: var(--text-secondary)">{{ p.phone || '-' }}</span>
                  </div>
                  <div>
                    <span class="ui-label">{{ t('patient.profile.email') }}</span>
                    <span style="color: var(--text-secondary)">{{ p.email || '-' }}</span>
                  </div>
                  <div>
                    <span class="ui-label">{{ t('patient.profile.address') }}</span>
                    <span style="color: var(--text-secondary)">{{ p.address || '-' }}</span>
                  </div>
                  <div>
                    <span class="ui-label">{{ t('patient.profile.city') }}</span>
                    <span style="color: var(--text-secondary)">{{ p.city || '-' }}</span>
                  </div>
                </div>
              </app-ui-card>
            </div>

            <div class="lg:col-span-2 space-y-6">
              <app-ui-card [title]="t('patient.profile.emergencyContact')">
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
                  <div>
                    <span class="ui-label">{{ t('patient.profile.emergencyContactName') }}</span>
                    <span class="font-semibold block" style="color: var(--text-primary)">{{ p.emergencyContactName || '-' }}</span>
                  </div>
                  <div>
                    <span class="ui-label">{{ t('patient.profile.emergencyContactPhone') }}</span>
                    <span style="color: var(--text-secondary)">{{ p.emergencyContactPhone || '-' }}</span>
                  </div>
                </div>
              </app-ui-card>

              <app-ui-card [title]="t('patient.profile.allergies')">
                <p class="text-sm whitespace-pre-line" style="color: var(--text-secondary)">
                  {{ p.allergies || t('patient.profile.noAllergies') }}
                </p>
              </app-ui-card>

              <app-ui-card [title]="t('patient.profile.medicalHistory')">
                <p class="text-sm whitespace-pre-line" style="color: var(--text-secondary)">
                  {{ p.medicalHistory || t('patient.profile.noMedicalHistory') }}
                </p>
              </app-ui-card>
            </div>
          </div>
        }
      </div>
    </app-shell>
  `,
})
export class PatientProfilePageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly patient = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.loadProfile();
  }

  private loadProfile(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMe().subscribe({
      next: (data) => {
        this.patient.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.t('patient.profile.loadError'));
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}

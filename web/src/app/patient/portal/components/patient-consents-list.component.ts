import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientConsent, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

const ALL_SCOPES = [
  { key: 'medical_records',   labelFr: 'Dossier médical',     labelEn: 'Medical records' },
  { key: 'prescriptions',     labelFr: 'Ordonnances',         labelEn: 'Prescriptions' },
  { key: 'lab_results',       labelFr: 'Résultats de labo',   labelEn: 'Lab results' },
  { key: 'allergies_history', labelFr: 'Allergies & ATCD',    labelEn: 'Allergies & history' },
];

const CHANNELS = [
  { key: 'PORTAL',    labelFr: 'Portail patient',  labelEn: 'Patient portal' },
  { key: 'OTP_SMS',   labelFr: 'OTP par SMS',      labelEn: 'SMS OTP' },
  { key: 'OTP_EMAIL', labelFr: 'OTP par email',    labelEn: 'Email OTP' },
];

@Component({
  selector: 'app-patient-consents-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
            <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4 flex flex-col gap-3">

              <!-- Ligne principale -->
              <div class="flex items-center justify-between gap-4">
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

                <div class="flex items-center gap-2 shrink-0">
                  @if (!consent.isCreator) {
                    <!-- Bouton paramètres scopes -->
                    <button
                      type="button"
                      (click)="toggleScopesPanel(consent.organizationId)"
                      class="p-1.5 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] text-[var(--text-muted)] hover:text-[var(--brand-primary)] hover:border-[var(--brand-primary)] transition-colors"
                      title="Paramètres d'accès granulaires"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.893.149c-.425.07-.765.383-.93.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.397.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.273-.806.108-1.204-.165-.397-.505-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.108-1.204l-.526-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z" />
                        <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                    </button>
                    <!-- Toggle statut consentement -->
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
                  } @else {
                    <span class="inline-flex items-center justify-center px-2.5 py-1 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-400">
                      {{ i18n.t('patient.consent.statusActive') }}
                    </span>
                  }
                </div>
              </div>

              <!-- Panneau scopes granulaires (accordéon) — TICKET-1306 -->
              @if (!consent.isCreator && openScopesPanel() === consent.organizationId) {
                <div class="border-t border-[var(--app-border)] pt-3 flex flex-col gap-3">
                  <p class="text-[11px] font-semibold text-[var(--text-secondary)]">Données accessibles par cette clinique :</p>

                  <div class="grid grid-cols-2 gap-2">
                    @for (scope of allScopes; track scope.key) {
                      <label class="flex items-center gap-2 text-xs text-[var(--text-primary)] cursor-pointer select-none">
                        <input
                          type="checkbox"
                          [checked]="isScopeEnabled(consent, scope.key)"
                          (change)="toggleScope(consent, scope.key)"
                          class="h-3.5 w-3.5 rounded border-gray-300 text-[var(--brand-primary)] focus:ring-[var(--brand-primary)]"
                        />
                        {{ scope.labelFr }}
                      </label>
                    }
                  </div>

                  <div class="flex flex-col gap-1">
                    <p class="text-[11px] font-semibold text-[var(--text-secondary)]">Canal de validation du consentement :</p>
                    <div class="flex flex-wrap gap-3">
                      @for (ch of channels; track ch.key) {
                        <label class="flex items-center gap-1.5 text-xs text-[var(--text-primary)] cursor-pointer">
                          <input
                            type="radio"
                            [name]="'channel_' + consent.organizationId"
                            [value]="ch.key"
                            [checked]="(consent.validationChannel ?? 'PORTAL') === ch.key"
                            (change)="setChannel(consent, ch.key)"
                            class="h-3.5 w-3.5 text-[var(--brand-primary)] focus:ring-[var(--brand-primary)]"
                          />
                          {{ ch.labelFr }}
                        </label>
                      }
                    </div>
                  </div>

                  <div class="flex justify-end">
                    <button
                      type="button"
                      (click)="saveScopes(consent)"
                      [disabled]="isSavingScopes(consent.organizationId)"
                      class="px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 transition-colors disabled:opacity-50"
                    >
                      {{ isSavingScopes(consent.organizationId) ? 'Enregistrement...' : 'Enregistrer les accès' }}
                    </button>
                  </div>
                </div>
              }

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
  readonly openScopesPanel = signal<string | null>(null);
  readonly savingScopes = signal<Record<string, boolean>>({});

  readonly allScopes = ALL_SCOPES;
  readonly channels = CHANNELS;

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

  toggleScopesPanel(orgId: string): void {
    this.openScopesPanel.update(current => current === orgId ? null : orgId);
  }

  isScopeEnabled(consent: PatientConsent, scopeKey: string): boolean {
    const scopes = consent.scopes ?? 'medical_records,prescriptions,lab_results,allergies_history';
    return scopes.split(',').map(s => s.trim()).includes(scopeKey);
  }

  toggleScope(consent: PatientConsent, scopeKey: string): void {
    const current = (consent.scopes ?? 'medical_records,prescriptions,lab_results,allergies_history')
      .split(',').map(s => s.trim());
    const updated = current.includes(scopeKey)
      ? current.filter(s => s !== scopeKey)
      : [...current, scopeKey];
    this.consents.update(list =>
      list.map(c => c.organizationId === consent.organizationId
        ? { ...c, scopes: updated.join(',') }
        : c
      )
    );
  }

  setChannel(consent: PatientConsent, channel: string): void {
    this.consents.update(list =>
      list.map(c => c.organizationId === consent.organizationId
        ? { ...c, validationChannel: channel }
        : c
      )
    );
  }

  isSavingScopes(orgId: string): boolean {
    return !!this.savingScopes()[orgId];
  }

  saveScopes(consent: PatientConsent): void {
    const scopes = consent.scopes ?? 'medical_records,prescriptions,lab_results,allergies_history';
    const channel = consent.validationChannel ?? 'PORTAL';
    this.savingScopes.update(prev => ({ ...prev, [consent.organizationId]: true }));
    this.portalService.updateConsentScopes(consent.organizationId, scopes, channel).subscribe({
      next: () => {
        this.savingScopes.update(prev => ({ ...prev, [consent.organizationId]: false }));
        this.openScopesPanel.set(null);
      },
      error: () => {
        this.savingScopes.update(prev => ({ ...prev, [consent.organizationId]: false }));
        alert('Erreur lors de l\'enregistrement des accès.');
      }
    });
  }
}

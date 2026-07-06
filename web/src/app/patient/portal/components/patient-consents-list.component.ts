import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  ConsentHistoryItem,
  PatientConsent,
  PatientPortalService
} from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

const ALL_SCOPES = [
  { key: 'medical_records' },
  { key: 'prescriptions' },
  { key: 'lab_results' },
  { key: 'allergies_history' },
];

const CHANNELS = [
  { key: 'PORTAL' },
  { key: 'OTP_SMS' },
  { key: 'OTP_EMAIL' },
  { key: 'AGENT_HABILITE' },
];

const CONSENT_TYPES = ['PONCTUEL', 'TEMPORAIRE', 'ETABLISSEMENT', 'PROFESSIONNEL', 'LIMITE', 'URGENCE'];

const CONSENT_STATUSES = ['REQUESTED', 'APPROVED', 'ACTIVE', 'REJECTED', 'EXPIRED', 'REVOKED'];

@Component({
  selector: 'app-patient-consents-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ui-card-subtle p-5 lg:p-6 flex flex-col gap-6">

      <!-- En-tête -->
      <div class="flex flex-col gap-1 border-b border-[var(--app-border)] pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="ui-label">{{ i18n.t('patient.dashboard.consent') }}</p>
          <h3 class="font-display text-xl font-extrabold" style="color: var(--text-primary)">
            {{ i18n.t('patient.consent.title') }}
          </h3>
        </div>
        <!-- Onglets -->
        <div class="flex gap-2 mt-2 sm:mt-0">
          <button
            type="button"
            id="tab-active-consents"
            (click)="activeTab.set('active')"
            [class]="activeTab() === 'active'
              ? 'px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)] text-white'
              : 'px-3 py-1.5 text-xs font-semibold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)] transition-colors'"
          >
            {{ i18n.t('patient.consent.tab.active') }}
          </button>
          <button
            type="button"
            id="tab-consent-history"
            (click)="activeTab.set('history'); loadHistory()"
            [class]="activeTab() === 'history'
              ? 'px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)] text-white'
              : 'px-3 py-1.5 text-xs font-semibold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)] transition-colors'"
          >
            {{ i18n.t('patient.consent.tab.history') }}
          </button>
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

        <!-- === Onglet Établissements === -->
        @if (activeTab() === 'active') {
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
                          {{ consent.status === 'ACTIVE' || consent.status === 'APPROVED' ? i18n.t('patient.consent.active') : i18n.t('patient.consent.revoked') }}
                        </span>
                      }
                    </div>
                  </div>

                  <div class="flex items-center gap-2 shrink-0">
                    @if (!consent.isCreator) {
                      <!-- Bouton paramètres scopes -->
                      <button
                        type="button"
                        id="btn-settings-{{ consent.organizationId }}"
                        (click)="toggleScopesPanel(consent.organizationId)"
                        class="p-1.5 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] text-[var(--text-muted)] hover:text-[var(--brand-primary)] hover:border-[var(--brand-primary)] transition-colors"
                        [title]="i18n.t('patient.consent.settingsTitle')"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4">
                          <path stroke-linecap="round" stroke-linejoin="round" d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 011.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.56.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.893.149c-.425.07-.765.383-.93.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 01-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.397.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 01-.12-1.45l.527-.737c.25-.35.273-.806.108-1.204-.165-.397-.505-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.108-1.204l-.526-.738a1.125 1.125 0 01.12-1.45l.773-.773a1.125 1.125 0 011.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894z" />
                          <path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                      </button>
                      <!-- Toggle statut consentement -->
                      <button
                        type="button"
                        id="btn-toggle-{{ consent.organizationId }}"
                        (click)="toggleConsent(consent)"
                        [attr.aria-label]="i18n.t('patient.consent.toggleAria') + consent.organizationName"
                        [class]="(consent.status === 'ACTIVE' || consent.status === 'APPROVED')
                          ? 'px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 transition-colors cursor-pointer'
                          : 'px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)] transition-colors cursor-pointer'"
                      >
                        {{ (consent.status === 'ACTIVE' || consent.status === 'APPROVED') ? i18n.t('patient.consent.active') : i18n.t('patient.consent.revoked') }}
                      </button>
                    } @else {
                      <span class="inline-flex items-center justify-center px-2.5 py-1 text-xs font-bold rounded-[var(--radius-brand-sm)] bg-emerald-50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-400">
                        {{ i18n.t('patient.consent.statusActive') }}
                      </span>
                    }
                  </div>
                </div>

                <!-- Panneau scopes granulaires (accordéon) -->
                @if (!consent.isCreator && openScopesPanel() === consent.organizationId) {
                  <div class="border-t border-[var(--app-border)] pt-3 flex flex-col gap-3">
                    <p class="text-[11px] font-semibold text-[var(--text-secondary)]">{{ i18n.t('patient.consent.accessibleData') }}</p>

                    <div class="grid grid-cols-2 gap-2">
                      @for (scope of allScopes; track scope.key) {
                        <label class="flex items-center gap-2 text-xs text-[var(--text-primary)] cursor-pointer select-none">
                          <input
                            type="checkbox"
                            [checked]="isScopeEnabled(consent, scope.key)"
                            (change)="toggleScope(consent, scope.key)"
                            class="ui-checkbox"
                          />
                          {{ i18n.t('patient.consent.scope.' + scope.key) }}
                        </label>
                      }
                    </div>

                    <div class="flex flex-col gap-1">
                      <p class="text-[11px] font-semibold text-[var(--text-secondary)]">{{ i18n.t('patient.consent.validationChannel') }}</p>
                      <div class="flex flex-wrap gap-3">
                        @for (ch of channels; track ch.key) {
                          <label class="flex items-center gap-1.5 text-xs text-[var(--text-primary)] cursor-pointer">
                            <input
                              type="radio"
                              [name]="'channel_' + consent.organizationId"
                              [value]="ch.key"
                              [checked]="(consent.validationChannel ?? 'PORTAL') === ch.key"
                              (change)="setChannel(consent, ch.key)"
                              class="ui-radio"
                            />
                            {{ i18n.t('patient.consent.channel.' + ch.key) }}
                          </label>
                        }
                      </div>
                    </div>

                    <div class="flex justify-end">
                      <button
                        type="button"
                        id="btn-save-scopes-{{ consent.organizationId }}"
                        (click)="saveScopes(consent)"
                        [disabled]="isSavingScopes(consent.organizationId)"
                        class="px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 transition-colors disabled:opacity-50"
                      >
                        {{ isSavingScopes(consent.organizationId) ? i18n.t('patient.consent.saving') : i18n.t('patient.consent.saveScopes') }}
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

        <!-- === Onglet Historique CDC === -->
        @if (activeTab() === 'history') {
          <div class="flex flex-col gap-3">
            @if (historyLoading()) {
              <div class="flex items-center justify-center py-8">
                <div class="w-7 h-7 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
              </div>
            } @else {
              @for (item of consentHistory(); track item.id) {
                <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4 flex flex-col gap-2">
                  <div class="flex items-start justify-between gap-3">
                    <div class="flex flex-col gap-1 min-w-0">
                      <!-- Type + Statut -->
                      <div class="flex items-center gap-2 flex-wrap">
                        <span class="text-xs font-bold text-[var(--text-primary)]">
                          {{ consentTypeLabel(item.consentType) }}
                        </span>
                        <span [class]="'inline-flex px-1.5 py-0.5 text-[10px] font-bold rounded border ' + statusCss(item.status)">
                          {{ statusLabel(item.status) }}
                        </span>
                      </div>
                      <!-- Motif -->
                      @if (item.reason) {
                        <p class="text-[11px] text-[var(--text-secondary)]">
                          <span class="font-semibold">{{ i18n.t('patient.consent.reason') }}</span> {{ item.reason }}
                        </p>
                      }
                      <!-- Dates -->
                      <div class="flex flex-wrap gap-3 text-[10px] text-[var(--text-muted)]">
                        @if (item.requestedAt) {
                          <span>{{ i18n.t('patient.consent.requestedAt') }} {{ item.requestedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                        }
                        @if (item.approvedAt) {
                          <span class="text-emerald-600 dark:text-emerald-400">{{ i18n.t('patient.consent.approvedAt') }} {{ item.approvedAt | date:'dd/MM/yyyy HH:mm' }}</span>
                        }
                        @if (item.expiresAt) {
                          <span [class]="isExpired(item.expiresAt) ? 'text-rose-500' : 'text-amber-600 dark:text-amber-400'">
                            {{ i18n.t('patient.consent.expiresAt') }} {{ item.expiresAt | date:'dd/MM/yyyy HH:mm' }}
                          </span>
                        }
                      </div>
                      <!-- Canal de validation -->
                      @if (item.validationChannel) {
                        <span class="text-[10px] text-[var(--text-muted)]">{{ i18n.t('patient.consent.channel') }} {{ i18n.t('patient.consent.channel.' + item.validationChannel) }}</span>
                      }
                    </div>
                    <!-- Bouton révocation (FR-CONSENT-004) -->
                    @if (item.status === 'APPROVED' || item.status === 'ACTIVE') {
                      <button
                        type="button"
                        id="btn-revoke-{{ item.id }}"
                        (click)="revokeConsent(item.id)"
                        [disabled]="isRevoking(item.id)"
                        class="shrink-0 px-2.5 py-1 text-[10px] font-bold rounded-[var(--radius-brand-sm)] border border-rose-300 dark:border-rose-800 text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 transition-colors disabled:opacity-50"
                      >
                        {{ isRevoking(item.id) ? '...' : i18n.t('patient.consent.revoke') }}
                      </button>
                    }
                    <!-- Boutons approbation/rejet pour REQUESTED -->
                    @if (item.status === 'REQUESTED') {
                      <div class="flex gap-1.5 shrink-0">
                        <button
                          type="button"
                          id="btn-approve-{{ item.id }}"
                          (click)="approveConsent(item.id)"
                          [disabled]="isRevoking(item.id)"
                          class="px-2.5 py-1 text-[10px] font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 transition-colors disabled:opacity-50"
                        >
                          {{ i18n.t('patient.consent.approve') }}
                        </button>
                        <button
                          type="button"
                          id="btn-reject-{{ item.id }}"
                          (click)="rejectConsent(item.id)"
                          [disabled]="isRevoking(item.id)"
                          class="px-2.5 py-1 text-[10px] font-bold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] text-[var(--text-secondary)] hover:border-rose-500 hover:text-rose-500 transition-colors disabled:opacity-50"
                        >
                          {{ i18n.t('patient.consent.reject') }}
                        </button>
                      </div>
                    }
                  </div>
                </div>
              } @empty {
                <div class="flex flex-col items-center justify-center p-8 border border-dashed border-[var(--app-border)] rounded-[var(--radius-brand-md)] text-[var(--text-muted)] bg-[var(--app-surface-muted)]">
                  <p class="text-sm font-semibold">{{ i18n.t('patient.consent.historyEmpty') }}</p>
                </div>
              }
            }
          </div>
        }

      }
    </div>
  `
})
export class PatientConsentsListComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  // Onglet actif
  readonly activeTab = signal<'active' | 'history'>('active');

  // === Onglet Établissements ===
  readonly consents = signal<PatientConsent[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly openScopesPanel = signal<string | null>(null);
  readonly savingScopes = signal<Record<string, boolean>>({});

  readonly allScopes = ALL_SCOPES;
  readonly channels = CHANNELS;

  // === Onglet Historique CDC ===
  readonly consentHistory = signal<ConsentHistoryItem[]>([]);
  readonly historyLoading = signal<boolean>(false);
  readonly revokingMap = signal<Record<string, boolean>>({});

  ngOnInit(): void {
    this.loadConsents();
  }

  // ─── Onglet Établissements ────────────────────────────────────────────────

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
    const isActive = consent.status === 'ACTIVE' || consent.status === 'APPROVED';
    const nextStatus = isActive ? 'REVOKED' : 'ACTIVE';
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
        alert(this.i18n.t('patient.consent.toggleError'));
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
        alert(this.i18n.t('patient.consent.saveScopesError'));
      }
    });
  }

  // ─── Onglet Historique CDC ────────────────────────────────────────────────

  loadHistory(): void {
    if (this.consentHistory().length > 0) return; // Déjà chargé
    this.historyLoading.set(true);
    this.portalService.getConsentHistory().subscribe({
      next: (data) => {
        this.consentHistory.set(data);
        this.historyLoading.set(false);
      },
      error: () => {
        this.historyLoading.set(false);
      }
    });
  }

  isRevoking(id: string): boolean {
    return !!this.revokingMap()[id];
  }

  /** FR-CONSENT-004 : Révoque un consentement approuvé */
  revokeConsent(id: string): void {
    this.revokingMap.update(prev => ({ ...prev, [id]: true }));
    this.portalService.revokeConsent(id).subscribe({
      next: (updated) => {
        this.consentHistory.update(list => list.map(c => c.id === id ? updated : c));
        this.revokingMap.update(prev => ({ ...prev, [id]: false }));
      },
      error: () => {
        alert(this.i18n.t('patient.consent.revokeError'));
        this.revokingMap.update(prev => ({ ...prev, [id]: false }));
      }
    });
  }

  approveConsent(id: string): void {
    this.revokingMap.update(prev => ({ ...prev, [id]: true }));
    this.portalService.approveConsent(id).subscribe({
      next: (updated) => {
        this.consentHistory.update(list => list.map(c => c.id === id ? updated : c));
        this.revokingMap.update(prev => ({ ...prev, [id]: false }));
      },
      error: () => {
        alert(this.i18n.t('patient.consent.approveError'));
        this.revokingMap.update(prev => ({ ...prev, [id]: false }));
      }
    });
  }

  rejectConsent(id: string): void {
    this.revokingMap.update(prev => ({ ...prev, [id]: true }));
    this.portalService.rejectConsent(id).subscribe({
      next: (updated) => {
        this.consentHistory.update(list => list.map(c => c.id === id ? updated : c));
        this.revokingMap.update(prev => ({ ...prev, [id]: false }));
      },
      error: () => {
        alert(this.i18n.t('patient.consent.rejectError'));
        this.revokingMap.update(prev => ({ ...prev, [id]: false }));
      }
    });
  }

  // ─── Helpers d'affichage ─────────────────────────────────────────────────

  consentTypeLabel(type: string): string {
    return CONSENT_TYPES.includes(type)
      ? this.i18n.t('patient.consent.type.' + type)
      : type;
  }

  statusLabel(status: string): string {
    return CONSENT_STATUSES.includes(status)
      ? this.i18n.t('patient.consent.status.' + status)
      : status;
  }

  statusCss(status: string): string {
    switch (status) {
      case 'REQUESTED':
        return 'bg-amber-50 dark:bg-amber-950/20 border-amber-200 dark:border-amber-900/40 text-amber-700 dark:text-amber-400';
      case 'APPROVED':
      case 'ACTIVE':
        return 'bg-emerald-50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-400';
      case 'REJECTED':
      case 'REVOKED':
        return 'bg-rose-50 dark:bg-rose-950/20 border-rose-200 dark:border-rose-900/40 text-rose-700 dark:text-rose-400';
      case 'EXPIRED':
        return 'bg-slate-50 dark:bg-slate-800/20 border-slate-200 dark:border-slate-700/40 text-slate-500 dark:text-slate-400';
      default:
        return 'bg-slate-50 border-slate-200 text-slate-500';
    }
  }

  channelLabel(channel: string): string {
    return this.i18n.t('patient.consent.channel.' + channel);
  }

  isExpired(expiresAt: string | null | undefined): boolean {
    if (!expiresAt) return false;
    return new Date(expiresAt) < new Date();
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExternalAccessResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

const ACCESS_SCOPES = [
  { key: 'medical_records',   labelFr: 'Dossier médical' },
  { key: 'prescriptions',     labelFr: 'Ordonnances' },
  { key: 'lab_results',       labelFr: 'Résultats de labo' },
  { key: 'allergies_history', labelFr: 'Allergies & ATCD' },
];

@Component({
  selector: 'app-patient-requests-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="ui-card-subtle p-5 lg:p-6 flex flex-col gap-4">
      <div class="flex flex-col gap-1 border-b border-[var(--app-border)] pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="ui-label">{{ i18n.t('patient.access.requests.label') || 'Sécurité & Accès' }}</p>
          <h3 class="font-display text-xl font-extrabold" style="color: var(--text-primary)">
            {{ i18n.t('patient.access.requests.title') }}
          </h3>
        </div>
      </div>

      <p class="text-xs text-[var(--text-secondary)] leading-relaxed">
        {{ i18n.t('patient.access.requests.subtitle') || "Gérez les demandes d'accès formulées par des cliniques ou praticiens externes au réseau Joprelys Connect." }}
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
        <div class="flex flex-col gap-6">
          
          <!-- Section 1 : Demandes en attente -->
          <div class="flex flex-col gap-3">
            <h4 class="font-display font-bold text-sm text-[var(--text-primary)]">
              {{ i18n.t('patient.access.requests.pending') || 'Demandes en attente' }}
              <span class="ml-1 text-xs px-2 py-0.5 rounded-[var(--radius-brand-sm)] bg-[var(--brand-primary)]/10 text-[var(--brand-primary)]">
                {{ pendingRequests().length }}
              </span>
            </h4>

            @for (req of pendingRequests(); track req.id) {
              <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                <div class="flex items-start gap-3 min-w-0">
                  <div class="p-2.5 rounded-[var(--radius-brand-sm)] bg-[var(--app-surface-muted)] text-[var(--brand-primary)] shrink-0">
                    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                      <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                    </svg>
                  </div>
                  <div class="min-w-0 space-y-1">
                    <h4 class="font-display font-bold text-sm text-[var(--text-primary)] truncate">
                      {{ req.requesterOrganizationName }}
                    </h4>
                    <p class="text-xs text-[var(--text-secondary)]">
                      <span class="font-semibold">{{ i18n.t('patient.access.requests.reason') }}</span> {{ req.reason }}
                    </p>
                    <p class="text-[10px] text-[var(--text-muted)]">
                      <span class="font-semibold">{{ i18n.t('patient.access.requests.duration') }}</span> 
                      {{ req.durationHours }} {{ i18n.t('patient.access.requests.hours') || 'heures' }}
                    </p>
                    <!-- TICKET-1307: Scopes granulaires pour l'approbation -->
                    <div class="pt-2 flex flex-col gap-1">
                      <p class="text-[10px] font-semibold text-[var(--text-secondary)]">Données autorisées :</p>
                      <div class="grid grid-cols-2 gap-1">
                        @for (scope of accessScopes; track scope.key) {
                          <label class="flex items-center gap-1.5 text-[10px] text-[var(--text-primary)] cursor-pointer select-none">
                            <input
                              type="checkbox"
                              [checked]="isScopeSelected(req.id, scope.key)"
                              (change)="toggleRequestScope(req.id, scope.key)"
                              class="h-3 w-3 rounded border-gray-300 text-[var(--brand-primary)] focus:ring-[var(--brand-primary)]"
                            />
                            {{ scope.labelFr }}
                          </label>
                        }
                      </div>
                    </div>
                  </div>
                </div>

                <div class="flex items-center gap-2 shrink-0 self-end sm:self-center">
                  <button
                    type="button"
                    (click)="approve(req.id)"
                    [disabled]="isActionLoading(req.id)"
                    class="px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] bg-[var(--brand-primary)] text-white hover:bg-[var(--brand-primary)]/90 transition-colors disabled:opacity-50 cursor-pointer"
                  >
                    {{ i18n.t('patient.access.requests.approve') }}
                  </button>
                  <button
                    type="button"
                    (click)="reject(req.id)"
                    [disabled]="isActionLoading(req.id)"
                    class="px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)] hover:border-rose-500 hover:text-rose-500 transition-colors disabled:opacity-50 cursor-pointer"
                  >
                    {{ i18n.t('patient.access.requests.reject') }}
                  </button>
                </div>
              </div>
            } @empty {
              <div class="flex flex-col items-center justify-center p-6 border border-dashed border-[var(--app-border)] rounded-[var(--radius-brand-md)] text-[var(--text-muted)] bg-[var(--app-surface-muted)]">
                <p class="text-xs font-semibold">{{ i18n.t('patient.access.requests.empty') }}</p>
              </div>
            }
          </div>

          <!-- Section 2 : Historique des demandes -->
          <div class="flex flex-col gap-3 border-t border-[var(--app-border)] pt-5">
            <h4 class="font-display font-bold text-sm text-[var(--text-primary)]">
              {{ i18n.t('patient.access.requests.history') || 'Historique des accès' }}
            </h4>

            @for (req of historicalRequests(); track req.id) {
              <div class="rounded-[var(--radius-brand-md)] border border-[var(--app-border)] bg-[var(--app-surface)] p-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                <div class="min-w-0 space-y-1">
                  <div class="flex items-center gap-2">
                    <h5 class="font-display font-bold text-sm text-[var(--text-primary)] truncate">
                      {{ req.requesterOrganizationName }}
                    </h5>
                    <span [class]="statusClass(req.status)">
                      {{ req.status }}
                    </span>
                  </div>
                  <p class="text-xs text-[var(--text-secondary)] truncate">
                    <span class="font-semibold">{{ i18n.t('patient.access.requests.reason') }}</span> {{ req.reason }}
                  </p>
                  @if (req.status === 'APPROUVEE' && req.expiresAt) {
                    <p class="text-[10px] text-emerald-600 dark:text-emerald-400">
                      {{ i18n.t('patient.access.requests.expires') || 'Expire le :' }} {{ req.expiresAt | date:'medium' }}
                    </p>
                  }
                </div>
              </div>
            } @empty {
              <div class="text-xs text-[var(--text-muted)] italic">
                {{ i18n.t('patient.access.requests.noHistory') || "Aucun historique d'accès." }}
              </div>
            }
          </div>

        </div>
      }
    </div>
  `
})
export class PatientRequestsListComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly requests = signal<ExternalAccessResponse[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly loadingActions = signal<Record<string, boolean>>({});
  // TICKET-1307: Scopes sélectionnés par demande (par défaut tous actifs)
  readonly scopeSelections = signal<Record<string, string[]>>({});
  readonly accessScopes = ACCESS_SCOPES;

  ngOnInit(): void {
    this.loadRequests();
  }

  loadRequests(): void {
    this.isLoading.set(true);
    this.portalService.getAccessRequests().subscribe({
      next: (data) => {
        this.requests.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('patient.access.requests.loadError') || "Impossible de charger les demandes d'accès.");
        this.isLoading.set(false);
      }
    });
  }

  pendingRequests() {
    return this.requests().filter(r => r.status === 'EN_ATTENTE');
  }

  historicalRequests() {
    return this.requests().filter(r => r.status !== 'EN_ATTENTE');
  }

  isActionLoading(id: string): boolean {
    return !!this.loadingActions()[id];
  }

  approve(id: string): void {
    this.loadingActions.update(prev => ({ ...prev, [id]: true }));
    const selectedScopes = this.scopeSelections()[id]
      ?? ACCESS_SCOPES.map(s => s.key);
    const scopesStr = selectedScopes.join(',');
    this.portalService.approveAccessRequestWithScopes(id, scopesStr).subscribe({
      next: (updated) => {
        this.requests.update(list => list.map(r => r.id === id ? updated : r));
        this.loadingActions.update(prev => ({ ...prev, [id]: false }));
      },
      error: () => {
        alert(this.i18n.t('patient.access.requests.updateError') || 'Erreur lors de la validation.');
        this.loadingActions.update(prev => ({ ...prev, [id]: false }));
      }
    });
  }

  isScopeSelected(reqId: string, scopeKey: string): boolean {
    const selected = this.scopeSelections()[reqId] ?? ACCESS_SCOPES.map(s => s.key);
    return selected.includes(scopeKey);
  }

  toggleRequestScope(reqId: string, scopeKey: string): void {
    const current = this.scopeSelections()[reqId] ?? ACCESS_SCOPES.map(s => s.key);
    const updated = current.includes(scopeKey)
      ? current.filter(s => s !== scopeKey)
      : [...current, scopeKey];
    this.scopeSelections.update(prev => ({ ...prev, [reqId]: updated }));
  }

  reject(id: string): void {
    this.loadingActions.update(prev => ({ ...prev, [id]: true }));
    this.portalService.rejectAccessRequest(id).subscribe({
      next: (updated) => {
        this.requests.update(list => list.map(r => r.id === id ? updated : r));
        this.loadingActions.update(prev => ({ ...prev, [id]: false }));
      },
      error: () => {
        alert(this.i18n.t('patient.access.requests.updateError') || 'Erreur lors du rejet.');
        this.loadingActions.update(prev => ({ ...prev, [id]: false }));
      }
    });
  }

  statusClass(status: string): string {
    const base = 'inline-flex items-center px-1.5 py-0.5 text-[10px] font-bold rounded-[var(--radius-brand-xs)] border';
    if (status === 'APPROUVEE') {
      return `${base} bg-emerald-50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/40 text-emerald-700 dark:text-emerald-400`;
    }
    if (status === 'REFUSEE') {
      return `${base} bg-rose-50 dark:bg-rose-950/20 border-rose-200 dark:border-rose-900/40 text-rose-700 dark:text-rose-400`;
    }
    return `${base} bg-[var(--app-surface-muted)] border-[var(--app-border)] text-[var(--text-muted)]`;
  }
}

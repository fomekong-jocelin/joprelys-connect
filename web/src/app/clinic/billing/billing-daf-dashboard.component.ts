import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { BillingApiService } from '../../patient/billing-api.service';
import { CashSession } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';
import { RbacApiService } from '../rbac/rbac-api.service';

@Component({
  selector: 'app-billing-daf-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    <div class="space-y-6">
      <!-- Top Actions & Export Box -->
      @if (canExport()) {
        <div class="ui-card-subtle p-4 space-y-4">
        <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2 flex items-center gap-1.5">
          <app-ui-icon name="document-text" />
          Exportation Comptable OHADA (Format Sage 100)
        </h3>
        
        <div class="flex flex-wrap gap-4 items-end text-xs">
          <div>
            <label class="font-bold text-[var(--text-secondary)] block mb-1">Date de début :</label>
            <input type="date" [(ngModel)]="startDate" class="ui-input w-40" />
          </div>
          <div>
            <label class="font-bold text-[var(--text-secondary)] block mb-1">Date de fin :</label>
            <input type="date" [(ngModel)]="endDate" class="ui-input w-40" />
          </div>
          
          <button (click)="exportSage100()" [disabled]="exporting()" class="ui-button ui-button-primary flex items-center gap-1.5">
            @if (exporting()) {
              <app-ui-icon name="arrow-path" class="animate-spin" />
              Génération en cours...
            } @else {
              <app-ui-icon name="document-text" />
              Exporter pour Sage 100
            }
          </button>
        </div>
        </div>
      }

      <!-- Cash Sessions Audit List -->
      @if (canReadCashHistory()) {
        <div class="ui-card-subtle p-4 space-y-3">
        <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
          Contrôle et supervision des Sessions de Caisses
        </h3>

        @if (loadingSessions()) {
          <p class="text-center text-[10px] text-[var(--text-muted)] py-6 italic">Chargement des sessions...</p>
        } @else if (sessions().length === 0) {
          <p class="text-center text-[10px] text-[var(--text-muted)] py-6 italic">Aucune session de caisse répertoriée.</p>
        } @else {
          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs border-collapse">
              <thead>
                <tr class="bg-[var(--app-surface-muted)] border-b border-[var(--app-border)] text-[10px] font-bold text-[var(--text-secondary)]">
                  <th class="p-2" scope="col">Ouverture</th>
                  <th class="p-2" scope="col">Caissier / Caisse</th>
                  <th class="p-2 text-right" scope="col">Attendu (Espèces)</th>
                  <th class="p-2 text-right" scope="col">Déclaré</th>
                  <th class="p-2 text-right" scope="col">Écart</th>
                  <th class="p-2" scope="col">Motif Écart</th>
                  <th class="p-2" scope="col">Statut</th>
                  <th class="p-2" scope="col">Action</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--app-border)]/40">
                @for (s of sessions(); track s.id) {
                  <tr class="hover:bg-[var(--app-surface-muted)]/30">
                    <td class="p-2 text-[10px]">
                      <div>{{ s.openedAt | date:'dd/MM/yyyy HH:mm' }}</div>
                      @if (s.closedAt) {
                        <div class="text-[9px] text-[var(--text-muted)]">Fermé: {{ s.closedAt | date:'dd/MM/yyyy HH:mm' }}</div>
                      } @else {
                        <span class="px-1.5 py-0.5 text-[8px] font-bold rounded-sm bg-emerald-500/10 text-emerald-600">Active</span>
                      }
                    </td>
                    <td class="p-2">
                      <div class="font-semibold">{{ s.cashRegisterName }}</div>
                      <div class="text-[9px] text-[var(--text-muted)] font-mono">{{ s.openedByUserId.substring(0, 8) }}...</div>
                    </td>
                    <td class="p-2 text-right">{{ s.closingBalance ? (s.closingBalance | number:'1.0-0') : '0' }} FCFA</td>
                    <td class="p-2 text-right font-semibold">{{ s.declaredBalance ? (s.declaredBalance | number:'1.0-0') : '0' }} FCFA</td>
                    <td class="p-2 text-right font-bold"
                        [class.text-rose-600]="s.discrepancyAmount && s.discrepancyAmount < 0"
                        [class.text-emerald-600]="s.discrepancyAmount && s.discrepancyAmount > 0"
                        [class.text-[var(--text-muted)]]="!s.discrepancyAmount || s.discrepancyAmount === 0">
                      {{ s.discrepancyAmount ? (s.discrepancyAmount > 0 ? '+' : '') + (s.discrepancyAmount | number:'1.0-0') : '0' }} FCFA
                    </td>
                    <td class="p-2 max-w-xs truncate text-[10px] text-[var(--text-secondary)] italic">
                      {{ s.discrepancyReason || '-' }}
                    </td>
                    <td class="p-2">
                      @if (s.discrepancyAmount && s.discrepancyAmount !== 0) {
                        @if (s.discrepancyResolved) {
                          <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm bg-emerald-500/10 text-emerald-600 flex items-center gap-1 w-max">
                            <app-ui-icon name="check" class="text-xs" /> Résolu
                          </span>
                        } @else {
                          <span class="px-1.5 py-0.5 text-[9px] font-bold rounded-sm bg-rose-500/10 text-rose-600 flex items-center gap-1 w-max">
                            <app-ui-icon name="information-circle" class="text-xs" /> Non résolu
                          </span>
                        }
                      } @else {
                        <span class="text-[10px] text-[var(--text-muted)]">-</span>
                      }
                    </td>
                    <td class="p-2">
                      @if (canResolveDiscrepancies() && s.discrepancyAmount && s.discrepancyAmount !== 0 && !s.discrepancyResolved) {
                        <button (click)="openResolveModal(s)" class="text-xs text-[var(--brand-cyan)] hover:underline flex items-center gap-1 font-bold">
                          <app-ui-icon name="check" />
                          Traiter
                        </button>
                      } @else if (s.discrepancyResolved) {
                        <button (click)="viewResolutionNotes(s)" class="text-xs text-[var(--text-muted)] hover:underline flex items-center gap-1">
                          <app-ui-icon name="information-circle" />
                          Notes
                        </button>
                      } @else {
                        <span class="text-[10px] text-[var(--text-muted)]">-</span>
                      }
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
        </div>
      }

      <!-- Resolution Modal -->
      @if (canResolveDiscrepancies() && resolveModalVisible()) {
        <div class="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div class="ui-card max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div class="flex justify-between items-center border-b border-[var(--app-border)]/40 pb-2">
              <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider">
                Résoudre un écart de caisse
              </h3>
              <button (click)="resolveModalVisible.set(false)" [attr.aria-label]="translate('common.aria.close', 'Fermer')" class="text-xs text-[var(--text-muted)] font-bold p-1 rounded-sm hover:bg-[var(--app-surface-muted)]">
                <app-ui-icon name="x-mark" class="text-base" />
              </button>
            </div>

            <div class="text-xs space-y-1 bg-[var(--app-surface-muted)] p-3 rounded-sm">
              <div>Session : <strong class="font-mono">{{ selectedSession()?.id }}</strong></div>
              <div>Caissier : <strong>{{ selectedSession()?.cashRegisterName }}</strong></div>
              <div>Écart constaté : <strong class="text-rose-600">{{ selectedSession()?.discrepancyAmount | number:'1.0-0' }} FCFA</strong></div>
              <div>Justification caissier : <span class="italic">"{{ selectedSession()?.discrepancyReason }}"</span></div>
            </div>

            <div>
              <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">Notes de résolution / Décision DAF :</label>
              <textarea [(ngModel)]="resolutionNotes" rows="4" placeholder="Saisir la justification du traitement de l'écart..." class="ui-input text-xs w-full resize-none"></textarea>
            </div>

            <div class="flex justify-end gap-2 pt-2 border-t border-[var(--app-border)]/40">
              <button (click)="resolveModalVisible.set(false)" class="ui-button ui-button-secondary text-xs">
                Annuler
              </button>
              <button (click)="submitResolution()" [disabled]="savingResolution() || !resolutionNotes.trim()" class="ui-button ui-button-primary text-xs disabled:opacity-50">
                @if (savingResolution()) {
                  Validation...
                } @else {
                  Valider le traitement
                }
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Resolution Notes View Modal -->
      @if (viewNotesVisible()) {
        <div class="fixed inset-0 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div class="ui-card max-w-md w-full p-6 space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div class="flex justify-between items-center border-b border-[var(--app-border)]/40 pb-2">
              <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider">
                Notes de résolution de l'écart
              </h3>
              <button (click)="viewNotesVisible.set(false)" [attr.aria-label]="translate('common.aria.close', 'Fermer')" class="text-xs text-[var(--text-muted)] font-bold p-1 rounded-sm hover:bg-[var(--app-surface-muted)]">
                <app-ui-icon name="x-mark" class="text-base" />
              </button>
            </div>

            <div class="text-xs space-y-2 bg-[var(--app-surface-muted)] p-3 rounded-sm">
              <div>Résolu le : <strong>{{ selectedSession()?.resolvedAt | date:'dd/MM/yyyy HH:mm' }}</strong></div>
              <div>Par l'utilisateur ID : <strong class="font-mono">{{ selectedSession()?.resolvedByUserId }}</strong></div>
              <div class="border-t border-[var(--app-border)]/40 pt-2">
                <strong>Décision / Notes :</strong>
                <p class="mt-1 italic text-[var(--text-secondary)]">"{{ selectedSession()?.resolutionNotes }}"</p>
              </div>
            </div>

            <div class="flex justify-end pt-2">
              <button (click)="viewNotesVisible.set(false)" class="ui-button ui-button-secondary text-xs">
                Fermer
              </button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
})
export class BillingDafDashboardComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly rbacApi = inject(RbacApiService);
  readonly canExport = computed(() => this.rbacApi.hasPermission('ACCOUNTING_EXPORT'));
  readonly canReadCashHistory = computed(() => this.rbacApi.hasPermission('CASH_HISTORY_READ'));
  readonly canResolveDiscrepancies = computed(() => this.rbacApi.hasPermission('CASH_DISCREPANCY_RESOLVE'));

  @Input({ required: true }) translate!: (key: string, defaultValue: string) => string;

  sessions = signal<CashSession[]>([]);
  loadingSessions = signal<boolean>(false);

  // Export State
  startDate = '';
  endDate = '';
  exporting = signal<boolean>(false);

  // Resolution Modal State
  resolveModalVisible = signal<boolean>(false);
  viewNotesVisible = signal<boolean>(false);
  selectedSession = signal<CashSession | null>(null);
  resolutionNotes = '';
  savingResolution = signal<boolean>(false);

  ngOnInit(): void {
    if (this.canReadCashHistory()) this.loadSessions();
    // Par défaut, plages de dates d'export sur les 30 derniers jours
    const today = new Date();
    const past = new Date();
    past.setDate(today.getDate() - 30);
    
    this.endDate = today.toISOString().split('T')[0];
    this.startDate = past.toISOString().split('T')[0];
  }

  loadSessions(): void {
    if (!this.canReadCashHistory()) return;
    this.loadingSessions.set(true);
    this.billingApi.listAllSessions().subscribe({
      next: (data) => {
        this.sessions.set(data);
        this.loadingSessions.set(false);
      },
      error: () => {
        this.loadingSessions.set(false);
      }
    });
  }

  exportSage100(): void {
    if (!this.canExport()) return;
    this.exporting.set(true);
    this.billingApi.exportAccounting(this.startDate, this.endDate).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `export_sage_100_${this.startDate}_to_${this.endDate}.csv`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => {
        this.exporting.set(false);
      }
    });
  }

  openResolveModal(session: CashSession): void {
    if (!this.canResolveDiscrepancies()) return;
    this.selectedSession.set(session);
    this.resolutionNotes = '';
    this.resolveModalVisible.set(true);
  }

  viewResolutionNotes(session: CashSession): void {
    this.selectedSession.set(session);
    this.viewNotesVisible.set(true);
  }

  submitResolution(): void {
    const session = this.selectedSession();
    if (!this.canResolveDiscrepancies() || !session || !this.resolutionNotes.trim()) {
      return;
    }

    this.savingResolution.set(true);
    this.billingApi.resolveDiscrepancy(session.id, this.resolutionNotes.trim()).subscribe({
      next: () => {
        this.savingResolution.set(false);
        this.resolveModalVisible.set(false);
        this.loadSessions();
      },
      error: () => {
        this.savingResolution.set(false);
      }
    });
  }
}

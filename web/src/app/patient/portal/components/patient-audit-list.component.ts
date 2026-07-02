import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientAuditLog, PatientPortalService } from '../services/patient-portal.service';

@Component({
  selector: 'app-patient-audit-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-card p-6 flex flex-col gap-6">
      <div>
        <h3 class="font-display font-extrabold text-lg text-[var(--text-primary)]">
          Journal de traçabilité (Sécurité & Audit)
        </h3>
        <p class="text-xs text-[var(--text-secondary)] mt-1">
          Consultez l'historique complet et tracé des accès à vos données médicales (DPU) par les professionnels et établissements de santé.
        </p>
      </div>

      @if (isLoading()) {
        <div class="flex items-center justify-center py-12">
          <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
        </div>
      } @else if (error()) {
        <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
          {{ error() }}
        </div>
      } @else {
        <div class="overflow-x-auto">
          <table class="ui-table w-full text-left border-collapse">
            <thead>
              <tr class="border-b border-slate-200 dark:border-slate-800">
                <th class="pb-3 text-xs font-black uppercase text-[var(--text-secondary)]">Date & Heure</th>
                <th class="pb-3 text-xs font-black uppercase text-[var(--text-secondary)]">Établissement</th>
                <th class="pb-3 text-xs font-black uppercase text-[var(--text-secondary)]">Action</th>
                <th class="pb-3 text-xs font-black uppercase text-[var(--text-secondary)]">Justification / Motif</th>
                <th class="pb-3 text-xs font-black uppercase text-[var(--text-secondary)] text-right">Statut</th>
              </tr>
            </thead>
            <tbody>
              @for (log of auditLogs(); track log.id) {
                <tr class="border-b border-slate-100 dark:border-slate-800/60 hover:bg-slate-50/50 dark:hover:bg-slate-900/30 transition-colors">
                  <td class="py-3 text-xs text-[var(--text-secondary)] font-mono">
                    {{ log.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}
                  </td>
                  <td class="py-3 text-xs font-bold text-[var(--text-primary)]">
                    {{ log.organizationName }}
                  </td>
                  <td class="py-3 text-xs">
                    <span [class]="getActionBadgeClass(log.action)" class="px-2 py-0.5 rounded-[var(--radius-brand-xs)] font-bold text-[10px] uppercase">
                      {{ formatActionName(log.action) }}
                    </span>
                  </td>
                  <td class="py-3 text-xs text-[var(--text-secondary)] italic max-w-xs truncate" [title]="log.reason || ''">
                    {{ log.reason || 'Aucune justification requise' }}
                  </td>
                  <td class="py-3 text-xs text-right">
                    <span [class]="log.status === 'SUCCESS' ? 'bg-emerald-50 dark:bg-emerald-950/20 text-emerald-700 dark:text-emerald-300' : 'bg-rose-50 dark:bg-rose-950/20 text-rose-700 dark:text-rose-300'" class="px-2 py-0.5 rounded-[var(--radius-brand-xs)] font-bold text-[10px] uppercase">
                      {{ log.status }}
                    </span>
                  </td>
                </tr>
              } @empty {
                <tr>
                  <td colspan="5" class="py-8 text-center text-xs text-[var(--text-secondary)]">
                    Aucun événement de sécurité répertorié pour le moment.
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `
})
export class PatientAuditListComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);

  readonly auditLogs = signal<PatientAuditLog[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadAuditLogs();
  }

  loadAuditLogs(): void {
    this.isLoading.set(true);
    this.portalService.getAuditLogs().subscribe({
      next: (data) => {
        this.auditLogs.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger le journal d'audit.");
        this.isLoading.set(false);
      }
    });
  }

  formatActionName(action: string): string {
    switch (action) {
      case 'VIEW_PORTAL_DASHBOARD':
        return 'Accès Portail';
      case 'DOWNLOAD_DOCUMENT':
        return 'Téléchargement Ordonnance';
      case 'EMERGENCY_ACCESS':
        return 'Urgence (Brise-Glace)';
      case 'CONSULTATION':
        return 'Consultation Médicale';
      case 'CREATE_PATIENT':
        return 'Création Fiche';
      case 'UPDATE_PATIENT':
        return 'Mise à jour Fiche';
      case 'REVOKE_DOCUMENT':
        return 'Révocation Ordonnance';
      default:
        return action;
    }
  }

  getActionBadgeClass(action: string): string {
    switch (action) {
      case 'EMERGENCY_ACCESS':
        return 'bg-rose-100 dark:bg-rose-950/30 text-rose-800 dark:text-rose-300';
      case 'DOWNLOAD_DOCUMENT':
      case 'REVOKE_DOCUMENT':
        return 'bg-blue-100 dark:bg-blue-950/30 text-blue-800 dark:text-blue-300';
      case 'VIEW_PORTAL_DASHBOARD':
        return 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300';
      default:
        return 'bg-indigo-100 dark:bg-indigo-950/30 text-indigo-800 dark:text-indigo-300';
    }
  }
}

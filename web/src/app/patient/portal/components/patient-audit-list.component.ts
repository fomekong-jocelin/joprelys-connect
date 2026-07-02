import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientAuditLog, PatientPortalService } from '../services/patient-portal.service';

@Component({
  selector: 'app-patient-audit-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-card p-5 flex flex-col gap-4">
      <!-- En-tête compact -->
      <div class="flex items-start justify-between gap-3">
        <div>
          <h3 class="font-display font-extrabold text-base text-[var(--text-primary)] flex items-center gap-2">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-4 h-4 text-[var(--brand-primary)] shrink-0">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
            </svg>
            Journal d'accès
          </h3>
          <p class="text-[11px] text-[var(--text-muted)] mt-0.5">Historique des accès à votre dossier médical.</p>
        </div>
        @if (auditLogs().length > 0) {
          <span class="shrink-0 text-[10px] font-bold px-2 py-0.5 rounded-[var(--radius-brand-xs)] bg-[var(--app-surface-muted)] text-[var(--text-muted)] uppercase tracking-wide">
            {{ auditLogs().length }} entrée{{ auditLogs().length > 1 ? 's' : '' }}
          </span>
        }
      </div>

      @if (isLoading()) {
        <div class="flex items-center justify-center py-8">
          <div class="w-7 h-7 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
        </div>
      } @else if (error()) {
        <div class="p-3 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-xs font-semibold">
          {{ error() }}
        </div>
      } @else if (auditLogs().length === 0) {
        <div class="flex flex-col items-center justify-center py-8 text-[var(--text-muted)]">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="w-10 h-10 mb-2 opacity-40">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25z" />
          </svg>
          <p class="text-xs font-semibold">Aucun événement enregistré</p>
        </div>
      } @else {
        <!-- Vue mobile : cards empilées -->
        <div class="flex flex-col gap-2 md:hidden">
          @for (log of visibleLogs(); track log.id) {
            <div class="p-3 rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface)] flex flex-col gap-2">
              <div class="flex items-center justify-between gap-2 flex-wrap">
                <span [class]="getActionBadgeClass(log.action)" class="px-2 py-0.5 rounded-[var(--radius-brand-xs)] font-bold text-[10px] uppercase leading-tight">
                  {{ formatActionName(log.action) }}
                </span>
                <span [class]="log.status === 'SUCCESS' ? 'bg-emerald-50 dark:bg-emerald-950/20 text-emerald-700 dark:text-emerald-300' : 'bg-rose-50 dark:bg-rose-950/20 text-rose-700 dark:text-rose-300'"
                      class="px-2 py-0.5 rounded-[var(--radius-brand-xs)] font-bold text-[10px] uppercase leading-tight">
                  {{ log.status }}
                </span>
              </div>
              <div class="flex items-center justify-between gap-2">
                <span class="text-xs font-bold text-[var(--text-primary)]">{{ log.organizationName }}</span>
                <span class="text-[10px] font-mono text-[var(--text-muted)]">{{ log.createdAt | date: 'dd/MM/yy HH:mm' }}</span>
              </div>
              @if (log.reason) {
                <p class="text-[11px] text-[var(--text-secondary)] italic truncate" [title]="log.reason">{{ log.reason }}</p>
              }
            </div>
          }
        </div>

        <!-- Vue desktop : tableau compact -->
        <div class="hidden md:block overflow-x-auto">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="border-b border-[var(--app-border)]">
                <th class="pb-2.5 pr-4 text-[10px] font-black uppercase tracking-wide text-[var(--text-muted)] whitespace-nowrap">Date & Heure</th>
                <th class="pb-2.5 pr-4 text-[10px] font-black uppercase tracking-wide text-[var(--text-muted)]">Établissement</th>
                <th class="pb-2.5 pr-4 text-[10px] font-black uppercase tracking-wide text-[var(--text-muted)]">Action</th>
                <th class="pb-2.5 pr-4 text-[10px] font-black uppercase tracking-wide text-[var(--text-muted)]">Motif</th>
                <th class="pb-2.5 text-[10px] font-black uppercase tracking-wide text-[var(--text-muted)] text-right">Statut</th>
              </tr>
            </thead>
            <tbody>
              @for (log of visibleLogs(); track log.id) {
                <tr class="border-b border-[var(--app-border)]/60 hover:bg-[var(--app-surface-muted)]/50 transition-colors">
                  <td class="py-2.5 pr-4 text-[11px] text-[var(--text-muted)] font-mono whitespace-nowrap">
                    {{ log.createdAt | date: 'dd/MM/yy HH:mm' }}
                  </td>
                  <td class="py-2.5 pr-4 text-[11px] font-semibold text-[var(--text-primary)] whitespace-nowrap">
                    {{ log.organizationName }}
                  </td>
                  <td class="py-2.5 pr-4">
                    <span [class]="getActionBadgeClass(log.action)" class="px-2 py-0.5 rounded-[var(--radius-brand-xs)] font-bold text-[10px] uppercase whitespace-nowrap">
                      {{ formatActionName(log.action) }}
                    </span>
                  </td>
                  <td class="py-2.5 pr-4 text-[11px] text-[var(--text-secondary)] italic max-w-[180px] truncate" [title]="log.reason || ''">
                    {{ log.reason || '—' }}
                  </td>
                  <td class="py-2.5 text-right">
                    <span [class]="log.status === 'SUCCESS' ? 'bg-emerald-50 dark:bg-emerald-950/20 text-emerald-700 dark:text-emerald-300' : 'bg-rose-50 dark:bg-rose-950/20 text-rose-700 dark:text-rose-300'"
                          class="px-2 py-0.5 rounded-[var(--radius-brand-xs)] font-bold text-[10px] uppercase">
                      {{ log.status }}
                    </span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <!-- Bouton Voir plus / Voir moins -->
        @if (auditLogs().length > pageSize) {
          <button
            (click)="toggleShowAll()"
            class="w-full mt-1 py-2 text-[11px] font-bold text-[var(--brand-primary)] border border-[var(--app-border)] rounded-[var(--radius-brand-sm)] hover:bg-[var(--app-surface-muted)] transition-colors cursor-pointer"
          >
            @if (showAll()) {
              ▲ Réduire
            } @else {
              ▼ Voir les {{ auditLogs().length - pageSize }} autres entrées
            }
          </button>
        }
      }
    </div>
  `
})
export class PatientAuditListComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);

  readonly auditLogs = signal<PatientAuditLog[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);
  readonly showAll = signal(false);
  readonly pageSize = 5;

  visibleLogs(): PatientAuditLog[] {
    return this.showAll() ? this.auditLogs() : this.auditLogs().slice(0, this.pageSize);
  }

  toggleShowAll(): void {
    this.showAll.set(!this.showAll());
  }

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
      case 'VIEW_PORTAL_DASHBOARD': return 'Accès portail';
      case 'DOWNLOAD_DOCUMENT': return 'Téléchargement';
      case 'EMERGENCY_ACCESS': return 'Urgence';
      case 'CONSULTATION': return 'Consultation';
      case 'CREATE_PATIENT': return 'Création fiche';
      case 'UPDATE_PATIENT': return 'Mise à jour';
      case 'REVOKE_DOCUMENT': return 'Révocation';
      case 'READ_AUDIT': return 'Lecture audit';
      case 'GENERATE_DOCUMENT': return 'Génération doc';
      default: return action;
    }
  }

  getActionBadgeClass(action: string): string {
    switch (action) {
      case 'EMERGENCY_ACCESS':
        return 'bg-rose-100 dark:bg-rose-950/30 text-rose-800 dark:text-rose-300';
      case 'DOWNLOAD_DOCUMENT':
      case 'REVOKE_DOCUMENT':
      case 'GENERATE_DOCUMENT':
        return 'bg-blue-100 dark:bg-blue-950/30 text-blue-800 dark:text-blue-300';
      case 'VIEW_PORTAL_DASHBOARD':
      case 'READ_AUDIT':
        return 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300';
      default:
        return 'bg-indigo-100 dark:bg-indigo-950/30 text-indigo-800 dark:text-indigo-300';
    }
  }
}

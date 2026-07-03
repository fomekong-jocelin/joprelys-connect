import { Component, inject, OnInit, signal, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientNotification, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-card-subtle p-5 lg:p-6 flex flex-col gap-4">
      <div class="flex flex-col gap-2 border-b border-[var(--app-border)] pb-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p class="ui-label">{{ i18n.t('patient.notifications.label') || 'Communication & Sécurité' }}</p>
          <h3 class="font-display text-xl font-extrabold" style="color: var(--text-primary)">
            {{ i18n.t('patient.notifications.title') || 'Centre de Notifications' }}
          </h3>
        </div>
        @if (unreadCount() > 0) {
          <button
            type="button"
            (click)="markAllAsRead()"
            class="px-3 py-1.5 text-xs font-bold rounded-[var(--radius-brand-sm)] border border-[var(--brand-primary)] text-[var(--brand-primary)] hover:bg-[var(--brand-primary)]/5 transition-colors cursor-pointer"
          >
            {{ i18n.t('patient.notifications.markAllRead') || 'Tout marquer comme lu' }}
          </button>
        }
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
        <div class="flex flex-col gap-3">
          @for (notif of notifications(); track notif.id) {
            <div 
              [class]="getNotificationClass(notif)"
              class="rounded-[var(--radius-brand-md)] border p-4 flex gap-3 transition-colors"
            >
              <div [class]="getIconClass(notif.type)" class="p-2.5 rounded-[var(--radius-brand-sm)] shrink-0 self-start">
                @if (notif.type === 'SECURITY' || notif.type === 'EMERGENCY') {
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75m-3-7.036A11.959 11.959 0 013.598 6 11.99 11.99 0 003 9.749c0 5.592 3.824 10.29 9 11.623 5.176-1.332 9-6.03 9-11.622 0-1.31-.21-2.571-.598-3.751h-.152c-3.196 0-6.1-1.248-8.25-3.285z" />
                  </svg>
                } @else {
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0M3.124 7.5A8.969 8.969 0 015.292 3m13.416 0a8.969 8.969 0 012.168 4.5" />
                  </svg>
                }
              </div>

              <div class="min-w-0 flex-1 space-y-1">
                <div class="flex items-center gap-2">
                  <h4 class="font-display font-bold text-sm text-[var(--text-primary)] truncate">
                    {{ notif.title }}
                  </h4>
                  @if (notif.status === 'NON_LU') {
                    <span class="w-2 h-2 rounded-full bg-[var(--brand-primary)] shrink-0" title="Nouvelle notification"></span>
                  }
                </div>
                <p class="text-xs text-[var(--text-secondary)] leading-relaxed">
                  {{ notif.message }}
                </p>
                <p class="text-[10px] text-[var(--text-muted)]">
                  {{ notif.createdAt | date:'medium' }}
                </p>
              </div>

              @if (notif.status === 'NON_LU') {
                <button
                  type="button"
                  (click)="markAsRead(notif.id)"
                  class="self-center px-2 py-1 text-[10px] font-bold rounded-[var(--radius-brand-sm)] border border-[var(--app-border)] bg-[var(--app-surface-muted)] text-[var(--text-secondary)] hover:border-[var(--brand-primary)] hover:text-[var(--brand-primary)] transition-colors cursor-pointer shrink-0"
                >
                  {{ i18n.t('patient.notifications.markRead') || 'Marquer lu' }}
                </button>
              }
            </div>
          } @empty {
            <div class="flex flex-col items-center justify-center p-8 border border-dashed border-[var(--app-border)] rounded-[var(--radius-brand-md)] text-[var(--text-muted)] bg-[var(--app-surface-muted)]">
              <p class="text-sm font-semibold">{{ i18n.t('patient.notifications.empty') || 'Aucune notification reçue.' }}</p>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class PatientNotificationsComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  @Output() countUpdated = new EventEmitter<number>();

  readonly notifications = signal<PatientNotification[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  unreadCount() {
    return this.notifications().filter(n => n.status === 'NON_LU').length;
  }

  ngOnInit(): void {
    this.loadNotifications();
  }

  loadNotifications(): void {
    this.isLoading.set(true);
    this.portalService.getNotifications().subscribe({
      next: (data) => {
        this.notifications.set(data);
        this.isLoading.set(false);
        this.countUpdated.emit(this.unreadCount());
      },
      error: () => {
        this.error.set(this.i18n.t('patient.notifications.loadError') || 'Impossible de charger les notifications.');
        this.isLoading.set(false);
      }
    });
  }

  markAsRead(id: string): void {
    this.portalService.markNotificationAsRead(id).subscribe({
      next: (updated) => {
        this.notifications.update(list => list.map(n => n.id === id ? updated : n));
        this.countUpdated.emit(this.unreadCount());
      },
      error: () => {
        alert(this.i18n.t('patient.notifications.updateError') || 'Erreur de mise à jour.');
      }
    });
  }

  markAllAsRead(): void {
    this.portalService.markAllNotificationsAsRead().subscribe({
      next: () => {
        this.notifications.update(list => list.map(n => ({ ...n, status: 'LU' })));
        this.countUpdated.emit(0);
      },
      error: () => {
        alert(this.i18n.t('patient.notifications.updateError') || 'Erreur de mise à jour.');
      }
    });
  }

  getNotificationClass(notif: PatientNotification): string {
    const isUnread = notif.status === 'NON_LU';
    const baseBorder = isUnread 
      ? 'border-[var(--brand-primary)] bg-[var(--app-surface)]'
      : 'border-[var(--app-border)] bg-[var(--app-surface-muted)] opacity-85';
    
    return baseBorder;
  }

  getIconClass(type: string): string {
    if (type === 'SECURITY' || type === 'EMERGENCY') {
      return 'bg-rose-100 dark:bg-rose-950/30 text-rose-800 dark:text-rose-400';
    }
    return 'bg-[var(--app-surface-muted)] text-[var(--brand-primary)]';
  }
}

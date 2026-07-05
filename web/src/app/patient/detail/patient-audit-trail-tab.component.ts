import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientDetailComponent } from '../patient-detail.component';
import { AuditApiService } from '../../audit/audit-api.service';
import { AuthTokenStorageService } from '../../auth/auth-token-storage.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AuditLog } from '../../audit/audit.models';

@Component({
  selector: 'app-patient-audit-trail-tab',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-4 animate-fade-in text-xs text-slate-700 dark:text-slate-300">
      @if (canViewAudit()) {
        <h3 class="text-xs font-black uppercase tracking-wider text-slate-400 dark:text-slate-500 mb-4">
          {{ i18n.t('patients.auditLogsTitle') }}
        </h3>

        @if (isLoadingAudit()) {
          <div class="py-12 text-center">
            <div class="inline-block w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
            <p class="mt-2 text-xs font-bold text-slate-400 dark:text-slate-500">{{ i18n.t('patients.auditLogsLoading') }}</p>
          </div>
        } @else if (auditLogs().length === 0) {
          <div class="p-8 text-center border border-dashed border-slate-200 dark:border-slate-800/80 rounded-xl">
            <p class="text-sm font-semibold text-slate-400 dark:text-slate-500">{{ i18n.t('patients.auditLogsEmpty') }}</p>
          </div>
        } @else {
          <div class="flow-root px-4">
            <ul role="list" class="-mb-8">
              @for (log of auditLogs(); track log.id; let last = $last) {
                <li>
                  <div class="relative pb-8">
                    @if (!last) {
                      <span class="absolute top-4 left-4 -ml-px h-full w-0.5 bg-slate-100 dark:bg-slate-800" aria-hidden="true"></span>
                    }
                    <div class="relative flex space-x-3">
                      <div>
                        <span 
                          [class]="log.status === 'SUCCESS' 
                            ? 'h-8 w-8 rounded-full bg-green-50 dark:bg-green-950/20 text-green-600 dark:text-green-400 flex items-center justify-center ring-8 ring-white dark:ring-slate-900'
                            : 'h-8 w-8 rounded-full bg-rose-50 dark:bg-rose-950/20 text-rose-600 dark:text-rose-400 flex items-center justify-center ring-8 ring-white dark:ring-slate-900'"
                        >
                          @if (log.status === 'SUCCESS') {
                            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                            </svg>
                          } @else {
                            <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                              <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                            </svg>
                          }
                        </span>
                      </div>
                      <div class="flex-1 min-w-0 pt-1.5 flex justify-between space-x-4">
                        <div>
                          <p class="text-sm font-semibold text-slate-800 dark:text-slate-200">
                            {{ log.reason || log.action }}
                          </p>
                          <p class="text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                            {{ i18n.t('patients.auditLogsUser') }} <span class="font-bold text-indigo-600 dark:text-indigo-400">{{ log.actorName || 'Système' }}</span>
                            | {{ i18n.t('patients.auditLogsAction') }} <span class="font-mono text-[10px] font-bold">{{ log.action }}</span> 
                            @if (log.ipAddress) {
                              | {{ i18n.t('patients.auditLogsIp') }} <span class="font-mono text-[10px]">{{ log.ipAddress }}</span>
                            }
                          </p>
                        </div>
                        <div class="text-right text-xs whitespace-nowrap text-slate-400 dark:text-slate-500">
                          <time [dateTime]="log.createdAt">{{ log.createdAt | date:'short' }}</time>
                        </div>
                      </div>
                    </div>
                  </div>
                </li>
              }
            </ul>
          </div>
        }
      }
    </div>
  `
})
export class PatientAuditTrailTabComponent implements OnInit {
  readonly parent = inject(PatientDetailComponent);
  private readonly auditApi = inject(AuditApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  readonly i18n = inject(I18nService);

  readonly session = this.tokenStorage.session;

  readonly auditLogs = signal<AuditLog[]>([]);
  readonly isLoadingAudit = signal(false);

  ngOnInit(): void {
    this.loadAudit();
  }

  loadAudit(): void {
    if (!this.canViewAudit()) {
      return;
    }
    const patient = this.parent.patient();
    if (!patient) return;

    this.isLoadingAudit.set(true);
    this.auditApi.getPatientLogs(patient.id).subscribe({
      next: (data) => {
        this.auditLogs.set(data);
        this.isLoadingAudit.set(false);
      },
      error: () => {
        this.isLoadingAudit.set(false);
      }
    });
  }

  canViewAudit(): boolean {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'ADMIN_CLINIQUE' || role === 'AUDITEUR';
  }
}

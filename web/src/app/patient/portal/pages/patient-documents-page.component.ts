import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../../shared/ui/page-header.component';
import { CardComponent } from '../../../shared/ui/card.component';
import { EmptyStateComponent } from '../../../shared/ui/empty-state.component';
import { PatientPortalConsultation, PatientPortalMeResponse, PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

@Component({
  selector: 'app-patient-documents-page',
  standalone: true,
  imports: [CommonModule, AppShellComponent, PageHeaderComponent, CardComponent, EmptyStateComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('patient.documents.title')"
        [subtitle]="t('patient.documents.subtitle')"
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
        } @else {
          @if (documents().length === 0) {
            <app-empty-state [message]="t('patient.documents.empty')" />
          } @else {
            <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
              @for (doc of documents(); track doc.visitId) {
                <app-ui-card class="flex flex-col justify-between h-full">
                  <div>
                    <div class="flex items-center justify-between gap-3 mb-3">
                      <span class="font-mono text-sm font-black" style="color: var(--text-primary)">
                        {{ doc.visitNumber }}
                      </span>
                      <span
                        class="inline-flex items-center border px-2 py-0.5 text-[11px] font-black uppercase"
                        [style.border-color]="statusColor(doc.documentStatus)"
                        [style.color]="statusColor(doc.documentStatus)"
                        style="border-radius: var(--radius-brand-sm)"
                      >
                        {{ doc.documentStatus || t('patient.documents.status.unknown') }}
                      </span>
                    </div>
                    <p class="text-sm font-semibold" style="color: var(--text-primary)">{{ doc.clinicName }}</p>
                    <p class="text-xs mt-1" style="color: var(--text-secondary)">{{ doc.doctorName }}</p>
                    <p class="text-xs mt-2" style="color: var(--text-muted)">{{ doc.visitDate | date:'mediumDate' }}</p>
                  </div>
                  <div class="mt-5 pt-4 border-t" style="border-color: var(--app-border)">
                    <button
                      type="button"
                      (click)="download(doc)"
                      [disabled]="downloadingId() === doc.visitId"
                      class="w-full inline-flex items-center justify-center gap-2 px-4 py-2 text-sm font-bold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] disabled:opacity-50 cursor-pointer transition-colors"
                      style="border-radius: var(--radius-brand-sm)"
                    >
                      @if (downloadingId() === doc.visitId) {
                        <span>{{ t('common.loading') }}</span>
                      } @else {
                        <span>{{ t('patient.documents.download') }}</span>
                      }
                    </button>
                  </div>
                </app-ui-card>
              }
            </div>
          }
        }
      </div>
    </app-shell>
  `,
})
export class PatientDocumentsPageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly patient = signal<PatientPortalMeResponse | null>(null);
  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly downloadingId = signal<string>('');

  readonly documents = signal<PatientPortalConsultation[]>([]);

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    this.isLoading.set(true);
    this.error.set('');
    this.portalService.getMe().subscribe({
      next: (data) => {
        this.patient.set(data);
        this.documents.set(
          (data.consultations || []).filter((c) => c.documentId).sort(
            (a, b) => new Date(b.visitDate).getTime() - new Date(a.visitDate).getTime()
          )
        );
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.error.set(err.error?.detail || this.t('patient.documents.loadError'));
      },
    });
  }

  download(doc: PatientPortalConsultation): void {
    if (!doc.documentId) return;
    this.downloadingId.set(doc.visitId);
    this.portalService.downloadDocument(doc.visitId).subscribe({
      next: (blob) => {
        this.downloadingId.set('');
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `document-${doc.visitNumber}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.downloadingId.set('');
        alert(this.t('patient.documents.downloadError'));
      },
    });
  }

  statusColor(status: string | null): string {
    if (!status) return 'var(--text-muted)';
    const upper = status.toUpperCase();
    if (upper === 'VALID') return 'var(--brand-success)';
    if (upper === 'REVOQUE' || upper === 'REVOKED' || upper === 'CANCELLED' || upper === 'ANNULE') return 'var(--brand-danger)';
    return 'var(--brand-primary)';
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppShellComponent } from '../../../shared/layout/app-shell.component';
import { PatientPortalService } from '../services/patient-portal.service';
import { I18nService } from '../../../core/i18n/i18n.service';

interface GroupedResult {
  resultNumber: string;
  examRequestNumber: string;
  validatorName: string;
  validatedAt?: string;
  createdAt: string;
  conclusion?: string;
  pdfFilePath?: string;
  id: string; // ID pour charger le PDF
  items: Array<{
    analyteName: string;
    value: string;
    unit?: string;
    referenceRange?: string;
    interpretation?: string;
    comment?: string;
    status?: string;
  }>;
}

@Component({
  selector: 'app-patient-results-page',
  standalone: true,
  imports: [CommonModule, AppShellComponent],
  template: `
    <app-shell>
      <div class="app-container py-6 space-y-6">
        <!-- Header -->
        <div class="flex flex-col md:flex-row md:items-center md:justify-between border-b border-[var(--app-border)] pb-5 gap-4">
          <div>
            <h1 class="text-2xl font-bold tracking-tight text-[var(--text-primary)]">
              {{ t('patient.results.title') }}
            </h1>
            <p class="mt-1 text-sm text-[var(--text-muted)]">
              {{ t('patient.results.subtitle') }}
            </p>
          </div>
          <!-- Boutons d'exportation -->
          <div class="flex flex-wrap gap-2">
            <button
              (click)="exportData('csv')"
              [disabled]="isLoading() || groupedResults().length === 0"
              class="inline-flex items-center justify-center px-3.5 py-1.5 text-xs font-semibold text-[var(--text-secondary)] dark:text-[var(--text-primary)] bg-white dark:bg-[var(--bg-input)] border border-[var(--app-border)] dark:border-slate-700 hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-700/50 disabled:opacity-50 cursor-pointer transition-colors shadow-sm rounded-[6px]"
            >
              {{ t('patient.results.exportCsv') }}
            </button>
            <button
              (click)="exportData('json')"
              [disabled]="isLoading() || groupedResults().length === 0"
              class="inline-flex items-center justify-center px-3.5 py-1.5 text-xs font-semibold text-[var(--text-secondary)] dark:text-[var(--text-primary)] bg-white dark:bg-[var(--bg-input)] border border-[var(--app-border)] dark:border-slate-700 hover:bg-[var(--app-surface-muted)] dark:hover:bg-slate-700/50 disabled:opacity-50 cursor-pointer transition-colors shadow-sm rounded-[6px]"
            >
              {{ t('patient.results.exportJson') }}
            </button>
          </div>
        </div>

        @if (isLoading()) {
          <div class="flex items-center justify-center py-12">
            <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
          </div>
        } @else if (error()) {
          <div class="p-4 bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold rounded-[6px]">
            {{ error() }}
          </div>
        } @else if (groupedResults().length === 0) {
          <div class="text-center py-16 bg-[var(--app-surface)] border border-[var(--app-border)] shadow-sm rounded-[6px]">
            <svg class="mx-auto h-12 w-12 text-[var(--text-muted)]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h3 class="mt-4 text-sm font-semibold text-[var(--text-primary)]">{{ t('patient.results.emptyTitle') }}</h3>
            <p class="mt-1 text-sm text-[var(--text-muted)]">{{ t('patient.results.emptyText') }}</p>
          </div>
        } @else {
          <div class="space-y-6">
            @for (group of groupedResults(); track group.resultNumber) {
              <div class="bg-[var(--app-surface)] border border-[var(--app-border)] shadow-sm rounded-[6px] overflow-hidden">
                <!-- Card Header -->
                <div class="bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/50 px-5 py-4 border-b border-[var(--app-border)] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                  <div>
                    <div class="flex items-center gap-2">
                      <span class="font-mono text-sm font-bold text-[var(--text-primary)]">
                        {{ group.resultNumber }}
                      </span>
                      <span class="text-xs text-[var(--text-muted)]">
                        ({{ t('lab.exams') }} : {{ group.examRequestNumber }})
                      </span>
                    </div>
                    <div class="mt-1 text-xs text-[var(--text-muted)]">
                      {{ t('lab.validatedAt') }} : {{ (group.validatedAt || group.createdAt) | date:'medium' }} · {{ t('lab.validatorName') }} : {{ group.validatorName }}
                    </div>
                  </div>
                  <div class="flex items-center gap-2">
                    @if (group.pdfFilePath) {
                      <button
                        (click)="downloadPdf(group.id, group.resultNumber)"
                        class="inline-flex items-center justify-center px-3 py-1.5 text-xs font-semibold text-white bg-[var(--brand-primary)] hover:bg-[var(--brand-primary-hover)] transition-colors shadow-sm rounded-[6px]"
                      >
                        {{ t('patient.results.downloadPdf') }}
                      </button>
                    }
                  </div>
                </div>

                <!-- Card Body -->
                <div class="p-5 space-y-4">
                  @if (group.conclusion) {
                    <div class="p-3 bg-blue-50 dark:bg-blue-950/20 border border-blue-100 dark:border-blue-900/30 text-[var(--text-secondary)] text-sm rounded-[4px]">
                      <span class="font-semibold block mb-1">{{ t('patient.results.conclusion') }}</span>
                      {{ group.conclusion }}
                    </div>
                  }

                  <!-- Table des analytes -->
                  <div class="overflow-x-auto">
                    <table class="w-full text-left border-collapse text-sm">
                      <thead>
                        <tr class="border-b border-[var(--app-border)] text-[var(--text-muted)] text-xs uppercase font-semibold">
                          <th class="py-2.5">{{ t('lab.analyteName') }}</th>
                          <th class="py-2.5 text-right">{{ t('lab.value') }}</th>
                          <th class="py-2.5 text-center">{{ t('lab.referenceRange') }}</th>
                          <th class="py-2.5 text-right">{{ t('lab.interpretation.label') }}</th>
                        </tr>
                      </thead>
                      <tbody class="divide-y divide-slate-100 dark:divide-slate-800/50">
                        @for (item of group.items; track item.analyteName) {
                          <tr class="text-[var(--text-secondary)]">
                            <td class="py-3 font-medium">{{ item.analyteName }}</td>
                            <td class="py-3 text-right">
                              <span class="font-bold text-[var(--text-primary)]">{{ item.value }}</span> {{ item.unit || '' }}
                            </td>
                            <td class="py-3 text-center text-[var(--text-muted)]">
                              {{ item.referenceRange || '-' }}
                            </td>
                            <td class="py-3 text-right">
                              <span
                                class="inline-flex items-center px-2 py-0.5 text-xs font-semibold rounded-[4px] border"
                                [ngClass]="getInterpretationClasses(item.interpretation)"
                              >
                                {{ getInterpretationLabel(item.interpretation) }}
                              </span>
                            </td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            }
          </div>
        }
      </div>
    </app-shell>
  `
})
export class PatientResultsPageComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);
  readonly i18n = inject(I18nService);

  readonly isLoading = signal(false);
  readonly error = signal('');
  readonly groupedResults = signal<GroupedResult[]>([]);

  patientId = '';

  ngOnInit(): void {
    this.loadResults();
  }

  loadResults(): void {
    this.isLoading.set(true);
    this.error.set('');

    // Récupérer les infos du patient pour l'ID d'exportation
    this.portalService.getMe().subscribe({
      next: (me) => {
        this.patientId = me.id;
        this.portalService.getOwnResults().subscribe({
          next: (results) => {
            this.groupedResults.set(this.groupResults(results));
            this.isLoading.set(false);
          },
          error: (err) => {
            this.error.set(err.error?.detail || err.error?.title || this.t('patient.results.loadError'));
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set(this.t('patient.results.profileLoadError'));
        this.isLoading.set(false);
      }
    });
  }

  groupResults(results: any[]): GroupedResult[] {
    const map = new Map<string, GroupedResult>();

    results.forEach((r) => {
      if (!map.has(r.resultNumber)) {
        map.set(r.resultNumber, {
          resultNumber: r.resultNumber,
          examRequestNumber: r.examRequestNumber,
          validatorName: r.validatorName,
          validatedAt: r.validatedAt,
          createdAt: r.createdAt,
          conclusion: r.conclusion,
          pdfFilePath: r.pdfFilePath,
          id: r.id,
          items: []
        });
      }
      map.get(r.resultNumber)!.items.push({
        analyteName: r.analyteName,
        value: r.value,
        unit: r.unit,
        referenceRange: r.referenceRange,
        interpretation: r.interpretation,
        comment: r.comment,
        status: r.status
      });
    });

    return Array.from(map.values()).sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    );
  }

  downloadPdf(resultId: string, resultNumber: string): void {
    this.portalService.downloadResultPdf(resultId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `result-${resultNumber}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => alert(this.t('patient.results.downloadError'))
    });
  }

  exportData(format: string): void {
    if (!this.patientId) return;
    this.portalService.exportResults(this.patientId, format).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `results-${this.patientId}.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => alert(this.t('patient.results.exportError'))
    });
  }

  getInterpretationLabel(inter?: string): string {
    if (!inter) return this.t('lab.interpretation.NORMAL');
    switch (inter.toUpperCase()) {
      case 'LOW':
        return this.t('lab.interpretation.LOW');
      case 'HIGH':
        return this.t('lab.interpretation.HIGH');
      case 'CRITICAL':
        return this.t('lab.interpretation.CRITICAL');
      default:
        return this.t('lab.interpretation.NORMAL');
    }
  }

  getInterpretationClasses(inter?: string): Record<string, boolean> {
    if (!inter) {
      return {
        'bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/40 text-[var(--text-secondary)] border-[var(--app-border)]/50': true
      };
    }
    switch (inter.toUpperCase()) {
      case 'LOW':
      case 'HIGH':
        return {
          'bg-amber-50 dark:bg-amber-950/20 text-amber-700 dark:text-amber-300 border-amber-100 dark:border-amber-900/30': true
        };
      case 'CRITICAL':
        return {
          'bg-rose-50 dark:bg-rose-950/20 text-rose-700 dark:text-rose-300 border-rose-100 dark:border-rose-900/30': true
        };
      default:
        return {
          'bg-emerald-50 dark:bg-emerald-950/20 text-[var(--brand-success-text)] border-emerald-100 dark:border-emerald-900/30': true
        };
    }
  }

  t(key: string): string {
    return this.i18n.t(key);
  }
}

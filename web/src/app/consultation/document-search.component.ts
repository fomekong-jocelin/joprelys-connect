import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { AppLogoComponent } from '../shared/ui/app-logo.component';
import { ConsultationApiService } from '../consultation/consultation-api.service';

@Component({
  selector: 'app-document-search',
  standalone: true,
  imports: [FormsModule, AppLogoComponent],
  template: `
    <div class="min-h-screen bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] flex flex-col justify-between py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-200">
      <div class="flex-grow flex flex-col justify-center">
        <!-- Logo and header -->
        <div class="sm:mx-auto sm:w-full sm:max-w-md flex flex-col items-center mb-8">
          <app-logo [showName]="true"></app-logo>
          <p class="mt-3 text-center text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
            {{ i18n.t('verify.search.subtitle') }}
          </p>
        </div>

        <div class="sm:mx-auto sm:w-full sm:max-w-lg">
          <div class="bg-[var(--app-surface)] shadow-xl rounded border border-[var(--app-border)]/80 p-8 md:p-10 relative overflow-hidden">

            <!-- Background accent -->
            <div class="absolute -top-32 -right-32 w-64 h-64 bg-[var(--brand-primary-subtle)] rounded-full blur-3xl pointer-events-none"></div>

            <h1 class="text-2xl font-black text-[var(--text-primary)] mb-2">
              {{ i18n.t('verify.search.title') }}
            </h1>
            <p class="text-sm text-[var(--text-muted)] mb-8">
              {{ i18n.t('verify.search.subtitle') }}
            </p>

            <div class="space-y-4">
              <!-- Document Number Input -->
              <div>
                <label
                  for="docNumberInput"
                  class="block text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider mb-2">
                  {{ i18n.t('verify.search.label') }}
                </label>
                <input
                  id="docNumberInput"
                  type="text"
                  [(ngModel)]="documentNumber"
                  [placeholder]="i18n.t('verify.search.placeholder')"
                  (keyup.enter)="search()"
                  class="w-full px-4 py-3 rounded bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/60 border border-[var(--app-border)] dark:border-slate-700/80 text-[var(--text-primary)] placeholder-slate-400 font-mono text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-400 transition-all duration-200" />
              </div>

              <!-- Error message -->
              @if (errorMessage()) {
                <p class="text-sm text-rose-600 dark:text-rose-400 font-medium flex items-center gap-1.5">
                  <svg class="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  {{ errorMessage() }}
                </p>
              }

              <!-- Search Button -->
              <button
                id="searchDocumentBtn"
                (click)="search()"
                [disabled]="isSearching() || !documentNumber.trim()"
                class="w-full flex items-center justify-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed text-white font-bold text-sm rounded transition-all duration-200 shadow-sm hover:shadow-indigo-500/30">
                @if (isSearching()) {
                  <svg class="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8z"></path>
                  </svg>
                  {{ i18n.t('common.processing') }}
                } @else {
                  <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                  </svg>
                  {{ i18n.t('verify.search.button') }}
                }
              </button>
            </div>

            <!-- Legal notice -->
            <div class="mt-8 bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/20 rounded p-4 border border-[var(--app-border)]/40">
              <div class="flex items-start gap-2.5">
                <span class="text-base shrink-0 select-none">🔒</span>
                <div class="space-y-0.5">
                  <span class="text-xs font-black text-[var(--text-secondary)]">
                    {{ i18n.t('verify.rgpdTitle') }}
                  </span>
                  <p class="text-[11px] font-medium leading-relaxed text-[var(--text-muted)]">
                    {{ i18n.t('verify.rgpdWarning') }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Footer -->
      <div class="mt-8 text-center text-xs font-medium text-[var(--text-muted)]">
        &copy; 2026 Joprelys HealthTech. All rights reserved.
      </div>
    </div>
  `
})
export class DocumentSearchComponent {
  private readonly router = inject(Router);
  private readonly apiService = inject(ConsultationApiService);
  readonly i18n = inject(I18nService);

  documentNumber = '';
  readonly isSearching = signal(false);
  readonly errorMessage = signal<string | null>(null);

  search(): void {
    const num = this.documentNumber.trim();
    if (!num) return;

    this.isSearching.set(true);
    this.errorMessage.set(null);

    this.apiService.verifyDocumentByNumber(num).subscribe({
      next: (data) => {
        this.isSearching.set(false);
        if (data?.id) {
          this.router.navigate(['/verify', data.id]);
        } else {
          this.errorMessage.set(this.i18n.t('verify.search.notFound'));
        }
      },
      error: () => {
        this.isSearching.set(false);
        this.errorMessage.set(this.i18n.t('verify.search.notFound'));
      }
    });
  }
}

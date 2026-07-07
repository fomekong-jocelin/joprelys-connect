import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { ConsultationApiService } from './consultation-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AppLogoComponent } from '../shared/ui/app-logo.component';

interface VerificationMetadata {
  documentNumber: string;
  status: 'VALID' | 'REVOKED' | 'REPLACED' | string;
  documentType: string;
  clinicName: string;
  doctorName: string;
  serviceName: string;
  patientName: string;
  issuedAt: string;
  legalNotice: string;
}

@Component({
  selector: 'app-verification',
  standalone: true,
  imports: [DatePipe, AppLogoComponent],
  template: `
    <div class="min-h-screen bg-[var(--app-surface-muted)] dark:bg-[var(--app-bg)] flex flex-col justify-between py-12 px-4 sm:px-6 lg:px-8 transition-colors duration-200">
      <div class="flex-grow flex flex-col justify-center">
        <!-- Logo and header -->
        <div class="sm:mx-auto sm:w-full sm:max-w-md flex flex-col items-center mb-8">
          <app-logo [showName]="true"></app-logo>
          <p class="mt-3 text-center text-xs font-semibold uppercase tracking-wider text-[var(--text-muted)]">
            {{ i18n.t('verify.subtitle') }}
          </p>
        </div>

        <div class="sm:mx-auto sm:w-full sm:max-w-xl">
          @if (isLoading()) {
            <!-- Loading State -->
            <div class="bg-[var(--app-surface)] shadow-2xl rounded border border-[var(--app-border)]/80 p-8 md:p-10 text-center space-y-4">
              <div class="relative flex justify-center items-center">
                <div class="w-16 h-16 rounded-full border-4 border-indigo-100 dark:border-indigo-950 border-t-indigo-600 dark:border-t-indigo-400 animate-spin"></div>
              </div>
              <p class="text-sm font-medium text-[var(--text-secondary)]">
                {{ i18n.t('verify.loading') }}
              </p>
            </div>
          } @else {
            <!-- Card Wrapper -->
            <div class="bg-[var(--app-surface)] shadow-2xl rounded border border-[var(--app-border)]/80 p-8 md:p-10 relative overflow-hidden transition-all duration-300 hover:shadow-indigo-500/5">
              
              <!-- Background glows for premium look -->
              <div class="absolute -top-40 -right-40 w-80 h-80 bg-[var(--brand-primary-subtle)] rounded-full blur-3xl pointer-events-none"></div>
              <div class="absolute -bottom-40 -left-40 w-80 h-80 bg-[var(--brand-success-subtle)] rounded-full blur-3xl pointer-events-none"></div>

              <div class="flex flex-col items-center text-center space-y-6">
                <!-- Status Icons & Badges -->
                @if (status() === 'VALID') {
                  <!-- Success State (Valid) -->
                  <div class="relative">
                    <div class="absolute inset-0 bg-emerald-500/20 rounded-full blur-xl animate-pulse"></div>
                    <div class="relative w-20 h-20 bg-emerald-50 dark:bg-emerald-950/40 text-[var(--brand-success-text)] border border-emerald-100 dark:border-emerald-900/30 rounded-full flex items-center justify-center">
                      <svg class="w-10 h-10 animate-[scaleIn_0.3s_ease-out]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-dasharray="24" stroke-dashoffset="0" d="M5 13l4 4L19 7" />
                      </svg>
                    </div>
                  </div>
                  <h2 class="text-xl md:text-2xl font-black text-[var(--brand-success-text)] tracking-tight">
                    {{ i18n.t('verify.status.valid') }}
                  </h2>
                } @else if (status() === 'REVOKED' || status() === 'REVOQUE' || status() === 'REPLACED' || status() === 'REMPLACE') {
                  <!-- Warning/Notice State (Revoked or Replaced) -->
                  <div class="relative">
                    <div class="absolute inset-0 bg-amber-500/20 rounded-full blur-xl animate-pulse"></div>
                    <div class="relative w-20 h-20 bg-amber-50 dark:bg-amber-950/40 text-amber-600 dark:text-amber-400 border border-amber-100 dark:border-amber-900/30 rounded-full flex items-center justify-center">
                      <svg class="w-10 h-10 animate-[scaleIn_0.3s_ease-out]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                      </svg>
                    </div>
                  </div>
                  <h2 class="text-xl md:text-2xl font-black text-amber-600 dark:text-amber-400 tracking-tight">
                    {{ (status() === 'REVOKED' || status() === 'REVOQUE') ? i18n.t('verify.status.revoked') : i18n.t('verify.status.replaced') }}
                  </h2>
                } @else if (status() === 'CANCELLED' || status() === 'ANNULE') {
                  <!-- Cancelled State (Red / Rose warning) -->
                  <div class="relative">
                    <div class="absolute inset-0 bg-rose-500/20 rounded-full blur-xl animate-pulse"></div>
                    <div class="relative w-20 h-20 bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-100 dark:border-rose-900/30 rounded-full flex items-center justify-center">
                      <svg class="w-10 h-10 animate-[scaleIn_0.3s_ease-out]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                      </svg>
                    </div>
                  </div>
                  <h2 class="text-xl md:text-2xl font-black text-rose-600 dark:text-rose-400 tracking-tight">
                    {{ i18n.t('verify.status.cancelled') }}
                  </h2>
                } @else {
                  <!-- Error State (Invalid/Not found) -->
                  <div class="relative">
                    <div class="absolute inset-0 bg-rose-500/20 rounded-full blur-xl animate-pulse"></div>
                    <div class="relative w-20 h-20 bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 border border-rose-100 dark:border-rose-900/30 rounded-full flex items-center justify-center">
                      <svg class="w-10 h-10 animate-[scaleIn_0.3s_ease-out]" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </div>
                  </div>
                  <h2 class="text-xl md:text-2xl font-black text-rose-600 dark:text-rose-400 tracking-tight">
                    {{ i18n.t('verify.status.invalid') }}
                  </h2>
                }

                <!-- Document Details Grid -->
                @if (metadata()) {
                  <div class="w-full border-t border-b border-[var(--app-border)]/80 py-6 my-4 text-left">
                    <div class="grid grid-cols-1 gap-y-5 sm:grid-cols-2 sm:gap-x-6 sm:gap-y-4">
                      
                      <!-- Document Number -->
                      <div class="sm:col-span-2 bg-[var(--app-surface-muted)] dark:bg-[var(--app-surface-muted)] rounded p-4 border border-slate-100/50 dark:border-slate-800/50">
                        <span class="text-xs font-bold text-[var(--text-muted)] uppercase tracking-wider block mb-1">
                          {{ i18n.t('verify.documentNumber') }}
                        </span>
                        <span class="font-mono text-sm md:text-base font-black tracking-wider text-[var(--text-primary)]">
                          {{ metadata()?.documentNumber }}
                        </span>
                      </div>

                      <!-- Document Type -->
                      @if (metadata()?.documentType) {
                        <div class="sm:col-span-2">
                          <span class="text-[10px] font-extrabold text-[var(--text-muted)] uppercase tracking-wider block mb-0.5">
                            {{ i18n.t('verify.documentType') }}
                          </span>
                          <span class="text-sm font-bold text-indigo-700 dark:text-indigo-400 bg-indigo-50  px-2 py-0.5 rounded inline-block">
                            {{ metadata()?.documentType }}
                          </span>
                        </div>
                      }

                      <!-- Patient Name -->
                      <div class="sm:col-span-2">
                        <span class="text-[10px] font-extrabold text-[var(--text-muted)] uppercase tracking-wider block mb-0.5">
                          {{ i18n.t('verify.patientName') }}
                        </span>
                        <span class="text-base font-extrabold text-[var(--text-primary)]">
                          {{ metadata()?.patientName }}
                        </span>
                      </div>

                      <!-- Issuing Doctor -->
                      <div>
                        <span class="text-[10px] font-extrabold text-[var(--text-muted)] uppercase tracking-wider block mb-0.5">
                          {{ i18n.t('verify.doctorName') }}
                        </span>
                        <span class="text-sm font-bold text-[var(--text-secondary)]">
                          {{ metadata()?.doctorName }}
                        </span>
                      </div>

                      <!-- Clinic Name -->
                      <div>
                        <span class="text-[10px] font-extrabold text-[var(--text-muted)] uppercase tracking-wider block mb-0.5">
                          {{ i18n.t('verify.clinicName') }}
                        </span>
                        <span class="text-sm font-bold text-[var(--text-secondary)]">
                          {{ metadata()?.clinicName }}
                        </span>
                      </div>

                      <!-- Service Name -->
                      @if (metadata()?.serviceName) {
                        <div>
                          <span class="text-[10px] font-extrabold text-[var(--text-muted)] uppercase tracking-wider block mb-0.5">
                            {{ i18n.t('verify.serviceName') }}
                          </span>
                          <span class="text-sm font-bold text-[var(--text-secondary)]">
                            {{ metadata()?.serviceName }}
                          </span>
                        </div>
                      }

                      <!-- Issued Date -->
                      <div class="sm:col-span-2">
                        <span class="text-[10px] font-extrabold text-[var(--text-muted)] uppercase tracking-wider block mb-0.5">
                          {{ i18n.t('verify.issuedAt') }}
                        </span>
                        <span class="text-sm font-semibold text-[var(--text-secondary)]">
                          {{ metadata()?.issuedAt | date:'medium' }}
                        </span>
                      </div>

                    </div>
                  </div>
                }

                <!-- Legal Notice (from CDC / backend) -->
                @if (metadata()?.legalNotice) {
                  <div class="w-full text-left bg-amber-50 dark:bg-amber-950/20 rounded p-4 border border-amber-100 dark:border-amber-900/30">
                    <div class="flex items-start gap-2.5">
                      <span class="text-base shrink-0 select-none">⚖️</span>
                      <div class="space-y-0.5">
                        <span class="text-xs font-black text-[var(--brand-warning-text)]">
                          {{ i18n.t('verify.legalNotice') }}
                        </span>
                        <p class="text-[11px] font-medium leading-relaxed text-[var(--brand-warning-text)]">
                          {{ metadata()?.legalNotice }}
                        </p>
                      </div>
                    </div>
                  </div>
                }

                <!-- Medical Confidentiality Warning Footer -->
                <div class="w-full text-left bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/20 rounded p-4 border border-[var(--app-border)]/40">
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
          }
        </div>
      </div>

      <!-- General Footer -->
      <div class="mt-8 text-center text-xs font-medium text-[var(--text-muted)]">
        &copy; 2026 Joprelys HealthTech. All rights reserved.
      </div>
    </div>
  `,
  styles: [`
    @keyframes scaleIn {
      0% {
        transform: scale(0.9);
        opacity: 0;
      }
      100% {
        transform: scale(1);
        opacity: 1;
      }
    }
  `]
})
export class VerificationComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly apiService = inject(ConsultationApiService);
  readonly i18n = inject(I18nService);

  readonly isLoading = signal(true);
  readonly status = signal<'VALID' | 'REVOKED' | 'REVOQUE' | 'REPLACED' | 'REMPLACE' | 'CANCELLED' | 'ANNULE' | 'INVALID'>('INVALID');
  readonly metadata = signal<VerificationMetadata | null>(null);

  ngOnInit(): void {
    const documentId = this.route.snapshot.paramMap.get('documentId');
    if (!documentId) {
      this.status.set('INVALID');
      this.isLoading.set(false);
      return;
    }

    this.apiService.verifyDocumentPublic(documentId).subscribe({
      next: (data) => {
        this.metadata.set(data);
        this.status.set(data.status || 'VALID');
        this.isLoading.set(false);
      },
      error: () => {
        this.status.set('INVALID');
        this.metadata.set(null);
        this.isLoading.set(false);
      }
    });
  }
}

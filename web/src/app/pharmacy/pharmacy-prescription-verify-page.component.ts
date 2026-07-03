import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { PharmacyApiService } from './pharmacy-api.service';
import { PharmacyDispensationPanelComponent } from './pharmacy-dispensation-panel.component';
import { PharmacyVerifyItem, PharmacyVerifyResponse } from './pharmacy.models';

@Component({
  selector: 'app-pharmacy-prescription-verify-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, AppShellComponent, PageHeaderComponent, ButtonComponent, PharmacyDispensationPanelComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('pharmacy.title')"
        [subtitle]="t('pharmacy.portal')"
        backLink="/dashboard"
        [backLabel]="t('common.back')"
      />

      <div class="app-container-wide pb-10">

        <div class="grid gap-5 lg:grid-cols-[380px_1fr]">
          <section class="ui-card-subtle p-5 md:p-6">
            <p class="ui-label">{{ t('pharmacy.verifySection') }}</p>
            <h2 class="mt-1 font-display text-lg font-extrabold" style="color: var(--text-primary)">
              {{ t('pharmacy.verifyTitle') }}
            </h2>
            <p class="mt-2 text-sm leading-relaxed" style="color: var(--text-secondary)">
              {{ t('pharmacy.verifyHelp') }}
            </p>

            <form class="mt-6 space-y-4" [formGroup]="form" (ngSubmit)="verify()">
              <label class="block">
                <span class="ui-label">{{ t('pharmacy.prescriptionNumber') }}</span>
                <input
                  class="ui-input pharmacy-compact-input mt-1"
                  formControlName="prescriptionNumber"
                  autocomplete="off"
                  [placeholder]="t('pharmacy.prescriptionNumberPlaceholder')"
                />
              </label>

              <label class="block">
                <span class="ui-label">{{ t('pharmacy.pinCode') }}</span>
                <input
                  class="ui-input pharmacy-compact-input mt-1 font-mono uppercase tracking-[0.18em]"
                  formControlName="pinCode"
                  maxlength="8"
                  autocomplete="one-time-code"
                  [placeholder]="t('pharmacy.pinCodePlaceholder')"
                />
              </label>

              @if (errorMessage()) {
                <div class="border p-3 text-sm font-semibold"
                     style="border-color: color-mix(in srgb, var(--brand-danger) 34%, transparent); border-radius: var(--radius-brand-md); color: var(--brand-danger); background: color-mix(in srgb, var(--brand-danger) 9%, var(--app-surface));">
                  {{ errorMessage() }}
                </div>
              }

              <app-ui-button type="submit" [disabled]="form.invalid || isLoading()" class="w-full">
                {{ isLoading() ? t('common.loading') : t('pharmacy.verifyButton') }}
              </app-ui-button>
            </form>
          </section>

          <section class="ui-card-subtle min-h-[420px] p-5 md:p-6">
            @if (!prescription() && !isLoading()) {
              <div class="flex min-h-[360px] flex-col items-center justify-center text-center">
                <div class="mb-4 grid h-12 w-12 place-items-center border"
                     style="border-radius: var(--radius-brand-lg); border-color: var(--app-border); color: var(--brand-primary); background: color-mix(in srgb, var(--brand-primary) 10%, transparent);">
                  <span class="font-display text-lg font-black">Rx</span>
                </div>
                <h2 class="ui-title text-xl">{{ t('pharmacy.emptyTitle') }}</h2>
                <p class="mt-2 max-w-md text-sm leading-relaxed" style="color: var(--text-secondary)">
                  {{ t('pharmacy.emptyDescription') }}
                </p>
              </div>
            }

            @if (isLoading()) {
              <div class="flex min-h-[360px] items-center justify-center">
                <p class="text-sm font-bold" style="color: var(--text-muted)">{{ t('common.loading') }}</p>
              </div>
            }

            @if (prescription(); as currentPrescription) {
              <div class="space-y-5">
                <div class="flex flex-col gap-3 border-b pb-5 md:flex-row md:items-start md:justify-between" style="border-color: var(--app-border)">
                  <div>
                    <p class="ui-label">{{ t('pharmacy.prescription') }}</p>
                    <h2 class="mt-1 font-mono text-xl font-black tracking-wide" style="color: var(--text-primary)">
                      {{ currentPrescription.prescriptionNumber }}
                    </h2>
                    <p class="mt-2 text-sm" style="color: var(--text-secondary)">
                      {{ t('pharmacy.issuedBy') }} {{ currentPrescription.doctorName }}
                    </p>
                  </div>
                  <span class="inline-flex w-fit items-center border px-3 py-1 text-xs font-black uppercase tracking-wide"
                        [style.border-color]="statusTone().border"
                        [style.background]="statusTone().background"
                        [style.color]="statusTone().color"
                        style="border-radius: var(--radius-brand-md);">
                    {{ statusLabel(currentPrescription.status) }}
                  </span>
                </div>

                <div class="grid gap-3 md:grid-cols-4">
                  <div class="ui-card-muted p-4 md:col-span-2">
                    <span class="ui-label">{{ t('pharmacy.patient') }}</span>
                    <p class="mt-1 font-bold" style="color: var(--text-primary)">{{ currentPrescription.patientName }}</p>
                  </div>
                  <div class="ui-card-muted p-4">
                    <span class="ui-label">{{ t('pharmacy.issuedAt') }}</span>
                    <p class="mt-1 text-sm font-bold" style="color: var(--text-primary)">
                      {{ currentPrescription.issuedAt | date:'mediumDate' }}
                    </p>
                  </div>
                  <div class="ui-card-muted p-4">
                    <span class="ui-label">{{ t('pharmacy.expiresAt') }}</span>
                    <p class="mt-1 text-sm font-bold" style="color: var(--text-primary)">
                      {{ currentPrescription.expiresAt | date:'mediumDate' }}
                    </p>
                  </div>
                </div>

                <div>
                  <div class="mb-3 flex items-center justify-between gap-3">
                    <div>
                      <p class="ui-label">{{ t('pharmacy.items') }}</p>
                      <h3 class="font-display text-base font-extrabold" style="color: var(--text-primary)">
                        {{ currentPrescription.items.length }} {{ t('pharmacy.itemsCount') }}
                      </h3>
                    </div>
                  </div>

                  <div class="hidden overflow-x-auto md:block">
                    <table class="ui-table">
                      <thead>
                        <tr>
                          <th>{{ t('pharmacy.drug') }}</th>
                          <th>{{ t('pharmacy.dosage') }}</th>
                          <th>{{ t('pharmacy.quantity') }}</th>
                          <th>{{ t('pharmacy.dispensed') }}</th>
                          <th>{{ t('pharmacy.substitution') }}</th>
                        </tr>
                      </thead>
                      <tbody>
                        @for (item of currentPrescription.items; track item.itemId) {
                          <tr>
                            <td>
                              <strong style="color: var(--text-primary)">{{ item.drugName }}</strong>
                              @if (item.instructions) {
                                <span class="mt-1 block text-xs" style="color: var(--text-muted)">{{ item.instructions }}</span>
                              }
                            </td>
                            <td>{{ item.dosage || '-' }} @if (item.form) { <span>· {{ item.form }}</span> }</td>
                            <td>{{ item.quantity }}</td>
                            <td>{{ item.quantityAlreadyDispensed }}</td>
                            <td>{{ substitutionLabel(item) }}</td>
                          </tr>
                        }
                      </tbody>
                    </table>
                  </div>

                  <div class="space-y-3 md:hidden">
                    @for (item of currentPrescription.items; track item.itemId) {
                      <article class="ui-card-muted p-4">
                        <div class="flex items-start justify-between gap-3">
                          <div>
                            <h4 class="font-bold" style="color: var(--text-primary)">{{ item.drugName }}</h4>
                            <p class="mt-1 text-sm" style="color: var(--text-secondary)">{{ item.dosage || '-' }}</p>
                          </div>
                          <span class="border px-2 py-1 text-[11px] font-black"
                                style="border-radius: var(--radius-brand-sm); border-color: var(--app-border); color: var(--text-muted);">
                            {{ item.quantity }}
                          </span>
                        </div>
                        <dl class="mt-3 grid grid-cols-2 gap-3 text-sm">
                          <div>
                            <dt class="ui-label">{{ t('pharmacy.dispensed') }}</dt>
                            <dd class="font-bold" style="color: var(--text-primary)">{{ item.quantityAlreadyDispensed }}</dd>
                          </div>
                          <div>
                            <dt class="ui-label">{{ t('pharmacy.substitution') }}</dt>
                            <dd class="font-bold" style="color: var(--text-primary)">{{ substitutionLabel(item) }}</dd>
                          </div>
                        </dl>
                        @if (item.instructions) {
                          <p class="mt-3 text-xs leading-relaxed" style="color: var(--text-muted)">{{ item.instructions }}</p>
                        }
                      </article>
                    }
                  </div>
                </div>

                <app-pharmacy-dispensation-panel
                  [prescription]="currentPrescription"
                  [pinCode]="verifiedPin()"
                  (prescriptionRefreshed)="prescription.set($event)"
                />
              </div>
            }
          </section>
        </div>
      </div>
    </app-shell>
  `,
  styles: [`
    .pharmacy-compact-input {
      border-radius: var(--radius-brand-sm);
    }
  `],
})
export class PharmacyPrescriptionVerifyPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly pharmacyApi = inject(PharmacyApiService);
  readonly i18n = inject(I18nService);

  readonly prescription = signal<PharmacyVerifyResponse | null>(null);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly verifiedPin = signal('');

  readonly form = this.fb.nonNullable.group({
    prescriptionNumber: ['', Validators.required],
    pinCode: ['', Validators.required],
  });

  readonly statusTone = computed(() => {
    const status = this.prescription()?.status;
    if (status === 'ACTIVE') {
      return {
        border: 'color-mix(in srgb, var(--brand-success) 42%, transparent)',
        background: 'color-mix(in srgb, var(--brand-success) 10%, var(--app-surface))',
        color: 'var(--brand-success)',
      };
    }
    if (status === 'PARTIALLY_DISPENSED') {
      return {
        border: 'color-mix(in srgb, var(--brand-primary) 42%, transparent)',
        background: 'color-mix(in srgb, var(--brand-primary) 10%, var(--app-surface))',
        color: 'var(--brand-primary)',
      };
    }
    return {
      border: 'color-mix(in srgb, var(--brand-danger) 38%, transparent)',
      background: 'color-mix(in srgb, var(--brand-danger) 9%, var(--app-surface))',
      color: 'var(--brand-danger)',
    };
  });

  verify(): void {
    if (this.form.invalid || this.isLoading()) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.prescription.set(null);

    const raw = this.form.getRawValue();
    this.pharmacyApi.verifyPrescription({
      prescriptionNumber: raw.prescriptionNumber.trim(),
      pinCode: raw.pinCode.trim().toUpperCase(),
    }).subscribe({
      next: (response) => {
        this.prescription.set(response);
        this.verifiedPin.set(raw.pinCode.trim().toUpperCase());
        this.isLoading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error?.detail || error.error?.title || this.t('pharmacy.verifyError'));
        this.isLoading.set(false);
      },
    });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  statusLabel(status: string): string {
    return this.t(`pharmacy.status.${status}`) === `pharmacy.status.${status}`
      ? status
      : this.t(`pharmacy.status.${status}`);
  }

  substitutionLabel(item: PharmacyVerifyItem): string {
    return item.substitutionAllowed ? this.t('common.active') : this.t('common.inactive');
  }
}

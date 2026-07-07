import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { ButtonComponent } from '../shared/ui/button.component';
import { PharmacyApiService } from './pharmacy-api.service';
import { PharmacyDispensationHistoryComponent } from './pharmacy-dispensation-history.component';
import {
  PharmacyDispensationHistoryEntry,
  PharmacyVerifyItem,
  PharmacyVerifyResponse,
} from './pharmacy.models';

interface DispensationLineDraft {
  available: boolean;
  quantityDispensed: number;
  substitutedWith: string;
}

@Component({
  selector: 'app-pharmacy-dispensation-panel',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, PharmacyDispensationHistoryComponent],
  template: `
    <section class="grid gap-5 xl:grid-cols-[minmax(0,1.1fr)_minmax(320px,0.9fr)] min-w-0">
      <div class="ui-card-muted min-w-0 p-4 md:p-5">
        <div class="mb-4 flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
          <div>
            <p class="ui-label">{{ t('pharmacy.dispenseSection') }}</p>
            <h3 class="font-display text-base font-extrabold" style="color: var(--text-primary)">
              {{ t('pharmacy.dispenseTitle') }}
            </h3>
          </div>
          @if (!canDispenseStatus()) {
            <span class="w-fit border px-3 py-1 text-xs font-black"
                  style="border-radius: var(--radius-brand-sm); border-color: color-mix(in srgb, var(--brand-danger) 34%, transparent); color: var(--brand-danger);">
              {{ t('pharmacy.dispenseBlocked') }}
            </span>
          }
        </div>

        <form class="space-y-4" [formGroup]="dispenseForm" (ngSubmit)="submitDispensation()">
          <div class="grid gap-3 md:grid-cols-2">
            <label class="block">
              <span class="ui-label">{{ t('pharmacy.pharmacyName') }}</span>
              <input class="ui-input pharmacy-compact-input mt-1" formControlName="pharmacyName" [placeholder]="t('pharmacy.pharmacyNamePlaceholder')" />
            </label>
            <label class="block">
              <span class="ui-label">{{ t('pharmacy.pharmacistLicense') }}</span>
              <input class="ui-input pharmacy-compact-input mt-1" formControlName="pharmacistLicense" [placeholder]="t('pharmacy.pharmacistLicensePlaceholder')" />
            </label>
          </div>

          <div class="overflow-x-auto">
            <table class="ui-table min-w-[780px]">
              <thead>
                <tr>
                  <th>{{ t('pharmacy.drug') }}</th>
                  <th>{{ t('pharmacy.remaining') }}</th>
                  <th>{{ t('pharmacy.available') }}</th>
                  <th>{{ t('pharmacy.quantityToDispense') }}</th>
                  <th>{{ t('pharmacy.substitutedWith') }}</th>
                </tr>
              </thead>
              <tbody>
                @for (item of prescription.items; track item.itemId) {
                  <tr>
                    <td>
                      <strong style="color: var(--text-primary)">{{ item.drugName }}</strong>
                      <span class="mt-1 block text-xs" style="color: var(--text-muted)">
                        {{ item.dosage || '-' }} · {{ item.quantity }}
                      </span>
                    </td>
                    <td>
                      @if (isQuantityFlexible(item)) {
                        <span class="text-xs italic" style="color: var(--text-muted)">
                          {{ t('pharmacy.unlimitedQuantity') }}
                        </span>
                      } @else {
                        {{ remainingQuantity(item) }}
                      }
                    </td>
                    <td>
                      <label class="inline-flex items-center gap-2 text-xs font-bold" style="color: var(--text-secondary)">
                        <input
                          type="checkbox"
                          [checked]="draftFor(item).available"
                          [disabled]="!canDispenseStatus() || (remainingQuantity(item) === 0 && !isQuantityFlexible(item))"
                          (change)="updateAvailability(item.itemId, $any($event.target).checked)"
                          class="ui-checkbox"
                        />
                        {{ draftFor(item).available ? t('pharmacy.availableYes') : t('pharmacy.availableNo') }}
                      </label>
                    </td>
                    <td>
                      <input
                        class="ui-input pharmacy-line-input"
                        type="number"
                        min="0"
                        [max]="isQuantityFlexible(item) ? 9999 : remainingQuantity(item)"
                        [value]="draftFor(item).quantityDispensed"
                        [disabled]="!canDispenseStatus() || (remainingQuantity(item) === 0 && !isQuantityFlexible(item)) || !draftFor(item).available"
                        (input)="updateQuantity(item.itemId, $any($event.target).value)"
                      />
                    </td>
                    <td>
                      <input
                        class="ui-input pharmacy-line-input"
                        [value]="draftFor(item).substitutedWith"
                        [disabled]="!item.substitutionAllowed || !canDispenseStatus()"
                        [placeholder]="item.substitutionAllowed ? t('pharmacy.substitutionPlaceholder') : t('pharmacy.substitutionNotAllowed')"
                        (input)="updateSubstitution(item.itemId, $any($event.target).value)"
                      />
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>

          @if (dispenseError()) {
            <div class="border p-3 text-sm font-semibold"
                 style="border-color: color-mix(in srgb, var(--brand-danger) 34%, transparent); border-radius: var(--radius-brand-md); color: var(--brand-danger); background: color-mix(in srgb, var(--brand-danger) 9%, var(--app-surface));">
              {{ dispenseError() }}
            </div>
          }

          @if (dispenseSuccess()) {
            <div class="border p-3 text-sm font-semibold"
                 style="border-color: color-mix(in srgb, var(--brand-success) 34%, transparent); border-radius: var(--radius-brand-md); color: var(--brand-success); background: color-mix(in srgb, var(--brand-success) 9%, var(--app-surface));">
              {{ dispenseSuccess() }}
            </div>
          }

          <div class="flex justify-end">
            <app-ui-button type="submit" [disabled]="!canSubmitDispensation() || isDispensing()">
              {{ isDispensing() ? t('common.saving') : t('pharmacy.dispenseButton') }}
            </app-ui-button>
          </div>
        </form>
      </div>

      <app-pharmacy-dispensation-history
        [history]="history()"
        [loading]="isHistoryLoading()"
        [error]="historyError()"
      />
    </section>
  `,
  styles: [`
    :host {
      display: block;
      min-width: 0;
    }

    .pharmacy-compact-input,
    .pharmacy-line-input {
      border-radius: var(--radius-brand-sm);
    }

    .pharmacy-line-input {
      min-width: 120px;
      padding: 0.55rem 0.65rem;
      font-size: 0.8125rem;
    }
  `],
})
export class PharmacyDispensationPanelComponent implements OnChanges {
  @Input({ required: true }) prescription!: PharmacyVerifyResponse;
  @Input({ required: true }) pinCode = '';
  @Output() prescriptionRefreshed = new EventEmitter<PharmacyVerifyResponse>();

  private readonly fb = inject(FormBuilder);
  private readonly pharmacyApi = inject(PharmacyApiService);
  readonly i18n = inject(I18nService);

  readonly history = signal<PharmacyDispensationHistoryEntry[]>([]);
  readonly isHistoryLoading = signal(false);
  readonly isDispensing = signal(false);
  readonly dispenseError = signal('');
  readonly dispenseSuccess = signal('');
  readonly historyError = signal('');
  readonly lineDrafts = signal<Record<string, DispensationLineDraft>>({});

  readonly dispenseForm = this.fb.nonNullable.group({
    pharmacyName: ['', Validators.required],
    pharmacistLicense: ['', Validators.required],
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['prescription']) {
      this.resetLineDrafts();
    }
    if (changes['prescription'] || changes['pinCode']) {
      this.loadHistory();
    }
  }

  submitDispensation(): void {
    if (!this.canSubmitDispensation() || this.isDispensing()) {
      this.dispenseForm.markAllAsTouched();
      return;
    }

    const raw = this.dispenseForm.getRawValue();
    this.isDispensing.set(true);
    this.dispenseError.set('');
    this.dispenseSuccess.set('');

    this.pharmacyApi.dispensePrescription({
      prescriptionNumber: this.prescription.prescriptionNumber,
      pinCode: this.pinCode,
      pharmacyName: raw.pharmacyName.trim(),
      pharmacistLicense: raw.pharmacistLicense.trim(),
      dispensedItems: this.prescription.items
        .map((item) => {
          const draft = this.draftFor(item);
          return {
            prescriptionItemId: item.itemId,
            quantityDispensed: draft.quantityDispensed,
            substitutedWith: draft.substitutedWith.trim() || undefined,
          };
        })
        .filter((item) => item.quantityDispensed > 0),
    }).subscribe({
      next: () => {
        this.dispenseSuccess.set(this.t('pharmacy.dispenseSuccess'));
        this.refreshPrescription();
      },
      error: (error) => {
        this.dispenseError.set(error.error?.detail || error.error?.title || this.t('pharmacy.dispenseError'));
        this.isDispensing.set(false);
      },
    });
  }

  canSubmitDispensation(): boolean {
    return this.canDispenseStatus() && this.dispenseForm.valid && this.hasDispensedLine();
  }

  canDispenseStatus(): boolean {
    return ['ACTIVE', 'PARTIALLY_DISPENSED'].includes(this.prescription?.status);
  }

  isQuantityFlexible(item: PharmacyVerifyItem): boolean {
    if (!item.quantity) return true;
    const digits = item.quantity.replace(/[^0-9]/g, '');
    return digits.length === 0;
  }

  remainingQuantity(item: PharmacyVerifyItem): number {
    if (this.isQuantityFlexible(item)) {
      return 9999;
    }
    return Math.max(this.parseQuantity(item.quantity) - item.quantityAlreadyDispensed, 0);
  }

  draftFor(item: PharmacyVerifyItem): DispensationLineDraft {
    return this.lineDrafts()[item.itemId] ?? { available: true, quantityDispensed: 0, substitutedWith: '' };
  }

  updateAvailability(itemId: string, available: boolean): void {
    this.updateDraft(itemId, {
      available,
      quantityDispensed: available ? this.lineDrafts()[itemId]?.quantityDispensed ?? 0 : 0,
    });
  }

  updateQuantity(itemId: string, value: string): void {
    const item = this.prescription.items.find((candidate) => candidate.itemId === itemId);
    if (!item) return;
    const parsed = Number.parseInt(value, 10);
    const quantity = Number.isFinite(parsed) ? parsed : 0;
    this.updateDraft(itemId, { quantityDispensed: Math.min(Math.max(quantity, 0), this.remainingQuantity(item)) });
  }

  updateSubstitution(itemId: string, value: string): void {
    this.updateDraft(itemId, { substitutedWith: value });
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private loadHistory(): void {
    if (!this.prescription?.prescriptionNumber || !this.pinCode) return;
    this.isHistoryLoading.set(true);
    this.historyError.set('');
    this.pharmacyApi.getDispensationHistory({
      prescriptionNumber: this.prescription.prescriptionNumber,
      pinCode: this.pinCode,
    }).subscribe({
      next: (history) => {
        this.history.set(history);
        this.isHistoryLoading.set(false);
      },
      error: (error) => {
        this.historyError.set(error.error?.detail || error.error?.title || this.t('pharmacy.historyError'));
        this.isHistoryLoading.set(false);
      },
    });
  }

  private refreshPrescription(): void {
    this.pharmacyApi.verifyPrescription({
      prescriptionNumber: this.prescription.prescriptionNumber,
      pinCode: this.pinCode,
    }).subscribe({
      next: (prescription) => {
        this.prescriptionRefreshed.emit(prescription);
        this.prescription = prescription;
        this.resetLineDrafts();
        this.loadHistory();
        this.isDispensing.set(false);
      },
      error: (error) => {
        this.dispenseError.set(error.error?.detail || error.error?.title || this.t('pharmacy.verifyError'));
        this.isDispensing.set(false);
      },
    });
  }

  private resetLineDrafts(): void {
    const drafts = Object.fromEntries((this.prescription?.items ?? []).map((item) => [
      item.itemId,
      { available: this.isQuantityFlexible(item) || this.remainingQuantity(item) > 0, quantityDispensed: 0, substitutedWith: '' },
    ]));
    this.lineDrafts.set(drafts);
  }

  private updateDraft(itemId: string, patch: Partial<DispensationLineDraft>): void {
    this.lineDrafts.update((drafts) => ({
      ...drafts,
      [itemId]: { ...(drafts[itemId] ?? { available: true, quantityDispensed: 0, substitutedWith: '' }), ...patch },
    }));
  }

  private hasDispensedLine(): boolean {
    return Object.values(this.lineDrafts()).some((line) => line.quantityDispensed > 0);
  }

  private parseQuantity(quantity: string): number {
    const digits = quantity?.replace(/[^0-9]/g, '') ?? '';
    return digits ? Number.parseInt(digits, 10) : 0;
  }
}

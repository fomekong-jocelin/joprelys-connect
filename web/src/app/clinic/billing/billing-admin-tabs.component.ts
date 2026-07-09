import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { InsuranceConvention, TariffGrid } from '../../patient/patient.models';
import { IconComponent } from '../../shared/ui/icon.component';

@Component({
  selector: 'app-billing-admin-tabs',
  standalone: true,
  imports: [CommonModule, FormsModule, IconComponent],
  template: `
    @if (activeTab === 'conventions') {
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="ui-card-subtle p-4 space-y-4">
          <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
            <app-ui-icon name="plus" /> {{ translate('billing.newConvention', 'Nouvelle Convention') }}
          </h3>

          <div class="space-y-3">
            <div>
              <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.insuranceName', "Nom de l'assurance") }} :</label>
              <input
                type="text"
                [(ngModel)]="newConventionName"
                [placeholder]="translate('billing.insuranceNamePlaceholder', 'Ex: AXA Cameroun')"
                class="ui-input text-xs w-full"
              />
            </div>
            <div>
              <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.coverageRate', 'Taux de couverture (0.0 à 1.0)') }} :</label>
              <input
                type="number"
                [(ngModel)]="newConventionRate"
                [placeholder]="translate('billing.coverageRatePlaceholder', 'Ex: 0.8')"
                min="0"
                max="1"
                step="0.05"
                class="ui-input text-xs w-full"
              />
            </div>
            <button (click)="submitConvention()" class="ui-button ui-button-primary w-full">
              <app-ui-icon name="handshake" />
              {{ translate('common.save', 'Enregistrer') }}
            </button>
          </div>
        </div>

        <div class="md:col-span-2 ui-card-subtle p-4 space-y-4">
          <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
            {{ translate('billing.activeConventions', 'Conventions Actives') }}
          </h3>

          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs border-collapse">
              <thead>
                <tr class="bg-[var(--app-surface-muted)] border-b border-[var(--app-border)] text-[10px] font-bold text-[var(--text-secondary)]">
                  <th class="p-2">{{ translate('billing.conventionName', 'Nom de la convention') }}</th>
                  <th class="p-2 text-right">{{ translate('billing.coveragePercentage', 'Taux de couverture') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--app-border)]/40">
                @for (c of conventions; track c.id) {
                  <tr class="hover:bg-[var(--app-surface-muted)]/30">
                    <td class="p-2 font-bold text-[var(--text-primary)]">{{ c.name }}</td>
                    <td class="p-2 text-right text-brand-cyan font-bold">{{ c.coveragePercentage * 100 }} %</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      </div>
    }

    @if (activeTab === 'tariffs') {
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="ui-card-subtle p-4 space-y-4">
          <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
            <app-ui-icon name="plus" /> {{ translate('billing.newTariff', 'Configurer Clé / Acte') }}
          </h3>

          <div class="space-y-3">
            <div>
              <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.tariffKey', 'Lettre Clé (ex: room standard, act, K)') }} :</label>
              <input
                type="text"
                [(ngModel)]="newTariffKey"
                [placeholder]="translate('billing.tariffKeyPlaceholder', 'Ex: ROOM_STANDARD, K, AMI')"
                class="ui-input text-xs w-full"
              />
            </div>
            <div>
              <label class="text-[10px] font-bold text-[var(--text-secondary)] block mb-1">{{ translate('billing.unitValue', 'Valeur Unitaire (FCFA)') }} :</label>
              <input
                type="number"
                [(ngModel)]="newTariffValue"
                [placeholder]="translate('billing.unitValuePlaceholder', 'Ex: 10000')"
                class="ui-input text-xs w-full"
              />
            </div>
            <button (click)="submitTariff()" class="ui-button ui-button-primary w-full">
              <app-ui-icon name="table-cells" />
              {{ translate('common.save', 'Enregistrer') }}
            </button>
          </div>
        </div>

        <div class="md:col-span-2 ui-card-subtle p-4 space-y-4">
          <h3 class="font-bold text-xs text-[var(--text-primary)] uppercase tracking-wider border-b border-[var(--app-border)]/40 pb-2">
            {{ translate('billing.currentTariffs', 'Grille Tarifaire Courante') }}
          </h3>

          <div class="overflow-x-auto">
            <table class="w-full text-left text-xs border-collapse">
              <thead>
                <tr class="bg-[var(--app-surface-muted)] border-b border-[var(--app-border)] text-[10px] font-bold text-[var(--text-secondary)]">
                  <th class="p-2">{{ translate('billing.tariffKeyLetter', 'Lettre Clé / Acte') }}</th>
                  <th class="p-2 text-right">{{ translate('billing.unitValue', 'Valeur Unitaire') }}</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-[var(--app-border)]/40">
                @for (t of tariffs; track t.id) {
                  <tr class="hover:bg-[var(--app-surface-muted)]/30">
                    <td class="p-2 font-mono text-xs font-bold text-[var(--text-primary)]">{{ t.keyLetter }}</td>
                    <td class="p-2 text-right font-bold text-emerald-600 dark:text-emerald-400">{{ t.unitValue | number:'1.0-0' }} FCFA</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        </div>
      </div>
    }
  `,
})
export class BillingAdminTabsComponent {
  @Input({ required: true }) activeTab!: 'billing' | 'conventions' | 'tariffs';
  @Input({ required: true }) conventions: InsuranceConvention[] = [];
  @Input({ required: true }) tariffs: TariffGrid[] = [];
  @Input({ required: true }) translate!: (key: string, defaultValue: string) => string;

  @Output() createConvention = new EventEmitter<{ name: string; rate: number }>();
  @Output() saveTariff = new EventEmitter<{ keyLetter: string; unitValue: number }>();

  newConventionName = '';
  newConventionRate: number | null = 0.8;
  newTariffKey = '';
  newTariffValue: number | null = null;

  submitConvention(): void {
    if (!this.newConventionName || this.newConventionRate === null) {
      return;
    }
    this.createConvention.emit({
      name: this.newConventionName,
      rate: this.newConventionRate,
    });
    this.newConventionName = '';
    this.newConventionRate = 0.8;
  }

  submitTariff(): void {
    if (!this.newTariffKey || this.newTariffValue === null) {
      return;
    }
    this.saveTariff.emit({
      keyLetter: this.newTariffKey,
      unitValue: this.newTariffValue,
    });
    this.newTariffKey = '';
    this.newTariffValue = null;
  }
}

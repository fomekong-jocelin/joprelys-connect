import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';

/** Paramètres financiers de l'établissement (ADR-20261017 D-13, D-17) et rappel des clés tarifaires attendues. */
@Component({
  selector: 'app-billing-settings-panel',
  standalone: true,
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="ui-card mb-6 space-y-3 p-4 sm:p-5" [attr.aria-label]="t('billing.settings.title')">
      <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('billing.settings.title') }}</h3>
      <p class="text-xs text-[var(--text-muted)]">{{ t('billing.settings.tariffKeysHint') }}</p>
      @if (loaded()) {
        <form class="grid gap-3 md:grid-cols-3" (submit)="save($event)">
          <label class="flex items-start gap-2 text-xs text-[var(--text-secondary)] md:col-span-2">
            <input type="checkbox" class="mt-0.5 h-4 w-4" name="labPaymentBeforeSampling" [(ngModel)]="labPaymentBeforeSampling" [disabled]="!canManage()" />
            <span>{{ t('billing.settings.labPaymentBeforeSampling') }}</span>
          </label>
          <label class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('billing.settings.creditNoteThreshold') }}
            <input class="ui-input mt-1" type="number" min="0" name="threshold" [(ngModel)]="creditNoteApprovalThreshold" [disabled]="!canManage()" />
          </label>
          @if (canManage()) {
            <div class="md:col-span-3 flex justify-end">
              <button type="submit" class="ui-button ui-button-primary" [disabled]="busy() || creditNoteApprovalThreshold < 0">{{ t('common.save') }}</button>
            </div>
          }
        </form>
      }
      @if (message()) { <p class="text-xs font-semibold text-[var(--brand-success-text)]" role="status">{{ message() }}</p> }
      @if (error()) { <p class="text-xs font-semibold text-[var(--brand-danger-text)]" role="alert">{{ error() }}</p> }
    </section>
  `,
})
export class BillingSettingsPanelComponent implements OnInit {
  private readonly billingApi = inject(BillingApiService);
  private readonly rbac = inject(RbacApiService);
  private readonly i18n = inject(I18nService);

  readonly loaded = signal(false);
  readonly busy = signal(false);
  readonly message = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  labPaymentBeforeSampling = false;
  creditNoteApprovalThreshold = 50000;

  t(key: string): string {
    return this.i18n.t(key);
  }

  canManage(): boolean {
    return this.rbac.hasPermission('BILLING_SETTINGS_MANAGE');
  }

  ngOnInit(): void {
    this.billingApi.getBillingSettings().subscribe({
      next: (settings) => {
        this.labPaymentBeforeSampling = settings.labPaymentBeforeSampling;
        this.creditNoteApprovalThreshold = settings.creditNoteApprovalThreshold;
        this.loaded.set(true);
      },
      error: () => this.loaded.set(false),
    });
  }

  save(event: Event): void {
    event.preventDefault();
    if (!this.canManage() || this.busy()) return;
    this.busy.set(true);
    this.billingApi.updateBillingSettings({
      labPaymentBeforeSampling: this.labPaymentBeforeSampling,
      creditNoteApprovalThreshold: this.creditNoteApprovalThreshold,
    }).subscribe({
      next: () => { this.busy.set(false); this.error.set(null); this.message.set(this.t('billing.settings.saved')); },
      error: (err) => { this.busy.set(false); this.message.set(null); this.error.set(err?.error?.detail || this.t('billing.settings.error')); },
    });
  }
}

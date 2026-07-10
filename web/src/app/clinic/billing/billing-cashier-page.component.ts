import { Component, inject } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { BillingCashRegisterComponent } from './billing-cash-register.component';

@Component({
  selector: 'app-billing-cashier-page',
  standalone: true,
  imports: [AppShellComponent, PageHeaderComponent, BillingCashRegisterComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('billing.cashierPage.title', 'Poste caissier')"
        [subtitle]="t('billing.cashierPage.subtitle', 'Traitez les règlements patient, suivez votre session et clôturez la caisse depuis un espace unique.')"
      ></app-page-header>
      <div class="app-container space-y-6 pb-12">
        <app-billing-cash-register></app-billing-cash-register>
      </div>
    </app-shell>
  `,
})
export class BillingCashierPageComponent {
  private readonly i18n = inject(I18nService);

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }
}

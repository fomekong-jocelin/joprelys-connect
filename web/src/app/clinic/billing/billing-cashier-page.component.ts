import { Component } from '@angular/core';
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
        title="Poste caissier"
        subtitle="Traitez les règlements patient, suivez votre session et clôturez la caisse depuis un espace unique."
      ></app-page-header>
      <div class="app-container space-y-6 pb-12">
        <app-billing-cash-register></app-billing-cash-register>
      </div>
    </app-shell>
  `,
})
export class BillingCashierPageComponent {}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BillingPaymentModalComponent } from './billing-payment-modal.component';

describe('BillingPaymentModalComponent', () => {
  let fixture: ComponentFixture<BillingPaymentModalComponent>;
  let component: BillingPaymentModalComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingPaymentModalComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingPaymentModalComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('translate', (_key: string, fallback: string) => fallback);
    fixture.componentRef.setInput('cashSessionOpen', true);
    fixture.componentRef.setInput('invoice', {
      id: 'invoice-2203',
      invoiceNumber: 'FAC-20260710-002203',
      patientShare: 15000,
    });
    fixture.componentRef.setInput('visible', true);
    fixture.detectChanges();
  });

  it('initializes the amount with the patient balance and exposes an accessible dialog', () => {
    expect(component.amount).toBe(15000);

    const dialog = (fixture.nativeElement as HTMLElement).querySelector('[role="dialog"]');
    expect(dialog).not.toBeNull();
    expect(dialog?.getAttribute('aria-modal')).toBe('true');
    expect(dialog?.getAttribute('aria-labelledby')).toBe('billing-payment-dialog-title');
  });

  it('blocks zero and overpayment amounts', () => {
    component.amount = 0;
    expect(component.canSubmit()).toBe(false);

    component.amount = 15001;
    expect(component.canSubmit()).toBe(false);

    component.amount = 15000;
    expect(component.canSubmit()).toBe(true);
  });

  it('requires and trims a reference for cheque payments', () => {
    component.method = 'CHECK';
    component.reference = '   ';
    expect(component.canSubmit()).toBe(false);

    component.reference = '  CHQ-2203  ';
    const submitSpy = vi.spyOn(component.submitPayment, 'emit');
    component.submit();

    expect(submitSpy).toHaveBeenCalledWith({
      amount: 15000,
      method: 'CHECK',
      reference: 'CHQ-2203',
    });
  });

  it('does not allow submission without an open cash session', () => {
    fixture.componentRef.setInput('cashSessionOpen', false);
    fixture.detectChanges();
    expect(component.canSubmit()).toBe(false);
  });

  it('closes with Escape when no payment is being saved', () => {
    const closeSpy = vi.spyOn(component.close, 'emit');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(closeSpy).toHaveBeenCalledOnce();
  });

  it('keeps the dialog open on Escape while saving', () => {
    fixture.componentRef.setInput('saving', true);
    fixture.detectChanges();
    const closeSpy = vi.spyOn(component.close, 'emit');

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(closeSpy).not.toHaveBeenCalled();
  });
});

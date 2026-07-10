import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { BillingApiService } from '../../patient/billing-api.service';
import { BillingEstimatesComponent } from './billing-estimates.component';

describe('BillingEstimatesComponent', () => {
  let component: BillingEstimatesComponent;
  let fixture: ComponentFixture<BillingEstimatesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingEstimatesComponent],
      providers: [
        {
          provide: BillingApiService,
          useValue: {
            listEstimates: vi.fn().mockReturnValue(of([])),
            listCreditNotes: vi.fn().mockReturnValue(of([])),
            getReceivablesByInvoice: vi.fn().mockReturnValue(of([]))
          }
        },
        {
          provide: I18nService,
          useValue: {
            t: vi.fn().mockImplementation((key: string) => key),
            locale: signal('fr')
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(BillingEstimatesComponent);
    component = fixture.componentInstance;
    component.patientId = 'patient-123';
    fixture.detectChanges();
  });

  it('shows a labelled remove action for every estimate line', () => {
    component.showEstimateForm.set(true);
    component.addEstimateItem();
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const removeButtons = Array.from(root.querySelectorAll<HTMLButtonElement>('.remove-line'));

    expect(removeButtons).toHaveLength(2);
    expect(removeButtons[0].textContent).toContain('Supprimer');
    expect(removeButtons[0].getAttribute('aria-label')).toBe('Supprimer cette ligne');
  });

  it('removes only the selected estimate line', () => {
    component.addEstimateItem();

    component.removeEstimateItem(0);

    expect(component.newEstimateItems()).toHaveLength(1);
    expect(component.newEstimateItems()[0].itemType).toBe('CONSULTATION');
  });
});

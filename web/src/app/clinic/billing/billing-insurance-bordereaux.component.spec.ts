import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { BillingApiService } from '../../patient/billing-api.service';
import { BillingInsuranceBordereauxComponent } from './billing-insurance-bordereaux.component';

describe('BillingInsuranceBordereauxComponent', () => {
  let fixture: ComponentFixture<BillingInsuranceBordereauxComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BillingInsuranceBordereauxComponent],
      providers: [{
        provide: BillingApiService,
        useValue: {
          listConventions: vi.fn().mockReturnValue(of([])),
          listInsuranceBordereaux: vi.fn().mockReturnValue(of([])),
        },
      }],
    }).compileComponents();

    fixture = TestBed.createComponent(BillingInsuranceBordereauxComponent);
    fixture.detectChanges();
  });

  it('keeps the list title and status filter compact on tablet and desktop', () => {
    const root = fixture.nativeElement as HTMLElement;
    const header = root.querySelector<HTMLElement>('.bordereaux-list-header');
    const title = root.querySelector<HTMLElement>('.bordereaux-list-title');
    const filter = root.querySelector<HTMLSelectElement>('.bordereaux-status-filter');

    expect(header?.classList).toContain('flex-col');
    expect(header?.classList).toContain('sm:flex-row');
    expect(title?.classList).toContain('whitespace-nowrap');
    expect(filter?.classList).toContain('w-full');
    expect(filter?.classList).toContain('sm:w-40');
    expect(filter?.classList).toContain('sm:shrink-0');
  });
});

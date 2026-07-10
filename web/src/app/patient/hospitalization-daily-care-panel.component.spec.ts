import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationDailyCarePanelComponent } from './hospitalization-daily-care-panel.component';

describe('HospitalizationDailyCarePanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationDailyCarePanelComponent>;
  let api: { getDailyCares: ReturnType<typeof vi.fn>; addDailyCare: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = {
      getDailyCares: vi.fn().mockReturnValue(of([{ id: 'care-1', careType: 'PANSEMENT', description: 'Pansement refait', billable: false, performedBy: 'Infirmier', performedAt: '2026-07-10T08:00:00Z' }])),
      addDailyCare: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [HospitalizationDailyCarePanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(HospitalizationDailyCarePanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
  });

  it('shows the care trace returned for the stay', () => {
    expect(api.getDailyCares).toHaveBeenCalledWith('stay-1');
    expect(fixture.nativeElement.textContent).toContain('Pansement refait');
    expect(fixture.nativeElement.textContent).toContain('Infirmier');
  });

  it('sends a billable care through the existing API', () => {
    fixture.componentInstance.billable.set(true);
    fixture.componentInstance.price.set(1500);
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addDailyCare).toHaveBeenCalledWith('stay-1', expect.objectContaining({ billable: true, price: 1500 }));
  });
});

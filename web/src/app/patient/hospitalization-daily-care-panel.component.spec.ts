import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationDailyCarePanelComponent } from './hospitalization-daily-care-panel.component';

describe('HospitalizationDailyCarePanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationDailyCarePanelComponent>;
  let api: { getDailyCares: ReturnType<typeof vi.fn>; addDailyCare: ReturnType<typeof vi.fn> };
  let permissions: Set<string>;

  beforeEach(async () => {
    permissions = new Set(['HOSPITALIZATION_CARE_WRITE']);
    api = {
      getDailyCares: vi.fn().mockReturnValue(of([{ id: 'care-1', careType: 'PANSEMENT', description: 'Pansement refait', billable: false, performedBy: 'Infirmier', performedAt: '2026-07-10T08:00:00Z' }])),
      addDailyCare: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [HospitalizationDailyCarePanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: RbacApiService, useValue: { hasPermission: (permission: string) => permissions.has(permission) } },
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

  it('sends a billable care with the dedicated permission', () => {
    fixture.componentInstance.billable.set(true);
    fixture.componentInstance.price.set(1500);
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addDailyCare).toHaveBeenCalledWith('stay-1', expect.objectContaining({ billable: true, price: 1500 }));
  });

  it('blocks care creation without the dedicated permission', () => {
    permissions.clear();
    fixture.componentRef.setInput('canModify', false);
    fixture.detectChanges();
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addDailyCare).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).not.toContain('Ajouter un soin');
  });
});

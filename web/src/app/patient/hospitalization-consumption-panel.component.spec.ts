import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationConsumptionPanelComponent } from './hospitalization-consumption-panel.component';

describe('HospitalizationConsumptionPanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationConsumptionPanelComponent>;
  let api: { getPatientConsumptions: ReturnType<typeof vi.fn>; addPatientConsumption: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = {
      getPatientConsumptions: vi.fn().mockReturnValue(of([{ id: 'item-1', itemName: 'Compresse', quantity: 2, unitPrice: 500, consumedBy: 'Infirmier', consumedAt: '2026-07-10T08:00:00Z' }])),
      addPatientConsumption: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({ imports: [HospitalizationConsumptionPanelComponent], providers: [{ provide: PatientApiService, useValue: api }, { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } }] }).compileComponents();
    fixture = TestBed.createComponent(HospitalizationConsumptionPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
  });

  it('shows consumed items for the active stay', () => {
    expect(api.getPatientConsumptions).toHaveBeenCalledWith('stay-1');
    expect(fixture.nativeElement.textContent).toContain('Compresse');
  });

  it('records a consumption through the existing backend API', () => {
    fixture.componentInstance.itemName.set('Seringue');
    fixture.componentInstance.quantity.set(3);
    fixture.componentInstance.save(new Event('submit'));
    expect(api.addPatientConsumption).toHaveBeenCalledWith('stay-1', expect.objectContaining({ itemName: 'Seringue', quantity: 3 }));
  });
});

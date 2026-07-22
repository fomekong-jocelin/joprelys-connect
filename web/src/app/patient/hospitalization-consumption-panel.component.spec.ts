import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationConsumptionPanelComponent } from './hospitalization-consumption-panel.component';

describe('HospitalizationConsumptionPanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationConsumptionPanelComponent>;
  let api: { getPatientConsumptions: ReturnType<typeof vi.fn>; addPatientConsumption: ReturnType<typeof vi.fn> };
  let permissions: Set<string>;

  beforeEach(async () => {
    permissions = new Set(['HOSPITALIZATION_CONSUMABLE_MANAGE']);
    api = {
      getPatientConsumptions: vi.fn().mockReturnValue(of([{ id: 'item-1', itemName: 'Compresse', quantity: 2, unitPrice: 500, consumedBy: 'Infirmier', consumedAt: '2026-07-10T08:00:00Z' }])),
      addPatientConsumption: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [HospitalizationConsumptionPanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: RbacApiService, useValue: { hasPermission: (permission: string) => permissions.has(permission) } },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(HospitalizationConsumptionPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
  });

  it('shows consumed items for the active stay', () => {
    expect(api.getPatientConsumptions).toHaveBeenCalledWith('stay-1');
    expect(fixture.nativeElement.textContent).toContain('Compresse');
  });

  it('records a consumption with the dedicated permission', () => {
    fixture.componentInstance.itemName.set('Seringue');
    fixture.componentInstance.quantity.set(3);
    fixture.componentInstance.save(new Event('submit'));
    expect(api.addPatientConsumption).toHaveBeenCalledWith('stay-1', expect.objectContaining({ itemName: 'Seringue', quantity: 3 }));
  });

  it('blocks consumption creation without the dedicated permission', () => {
    permissions.clear();
    fixture.detectChanges();
    fixture.componentInstance.itemName.set('Seringue');
    fixture.componentInstance.quantity.set(3);
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addPatientConsumption).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).not.toContain('Ajouter un article');
  });
});

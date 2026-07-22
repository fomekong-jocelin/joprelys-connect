import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationMedicationPanelComponent } from './hospitalization-medication-panel.component';

describe('HospitalizationMedicationPanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationMedicationPanelComponent>;
  let api: { getMedicationAdministrations: ReturnType<typeof vi.fn>; addMedicationAdministration: ReturnType<typeof vi.fn> };
  let permissions: Set<string>;

  beforeEach(async () => {
    permissions = new Set(['HOSPITALIZATION_MEDICATION_ADMINISTER']);
    api = {
      getMedicationAdministrations: vi.fn().mockReturnValue(of([{ id: 'med-1', medicationName: 'Paracétamol', dose: '1 g', administeredBy: 'Infirmier', administeredAt: '2026-07-10T08:00:00Z' }])),
      addMedicationAdministration: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [HospitalizationMedicationPanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: RbacApiService, useValue: { hasPermission: (permission: string) => permissions.has(permission) } },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(HospitalizationMedicationPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
  });

  it('displays the administration trace for the stay', () => {
    expect(api.getMedicationAdministrations).toHaveBeenCalledWith('stay-1');
    expect(fixture.nativeElement.textContent).toContain('Paracétamol');
    expect(fixture.nativeElement.textContent).toContain('Infirmier');
  });

  it('records administration with the dedicated permission', () => {
    fixture.componentInstance.name.set('Amoxicilline');
    fixture.componentInstance.dose.set('500 mg');
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addMedicationAdministration).toHaveBeenCalledWith('stay-1', expect.objectContaining({ medicationName: 'Amoxicilline', dose: '500 mg', prescriptionItemId: null }));
  });

  it('does not turn prescription access into administration access', () => {
    permissions.clear();
    permissions.add('PHARMACY_PRESCRIPTION_READ');
    fixture.componentRef.setInput('canModify', false);
    fixture.detectChanges();
    fixture.componentInstance.name.set('Amoxicilline');
    fixture.componentInstance.dose.set('500 mg');
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addMedicationAdministration).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).not.toContain('Administrer');
  });
});

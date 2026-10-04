import { readFileSync } from 'node:fs';
const dictionary: Record<string, string> = JSON.parse(readFileSync('src/assets/i18n/features/hospital-continuity/fr.json', 'utf8'));
import { HospitalizationLocationApiService } from './hospitalization-location-api.service';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationMedicationPanelComponent } from './hospitalization-medication-panel.component';

describe('HospitalizationMedicationPanelComponent', () => {
  let fixture: ComponentFixture<HospitalizationMedicationPanelComponent>;
  let api: { getMedicationAdministrations: ReturnType<typeof vi.fn>; addMedicationAdministration: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = {
      getMedicationAdministrations: vi.fn().mockReturnValue(of([{ id: 'med-1', medicationName: 'Paracétamol', dose: '1 g', administeredBy: 'Infirmier', administeredAt: '2026-07-10T08:00:00Z' }])),
      addMedicationAdministration: vi.fn().mockReturnValue(of({})),
    };
    await TestBed.configureTestingModule({
      imports: [HospitalizationMedicationPanelComponent],
      providers: [{ provide: HospitalizationLocationApiService, useValue: { eligibleMedications: () => of([{ prescriptionItemId: 'item-1', medicationName: 'Amoxicilline', dosage: '500 mg', prescriptionNumber: 'RX-1' }]) } }, { provide: PatientApiService, useValue: api }, { provide: I18nService, useValue: { t: (key: string, fallback?: string) => dictionary[key] ?? fallback ?? key } }],
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

  it('keeps the backend as the source of truth when recording administration', () => {
    fixture.componentInstance.prescriptionItemId.set('item-1');
    fixture.componentInstance.dose.set('500 mg');
    fixture.componentInstance.save(new Event('submit'));

    expect(api.addMedicationAdministration).toHaveBeenCalledWith('stay-1', expect.objectContaining({ prescriptionItemId: 'item-1', medicationName: 'Amoxicilline', dose: '500 mg' }));
  });
  it('rejects a free entry without an eligible prescription', () => {
    fixture.componentInstance.prescriptionItemId.set('unknown');
    fixture.componentInstance.dose.set('500 mg');
    fixture.componentInstance.save(new Event('submit'));
    expect(api.addMedicationAdministration).not.toHaveBeenCalled();
  });
  it('does not send a second request while the first is pending', () => {
    api.addMedicationAdministration.mockReturnValue(new Subject());
    fixture.componentInstance.prescriptionItemId.set('item-1');
    fixture.componentInstance.dose.set('500 mg');
    fixture.componentInstance.save(new Event('submit'));
    fixture.componentInstance.save(new Event('submit'));
    expect(api.addMedicationAdministration).toHaveBeenCalledOnce();
  });
  it('does not administer with read-only rights', () => {
    fixture.componentRef.setInput('canModify', false);
    fixture.componentInstance.prescriptionItemId.set('item-1');
    fixture.componentInstance.dose.set('500 mg');
    fixture.componentInstance.save(new Event('submit'));
    expect(api.addMedicationAdministration).not.toHaveBeenCalled();
  });
});

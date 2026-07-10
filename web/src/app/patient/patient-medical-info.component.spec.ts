import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientMedicalInfoComponent } from './patient-medical-info.component';
import { PatientApiService } from './patient-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { EmergencyApiService } from '../emergency/emergency-api.service';

describe('PatientMedicalInfoComponent', () => {
  let component: PatientMedicalInfoComponent;
  let fixture: ComponentFixture<PatientMedicalInfoComponent>;
  let mockApi: any;
  let mockEmergencyApi: any;
  let mockI18n: any;

  beforeEach(async () => {
    mockApi = {
      getAllergies: vi.fn().mockReturnValue(of([])),
      addAllergy: vi.fn().mockReturnValue(of({ id: 'all-1', substance: 'Pénicilline', severity: 'HIGH', status: 'ACTIVE' })),
      deleteAllergy: vi.fn().mockReturnValue(of(null)),
      getMedicalHistory: vi.fn().mockReturnValue(of([])),
      addMedicalHistory: vi.fn().mockReturnValue(of({ id: 'hist-1', category: 'MEDICAL', description: 'Diabète', isOngoing: true, important: true })),
      deleteMedicalHistory: vi.fn().mockReturnValue(of(null)),
      updateMedicalHistory: vi.fn().mockReturnValue(of({ id: 'hist-1', category: 'MEDICAL', description: 'Diabète', isOngoing: false })),
      getVaccinations: vi.fn().mockReturnValue(of([]))
    };
    mockEmergencyApi = {
      getPatientEmergencies: vi.fn().mockReturnValue(of([]))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key: string) => ({
        'patients.medicalInfo.emergencies.title': 'Passages aux urgences',
        'patients.medicalInfo.emergencies.empty': 'Aucun passage aux urgences enregistré pour ce patient.'
      } as Record<string, string>)[key] ?? key),
      locale: signal('fr')
    };

    await TestBed.configureTestingModule({
      imports: [PatientMedicalInfoComponent],
      providers: [
        { provide: PatientApiService, useValue: mockApi },
        { provide: EmergencyApiService, useValue: mockEmergencyApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientMedicalInfoComponent);
    component = fixture.componentInstance;
    component.patientId = 'patient-123';
    fixture.detectChanges();
  });

  it('should initialize and load patient data', () => {
    expect(mockApi.getAllergies).toHaveBeenCalledWith('patient-123');
    expect(mockApi.getMedicalHistory).toHaveBeenCalledWith('patient-123');
    expect(mockApi.getVaccinations).toHaveBeenCalledWith('patient-123');
    expect(mockEmergencyApi.getPatientEmergencies).toHaveBeenCalledWith('patient-123');
  });

  it('uses shared SVG icons and resolved emergency translations', () => {
    const root = fixture.nativeElement as HTMLElement;
    const visibleText = root.textContent ?? '';

    expect(root.querySelectorAll('app-ui-icon').length).toBeGreaterThanOrEqual(4);
    expect(visibleText).toContain('Passages aux urgences');
    expect(visibleText).toContain('Aucun passage aux urgences enregistré pour ce patient.');
    expect(visibleText).not.toContain('patients.medicalInfo.emergencies.title');
    expect(visibleText).not.toMatch(/[🛡📋💉🚨💚⚡]/u);
  });

  it('should add allergy and reload the allergies list', () => {
    component.openAllergyModal();
    component.allergySubstance = 'Pénicilline';
    component.allergySeverity = 'HIGH';
    component.allergyReaction = 'Choc';
    component.allergyComment = 'Grave';

    const event = new Event('submit');
    component.saveAllergy(event);

    expect(mockApi.addAllergy).toHaveBeenCalledWith('patient-123', {
      substance: 'Pénicilline',
      severity: 'HIGH',
      reaction: 'Choc',
      comment: 'Grave',
      status: 'ACTIVE'
    });
    expect(component.showAllergyModal()).toBe(false);
  });

  it('should soft-delete allergy when confirmed', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    const mockAllergy = { id: 'all-1', substance: 'Pénicilline', severity: 'HIGH' as const, status: 'ACTIVE' as const, patientId: 'patient-123' };
    component.deleteAllergy(mockAllergy);

    expect(mockApi.deleteAllergy).toHaveBeenCalledWith('patient-123', 'all-1');
  });

  it('should add medical history with important flag', () => {
    component.openHistoryModal();
    component.historyCategory = 'MEDICAL';
    component.historyDescription = 'Diabète type 2';
    component.historyIsOngoing = true;
    component.historyIsImportant = true;

    const event = new Event('submit');
    component.saveHistory(event);

    expect(mockApi.addMedicalHistory).toHaveBeenCalledWith('patient-123', {
      category: 'MEDICAL',
      description: 'Diabète type 2',
      onsetDate: undefined,
      isOngoing: true,
      comment: undefined,
      important: true
    });
    expect(component.showHistoryModal()).toBe(false);
  });

  it('should soft-delete medical history when confirmed', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);

    const mockHistory = { id: 'hist-1', patientId: 'patient-123', category: 'MEDICAL' as const, description: 'Diabète', isOngoing: true, important: true };
    component.deleteHistory(mockHistory);

    expect(mockApi.deleteMedicalHistory).toHaveBeenCalledWith('patient-123', 'hist-1');
  });
});

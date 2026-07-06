import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DuplicatesPageComponent } from './duplicates-page.component';
import { PatientApiService } from '../../patient/patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';

describe('DuplicatesPageComponent', () => {
  let component: DuplicatesPageComponent;
  let fixture: ComponentFixture<DuplicatesPageComponent>;
  let mockPatientApi: any;
  let mockI18n: any;

  const mockCandidates = [
    {
      id: 'candidate-1',
      similarityScore: 92.5,
      sourcePatient: {
        id: 'patient-a',
        fullName: 'Jean Dupuy',
        birthDate: '1980-05-12T00:00:00Z',
        phone: '+237677777777',
        globalPatientNumber: 'DPU-A'
      },
      targetPatient: {
        id: 'patient-b',
        fullName: 'Jean Dupui',
        birthDate: '1980-05-12T00:00:00Z',
        phone: '+237677777777',
        globalPatientNumber: 'DPU-B'
      }
    }
  ];

  beforeEach(async () => {
    mockPatientApi = {
      getDuplicates: vi.fn().mockReturnValue(of(mockCandidates)),
      ignoreDuplicate: vi.fn().mockReturnValue(of(null)),
      mergePatients: vi.fn().mockReturnValue(of(null))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key),
      locale: signal('fr')
    };

    await TestBed.configureTestingModule({
      imports: [DuplicatesPageComponent],
      providers: [
        provideRouter([]),
        { provide: PatientApiService, useValue: mockPatientApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DuplicatesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load duplicate candidates on init', () => {
    expect(mockPatientApi.getDuplicates).toHaveBeenCalled();
    expect(component.isLoading()).toBe(false);
    expect(component.candidates()).toEqual(mockCandidates as any);
  });

  it('should handle merge assistant modals', () => {
    expect(component.selectedCandidate()).toBeNull();
    component.openMergeAssistant(mockCandidates[0] as any);
    expect(component.selectedCandidate()).toEqual(mockCandidates[0] as any);
    expect(component.selectedPrimaryId()).toBe('patient-a');

    component.closeMergeAssistant();
    expect(component.selectedCandidate()).toBeNull();
  });

  it('should ignore duplicate candidate and reload', () => {
    component.ignoreCandidate(mockCandidates[0] as any);
    expect(mockPatientApi.ignoreDuplicate).toHaveBeenCalledWith('candidate-1');
    expect(mockPatientApi.getDuplicates).toHaveBeenCalledTimes(2);
  });

  it('should merge duplicate candidates and reload', () => {
    component.openMergeAssistant(mockCandidates[0] as any);
    component.confirmMerge();
    expect(mockPatientApi.mergePatients).toHaveBeenCalledWith('patient-a', 'patient-b');
    expect(component.selectedCandidate()).toBeNull();
    expect(mockPatientApi.getDuplicates).toHaveBeenCalledTimes(2);
  });
});

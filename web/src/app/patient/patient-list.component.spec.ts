import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Observable, of } from 'rxjs';
import { PatientApiService } from './patient-api.service';
import { PatientListComponent } from './patient-list.component';
import { Patient } from './patient.models';

describe('PatientListComponent', () => {
  let component: PatientListComponent;
  let fixture: ComponentFixture<PatientListComponent>;
  let router: Router;
  let mockApi: {
    list: ReturnType<typeof vi.fn>;
    getById: ReturnType<typeof vi.fn>;
    triggerEmergencyAccess: ReturnType<typeof vi.fn>;
  };

  const mockPatients: Patient[] = [
    {
      id: '1',
      organizationId: 'org1',
      globalPatientNumber: 'DPU-JOP-20260702-000001',
      localPatientNumber: 'PAT-20260702-000001',
      fullName: 'Jean Dupont',
      gender: 'MASCULIN',
      birthDate: '1990-05-15',
      phone: '+237699999999',
      city: 'Douala',
      status: 'ACTIVE',
      createdAt: '2026-07-02T12:00:00Z',
      updatedAt: '2026-07-02T12:00:00Z',
    },
  ];

  beforeEach(async () => {
    mockApi = {
      list: vi.fn().mockReturnValue(of(mockPatients)),
      getById: vi.fn().mockReturnValue(of(mockPatients[0])),
      triggerEmergencyAccess: vi.fn().mockReturnValue(of(void 0)),
    };

    await TestBed.configureTestingModule({
      imports: [PatientListComponent],
      providers: [
        provideRouter([
          { path: 'patients/:id', redirectTo: '' },
          { path: 'clinic/emergencies', redirectTo: '' },
        ]),
        { provide: PatientApiService, useValue: mockApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.useRealTimers();
    fixture.destroy();
  });

  it('should load patients on init', () => {
    expect(mockApi.list).toHaveBeenCalledWith('');
    expect(component.list()).toEqual(mockPatients);
    expect(component.loading()).toBe(false);
  });

  it('should keep the patient header close to the search workspace on mobile', () => {
    const headerSection = fixture.nativeElement.querySelector('app-page-header section');
    const workspaceContainer = fixture.nativeElement.querySelector('app-page-header + .app-container');

    expect(headerSection?.classList.contains('pb-2')).toBe(true);
    expect(workspaceContainer?.classList.contains('pt-2')).toBe(true);
  });

  it('should render search as the primary workspace control without a separate search button', () => {
    const workspace = fixture.nativeElement.querySelector('[data-testid="patient-workspace-actions"]');
    const search = fixture.nativeElement.querySelector('[data-testid="patient-search-input"]') as HTMLInputElement;

    expect(workspace).not.toBeNull();
    expect(search).not.toBeNull();
    expect(search.type).toBe('text');
    expect(search.getAttribute('inputmode')).toBe('search');
    expect(search.getAttribute('enterkeyhint')).toBe('search');
    expect(search.getAttribute('placeholder')).toBe('patients.searchPlaceholder');

    const renderedButtons = Array.from(workspace.querySelectorAll('button')) as HTMLButtonElement[];
    expect(renderedButtons.some(button => button.textContent?.includes('common.search'))).toBe(false);
  });

  it('should search progressively after a short debounce without requiring Enter', () => {
    vi.useFakeTimers();
    const initialCalls = mockApi.list.mock.calls.length;
    const search = fixture.nativeElement.querySelector('[data-testid="patient-search-input"]') as HTMLInputElement;

    search.value = 'Je';
    search.dispatchEvent(new Event('input', { bubbles: true }));

    expect(mockApi.list).toHaveBeenCalledTimes(initialCalls);

    vi.advanceTimersByTime(249);
    expect(mockApi.list).toHaveBeenCalledTimes(initialCalls);

    vi.advanceTimersByTime(1);
    expect(mockApi.list).toHaveBeenLastCalledWith('Je');
    expect(component.appliedSearchQuery()).toBe('Je');
  });

  it('should cancel the previous in-flight search as soon as the user keeps typing', () => {
    vi.useFakeTimers();
    const cancelled = vi.fn();
    mockApi.list.mockImplementation((query: string) => {
      if (query === 'J') {
        return new Observable<Patient[]>(() => () => cancelled());
      }
      return of(mockPatients);
    });

    const search = fixture.nativeElement.querySelector('[data-testid="patient-search-input"]') as HTMLInputElement;
    search.value = 'J';
    search.dispatchEvent(new Event('input', { bubbles: true }));
    vi.advanceTimersByTime(250);

    expect(mockApi.list).toHaveBeenLastCalledWith('J');
    expect(cancelled).not.toHaveBeenCalled();

    search.value = 'Je';
    search.dispatchEvent(new Event('input', { bubbles: true }));

    expect(cancelled).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(250);
    expect(mockApi.list).toHaveBeenLastCalledWith('Je');
  });

  it('should keep Enter compatible as an immediate search shortcut', () => {
    const loadSpy = vi.spyOn(component, 'load');
    const search = fixture.nativeElement.querySelector('[data-testid="patient-search-input"]') as HTMLInputElement;

    search.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', bubbles: true }));

    expect(loadSpy).toHaveBeenCalled();
  });

  it('should clear an active search and reload the unfiltered patient list', () => {
    component.searchQuery.set('Jean');
    component.load();
    fixture.detectChanges();

    expect(component.appliedSearchQuery()).toBe('Jean');
    expect(fixture.nativeElement.querySelector('[data-testid="patient-search-clear"]')).not.toBeNull();

    component.clearSearch();

    expect(component.searchQuery()).toBe('');
    expect(component.appliedSearchQuery()).toBe('');
    expect(mockApi.list).toHaveBeenLastCalledWith('');
  });

  it('should keep admission accessible next to the search workspace', () => {
    expect(component.showCreateForm()).toBe(false);
    const admissionAction = fixture.nativeElement.querySelector('[data-testid="patient-admission-action"]');

    expect(admissionAction).not.toBeNull();

    component.toggleCreateForm();

    expect(component.showCreateForm()).toBe(true);
  });

  it('should render each mobile patient as one compact clickable card without a nested view button', () => {
    const mobileList = fixture.nativeElement.querySelector('[data-testid="patient-mobile-list"]');
    const card = fixture.nativeElement.querySelector('[data-testid="patient-mobile-card"]') as HTMLButtonElement;

    expect(mobileList).not.toBeNull();
    expect(card).not.toBeNull();
    expect(card.tagName).toBe('BUTTON');
    expect(card.querySelector('app-ui-button')).toBeNull();
    expect(card.textContent).toContain('Jean Dupont');
    expect(card.textContent).toContain('DPU-JOP-20260702-000001');
    expect(card.textContent).toContain('+237699999999');
    expect(card.textContent).toContain('Douala');
  });

  it('should open the patient record when the mobile card is activated', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    const card = fixture.nativeElement.querySelector('[data-testid="patient-mobile-card"]') as HTMLButtonElement;

    card.click();

    expect(navigateSpy).toHaveBeenCalledWith(['/patients', '1'], { state: { patient: mockPatients[0] } });
  });

  it('should preserve the desktop comparison table and non-wrapping identifiers', () => {
    const table = fixture.nativeElement.querySelector('[data-testid="patient-desktop-table"]');
    const dpuCell = table?.querySelector('tbody td:nth-child(2)');
    const localCell = table?.querySelector('tbody td:nth-child(3)');

    expect(table).not.toBeNull();
    expect(dpuCell?.classList.contains('whitespace-nowrap')).toBe(true);
    expect(localCell?.classList.contains('whitespace-nowrap')).toBe(true);
  });

  it('should navigate to the patient record after a normal admission', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);
    component.showCreateForm.set(true);

    component.onAdmissionCompleted({
      carePath: 'NORMAL',
      patientId: '1',
      patientDisplayName: 'Jean Dupont',
      visitId: 'visit-1',
    });

    expect(component.showCreateForm()).toBe(false);
    expect(navigateSpy).toHaveBeenCalledWith(['/patients', '1']);
  });

  it('should navigate to emergency operations after an emergency admission', () => {
    const navigateSpy = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    component.onAdmissionCompleted({
      carePath: 'EMERGENCY',
      patientId: 'urg-temp-1',
      patientDisplayName: 'URG-TEMP-20260711-000001',
      emergencyId: 'emergency-1',
    });

    expect(navigateSpy).toHaveBeenCalledWith(['/clinic/emergencies']);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { Patient } from './patient.models';
import { PatientSearchPickerComponent } from './patient-search-picker.component';

describe('PatientSearchPickerComponent', () => {
  let fixture: ComponentFixture<PatientSearchPickerComponent>;
  let component: PatientSearchPickerComponent;
  let patientApi: { list: ReturnType<typeof vi.fn> };

  const homonyms = [
    { id: 'p1', fullName: 'NGO Marie', globalPatientNumber: 'DPU-1', birthDate: '1990-01-01', phone: '+237600000001' },
    { id: 'p2', fullName: 'NGO Marie', globalPatientNumber: 'DPU-2', birthDate: '2001-05-12', phone: '+237600000002' },
  ] as Patient[];

  beforeEach(async () => {
    vi.useFakeTimers();
    patientApi = { list: vi.fn().mockReturnValue(of(homonyms)) };
    await TestBed.configureTestingModule({
      imports: [PatientSearchPickerComponent],
      providers: [
        { provide: PatientApiService, useValue: patientApi },
        { provide: I18nService, useValue: { t: (key: string) => key } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PatientSearchPickerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => vi.useRealTimers());

  it('searches on the server after a short pause and shows what distinguishes homonyms', () => {
    component.onQuery('ngo');
    vi.advanceTimersByTime(300);
    fixture.detectChanges();

    expect(patientApi.list).toHaveBeenCalledWith('ngo');
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('DPU-1');
    expect(text).toContain('01/01/1990');
    expect(text).toContain('12/05/2001');
  });

  it('does not query the server for a single character', () => {
    component.onQuery('n');
    vi.advanceTimersByTime(300);

    expect(patientApi.list).not.toHaveBeenCalled();
  });

  it('emits the chosen patient', () => {
    const emitted: Array<Patient | null> = [];
    component.patientSelected.subscribe((patient) => emitted.push(patient));

    component.choose(homonyms[1]);

    expect(emitted).toEqual([homonyms[1]]);
  });
});

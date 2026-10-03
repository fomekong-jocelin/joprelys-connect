import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { Visit } from '../../visit/visit.models';
import { VisitApiService } from '../../visit/visit-api.service';
import { VisitDetailsDrawerComponent } from './visit-details-drawer.component';

describe('VisitDetailsDrawerComponent', () => {
  let fixture: ComponentFixture<VisitDetailsDrawerComponent>;
  let component: VisitDetailsDrawerComponent;

  const base: Visit = {
    id: 'visit-1',
    visitNumber: 'VIS-001',
    patientId: 'patient-1',
    patientName: 'Patient Test',
    patientDpu: 'DPU-001',
    reason: 'Fièvre',
    orientation: 'CONSULTATION',
    status: 'EN_COURS',
    createdAt: '2026-07-29T16:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VisitDetailsDrawerComponent],
      providers: [
        { provide: I18nService, useValue: { t: (key: string) => key } },
        { provide: VisitApiService, useValue: { getVitalsHistory: vi.fn().mockReturnValue(of([])) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(VisitDetailsDrawerComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('currentUserId', 'doctor-1');
    fixture.componentRef.setInput('canStartConsultation', true);
  });

  afterEach(() => vi.restoreAllMocks());

  it('offers to start the consultation for a patient ready for the doctor', () => {
    fixture.componentRef.setInput('visit', { ...base, careStage: 'PRET_MEDECIN' });
    fixture.detectChanges();

    expect(component.startLabel()).toBe('patient.detail.startConsultation');
    expect(component.heldByMe()).toBe(false);
  });

  it('lets the practitioner resume or release a patient they hold', () => {
    fixture.componentRef.setInput('visit', {
      ...base, careStage: 'EN_CONSULTATION', consultingPractitionerId: 'doctor-1', consultingPractitionerName: 'Dr Alpha',
    });
    fixture.detectChanges();

    expect(component.startLabel()).toBe('queue.action.resumeConsultation');
    expect(fixture.nativeElement.textContent).toContain('queue.action.release');
  });

  it('asks for confirmation before taking over a colleague patient', () => {
    fixture.componentRef.setInput('visit', {
      ...base, careStage: 'EN_CONSULTATION', consultingPractitionerId: 'doctor-2', consultingPractitionerName: 'Dr Beta',
    });
    fixture.detectChanges();
    const emitted: boolean[] = [];
    component.startConsultation.subscribe((takeOver) => emitted.push(takeOver));
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

    component.requestStart();

    expect(confirmSpy).toHaveBeenCalled();
    expect(emitted).toEqual([true]);
    expect(component.startLabel()).toBe('queue.action.takeOver');
  });
});

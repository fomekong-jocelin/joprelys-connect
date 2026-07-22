import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { HospitalizationStayHeaderComponent } from './hospitalization-stay-header.component';
import { PatientApiService } from './patient-api.service';
import { Hospitalization } from './patient.models';

describe('HospitalizationStayHeaderComponent', () => {
  let fixture: ComponentFixture<HospitalizationStayHeaderComponent>;
  let hasPermission: ReturnType<typeof vi.fn>;
  let confirmPhysicalDeparture: ReturnType<typeof vi.fn>;

  const stay: Hospitalization = {
    id: 'stay-1',
    patientId: 'patient-1',
    organizationId: 'organization-1',
    visitId: 'visit-1',
    responsiblePractitionerId: 'practitioner-1',
    serviceName: 'Médecine',
    roomNumber: '201',
    bedNumber: 'B',
    admissionReason: 'Surveillance clinique',
    status: 'EN_COURS',
    admittedAt: '2026-07-10T08:00:00Z',
    hospitalizationNumber: 'HOS-001',
    version: 0,
  };

  beforeEach(async () => {
    hasPermission = vi.fn((permission: string) => permission === 'HOSPITALIZATION_TRANSFER');
    confirmPhysicalDeparture = vi.fn(() => of({}));

    await TestBed.configureTestingModule({
      imports: [HospitalizationStayHeaderComponent],
      providers: [
        { provide: I18nService, useValue: { t: (key: string, fallback: string) => fallback || key } },
        { provide: RbacApiService, useValue: { hasPermission } },
        { provide: PatientApiService, useValue: { confirmPhysicalDeparture } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HospitalizationStayHeaderComponent);
    fixture.componentRef.setInput('stay', stay);
    fixture.componentRef.setInput('responsibleName', 'Dr. Mbarga');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
  });

  it('renders the clinical context before stay actions', () => {
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('HOS-001');
    expect(text).toContain('Médecine');
    expect(text).toContain('Dr. Mbarga');
    expect(text).toContain('Surveillance clinique');
  });

  it('shows transfer independently from discharge permission', () => {
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Transférer');
    expect(text).not.toContain('Décider la sortie médicale');
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_TRANSFER');
    expect(hasPermission).toHaveBeenCalledWith('HOSPITALIZATION_DISCHARGE_DECIDE');
  });

  it('emits the requested authorized action', () => {
    const transfer = vi.fn();
    fixture.componentInstance.transfer.subscribe(transfer);

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    buttons.find((button) => button.textContent?.includes('Transférer'))?.click();

    expect(transfer).toHaveBeenCalledOnce();
  });

  it('shows only physical departure after the medical decision', () => {
    hasPermission.mockImplementation(
      (permission: string) => permission === 'HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM',
    );
    fixture.componentRef.setInput('stay', {
      ...stay,
      dischargeDecidedAt: '2026-07-11T10:00:00Z',
      dischargeDiagnosis: 'État stabilisé',
      dischargeInstructions: 'Suivi ambulatoire',
    });
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Sortie médicale décidée');
    expect(text).toContain('Patient encore présent');
    expect(text).toContain('Confirmer le départ physique');
    expect(text).not.toContain('Transférer de lit');
    expect(text).not.toContain('Décider la sortie médicale');
  });

  it('requires explicit confirmation before calling the physical departure API', () => {
    hasPermission.mockImplementation(
      (permission: string) => permission === 'HOSPITALIZATION_PHYSICAL_DEPARTURE_CONFIRM',
    );
    fixture.componentRef.setInput('stay', {
      ...stay,
      dischargeDecidedAt: '2026-07-11T10:00:00Z',
    });
    fixture.detectChanges();

    fixture.componentInstance.openPhysicalDepartureDialog();
    fixture.detectChanges();

    const submit = Array.from(fixture.nativeElement.querySelectorAll('button'))
      .find((button: HTMLButtonElement) => button.type === 'submit') as HTMLButtonElement;
    expect(submit.disabled).toBe(true);
    expect(confirmPhysicalDeparture).not.toHaveBeenCalled();
  });
});

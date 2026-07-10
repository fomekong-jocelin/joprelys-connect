import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import { HospitalizationStayHeaderComponent } from './hospitalization-stay-header.component';
import { Hospitalization } from './patient.models';

describe('HospitalizationStayHeaderComponent', () => {
  let fixture: ComponentFixture<HospitalizationStayHeaderComponent>;

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
    await TestBed.configureTestingModule({
      imports: [HospitalizationStayHeaderComponent],
      providers: [{ provide: I18nService, useValue: { t: (key: string, fallback: string) => fallback || key } }],
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

  it('emits the requested action', () => {
    const transfer = vi.fn();
    fixture.componentInstance.transfer.subscribe(transfer);

    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    buttons.find((button) => button.textContent?.includes('Transférer'))?.click();

    expect(transfer).toHaveBeenCalledOnce();
  });
});

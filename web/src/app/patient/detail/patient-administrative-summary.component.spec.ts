import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { Patient } from '../patient.models';
import { PatientAdministrativeSummaryComponent } from './patient-administrative-summary.component';

describe('PatientAdministrativeSummaryComponent mobile layout', () => {
  let fixture: ComponentFixture<PatientAdministrativeSummaryComponent>;

  const patient: Patient = {
    id: 'patient-1',
    organizationId: 'org-1',
    globalPatientNumber: 'DPU-001',
    localPatientNumber: 'PAT-001',
    fullName: 'Jocelin FOMEKONG',
    gender: 'MASCULIN',
    birthDate: '1987-06-29',
    phone: '+237123456789',
    email: 'jocelin.fomekong@indyli-services.com',
    city: 'Douala',
    district: 'Bonapriso',
    address: '123 100 rue',
    bloodGroup: 'O+',
    emergencyContactName: 'Contact Test',
    emergencyContactPhone: '+237600000000',
    status: 'ACTIVE',
    identityStatus: 'VERIFIED',
    createdAt: '2026-07-29T00:00:00Z',
    updatedAt: '2026-07-29T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientAdministrativeSummaryComponent],
      providers: [
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: signal<'fr' | 'en'>('fr'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientAdministrativeSummaryComponent);
    fixture.componentRef.setInput('patient', patient);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('uses one aligned information sheet instead of a mosaic of muted cards', () => {
    const sheet = fixture.nativeElement.querySelector('[data-testid="patient-identity-information-sheet"]');

    expect(sheet).not.toBeNull();
    expect(sheet.querySelectorAll('.ui-card-muted').length).toBe(0);
  });

  it('preserves long identity values without truncation', () => {
    expect(fixture.nativeElement.textContent).toContain('jocelin.fomekong@indyli-services.com');
    expect(fixture.nativeElement.textContent).toContain('123 100 rue');

    const anywhereValues = fixture.nativeElement.querySelectorAll('[class*="overflow-wrap:anywhere"]');
    expect(anywhereValues.length).toBeGreaterThanOrEqual(2);
  });

  it('keeps the emergency contact folded by default and independently expandable', () => {
    expect(fixture.componentInstance.contactExpanded()).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('Contact Test');

    fixture.componentInstance.toggleContact();
    fixture.detectChanges();

    expect(fixture.componentInstance.contactExpanded()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Contact Test');
    expect(fixture.nativeElement.textContent).toContain('+237600000000');
  });

  it('formats birth date and age as one compact value', () => {
    expect(fixture.componentInstance.birthDateLabel('1987-06-29')).toContain('29/06/1987 ·');
  });
});

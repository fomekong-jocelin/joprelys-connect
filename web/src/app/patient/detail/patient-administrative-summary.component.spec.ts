import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../../core/i18n/i18n.service';
import { Patient } from '../patient.models';
import { PatientAdministrativeSummaryComponent } from './patient-administrative-summary.component';

describe('PatientAdministrativeSummaryComponent disclosure', () => {
  let fixture: ComponentFixture<PatientAdministrativeSummaryComponent>;

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
    fixture.componentRef.setInput('patient', {
      id: 'patient-1',
      fullName: 'Jocelin FOMEKONG',
      gender: 'MASCULIN',
      birthDate: '1987-06-29',
      city: 'Douala',
      status: 'ACTIVE',
      identityStatus: 'VERIFIED',
      emergencyContactName: 'Contact Test',
      emergencyContactPhone: '+237600000000',
    } as Patient);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('should keep emergency contact details folded by default', () => {
    expect(fixture.componentInstance.contactExpanded()).toBe(false);
    expect(fixture.nativeElement.textContent).not.toContain('Contact Test');
  });

  it('should reveal emergency contact details only after an explicit action', () => {
    fixture.componentInstance.toggleContact();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Contact Test');
  });
});

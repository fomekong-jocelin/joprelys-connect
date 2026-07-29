import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { PatientRecordNavigationComponent } from './patient-record-navigation.component';

describe('PatientRecordNavigationComponent', () => {
  let fixture: ComponentFixture<PatientRecordNavigationComponent>;
  let component: PatientRecordNavigationComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PatientRecordNavigationComponent],
      providers: [
        provideRouter([]),
        {
          provide: I18nService,
          useValue: {
            t: (key: string) => key,
            locale: signal('fr'),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PatientRecordNavigationComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('patientId', 'patient-1');
    fixture.componentRef.setInput('canViewConsultations', true);
    fixture.componentRef.setInput('canViewLabOrders', false);
    fixture.componentRef.setInput('canViewHospitalizations', true);
    fixture.componentRef.setInput('canViewAudit', false);
    fixture.detectChanges();
  });

  afterEach(() => fixture.destroy());

  it('shows only sections allowed by effective permissions', () => {
    expect(component.sections().map(section => section.segment)).toEqual([
      'profile',
      'consultations',
      'hospitalizations',
    ]);
  });

  it('uses a compact mobile section picker instead of a horizontal tab rail', () => {
    const mobileButton = fixture.nativeElement.querySelector('.md\\:hidden button');
    expect(mobileButton).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.overflow-x-auto')).toBeNull();
  });

  it('closes the mobile picker after navigation selection', () => {
    component.mobileOpen.set(true);
    component.closeMobile();
    expect(component.mobileOpen()).toBe(false);
  });
});

import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientSelfRegistrationComponent } from './patient-self-registration.component';
import { PatientApiService } from '../patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';

describe('PatientSelfRegistrationComponent', () => {
  let component: PatientSelfRegistrationComponent;
  let fixture: ComponentFixture<PatientSelfRegistrationComponent>;
  let mockPatientApi: any;
  let mockI18n: any;
  let queryParamsSubject: any;

  beforeEach(async () => {
    mockPatientApi = {
      getPublicCaptcha: vi.fn().mockReturnValue(of({
        captchaId: 'captcha-123',
        question: 'Quelle est la température corporelle normale moyenne ?'
      })),
      submitPublicPreRegistration: vi.fn().mockReturnValue(of({
        id: 'prereg-123',
        status: 'AWAITING_VALIDATION'
      }))
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key),
      locale: signal('fr'),
      toggle: vi.fn()
    };

    // Mock activated route query params
    queryParamsSubject = of({ orgId: '3ffc3039-8213-4613-975a-39503dc0ce5d' });

    await TestBed.configureTestingModule({
      imports: [PatientSelfRegistrationComponent],
      providers: [
        { provide: PatientApiService, useValue: mockPatientApi },
        { provide: I18nService, useValue: mockI18n },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: queryParamsSubject
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PatientSelfRegistrationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load captcha if orgId query param is present', () => {
    expect(component).toBeTruthy();
    expect(component.organizationId()).toBe('3ffc3039-8213-4613-975a-39503dc0ce5d');
    expect(component.organizationIdMissing()).toBe(false);
    expect(mockPatientApi.getPublicCaptcha).toHaveBeenCalled();
    expect(component.captchaId()).toBe('captcha-123');
    expect(component.captchaQuestion()).toBe('Quelle est la température corporelle normale moyenne ?');
  });

  it('should display error if orgId query param is missing or invalid', () => {
    // Recreate with invalid route
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [PatientSelfRegistrationComponent],
      providers: [
        { provide: PatientApiService, useValue: mockPatientApi },
        { provide: I18nService, useValue: mockI18n },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ orgId: 'invalid-uuid-format' })
          }
        }
      ]
    });
    
    const newFixture = TestBed.createComponent(PatientSelfRegistrationComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.organizationIdMissing()).toBe(true);
  });

  it('should toggle language when toggleLanguage is called', () => {
    component.toggleLanguage();
    expect(mockI18n.toggle).toHaveBeenCalled();
  });

  it('should validate form and show error if required fields are missing', () => {
    component.firstName.set('');
    component.lastName.set('Doe');
    component.submitForm();
    expect(component.errorMessage()).toBe('selfRegistration.errorRequired');
    expect(mockPatientApi.submitPublicPreRegistration).not.toHaveBeenCalled();
  });

  it('should validate form and show error if captcha is missing', () => {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.gender.set('MASCULIN');
    component.birthDate.set('1990-01-15');
    component.captchaAnswer.set('');
    component.submitForm();
    expect(component.errorMessage()).toBe('selfRegistration.errorCaptcha');
    expect(mockPatientApi.submitPublicPreRegistration).not.toHaveBeenCalled();
  });

  it('should submit successfully with correct values', () => {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.gender.set('MASCULIN');
    component.birthDate.set('1990-01-15');
    component.phone.set('+237677123456');
    component.captchaAnswer.set('37');

    component.submitForm();

    expect(component.errorMessage()).toBeNull();
    expect(mockPatientApi.submitPublicPreRegistration).toHaveBeenCalledWith({
      organizationId: '3ffc3039-8213-4613-975a-39503dc0ce5d',
      firstName: 'John',
      lastName: 'Doe',
      gender: 'MASCULIN',
      birthDate: '1990-01-15',
      phone: '+237677123456',
      email: undefined,
      address: undefined,
      bloodGroup: undefined,
      emergencyContactName: undefined,
      emergencyContactPhone: undefined,
      emergencyContactRelation: undefined,
      captchaId: 'captcha-123',
      captchaAnswer: '37'
    });
    expect(component.registrationSuccess()).toBe(true);
  });

  it('should handle API submission error and reload captcha', () => {
    mockPatientApi.submitPublicPreRegistration.mockReturnValue(throwError(() => ({
      status: 400,
      error: { detail: 'Incorrect captcha answer' }
    })));

    component.firstName.set('John');
    component.lastName.set('Doe');
    component.gender.set('MASCULIN');
    component.birthDate.set('1990-01-15');
    component.captchaAnswer.set('wrong');

    // Reset calls count
    mockPatientApi.getPublicCaptcha.mockClear();

    component.submitForm();

    expect(component.registrationSuccess()).toBe(false);
    expect(component.errorMessage()).toBe('Incorrect captcha answer');
    expect(mockPatientApi.getPublicCaptcha).toHaveBeenCalled(); // reload captcha called
  });
});

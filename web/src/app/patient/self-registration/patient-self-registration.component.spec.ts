import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PatientSelfRegistrationComponent } from './patient-self-registration.component';
import { PatientApiService } from '../patient-api.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { signal } from '@angular/core';

describe('PatientSelfRegistrationComponent', () => {
  let component: PatientSelfRegistrationComponent;
  let fixture: ComponentFixture<PatientSelfRegistrationComponent>;
  let mockPatientApi: any;
  let mockI18n: any;
  let queryParamsSubject: any;

  const fillRequiredIdentity = (): void => {
    component.firstName.set('John');
    component.lastName.set('Doe');
    component.gender.set('MASCULIN');
    component.birthDate.set('1990-01-15');
  };

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
      setLocale: vi.fn().mockResolvedValue(undefined)
    };

    // Mock activated route query params
    queryParamsSubject = of({ orgId: '3ffc3039-8213-4613-975a-39503dc0ce5d' });

    await TestBed.configureTestingModule({
      imports: [PatientSelfRegistrationComponent],
      providers: [
        provideRouter([]),
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
    expect(component.currentStep()).toBe(1);
  });

  it('should display error if orgId query param is missing or invalid', async () => {
    // Recreate with invalid route
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [PatientSelfRegistrationComponent],
      providers: [
        provideRouter([]),
        { provide: PatientApiService, useValue: mockPatientApi },
        { provide: I18nService, useValue: mockI18n },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({ orgId: 'invalid-uuid-format' })
          }
        }
      ]
    }).compileComponents();

    const newFixture = TestBed.createComponent(PatientSelfRegistrationComponent);
    const newComponent = newFixture.componentInstance;
    newFixture.detectChanges();

    expect(newComponent.organizationIdMissing()).toBe(true);
  });

  it('should select a language explicitly', () => {
    component.setLang('en');
    expect(mockI18n.setLocale).toHaveBeenCalledWith('en');
  });

  it('should expose exits to the public site and patient login before filling the form', () => {
    const element: HTMLElement = fixture.nativeElement;
    const publicSiteLink = element.querySelector('a[href="https://joprelys.com"]');
    const patientLoginLink = element.querySelector('a[href="/patient/login"]');

    expect(publicSiteLink).not.toBeNull();
    expect(patientLoginLink).not.toBeNull();
  });

  it('should block step 1 progression while required identity fields are missing', () => {
    component.firstName.set('');
    component.lastName.set('Doe');

    component.nextStep();

    expect(component.currentStep()).toBe(1);
    expect(component.errorMessage()).toBe('selfRegistration.errorRequired');
  });

  it('should navigate forward and backward through the progressive form', () => {
    fillRequiredIdentity();

    component.nextStep();
    expect(component.currentStep()).toBe(2);

    component.nextStep();
    expect(component.currentStep()).toBe(3);

    component.nextStep();
    expect(component.currentStep()).toBe(4);

    component.previousStep();
    expect(component.currentStep()).toBe(3);
    expect(component.firstName()).toBe('John');
  });

  it('should validate form and return to step 1 if required fields are missing', () => {
    component.currentStep.set(4);
    component.firstName.set('');
    component.lastName.set('Doe');

    component.submitForm();

    expect(component.currentStep()).toBe(1);
    expect(component.errorMessage()).toBe('selfRegistration.errorRequired');
    expect(mockPatientApi.submitPublicPreRegistration).not.toHaveBeenCalled();
  });

  it('should validate form and keep the user on step 4 if captcha is missing', () => {
    fillRequiredIdentity();
    component.currentStep.set(4);
    component.captchaAnswer.set('');

    component.submitForm();

    expect(component.currentStep()).toBe(4);
    expect(component.errorMessage()).toBe('selfRegistration.errorCaptcha');
    expect(mockPatientApi.submitPublicPreRegistration).not.toHaveBeenCalled();
  });

  it('should submit successfully with the existing request contract', () => {
    fillRequiredIdentity();
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

  it('should reset the progressive form to step 1 after success', () => {
    fillRequiredIdentity();
    component.currentStep.set(4);
    component.registrationSuccess.set(true);

    component.resetForm();

    expect(component.currentStep()).toBe(1);
    expect(component.registrationSuccess()).toBe(false);
    expect(component.firstName()).toBe('');
    expect(mockPatientApi.getPublicCaptcha).toHaveBeenCalled();
  });

  it('should handle API submission error and reload captcha', () => {
    mockPatientApi.submitPublicPreRegistration.mockReturnValue(throwError(() => ({
      status: 400,
      error: { detail: 'Incorrect captcha answer' }
    })));

    fillRequiredIdentity();
    component.currentStep.set(4);
    component.captchaAnswer.set('wrong');

    // Reset calls count
    mockPatientApi.getPublicCaptcha.mockClear();

    component.submitForm();

    expect(component.registrationSuccess()).toBe(false);
    expect(component.errorMessage()).toBe('Incorrect captcha answer');
    expect(mockPatientApi.getPublicCaptcha).toHaveBeenCalled(); // reload captcha called
  });
});

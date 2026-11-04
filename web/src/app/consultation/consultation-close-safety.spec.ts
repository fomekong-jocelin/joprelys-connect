import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { EMPTY, of, throwError } from 'rxjs';
import { LabOrderApiService } from '../clinic/lab/lab-api.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from '../patient/patient-api.service';
import { DialogService } from '../shared/ui/dialog.service';
import { VisitApiService } from '../visit/visit-api.service';
import { ConsultationApiService } from './consultation-api.service';
import { ConsultationComponent } from './consultation.component';

describe('ConsultationComponent — validation & clôture sécurisées', () => {
  let patientApi: {
    getById: ReturnType<typeof vi.fn>;
    getAllergies: ReturnType<typeof vi.fn>;
    getMedicalHistory: ReturnType<typeof vi.fn>;
  };
  let consultationApi: { getConsultation: ReturnType<typeof vi.fn>; getPrescription: ReturnType<typeof vi.fn>; saveConsultation: ReturnType<typeof vi.fn> };
  let dialog: { confirm: ReturnType<typeof vi.fn> };

  function create(): ConsultationComponent {
    const component = TestBed.createComponent(ConsultationComponent).componentInstance;
    component.ngOnInit();
    component.form.patchValue({ symptoms: 'Fièvre', diagnosis: 'Paludisme simple' });
    return component;
  }

  beforeEach(async () => {
    patientApi = {
      getById: vi.fn().mockReturnValue(of({ id: 'patient-1', fullName: 'Patient Test' })),
      getAllergies: vi.fn().mockReturnValue(of([])),
      getMedicalHistory: vi.fn().mockReturnValue(of([])),
    };
    consultationApi = {
      getConsultation: vi.fn().mockReturnValue(EMPTY),
      getPrescription: vi.fn().mockReturnValue(EMPTY),
      saveConsultation: vi.fn().mockReturnValue(EMPTY),
    };
    dialog = { confirm: vi.fn().mockResolvedValue(false) };

    await TestBed.configureTestingModule({
      imports: [ConsultationComponent],
      providers: [
        FormBuilder,
        provideHttpClient(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 'visit-1' } } } },
        {
          provide: VisitApiService,
          useValue: { getVitals: () => of(null), getById: () => of({ patientId: 'patient-1', visitNumber: 'V-1' }) },
        },
        { provide: LabOrderApiService, useValue: { getPatientLabOrders: () => of([]) } },
        { provide: ConsultationApiService, useValue: consultationApi },
        { provide: PatientApiService, useValue: patientApi },
        { provide: RbacApiService, useValue: { hasPermission: () => true } },
        { provide: DialogService, useValue: dialog },
        { provide: I18nService, useValue: { t: (key: string) => key } },
      ],
    }).compileComponents();
  });

  it('bloque la clôture tant que les allergies ne sont pas chargées avec succès', async () => {
    patientApi.getAllergies.mockReturnValue(throwError(() => new Error('503')));
    const component = create();

    expect(component.allergiesVerified()).toBe(false);
    expect(component.canValidateAndClose()).toBe(false);

    await component.requestValidateAndClose();
    component.onSave(true);

    expect(dialog.confirm).not.toHaveBeenCalled();
    expect(consultationApi.saveConsultation).not.toHaveBeenCalled();
  });

  it('demande une confirmation récapitulative et ne clôture pas si elle est refusée', async () => {
    const component = create();

    await component.requestValidateAndClose();

    expect(dialog.confirm).toHaveBeenCalledWith(expect.objectContaining({
      title: 'consultation.closeConfirm.title',
      confirmLabel: 'consultation.closeConfirm.action',
      tone: 'warning',
    }));
    expect(consultationApi.saveConsultation).not.toHaveBeenCalled();
  });

  it('enregistre puis clôture après confirmation explicite', async () => {
    dialog.confirm.mockResolvedValue(true);
    const component = create();

    await component.requestValidateAndClose();

    expect(consultationApi.saveConsultation).toHaveBeenCalledWith('visit-1', expect.objectContaining({
      diagnosis: 'Paludisme simple',
    }));
  });

  it("réautorise la clôture après une relance réussie du chargement des allergies", () => {
    patientApi.getAllergies.mockReturnValueOnce(throwError(() => new Error('timeout')));
    const component = create();
    expect(component.canValidateAndClose()).toBe(false);

    component.patientContext.retryFailed();

    expect(component.canValidateAndClose()).toBe(true);
  });
});

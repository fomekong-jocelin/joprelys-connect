import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';
import { provideI18nTesting } from '../../testing/i18n-testing';
import { PharmacyApiService } from './pharmacy-api.service';
import { PharmacyDispensationPanelComponent } from './pharmacy-dispensation-panel.component';
import { PharmacyPrescriptionVerifyPageComponent } from './pharmacy-prescription-verify-page.component';

describe('PharmacyApiService', () => {
  let service: PharmacyApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), PharmacyApiService],
    });
    service = TestBed.inject(PharmacyApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should post verification request to public pharmacy endpoint', () => {
    service.verifyPrescription({ prescriptionNumber: 'ORD-1', pinCode: '1234' }).subscribe((response) => {
      expect(response.prescriptionNumber).toBe('ORD-1');
    });

    const req = httpTesting.expectOne('/api/public/pharmacy/prescriptions/verify');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ prescriptionNumber: 'ORD-1', pinCode: '1234' });
    req.flush({
      prescriptionId: 'prescription-1',
      prescriptionNumber: 'ORD-1',
      status: 'ACTIVE',
      patientName: 'Jean Patient',
      doctorName: 'Dr Alpha',
      issuedAt: '2026-07-03T09:00:00Z',
      expiresAt: '2026-10-03T09:00:00Z',
      items: [],
    });
  });

  it('should post dispensation request to public pharmacy endpoint', () => {
    service.dispensePrescription({
      prescriptionNumber: 'ORD-1',
      pinCode: '1234',
      pharmacyName: 'Pharmacie du Centre',
      pharmacistLicense: 'LIC-123',
      dispensedItems: [{ prescriptionItemId: 'item-1', quantityDispensed: 2, substitutedWith: 'Generique' }],
    }).subscribe((response) => {
      expect(response).toBeNull();
    });

    const req = httpTesting.expectOne('/api/public/pharmacy/prescriptions/dispense');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.dispensedItems[0]).toEqual({
      prescriptionItemId: 'item-1',
      quantityDispensed: 2,
      substitutedWith: 'Generique',
    });
    req.flush(null);
  });

  it('should post history request without exposing pin in query string', () => {
    service.getDispensationHistory({ prescriptionNumber: 'ORD-1', pinCode: '1234' }).subscribe((response) => {
      expect(response.length).toBe(1);
    });

    const req = httpTesting.expectOne('/api/public/pharmacy/prescriptions/history');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ prescriptionNumber: 'ORD-1', pinCode: '1234' });
    req.flush([{
      dispensationId: 'disp-1',
      dispensedAt: '2026-07-03T09:00:00Z',
      pharmacyName: 'Pharmacie du Centre',
      pharmacistLicense: 'LIC-123',
      items: [],
    }]);
  });
});

describe('PharmacyPrescriptionVerifyPageComponent', () => {
  let fixture: ComponentFixture<PharmacyPrescriptionVerifyPageComponent>;
  let pharmacyApi: {
    verifyPrescription: ReturnType<typeof vi.fn>;
    dispensePrescription: ReturnType<typeof vi.fn>;
    getDispensationHistory: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    pharmacyApi = {
      verifyPrescription: vi.fn().mockReturnValue(of({
        prescriptionId: 'prescription-1',
        prescriptionNumber: 'ORD-20260703-000042',
        status: 'ACTIVE',
        patientName: 'Jean Patient',
        doctorName: 'Dr Alpha',
        issuedAt: '2026-07-03T09:00:00Z',
        expiresAt: '2026-10-03T09:00:00Z',
        items: [
          {
            itemId: 'item-1',
            drugName: 'Amoxicilline',
            dosage: '500mg',
            form: 'Comprime',
            quantity: '3',
            quantityAlreadyDispensed: 0,
            substitutionAllowed: true,
            instructions: '1 matin et soir',
          },
        ],
      })),
      dispensePrescription: vi.fn().mockReturnValue(of(null)),
      getDispensationHistory: vi.fn().mockReturnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [PharmacyPrescriptionVerifyPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PharmacyApiService, useValue: pharmacyApi },
        provideI18nTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PharmacyPrescriptionVerifyPageComponent);
    fixture.detectChanges();
  });

  it('should verify prescription and render minimal prescription details', () => {
    const component = fixture.componentInstance;
    component.form.setValue({ prescriptionNumber: ' ORD-20260703-000042 ', pinCode: '1234' });

    component.verify();
    fixture.detectChanges();

    expect(pharmacyApi.verifyPrescription).toHaveBeenCalledWith({
      prescriptionNumber: 'ORD-20260703-000042',
      pinCode: '1234',
    });

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('ORD-20260703-000042');
    expect(element.textContent).toContain('Jean Patient');
    expect(element.textContent).toContain('Amoxicilline');
    expect(element.textContent).toContain('Disponibilité et quantités servies');
  });

  it('should render initial empty state before verification', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Aucune ordonnance affichée');
  });

  it('should render expired prescription status when returned by backend', () => {
    pharmacyApi.verifyPrescription.mockReturnValueOnce(of({
      prescriptionId: 'prescription-2',
      prescriptionNumber: 'ORD-20260703-000043',
      status: 'EXPIRED',
      patientName: 'Patiente Expiree',
      doctorName: 'Dr Beta',
      issuedAt: '2026-01-03T09:00:00Z',
      expiresAt: '2026-04-03T09:00:00Z',
      items: [],
    }));

    const component = fixture.componentInstance;
    component.form.setValue({ prescriptionNumber: 'ORD-20260703-000043', pinCode: '1234' });
    component.verify();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('ORD-20260703-000043');
    expect(element.textContent).toContain('Expirée');
  });

  it('should show backend error when verification fails', () => {
    pharmacyApi.verifyPrescription.mockReturnValueOnce(throwError(() => ({
      error: { detail: 'PIN incorrect' },
    })));

    const component = fixture.componentInstance;
    component.form.setValue({ prescriptionNumber: 'ORD-20260703-000042', pinCode: '0000' });
    component.verify();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('PIN incorrect');
  });

  it('should submit a partial dispensation and refresh prescription details', () => {
    const component = fixture.componentInstance;
    component.form.setValue({ prescriptionNumber: 'ORD-20260703-000042', pinCode: '1234' });
    component.verify();
    fixture.detectChanges();

    const panel = fixture.debugElement.query(By.directive(PharmacyDispensationPanelComponent)).componentInstance;

    panel.dispenseForm.setValue({ pharmacyName: 'Pharmacie du Centre', pharmacistLicense: 'LIC-123' });
    panel.updateQuantity('item-1', '1');
    panel.submitDispensation();

    expect(pharmacyApi.dispensePrescription).toHaveBeenCalledWith({
      prescriptionNumber: 'ORD-20260703-000042',
      pinCode: '1234',
      pharmacyName: 'Pharmacie du Centre',
      pharmacistLicense: 'LIC-123',
      dispensedItems: [{ prescriptionItemId: 'item-1', quantityDispensed: 1, substitutedWith: undefined }],
    });
    expect(pharmacyApi.verifyPrescription).toHaveBeenCalledTimes(2);
  });

  it('should surface backend dispensation errors', () => {
    pharmacyApi.dispensePrescription.mockReturnValueOnce(throwError(() => ({
      error: { detail: 'Quantité dispensée excède la quantité prescrite' },
    })));

    const component = fixture.componentInstance;
    component.form.setValue({ prescriptionNumber: 'ORD-20260703-000042', pinCode: '1234' });
    component.verify();
    fixture.detectChanges();

    const panel = fixture.debugElement.query(By.directive(PharmacyDispensationPanelComponent)).componentInstance;

    panel.dispenseForm.setValue({ pharmacyName: 'Pharmacie du Centre', pharmacistLicense: 'LIC-123' });
    panel.updateQuantity('item-1', '1');
    panel.submitDispensation();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Quantité dispensée excède la quantité prescrite');
  });
});

import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { LabOrderApiService } from './lab-api.service';
import { LabOrdersPageComponent } from './lab-orders-page.component';
import { I18nService } from '../../core/i18n/i18n.service';

import { ExamType, LabOrderStatus, LabOrder } from './lab.models';

const labOrder: LabOrder = {
  id: 'order-1',
  examRequestNumber: 'EXAM-REQ-20260703-000042',
  patientId: 'patient-1',
  patientName: 'Jean Patient',
  requesterPractitionerId: 'doctor-1',
  requesterPractitionerName: 'Dr Alpha',
  sourceOrganizationId: 'org-1',
  examType: ExamType.LABORATOIRE,
  exams: ['NFS', 'Glycemie'],
  reason: 'Controle',
  priority: 'NORMALE',
  status: LabOrderStatus.REQUESTED,
  createdAt: '2026-07-03T09:00:00Z',
};

describe('LabOrderApiService', () => {
  let service: LabOrderApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), LabOrderApiService],
    });
    service = TestBed.inject(LabOrderApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('should get lab orders from tenant endpoint', () => {
    service.getLabOrders().subscribe((response) => expect(response[0].examRequestNumber).toBe('EXAM-REQ-20260703-000042'));

    const req = httpTesting.expectOne('/api/lab-orders');
    expect(req.request.method).toBe('GET');
    req.flush([labOrder]);
  });

  it('should upload lab results with api key header', () => {
    service.uploadResults({
      examRequestNumber: 'EXAM-REQ-20260703-000042',
      validatorName: 'Dr Bio',
      results: [{ analyteName: 'Glycemie', value: '0.95' }],
    }, 'secret-key').subscribe((response) => expect(response).toBeNull());

    const req = httpTesting.expectOne('/api/public/lab-integration/upload');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-API-KEY')).toBe('secret-key');
    expect(req.request.body.examRequestNumber).toBe('EXAM-REQ-20260703-000042');
    req.flush(null);
  });

  it('should update lab order status', () => {
    service.updateStatus('order-1', 'IN_PROGRESS').subscribe((response) => expect(response.status).toBe('IN_PROGRESS'));

    const req = httpTesting.expectOne('/api/lab-orders/order-1/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'IN_PROGRESS' });
    req.flush({ ...labOrder, status: 'IN_PROGRESS' });
  });

  it('should get patient lab results', () => {
    service.getPatientResults('patient-1').subscribe((response) => expect(response[0].analyteName).toBe('Glycemie'));

    const req = httpTesting.expectOne('/api/lab-orders/patient/patient-1/results');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'result-1', resultNumber: 'RES-1', examRequestNumber: labOrder.examRequestNumber, patientId: 'patient-1', validatorName: 'Dr Bio', analyteName: 'Glycemie', value: '0.95', createdAt: '2026-07-03T09:00:00Z' }]);
  });
});

describe('LabOrdersPageComponent', () => {
  let fixture: ComponentFixture<LabOrdersPageComponent>;
  let labApi: {
    getLabOrders: ReturnType<typeof vi.fn>;
    getPatientResults: ReturnType<typeof vi.fn>;
    updateStatus: ReturnType<typeof vi.fn>;
    uploadResults: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    labApi = {
      getLabOrders: vi.fn().mockReturnValue(of([labOrder])),
      getPatientResults: vi.fn().mockReturnValue(of([{ id: 'result-1', resultNumber: 'RES-1', examRequestNumber: labOrder.examRequestNumber, patientId: 'patient-1', validatorName: 'Dr Bio', analyteName: 'Glycemie', value: '0.95', unit: 'g/L', createdAt: '2026-07-03T09:00:00Z' }])),
      updateStatus: vi.fn().mockReturnValue(of({ ...labOrder, status: 'IN_PROGRESS' })),
      uploadResults: vi.fn().mockReturnValue(of(null)),
    };

    await TestBed.configureTestingModule({
      imports: [LabOrdersPageComponent],
      providers: [
        provideRouter([]),
        I18nService,
        { provide: LabOrderApiService, useValue: labApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LabOrdersPageComponent);
    fixture.detectChanges();
  });

  it('should render lab order list and selected request details', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('EXAM-REQ-20260703-000042');
    expect(text).toContain('Jean Patient');
    expect(text).toContain('NFS');
    expect(text).toContain('Glycemie');
  });

  it('should update selected order status', () => {
    const component = fixture.componentInstance;
    component.statusDraft.set(LabOrderStatus.IN_PROGRESS);

    component.updateStatus(labOrder);

    expect(labApi.updateStatus).toHaveBeenCalledWith('order-1', 'IN_PROGRESS');
    expect(component.selectedOrder()?.status).toBe('IN_PROGRESS');
  });

  it('should submit lab results for selected order', () => {
    const component = fixture.componentInstance;
    component.resultForm.patchValue({
      apiKey: 'secret-key',
      validatorName: 'Dr Bio',
      validatedAt: '2026-07-03T12:30',
    });
    component.results.at(0).patchValue({
      analyteName: 'Glycemie',
      value: '0.95',
      unit: 'g/L',
      referenceRange: '0.7-1.1',
      interpretation: 'NORMAL',
    });

    component.submitResults(labOrder);

    expect(labApi.uploadResults).toHaveBeenCalledWith(expect.objectContaining({
      examRequestNumber: 'EXAM-REQ-20260703-000042',
      validatorName: 'Dr Bio',
      results: [expect.objectContaining({ analyteName: 'Glycemie', value: '0.95' })],
    }), 'secret-key');
  });

  it('should show backend upload errors', () => {
    labApi.uploadResults.mockReturnValueOnce(throwError(() => ({ error: { detail: 'Clé API invalide' } })));
    const component = fixture.componentInstance;
    component.resultForm.patchValue({ apiKey: 'bad-key', validatorName: 'Dr Bio' });
    component.results.at(0).patchValue({ analyteName: 'CRP', value: '12' });

    component.submitResults(labOrder);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Clé API invalide');
  });

  it('should filter orders by search query and priority', () => {
    const component = fixture.componentInstance;
    const labOrder2: LabOrder = {
      ...labOrder,
      id: 'order-2',
      examRequestNumber: 'EXAM-REQ-20260703-999999',
      patientName: 'Alice Patient',
      priority: 'URGENTE',
    };
    component.orders.set([labOrder, labOrder2]);
    expect(component.filteredOrders().length).toBe(2);

    // Filter by name
    component.searchQuery.set('Alice');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].patientName).toBe('Alice Patient');

    // Reset search query
    component.searchQuery.set('');
    expect(component.filteredOrders().length).toBe(2);

    // Filter by priority
    component.filterPriority.set('URGENTE');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].priority).toBe('URGENTE');
  });
});

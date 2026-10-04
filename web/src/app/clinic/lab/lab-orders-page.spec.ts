import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { LabOrderApiService } from './lab-api.service';
import { LabOrdersPageComponent } from './lab-orders-page.component';
import { ExamType, LabOrder, LabOrderStatus } from './lab.models';

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

const structuredLabOrder: LabOrder = {
  ...labOrder,
  id: 'order-structured',
  status: LabOrderStatus.IN_PROGRESS,
  exams: ['NFS', 'CRP', 'Radiographie'],
  items: [
    { id: 'item-nfs', examName: 'NFS', status: LabOrderStatus.VALIDATED },
    { id: 'item-crp', examName: 'CRP', status: LabOrderStatus.IN_PROGRESS },
    { id: 'item-radio', examName: 'Radiographie', status: LabOrderStatus.REQUESTED },
  ],
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

  it('should upload lab results with api key header and item id', () => {
    service.uploadResults({
      examRequestNumber: 'EXAM-REQ-20260703-000042',
      labOrderItemId: 'item-crp',
      validatorName: 'Dr Bio',
      results: [{ analyteName: 'CRP', value: '12' }],
    }, 'secret-key').subscribe((response) => expect(response).toBeNull());

    const req = httpTesting.expectOne('/api/public/lab-integration/upload');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.get('X-API-KEY')).toBe('secret-key');
    expect(req.request.body.examRequestNumber).toBe('EXAM-REQ-20260703-000042');
    expect(req.request.body.labOrderItemId).toBe('item-crp');
    req.flush(null);
  });

  it('should update lab order status for legacy requests', () => {
    service.updateStatus('order-1', 'IN_PROGRESS').subscribe((response) => expect(response.status).toBe('IN_PROGRESS'));

    const req = httpTesting.expectOne('/api/lab-orders/order-1/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'IN_PROGRESS' });
    req.flush({ ...labOrder, status: 'IN_PROGRESS' });
  });

  it('should update exactly one exam item status', () => {
    service.updateItemStatus('order-structured', 'item-crp', 'SAMPLE_COLLECTED').subscribe((response) => {
      expect(response.items?.find((item) => item.id === 'item-crp')?.status).toBe(LabOrderStatus.SAMPLE_COLLECTED);
    });

    const req = httpTesting.expectOne('/api/lab-orders/order-structured/items/item-crp/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'SAMPLE_COLLECTED' });
    req.flush({
      ...structuredLabOrder,
      items: structuredLabOrder.items?.map((item) =>
        item.id === 'item-crp' ? { ...item, status: LabOrderStatus.SAMPLE_COLLECTED } : item),
    });
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
    updateItemStatus: ReturnType<typeof vi.fn>;
    uploadResults: ReturnType<typeof vi.fn>;
  };
  let permissions: Set<string>;

  beforeEach(async () => {
    labApi = {
      getLabOrders: vi.fn().mockReturnValue(of([labOrder])),
      getPatientResults: vi.fn().mockReturnValue(of([{ id: 'result-1', resultNumber: 'RES-1', examRequestNumber: labOrder.examRequestNumber, patientId: 'patient-1', validatorName: 'Dr Bio', analyteName: 'Glycemie', value: '0.95', unit: 'g/L', createdAt: '2026-07-03T09:00:00Z' }])),
      updateStatus: vi.fn().mockReturnValue(of({ ...labOrder, status: LabOrderStatus.IN_PROGRESS })),
      updateItemStatus: vi.fn().mockReturnValue(of(structuredLabOrder)),
      uploadResults: vi.fn().mockReturnValue(of(null)),
    };
    permissions = new Set(['LAB_QUEUE_READ', 'LAB_ORDER_READ', 'LAB_ORDER_WRITE']);

    await TestBed.configureTestingModule({
      imports: [LabOrdersPageComponent],
      providers: [
        provideRouter([]),
        I18nService,
        { provide: LabOrderApiService, useValue: labApi },
        {
          provide: RbacApiService,
          useValue: {
            hasPermission: (permission: string) => permissions.has(permission),
          },
        },
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

  it('should update selected order status for legacy requests', () => {
    const component = fixture.componentInstance;
    component.statusDraft.set(LabOrderStatus.IN_PROGRESS);

    component.updateStatus(labOrder);

    expect(labApi.updateStatus).toHaveBeenCalledWith('order-1', 'IN_PROGRESS');
    expect(component.selectedOrder()?.status).toBe(LabOrderStatus.IN_PROGRESS);
  });

  it('should update one structured exam without touching siblings', () => {
    const component = fixture.componentInstance;
    component.selectOrder(structuredLabOrder);
    const crp = structuredLabOrder.items?.[1];
    expect(crp).toBeDefined();

    component.updateItemStatus(structuredLabOrder, crp!, LabOrderStatus.IN_PROGRESS);

    expect(labApi.updateItemStatus).toHaveBeenCalledWith('order-structured', 'item-crp', LabOrderStatus.IN_PROGRESS);
  });

  it('should submit legacy lab results for selected order', () => {
    const component = fixture.componentInstance;
    component.resultForm.patchValue({
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
    }));
  });

  it('should submit structured results with selected item id', () => {
    const component = fixture.componentInstance;
    component.selectOrder(structuredLabOrder);
    component.selectItem(structuredLabOrder.items![1]);
    component.resultForm.patchValue({ validatorName: 'Dr Bio' });
    component.results.at(0).patchValue({ analyteName: 'CRP', value: '12' });

    component.submitResults(structuredLabOrder);

    expect(labApi.uploadResults).toHaveBeenCalledWith(expect.objectContaining({
      examRequestNumber: structuredLabOrder.examRequestNumber,
      labOrderItemId: 'item-crp',
      results: [expect.objectContaining({ analyteName: 'CRP', value: '12' })],
    }));
  });

  it('should keep results scoped to selected exam', () => {
    const component = fixture.componentInstance;
    component.selectOrder(structuredLabOrder);
    component.patientResults.set([
      { id: 'nfs-result', resultNumber: 'RES-NFS', examRequestNumber: structuredLabOrder.examRequestNumber, labOrderItemId: 'item-nfs', examName: 'NFS', patientId: 'patient-1', validatorName: 'Dr Bio', analyteName: 'Hb', value: '13', createdAt: '2026-07-03T09:00:00Z', version: 1 },
      { id: 'crp-result', resultNumber: 'RES-CRP', examRequestNumber: structuredLabOrder.examRequestNumber, labOrderItemId: 'item-crp', examName: 'CRP', patientId: 'patient-1', validatorName: 'Dr Bio', analyteName: 'CRP', value: '12', createdAt: '2026-07-03T09:00:00Z', version: 1 },
    ]);
    component.selectItem(structuredLabOrder.items![1]);

    expect(component.filteredResults(structuredLabOrder).map((result) => result.id)).toEqual(['crp-result']);
  });

  it('should show backend upload errors', () => {
    labApi.uploadResults.mockReturnValueOnce(throwError(() => ({ error: { detail: 'Erreur de validation' } })));
    const component = fixture.componentInstance;
    component.resultForm.patchValue({ validatorName: 'Dr Bio' });
    component.results.at(0).patchValue({ analyteName: 'CRP', value: '12' });

    component.submitResults(labOrder);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Erreur de validation');
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

    component.searchQuery.set('Alice');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].patientName).toBe('Alice Patient');

    component.searchQuery.set('');
    expect(component.filteredOrders().length).toBe(2);

    component.filterPriority.set('URGENTE');
    expect(component.filteredOrders().length).toBe(1);
    expect(component.filteredOrders()[0].priority).toBe('URGENTE');
  });

  it('should not load or expose result actions with queue-only permission', () => {
    permissions = new Set(['LAB_QUEUE_READ']);
    labApi.getPatientResults.mockClear();
    labApi.updateStatus.mockClear();
    labApi.updateItemStatus.mockClear();
    labApi.uploadResults.mockClear();

    const restrictedFixture = TestBed.createComponent(LabOrdersPageComponent);
    restrictedFixture.detectChanges();
    const component = restrictedFixture.componentInstance;
    component.selectOrder(labOrder);
    component.updateStatus(labOrder);
    component.submitResults(labOrder);
    restrictedFixture.detectChanges();

    expect(labApi.getPatientResults).not.toHaveBeenCalled();
    expect(labApi.updateStatus).not.toHaveBeenCalled();
    expect(labApi.updateItemStatus).not.toHaveBeenCalled();
    expect(labApi.uploadResults).not.toHaveBeenCalled();
    expect((restrictedFixture.nativeElement as HTMLElement).textContent).not.toContain('Clé API');
  });
});

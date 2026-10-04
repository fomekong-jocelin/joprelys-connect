import { TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationConsentPanelComponent } from './hospitalization-consent-panel.component';
import { HospitalizationOperatingReportPanelComponent } from './hospitalization-operating-report-panel.component';

describe('Extracted hospitalization clinical panels', () => {
  const api = {
    getConsents: vi.fn<PatientApiService['getConsents']>(),
    addConsent: vi.fn<PatientApiService['addConsent']>(),
    getOperatingReports: vi.fn<PatientApiService['getOperatingReports']>(),
    createOperatingReport: vi.fn<PatientApiService['createOperatingReport']>(),
    validateOperatingReport: vi.fn<PatientApiService['validateOperatingReport']>(),
  };

  beforeEach(async () => {
    vi.resetAllMocks();
    api.getConsents.mockReturnValue(of([]));
    api.getOperatingReports.mockReturnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [HospitalizationConsentPanelComponent, HospitalizationOperatingReportPanelComponent],
      providers: [
        { provide: PatientApiService, useValue: api },
        { provide: I18nService, useValue: { t: (key: string) => key } },
      ],
    }).compileComponents();
  });

  it('keeps the consent attachment, witness and signature in the request', () => {
    const fixture = TestBed.createComponent(HospitalizationConsentPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true);
    fixture.detectChanges();
    const panel = fixture.componentInstance;
    panel.witnessName = '  Témoin  '; panel.patientSignaturePresent = true;
    panel.consentFile = new File(['pdf'], 'consent.pdf', { type: 'application/pdf' });
    api.addConsent.mockReturnValue(new Subject());
    panel.saveConsent(new Event('submit')); panel.saveConsent(new Event('submit'));
    expect(api.addConsent).toHaveBeenCalledOnce();
    const [stay, form] = api.addConsent.mock.calls[0]!;
    expect(stay).toBe('stay-1');
    expect(form.get('witnessName')).toBe('Témoin');
    expect(form.get('patientSignaturePresent')).toBe('true');
    expect(form.get('file')).toBe(panel.consentFile);
  });

  it('displays a consent loading failure and supports retry without an empty-state claim', () => {
    api.getConsents.mockReturnValueOnce(throwError(() => new Error('offline')));
    const fixture = TestBed.createComponent(HospitalizationConsentPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('patients.hospitalization.workflow.noConsent');
    fixture.componentInstance.load(); fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBeNull();
    expect(api.getConsents).toHaveBeenCalledTimes(2);
  });

  it('preserves operating report actors, clinical fields, K units and implant trace', () => {
    const fixture = TestBed.createComponent(HospitalizationOperatingReportPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true); fixture.detectChanges();
    const panel = fixture.componentInstance;
    panel.procedureName = 'Appendicectomie'; panel.preOperativeDiagnosis = 'Pré'; panel.postOperativeDiagnosis = 'Post';
    panel.surgeonId = 'doctor-1'; panel.anesthetistId = 'nurse-1'; panel.kSurgeonValue = 10;
    panel.newImplantName = 'Suture'; panel.newImplantLot = 'LOT-1'; panel.newImplantQty = 2; panel.newImplantPrice = 1200;
    panel.addImplantToList(); api.createOperatingReport.mockReturnValue(new Subject());
    panel.saveOperatingReport(new Event('submit'));
    expect(api.createOperatingReport).toHaveBeenCalledWith('stay-1', expect.objectContaining({
      surgeonId: 'doctor-1', anesthetistId: 'nurse-1', preOperativeDiagnosis: 'Pré', postOperativeDiagnosis: 'Post',
      kSurgeonValue: 10, implants: [expect.objectContaining({ implantName: 'Suture', lotNumber: 'LOT-1', quantity: 2, unitPrice: 1200 })],
    }));
  });

  it('waits for confirmation and sends one validation while pending', () => {
    const fixture = TestBed.createComponent(HospitalizationOperatingReportPanelComponent);
    fixture.componentRef.setInput('hospitalizationId', 'stay-1');
    fixture.componentRef.setInput('canModify', true); fixture.detectChanges();
    const panel = fixture.componentInstance;
    api.validateOperatingReport.mockReturnValue(new Subject());
    panel.validateReport('report-1');
    expect(api.validateOperatingReport).not.toHaveBeenCalled();
    panel.confirmValidation(); panel.confirmValidation();
    expect(api.validateOperatingReport).toHaveBeenCalledOnce();
  });

  it('blocks consent and operating report mutation for read-only profiles', () => {
    const consent = TestBed.createComponent(HospitalizationConsentPanelComponent).componentInstance;
    consent.hospitalizationId = 'stay-1'; consent.saveConsent(new Event('submit'));
    const report = TestBed.createComponent(HospitalizationOperatingReportPanelComponent).componentInstance;
    report.hospitalizationId = 'stay-1'; report.procedureName = 'Intervention';
    report.saveOperatingReport(new Event('submit')); report.validateReport('report-1'); report.confirmValidation();
    expect(api.addConsent).not.toHaveBeenCalled();
    expect(api.createOperatingReport).not.toHaveBeenCalled();
    expect(api.validateOperatingReport).not.toHaveBeenCalled();
  });
});

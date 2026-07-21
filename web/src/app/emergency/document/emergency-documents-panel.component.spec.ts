import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyDocumentApiService } from './emergency-document-api.service';
import { EmergencyDocumentsPanelComponent } from './emergency-documents-panel.component';

describe('EmergencyDocumentsPanelComponent', () => {
  let fixture: ComponentFixture<EmergencyDocumentsPanelComponent>;
  let api: {
    listByEmergency: ReturnType<typeof vi.fn>;
    generateBundle: ReturnType<typeof vi.fn>;
  };

  const document = {
    id: 'document-1',
    visitId: 'visit-1',
    originPatientId: 'source-1',
    documentNumber: 'DOC-20260721-000001',
    documentType: 'FICHE_URGENCE' as const,
    status: 'VALID',
    hash: 'abc123',
    version: 1,
    verificationUrl: 'https://verify.example/doc-1',
    createdAt: '2026-07-21T10:00:00Z',
  };

  beforeEach(async () => {
    api = {
      listByEmergency: vi.fn().mockReturnValue(of([])),
      generateBundle: vi.fn().mockReturnValue(of([document])),
    };

    await TestBed.configureTestingModule({
      imports: [EmergencyDocumentsPanelComponent],
      providers: [
        { provide: EmergencyDocumentApiService, useValue: api },
        { provide: I18nService, useValue: { t: (_key: string, fallback: string) => fallback } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EmergencyDocumentsPanelComponent);
    fixture.componentRef.setInput('emergencyId', 'emergency-1');
    fixture.detectChanges();
  });

  it('loads the existing documents for the emergency', () => {
    expect(api.listByEmergency).toHaveBeenCalledWith('emergency-1');
  });

  it('generates an idempotent document bundle and renders its evidence', () => {
    fixture.componentInstance.generate();
    fixture.detectChanges();

    expect(api.generateBundle).toHaveBeenCalledWith('emergency-1');
    expect(fixture.componentInstance.documents()).toEqual([document]);
    expect(fixture.nativeElement.textContent).toContain('DOC-20260721-000001');
    expect(fixture.nativeElement.textContent).toContain('abc123');
  });

  it('keeps the source patient provenance in the document evidence', () => {
    fixture.componentInstance.generate();

    expect(fixture.componentInstance.documents()[0].originPatientId).toBe('source-1');
  });

  it('emits the hospitalization continuation action', () => {
    const continuation = vi.fn();
    fixture.componentInstance.hospitalizationRequested.subscribe(continuation);

    fixture.componentInstance.hospitalizationRequested.emit();

    expect(continuation).toHaveBeenCalledOnce();
  });
});

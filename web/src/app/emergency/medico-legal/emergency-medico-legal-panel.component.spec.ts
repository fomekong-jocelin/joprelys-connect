import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { EmergencyMedicoLegalApiService } from './emergency-medico-legal-api.service';
import { EmergencyMedicoLegalPanelComponent } from './emergency-medico-legal-panel.component';
import { EmergencyMedicoLegalDossier } from './emergency-medico-legal.models';

describe('EmergencyMedicoLegalPanelComponent', () => {
  let fixture: ComponentFixture<EmergencyMedicoLegalPanelComponent>;
  let component: EmergencyMedicoLegalPanelComponent;
  let api: {
    getDossier: ReturnType<typeof vi.fn>;
    addThirdParty: ReturnType<typeof vi.fn>;
    recordCapacity: ReturnType<typeof vi.fn>;
    addLegalBasis: ReturnType<typeof vi.fn>;
    addBelonging: ReturnType<typeof vi.fn>;
    transferBelonging: ReturnType<typeof vi.fn>;
  };

  const dossier: EmergencyMedicoLegalDossier = {
    emergencyId: 'emergency-1',
    thirdParties: [],
    identityStatements: [],
    capacityHistory: [],
    legalBases: [],
    belongings: [],
  };

  beforeEach(async () => {
    api = {
      getDossier: vi.fn().mockReturnValue(of(dossier)),
      addThirdParty: vi.fn().mockReturnValue(of({
        ...dossier,
        thirdParties: [{
          id: 'third-party-1',
          fullName: 'Paul Tamo',
          consentToContact: true,
          legalRepresentativeClaimed: false,
          sourceType: 'WITNESS',
          confidenceLevel: 'LOW',
          qualities: ['WITNESS', 'DECLARANT'],
          createdAt: '2026-07-11T18:00:00Z',
        }],
      })),
      recordCapacity: vi.fn().mockReturnValue(of(dossier)),
      addLegalBasis: vi.fn().mockReturnValue(of(dossier)),
      addBelonging: vi.fn().mockReturnValue(of(dossier)),
      transferBelonging: vi.fn().mockReturnValue(of(dossier)),
    };

    await TestBed.configureTestingModule({
      imports: [EmergencyMedicoLegalPanelComponent],
      providers: [
        { provide: EmergencyMedicoLegalApiService, useValue: api },
        {
          provide: I18nService,
          useValue: {
            locale: vi.fn().mockReturnValue('fr'),
            t: vi.fn((key: string, fallback?: string) => fallback ?? key),
          },
        },
        {
          provide: ApiErrorI18nService,
          useValue: { message: vi.fn().mockReturnValue('Erreur') },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EmergencyMedicoLegalPanelComponent);
    fixture.componentRef.setInput('emergencyId', 'emergency-1');
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('loads the protected medico-legal dossier', () => {
    expect(api.getDossier).toHaveBeenCalledWith('emergency-1');
    expect(component.dossier()).toEqual(dossier);
  });

  it('delegates third-party creation and refreshes the dossier', () => {
    component.addThirdParty({
      fullName: 'Paul Tamo',
      consentToContact: true,
      legalRepresentativeClaimed: false,
      sourceType: 'WITNESS',
      confidenceLevel: 'LOW',
      qualities: ['WITNESS', 'DECLARANT'],
      identityStatements: [],
    });

    expect(api.addThirdParty).toHaveBeenCalledWith(
      'emergency-1',
      expect.objectContaining({ fullName: 'Paul Tamo' }),
    );
    expect(component.dossier()?.thirdParties[0].fullName).toBe('Paul Tamo');
  });
});

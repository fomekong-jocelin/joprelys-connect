import { provideHttpClient } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConsultationApiService } from '../consultation/ai-consultation-api.service';
import { AiVitalsApiService } from '../consultation/ai-vitals-api.service';
import { Visit } from '../visit/visit.models';
import { VisitApiService } from '../visit/visit-api.service';
import { DashboardComponent } from './dashboard.component';
import { RbacApiService } from './rbac/rbac-api.service';

describe('Dashboard Smart Vitals placement', () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let component: DashboardComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: AuthTokenStorageService,
          useValue: { session: () => null },
        },
        {
          provide: VisitApiService,
          useValue: {
            getActiveVisits: vi.fn().mockReturnValue(of([])),
            saveVitals: vi.fn().mockReturnValue(of({})),
            closeVisit: vi.fn().mockReturnValue(of({})),
          },
        },
        {
          provide: RbacApiService,
          useValue: { hasPermission: vi.fn().mockReturnValue(true) },
        },
        {
          provide: I18nService,
          useValue: {
            t: (key: string, fallback?: string) => fallback ?? key,
            currentLanguage: () => 'fr',
            locale: signal<'fr' | 'en'>('fr'),
            setLocale: vi.fn(),
          },
        },
        {
          provide: AiVitalsApiService,
          useValue: {
            analyzeText: vi.fn(),
            analyzeAudio: vi.fn(),
          },
        },
        {
          provide: AiConsultationApiService,
          useValue: { synthesizeSpeech: vi.fn() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
  });

  it('should render the assistant declaratively inside the vitals modal', () => {
    const visit = {
      id: 'visit-1',
      patientName: 'Patient Test',
      patientDpu: 'DPU-001',
      createdAt: '2026-07-26T09:00:00Z',
      vitals: null,
    } as unknown as Visit;
    component.selectedVisitForVitals.set(visit);
    component.showVitalsModal.set(true);
    fixture.detectChanges();

    const modalPanel = fixture.nativeElement.querySelector('[role="dialog"]') as HTMLElement;
    const assistantHost = modalPanel?.querySelector('app-smart-vitals-assistant') as HTMLElement;
    const assistantSection = assistantHost?.querySelector('section') as HTMLElement;

    expect(assistantHost).toBeTruthy();
    expect(modalPanel.contains(assistantHost)).toBe(true);
    expect(assistantSection.className).toContain('relative');
    expect(assistantSection.className).not.toContain('fixed');
    expect(component.currentVitalsForAssistant()).toEqual({});
  });
});

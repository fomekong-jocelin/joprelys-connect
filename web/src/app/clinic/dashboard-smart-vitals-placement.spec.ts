import { provideHttpClient } from '@angular/common/http';
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

  afterEach(() => {
    try {
      (component as any).destroyVitalsAssistant();
    } catch {
      // no-op
    }
    document.querySelector('#smart-vitals-placement-test')?.remove();
  });

  it('should mount the assistant inside the modal instead of as a floating body overlay', () => {
    const modalRoot = document.createElement('app-dashboard');
    modalRoot.id = 'smart-vitals-placement-test';
    modalRoot.innerHTML = `
      <div class="fixed inset-0 z-[60]">
        <div class="relative">
          <hr />
          <div id="vitals-form">Vitals form</div>
          <button id="save-vitals">Enregistrer</button>
        </div>
      </div>
    `;
    document.body.appendChild(modalRoot);

    const visit = {
      id: 'visit-1',
      patientName: 'Patient Test',
      vitals: null,
    } as unknown as Visit;
    component.selectedVisitForVitals.set(visit);
    component.showVitalsModal.set(true);

    (component as any).mountVitalsAssistant(visit);

    const modalPanel = modalRoot.querySelector('div.relative') as HTMLElement;
    const assistantHost = modalPanel.querySelector('app-smart-vitals-assistant') as HTMLElement;
    const assistantSection = assistantHost?.querySelector('section') as HTMLElement;
    const saveButton = modalPanel.querySelector('#save-vitals');

    expect(assistantHost).toBeTruthy();
    expect(modalPanel.contains(assistantHost)).toBe(true);
    expect(assistantSection.className).toContain('relative');
    expect(assistantSection.className).not.toContain('fixed');
    expect(saveButton).toBeTruthy();
  });
});

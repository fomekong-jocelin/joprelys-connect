import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { VisitApiService } from '../visit/visit-api.service';
import { I18nService } from '../core/i18n/i18n.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let mockAuthToken: any;
  let mockVisitApi: any;
  let mockI18n: any;

  beforeEach(async () => {
    mockAuthToken = {
      session: signal({
        name: 'Jean Medecin',
        email: 'medecin@joprelys.local',
        role: 'MEDECIN',
        org: 'org-1'
      })
    };

    mockVisitApi = {
      getActiveVisits: vi.fn().mockReturnValue(of([])),
      closeVisit: vi.fn()
    };

    mockI18n = {
      t: vi.fn().mockImplementation((key) => key)
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: AuthTokenStorageService, useValue: mockAuthToken },
        { provide: VisitApiService, useValue: mockVisitApi },
        { provide: I18nService, useValue: mockI18n }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load active visits on init if clinical role', () => {
    expect(mockVisitApi.getActiveVisits).toHaveBeenCalled();
  });
});

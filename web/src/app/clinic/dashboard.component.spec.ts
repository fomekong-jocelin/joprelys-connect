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

  it('should calculate BMI correctly when weight and height are provided', () => {
    component.vitalsWeight = 70;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBe(22.86); // 70 / 1.75^2 = 22.857 -> 22.86
  });

  it('should return null BMI if height or weight is missing', () => {
    component.vitalsWeight = undefined;
    component.vitalsHeight = 175;
    expect(component.computedBmi).toBeNull();

    component.vitalsWeight = 70;
    component.vitalsHeight = undefined;
    expect(component.computedBmi).toBeNull();
  });
});

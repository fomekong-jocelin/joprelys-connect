import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ConsentManagementService } from '../privacy/consent-management.service';
import { I18nService } from './i18n.service';

describe('I18nService', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        I18nService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ConsentManagementService,
          useValue: { preferencesAllowed: () => false },
        },
      ],
    });
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    localStorage.clear();
  });

  it('should fetch translation assets with a bootstrap cache-busting revision', async () => {
    const service = TestBed.inject(I18nService);
    const http = TestBed.inject(HttpTestingController);

    const initialization = service.init();
    const requests = http.match((request) => request.url.startsWith('/assets/i18n/'));

    expect(requests.length).toBeGreaterThan(1);
    expect(requests.every((request) => /[?&]v=[a-z0-9]+$/i.test(request.request.urlWithParams))).toBe(true);

    for (const request of requests) {
      if (request.request.url.startsWith('/assets/i18n/features/admission/fr.json')) {
        request.flush({
          'admission.orientationOption.CONSULTATION': 'Consultation générale / Examen',
          'admission.serviceSelectPlaceholder': '-- Sélectionner un service --',
        });
      } else {
        request.flush({});
      }
    }

    await initialization;

    expect(service.t('admission.orientationOption.CONSULTATION')).toBe('Consultation générale / Examen');
    expect(service.t('admission.serviceSelectPlaceholder')).toBe('-- Sélectionner un service --');
  });
});

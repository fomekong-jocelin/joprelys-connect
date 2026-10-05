import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LandingDemoService, DemoLeadPayload } from './landing-demo.service';

describe('LandingDemoService', () => {
  let service: LandingDemoService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        LandingDemoService
      ]
    });

    service = TestBed.inject(LandingDemoService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('posts to the backend without persisting contact data in the browser', () => {
    const payload: DemoLeadPayload = {
      fullName: 'Dr. Test',
      organizationName: 'Clinique Espoir',
      phone: '+237 699000000',
      city: 'Douala',
      role: 'Directeur',
      locale: 'fr'
    };

    let resultResponse: any = null;
    service.submitDemo(payload).subscribe((res) => {
      resultResponse = res;
    });

    expect(localStorage.getItem('joprelys_demo_leads_backup')).toBeNull();

    const req = httpTesting.expectOne('/api/public/demo-requests');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.fullName).toBe('Dr. Test');

    req.flush({
      id: 'UUID-1234',
      fullName: 'Dr. Test',
      organizationName: 'Clinique Espoir',
      status: 'NEW',
      createdAt: '2026-10-04T12:00:00Z',
      message: 'Demande enregistrée'
    });

    expect(resultResponse).toBeTruthy();
    expect(resultResponse.id).toBe('UUID-1234');

    expect(localStorage.getItem('joprelys_demo_leads_backup')).toBeNull();
  });

  it('propagates network failure instead of returning an apparent success', () => {
    const payload: DemoLeadPayload = {
      fullName: 'Dr. Offline',
      organizationName: 'Hôpital Régional',
      phone: '+237 677112233',
      city: 'Bafoussam',
      locale: 'fr'
    };

    const next = vi.fn();
    const error = vi.fn();
    service.submitDemo(payload).subscribe({
      next,
      error
    });

    const req = httpTesting.expectOne('/api/public/demo-requests');
    req.error(new ProgressEvent('Network error'));

    expect(next).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledOnce();
    expect(localStorage.getItem('joprelys_demo_leads_backup')).toBeNull();
  });

  it('builds pre-filled WhatsApp URLs correctly in French and English', () => {
    const payload: DemoLeadPayload = {
      fullName: 'Dr. Paul',
      organizationName: 'Cabinet Médical',
      phone: '+237 600000000',
      city: 'Yaoundé',
      role: 'Médecin'
    };

    const waFr = service.buildWhatsAppUrl(payload, 'fr');
    expect(waFr).toContain('https://wa.me/237691501780');
    expect(decodeURIComponent(waFr)).toContain('Cabinet Médical');
    expect(decodeURIComponent(waFr)).toContain('Dr. Paul');

    const waEn = service.buildWhatsAppUrl(payload, 'en');
    expect(waEn).toContain('https://wa.me/237691501780');
    expect(decodeURIComponent(waEn)).toContain('Hello Joprelys Connect');
  });

  it('bounds a stalled HTTP request and releases it with an error', async () => {
    vi.useFakeTimers();
    try {
      const error = vi.fn();
      service.submitDemo({ fullName: 'Test', organizationName: 'Clinic', phone: '600000000' })
        .subscribe({ error });
      const request = httpTesting.expectOne('/api/public/demo-requests');
      await vi.advanceTimersByTimeAsync(20_000);
      expect(error).toHaveBeenCalledOnce();
      expect(request.cancelled).toBe(true);
    } finally {
      vi.useRealTimers();
    }
  });
});

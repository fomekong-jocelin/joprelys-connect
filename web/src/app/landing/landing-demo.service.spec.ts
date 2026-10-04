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

  it('saves lead locally and posts to backend', () => {
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

    // Check offline backup was written immediately
    const backupRaw = localStorage.getItem('joprelys_demo_leads_backup');
    expect(backupRaw).toBeTruthy();
    expect(backupRaw).toContain('Clinique Espoir');

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

    // Local status updated to SYNCED
    const updatedBackup = localStorage.getItem('joprelys_demo_leads_backup');
    expect(updatedBackup).toContain('SYNCED');
    expect(updatedBackup).toContain('UUID-1234');
  });

  it('preserves lead in local storage when server fails (zero loss guarantee)', () => {
    const payload: DemoLeadPayload = {
      fullName: 'Dr. Offline',
      organizationName: 'Hôpital Régional',
      phone: '+237 677112233',
      city: 'Bafoussam',
      locale: 'fr'
    };

    let resultResponse: any = 'NOT_SET';
    service.submitDemo(payload).subscribe((res) => {
      resultResponse = res;
    });

    const req = httpTesting.expectOne('/api/public/demo-requests');
    req.error(new ProgressEvent('Network error'));

    // Should return null gracefully without throwing
    expect(resultResponse).toBeNull();

    // Lead is still safely preserved in local storage!
    const backupRaw = localStorage.getItem('joprelys_demo_leads_backup');
    expect(backupRaw).toBeTruthy();
    expect(backupRaw).toContain('Hôpital Régional');
    expect(backupRaw).toContain('PENDING');
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
    expect(waFr).toContain('https://wa.me/237691893198');
    expect(decodeURIComponent(waFr)).toContain('Cabinet Médical');
    expect(decodeURIComponent(waFr)).toContain('Dr. Paul');

    const waEn = service.buildWhatsAppUrl(payload, 'en');
    expect(waEn).toContain('https://wa.me/237691893198');
    expect(decodeURIComponent(waEn)).toContain('Hello Joprelys Connect');
  });
});

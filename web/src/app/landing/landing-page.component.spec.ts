import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { LandingPageComponent } from './landing-page.component';
import { I18nService } from '../core/i18n/i18n.service';
import { LandingDemoService, DemoLeadResponse } from './landing-demo.service';

describe('LandingPageComponent', () => {
  let component: LandingPageComponent;
  let fixture: ComponentFixture<LandingPageComponent>;
  let i18nService: I18nService;
  let demoService: LandingDemoService;

  beforeEach(async () => {
    localStorage.clear();
    await TestBed.configureTestingModule({
      imports: [LandingPageComponent],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        I18nService,
        LandingDemoService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LandingPageComponent);
    component = fixture.componentInstance;
    i18nService = TestBed.inject(I18nService);
    demoService = TestBed.inject(LandingDemoService);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('creates the component successfully', () => {
    expect(component).toBeTruthy();
  });

  it('renders French slogan by default in hero and footer', () => {
    expect(component.t().hero.title).toBe(
      'Parce qu’elle est précieuse, nous innovons pour la protéger.'
    );
    expect(component.t().footer.slogan).toBe(
      '« Parce qu’elle est précieuse, nous innovons pour la protéger. »'
    );
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Parce qu’elle est précieuse');
  });

  it('switches to English when calling toggleLang()', () => {
    component.toggleLang();
    fixture.detectChanges();

    expect(i18nService.currentLanguage()).toBe('en');
    expect(component.t().hero.title).toBe(
      'Because it is precious, we innovate to protect it.'
    );
    expect(component.t().hero.ctaDemo).toBe('Request a Live Demo');
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Because it is precious');
  });

  it('renders Rentila-inspired organic blobs and wave dividers without floating chips', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const blobs = compiled.querySelectorAll('.rentila-blob');
    expect(blobs.length).toBeGreaterThanOrEqual(2);

    const curves = compiled.querySelectorAll('.rentila-curve-divider');
    expect(curves.length).toBeGreaterThanOrEqual(3);

    // Floating chips were removed per user request
    const floatChips = compiled.querySelectorAll('.hero-float-chip');
    expect(floatChips.length).toBe(0);
  });

  it('toggles mobile menu drawer', () => {
    expect(component.mobileMenuOpen()).toBe(false);
    component.toggleMobileMenu();
    expect(component.mobileMenuOpen()).toBe(true);
    component.closeMobileMenu();
    expect(component.mobileMenuOpen()).toBe(false);
  });

  it('toggles FAQ item accordions', () => {
    expect(component.activeFaq()).toBe(0);
    component.toggleFaq(1);
    expect(component.activeFaq()).toBe(1);
    component.toggleFaq(1);
    expect(component.activeFaq()).toBeNull();
  });

  it('submits demo form and displays success state with reference ID and WhatsApp URL', () => {
    vi.spyOn(demoService, 'submitDemo').mockReturnValue(
      of({
        id: 'DEMO-2026-001',
        fullName: 'Dr. Test',
        organizationName: 'Clinique de Test',
        status: 'NEW',
        createdAt: new Date().toISOString(),
        message: 'Demande enregistrée avec succès.'
      })
    );

    expect(component.demoSuccess()).toBe(false);
    component.demoFullName = 'Dr. Test';
    component.demoOrgName = 'Clinique de Test';
    component.demoPhone = '+237 600000000';
    component.demoCity = 'Douala';

    component.submitDemoRequest();

    expect(component.isSubmittingDemo()).toBe(false);
    expect(component.demoSuccess()).toBe(true);
    expect(component.demoSubmittedLead()?.id).toBe('DEMO-2026-001');
    expect(component.demoWhatsAppUrl()).toContain('wa.me/237691501780');

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('DEMO-2026-001');
  });

  function fillForm(): void {
    component.demoFullName = 'Dr. Test';
    component.demoOrgName = 'Clinique Test';
    component.demoPhone = '+237600000000';
  }

  it('keeps the form and offers the correct contacts on HTTP failure, then allows retry', () => {
    const submit = vi.spyOn(demoService, 'submitDemo').mockReturnValue(throwError(() => new Error('Offline')));
    fillForm();
    component.submitDemoRequest();
    fixture.detectChanges();
    expect(component.demoSuccess()).toBe(false);
    expect(component.demoError()).toBe(true);
    expect(component.isSubmittingDemo()).toBe(false);
    expect(component.demoFullName).toBe('Dr. Test');
    expect(fixture.nativeElement.querySelector('.demo-form [role="alert"]').textContent)
      .toContain(component.t().demo.errorDesc);
    expect(fixture.nativeElement.querySelector('a[href^="https://wa.me/"]').href)
      .toContain('wa.me/237691501780');
    expect(fixture.nativeElement.querySelector('a[href="mailto:contact@joprelys.com"]')).toBeTruthy();
    submit.mockReturnValue(of({ id: 'retry-id' } as DemoLeadResponse));
    component.submitDemoRequest();
    expect(component.demoError()).toBe(false);
    expect(component.demoSuccess()).toBe(true);
  });

  it('never confirms a null response and translates the error and links to English', () => {
    vi.spyOn(demoService, 'submitDemo').mockReturnValue(of(null as unknown as DemoLeadResponse));
    fillForm();
    component.toggleLang();
    component.submitDemoRequest();
    fixture.detectChanges();
    expect(component.demoSuccess()).toBe(false);
    expect(component.demoError()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Your request could not be confirmed');
    expect(decodeURIComponent(component.demoWhatsAppUrl())).toContain('Hello Joprelys Connect');
  });

  it('prevents a second request while the first is pending', () => {
    const pending = new Subject<DemoLeadResponse>();
    const submit = vi.spyOn(demoService, 'submitDemo').mockReturnValue(pending);
    fillForm();
    component.submitDemoRequest();
    component.submitDemoRequest();
    expect(submit).toHaveBeenCalledOnce();
    expect(component.isSubmittingDemo()).toBe(true);
    pending.error(new Error('Offline'));
    expect(component.isSubmittingDemo()).toBe(false);
  });

  it('rejects whitespace-only required fields without submitting', () => {
    const submit = vi.spyOn(demoService, 'submitDemo');
    fillForm();
    component.demoFullName = '   ';
    component.submitDemoRequest();
    expect(submit).not.toHaveBeenCalled();
  });

  it('keeps direct contacts available before submission and validates the optional email', async () => {
    for (const [id, value] of [['demo-name', 'Dr. Test'], ['demo-org', 'Clinic'],
      ['demo-phone', '+237600000000'], ['demo-email', 'invalid-email']]) {
      const input = fixture.nativeElement.querySelector(`#${id}`) as HTMLInputElement;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.demo-form button[type="submit"]').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('a[href="mailto:contact@joprelys.com"]')).toBeTruthy();
    const email = fixture.nativeElement.querySelector('#demo-email') as HTMLInputElement;
    email.value = '';
    email.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.demo-form button[type="submit"]').disabled).toBe(false);
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { LandingPageComponent } from './landing-page.component';
import { I18nService } from '../core/i18n/i18n.service';
import { LandingDemoService } from './landing-demo.service';

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
    expect(component.demoWhatsAppUrl()).toContain('wa.me/237691893198');

    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('DEMO-2026-001');
  });
});

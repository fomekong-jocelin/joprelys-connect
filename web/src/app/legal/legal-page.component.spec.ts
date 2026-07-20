import { signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { ThemeService } from '../core/theme/theme.service';
import { LegalPageComponent } from './legal-page.component';

describe('LegalPageComponent', () => {
  let mockI18n: {
    locale: WritableSignal<'fr' | 'en'>;
    t: ReturnType<typeof vi.fn>;
    setLocale: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    localStorage.clear();
    mockI18n = {
      locale: signal<'fr' | 'en'>('fr'),
      t: vi.fn((key: string) => key),
      setLocale: vi.fn(async (lang: 'fr' | 'en') => mockI18n.locale.set(lang)),
    };

    await TestBed.configureTestingModule({
      imports: [LegalPageComponent],
      providers: [
        provideRouter([]),
        { provide: I18nService, useValue: mockI18n },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { data: { legalDocument: 'privacy' } } },
        },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should render the selected legal document and all public navigation entries', () => {
    const fixture = TestBed.createComponent(LegalPageComponent);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const navigationLinks = root.querySelectorAll('.legal-navigation nav a');

    expect(root.querySelector('app-logo')).not.toBeNull();
    expect(root.querySelector('h1')?.textContent).toContain('legal.privacy.title');
    expect(navigationLinks.length).toBe(7);
    expect(root.textContent).toContain('confidentialite@joprelys.com');
    expect(root.textContent).toContain('legal.common.reviewNotice');
  });

  it('should switch the public page language', () => {
    const fixture = TestBed.createComponent(LegalPageComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const buttons = root.querySelectorAll<HTMLButtonElement>('.legal-language-switch button');
    buttons[1].click();

    expect(mockI18n.setLocale).toHaveBeenCalledWith('en');
    expect(component.locale()).toBe('en');
  });

  it('should switch and persist the theme', () => {
    const themeService = TestBed.inject(ThemeService);
    themeService.setTheme('light');

    const fixture = TestBed.createComponent(LegalPageComponent);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    root.querySelector<HTMLButtonElement>('.legal-icon-button')?.click();

    expect(themeService.theme()).toBe('dark');
    expect(localStorage.getItem('joprelys.theme')).toBe('dark');
  });
});

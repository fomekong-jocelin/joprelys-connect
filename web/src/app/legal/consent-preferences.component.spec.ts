import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { CONSENT_STORAGE_KEY, ConsentManagementService } from '../core/privacy/consent-management.service';
import { ThemeService } from '../core/theme/theme.service';
import { ConsentPreferencesComponent } from './consent-preferences.component';

describe('ConsentPreferencesComponent', () => {
  let mockI18n: {
    locale: ReturnType<typeof signal<'fr' | 'en'>>;
    t: ReturnType<typeof vi.fn>;
    setLocale: ReturnType<typeof vi.fn>;
  };
  let mockTheme: {
    theme: ReturnType<typeof signal<'light' | 'dark'>>;
    setTheme: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    localStorage.clear();
    mockI18n = {
      locale: signal<'fr' | 'en'>('fr'),
      t: vi.fn((key: string) => key),
      setLocale: vi.fn(async (lang: 'fr' | 'en') => mockI18n.locale.set(lang)),
    };
    mockTheme = {
      theme: signal<'light' | 'dark'>('light'),
      setTheme: vi.fn((theme: 'light' | 'dark') => mockTheme.theme.set(theme)),
    };

    await TestBed.configureTestingModule({
      imports: [ConsentPreferencesComponent],
      providers: [
        provideRouter([]),
        { provide: I18nService, useValue: mockI18n },
        { provide: ThemeService, useValue: mockTheme },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should render the four consent categories with optional choices denied by default', () => {
    const fixture = TestBed.createComponent(ConsentPreferencesComponent);
    fixture.detectChanges();

    const root = fixture.nativeElement as HTMLElement;
    const checkbox = root.querySelector<HTMLInputElement>('input[type="checkbox"]');

    expect(root.textContent).toContain('consent.category.necessary.title');
    expect(root.textContent).toContain('consent.category.preferences.title');
    expect(root.textContent).toContain('consent.category.analytics.title');
    expect(root.textContent).toContain('consent.category.marketing.title');
    expect(checkbox?.checked).toBe(false);
  });

  it('should save available preference consent and persist the current interface choices', () => {
    const fixture = TestBed.createComponent(ConsentPreferencesComponent);
    const component = fixture.componentInstance;
    const service = TestBed.inject(ConsentManagementService);

    component.preferencesEnabled.set(true);
    component.save();

    expect(service.preferencesAllowed()).toBe(true);
    expect(localStorage.getItem(CONSENT_STORAGE_KEY)).not.toBeNull();
    expect(mockTheme.setTheme).toHaveBeenCalledWith('light');
    expect(mockI18n.setLocale).toHaveBeenCalledWith('fr');
    expect(component.feedbackKey()).toBe('consent.actions.saved');
  });

  it('should reject optional preferences and clear known preference storage', () => {
    localStorage.setItem('joprelys.theme', 'dark');
    localStorage.setItem('joprelys.locale', 'en');

    const fixture = TestBed.createComponent(ConsentPreferencesComponent);
    const component = fixture.componentInstance;
    component.rejectOptional();

    expect(localStorage.getItem('joprelys.theme')).toBeNull();
    expect(localStorage.getItem('joprelys.locale')).toBeNull();
    expect(component.preferencesEnabled()).toBe(false);
    expect(component.feedbackKey()).toBe('consent.actions.rejected');
  });
});

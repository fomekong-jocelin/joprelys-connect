import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { LegalFooterLinkDirective } from './legal-footer-link.directive';

@Component({
  imports: [LegalFooterLinkDirective],
  template: `
    <a href="#">Conditions</a>
    <a href="#">Confidentialité</a>
  `,
})
class TestHostComponent {}

describe('LegalFooterLinkDirective', () => {
  const navigateByUrl = vi.fn();

  beforeEach(async () => {
    navigateByUrl.mockReset();
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [
        { provide: Router, useValue: { navigateByUrl } },
        {
          provide: I18nService,
          useValue: {
            locale: signal<'fr' | 'en'>('fr'),
            t: (key: string) => ({
              'common.footer.terms': 'Conditions',
              'common.footer.privacy': 'Confidentialité',
            }[key] ?? key),
          },
        },
      ],
    }).compileComponents();
  });

  it('should route the terms and privacy footer links', () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll<HTMLAnchorElement>('a');
    links[0].click();
    links[1].click();

    expect(navigateByUrl).toHaveBeenNthCalledWith(1, '/legal/terms');
    expect(navigateByUrl).toHaveBeenNthCalledWith(2, '/legal/privacy');
  });
});

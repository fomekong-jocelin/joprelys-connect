import { Directive, ElementRef, inject } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';

@Directive({
  selector: 'a[href="#"]',
  host: {
    '(click)': 'onClick($event)',
  },
})
export class LegalFooterLinkDirective {
  private readonly element = inject<ElementRef<HTMLAnchorElement>>(ElementRef);
  private readonly router = inject(Router);
  private readonly i18n = inject(I18nService);

  onClick(event: Event): void {
    event.preventDefault();
    const label = this.element.nativeElement.textContent?.trim();

    if (label === this.i18n.t('common.footer.terms')) {
      void this.router.navigateByUrl('/legal/terms');
      return;
    }

    if (label === this.i18n.t('common.footer.privacy')) {
      void this.router.navigateByUrl('/legal/privacy');
      return;
    }

    window.location.href = 'mailto:support@joprelys.com';
  }
}

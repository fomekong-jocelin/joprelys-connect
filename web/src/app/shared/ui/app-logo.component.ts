import { Component, input } from '@angular/core';
import { APP_BRAND_CONFIG } from '../../core/config/app-brand.config';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="flex items-center gap-2.5">
      <svg class="h-8 w-8 shrink-0" viewBox="0 0 100 100" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <path d="M30 65 C15 50, 15 30, 30 15 C45 0, 65 0, 80 15 C95 30, 95 50, 80 65 L65 80" stroke="url(#logo-grad)" stroke-width="12" stroke-linecap="round" fill="none"/>
        <path d="M70 35 C85 50, 85 70, 70 85 C55 100, 35 100, 20 85 C5 70, 5 50, 20 35 L35 20" stroke="url(#logo-grad-reverse)" stroke-width="12" stroke-linecap="round" fill="none"/>
        <defs>
          <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stop-color="#0B91B2" />
            <stop offset="100%" stop-color="#0A1D3D" />
          </linearGradient>
          <linearGradient id="logo-grad-reverse" x1="100%" y1="100%" x2="0%" y2="0%">
            <stop offset="0%" stop-color="#0B91B2" />
            <stop offset="100%" stop-color="#16A34A" />
          </linearGradient>
        </defs>
      </svg>
      @if (showName()) {
        <span class="font-display text-xl font-extrabold tracking-tight" style="color: var(--text-primary)">
          {{ appNamePrefix }}<span style="color: var(--brand-primary)">{{ appNameSuffix }}</span>
        </span>
      }
    </div>
  `,
})
export class AppLogoComponent {
  readonly showName = input(true);
  readonly appNamePrefix = APP_BRAND_CONFIG.appShortName;
  readonly appNameSuffix = ' Connect';
}

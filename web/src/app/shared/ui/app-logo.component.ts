import { Component, computed, input } from '@angular/core';
import { APP_BRAND_CONFIG } from '../../core/config/app-brand.config';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="flex items-center" [class]="size() === 'lg' ? 'gap-3' : 'gap-2.5'">
      <img
        [src]="logoPath()"
        [alt]="brand.appShortName"
        [class]="size() === 'lg' ? 'h-9 w-auto' : 'h-8 w-auto'"
        class="app-logo-image shrink-0 transition-all duration-300"
        [class.app-logo-image-on-dark]="appearance() === 'on-dark'"
      />
      @if (showName()) {
        <span
          class="font-display font-extrabold tracking-tight transition-colors duration-300"
          [class]="size() === 'lg' ? 'text-2xl' : 'text-xl'"
          [style.color]="
            appearance() === 'on-dark' ? 'var(--color-brand-gray)' : 'var(--brand-primary)'
          "
        >
          {{ brand.productName }}
        </span>
      }
    </div>
  `,
  styles: `
    :host-context([data-theme='dark']) .app-logo-image:not(.app-logo-image-on-dark) {
      filter: brightness(0) invert(1);
    }

    .app-logo-image-on-dark {
      filter: none !important;
    }
  `,
})
export class AppLogoComponent {
  readonly brand = APP_BRAND_CONFIG;
  readonly showName = input(true);
  readonly size = input<'md' | 'lg'>('md');
  readonly appearance = input<'default' | 'on-dark'>('default');
  readonly logoPath = computed(() =>
    this.appearance() === 'on-dark' ? this.brand.logoOnDarkPath : this.brand.logoPath,
  );
}

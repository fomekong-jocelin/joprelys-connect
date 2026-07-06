import { Component, input } from '@angular/core';

@Component({
  selector: 'app-logo',
  standalone: true,
  template: `
    <div class="flex items-center" [class]="size() === 'lg' ? 'gap-3' : 'gap-2.5'">
      <img
        src="assets/branding/logo_principal.png"
        alt="Joprelys"
        [class]="size() === 'lg' ? 'h-9 w-auto' : 'h-8 w-auto'"
        class="shrink-0 transition-all duration-300 dark:brightness-0 dark:invert"
      />
      @if (showName()) {
        <span
          class="font-display font-extrabold tracking-tight transition-colors duration-300"
          [class]="size() === 'lg' ? 'text-2xl' : 'text-xl'"
          style="color: var(--brand-primary)"
        >
          Connect
        </span>
      }
    </div>
  `,
})
export class AppLogoComponent {
  readonly showName = input(true);
  readonly size = input<'md' | 'lg'>('md');
}


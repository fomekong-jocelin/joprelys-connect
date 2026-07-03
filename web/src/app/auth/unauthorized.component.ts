import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AppShellComponent } from '../shared/layout/app-shell.component';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [AppShellComponent],
  template: `
    <app-shell>
      <div class="min-h-[60vh] flex flex-col items-center justify-center p-6 transition-colors duration-300">
        <div class="w-full max-w-[400px] text-center space-y-6">
          <div class="flex justify-center">
            <div class="w-16 h-16 rounded-full bg-red-50 dark:bg-red-950/20 flex items-center justify-center text-red-500">
              <svg class="h-8 w-8" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
          </div>

          <div class="space-y-2">
            <h1 class="font-display font-bold text-2xl text-brand-night dark:text-white">Accès refusé</h1>
            <p class="text-sm text-slate-500 dark:text-slate-400">Vous n'avez pas les autorisations nécessaires pour accéder à cette page.</p>
          </div>

          <button 
            (click)="goBack()"
            class="w-full min-h-[46px] flex items-center justify-center py-3 px-4 rounded-xl text-sm font-semibold text-white bg-brand-cyan hover:bg-[#097b98] active:bg-[#076881] focus:outline-hidden transition-all duration-150 cursor-pointer"
          >
            Retour au tableau de bord
          </button>
        </div>
      </div>
    </app-shell>
  `
})
export class UnauthorizedComponent {
  private readonly router = inject(Router);

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }
}


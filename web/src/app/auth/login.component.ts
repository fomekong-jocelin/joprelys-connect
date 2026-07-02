import { Component, computed, inject, signal, effect } from '@angular/core';
import { Router } from '@angular/router';
import { AuthApiService } from './auth-api.service';
import { AuthTokenStorageService } from './auth-token-storage.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly authApi = inject(AuthApiService);
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly router = inject(Router);

  readonly email = signal('');
  readonly password = signal('');
  readonly error = signal<string | null>(null);
  readonly loading = signal(false);
  readonly session = this.tokenStorage.session;
  readonly canSubmit = computed(() => this.isValidEmail(this.email()) && this.password().length > 0 && !this.loading());

  constructor() {
    effect(() => {
      if (this.session()) {
        this.router.navigate(['/dashboard']);
      }
    });
  }

  submit(): void {
    this.error.set(null);
    if (!this.canSubmit()) {
      this.error.set('Renseignez un e-mail valide et un mot de passe.');
      return;
    }

    this.loading.set(true);
    this.authApi.login({ email: this.email(), password: this.password() }).subscribe({
      next: () => {
        this.password.set('');
        this.loading.set(false);
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.error.set('Identifiants invalides.');
        this.loading.set(false);
      },
    });
  }

  logout(): void {
    this.loading.set(true);
    this.authApi.logout().subscribe({
      next: () => this.loading.set(false),
      error: () => this.loading.set(false),
    });
  }

  updateEmail(event: Event): void {
    this.email.set(this.inputValue(event));
  }

  updatePassword(event: Event): void {
    this.password.set(this.inputValue(event));
  }

  private inputValue(event: Event): string {
    return event.target instanceof HTMLInputElement ? event.target.value : '';
  }

  private isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }
}

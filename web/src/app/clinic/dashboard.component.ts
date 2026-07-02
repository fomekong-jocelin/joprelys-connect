import { Component, computed, inject } from '@angular/core';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { RouterLink } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  imports: [AppShellComponent, RouterLink]
})
export class DashboardComponent {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly i18n = inject(I18nService);

  readonly session = this.tokenStorage.session;
  readonly welcomeLabel = computed(() => this.i18n.t('dashboard.welcome'));
  readonly authorizedLabel = computed(() => this.i18n.t('dashboard.authorized'));
  readonly roleLabel = computed(() => this.i18n.t('dashboard.role'));
}

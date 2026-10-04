import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from './organization-api.service';
import { Organization } from './organizations.models';

@Component({
  selector: 'app-interop-page',
  imports: [AppShellComponent, PageHeaderComponent, RouterLink],
  template: `
    <app-shell>
      <app-page-header [title]="t('interop.title')" [subtitle]="t('interop.subtitle')" />
      <main class="app-container space-y-4 pb-10">
        @if (loading()) { <p role="status">{{ t('common.loading') }}</p> }
        @else if (error()) { <p role="alert" class="text-[var(--brand-danger-text)]">{{ t('interop.error') }}</p> }
        @else {
          @for (organization of organizations(); track organization.id) {
            <section class="ui-card p-4 flex flex-wrap items-center justify-between gap-3">
              <div><h2 class="font-semibold">{{ organization.name }}</h2>
                <p class="text-sm text-[var(--text-secondary)]">{{ t(organization.apiEnabled ? 'interop.enabled' : 'interop.disabled') }}</p>
              </div>
              <a class="ui-button ui-button-secondary" routerLink="/organizations" [queryParams]="{organizationId: organization.id}">{{ t('interop.manage') }}</a>
            </section>
          } @empty { <p>{{ t('interop.empty') }}</p> }
        }
      </main>
    </app-shell>
  `,
})
export class InteropPageComponent implements OnInit {
  private readonly api = inject(OrganizationApiService);
  private readonly i18n = inject(I18nService);
  readonly organizations = signal<Organization[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly t = (key: string) => this.i18n.t(key);
  ngOnInit(): void {
    this.api.list().subscribe({
      next: values => { this.organizations.set(values); this.loading.set(false); },
      error: () => { this.error.set(true); this.loading.set(false); },
    });
  }
}

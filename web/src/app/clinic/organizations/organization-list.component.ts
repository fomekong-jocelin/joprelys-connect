import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { OrganizationApiService } from './organization-api.service';
import { OrganizationFormComponent, OrganizationFormLabels } from './organization-form.component';
import { OrganizationTableComponent, OrganizationTableLabels } from './organization-table.component';
import { Organization } from './organizations.models';

@Component({
  selector: 'app-organization-list',
  templateUrl: './organization-list.component.html',
  imports: [AlertComponent, AppShellComponent, ButtonComponent, OrganizationFormComponent, OrganizationTableComponent, PageHeaderComponent],
})
export class OrganizationListComponent implements OnInit {
  private readonly api = inject(OrganizationApiService);
  private readonly i18n = inject(I18nService);

  readonly list = signal<Organization[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly name = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly address = signal('');
  readonly city = signal('');
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly showCreateForm = signal(false);

  readonly pageTitle = computed(() => this.i18n.t('organizations.title'));
  readonly pageSubtitle = computed(() => this.i18n.t('organizations.subtitle'));
  readonly createLabel = computed(() =>
    this.showCreateForm() ? this.i18n.t('common.cancel') : this.i18n.t('organizations.create')
  );
  readonly backLabel = computed(() => this.i18n.t('common.back'));
  readonly formLabels = computed<OrganizationFormLabels>(() => ({
    title: this.i18n.t('organizations.createTitle'),
    name: this.i18n.t('organizations.name'),
    namePlaceholder: this.i18n.t('organizations.namePlaceholder'),
    email: this.i18n.t('organizations.contactEmail'),
    emailPlaceholder: 'Ex: contact@saintjean.local',
    phone: this.i18n.t('organizations.phone'),
    phonePlaceholder: this.i18n.t('organizations.phonePlaceholder'),
    city: this.i18n.t('organizations.city'),
    cityPlaceholder: this.i18n.t('organizations.cityPlaceholder'),
    address: this.i18n.t('organizations.address'),
    addressPlaceholder: this.i18n.t('organizations.addressPlaceholder'),
    cancel: this.i18n.t('common.cancel'),
    save: this.i18n.t('common.save'),
    saving: this.i18n.t('common.saving'),
  }));
  readonly tableLabels = computed<OrganizationTableLabels>(() => ({
    title: this.i18n.t('organizations.tableTitle'),
    loading: this.i18n.t('common.loading'),
    empty: this.i18n.t('organizations.empty'),
    clinic: this.i18n.t('organizations.name'),
    city: this.i18n.t('organizations.city'),
    contact: this.i18n.t('common.contact'),
    address: this.i18n.t('organizations.address'),
    status: this.i18n.t('organizations.status'),
    actions: this.i18n.t('common.actions'),
    active: this.i18n.t('common.active'),
    inactive: this.i18n.t('common.inactive'),
    activate: this.i18n.t('common.activate'),
    deactivate: this.i18n.t('common.deactivate'),
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.list().subscribe({
      next: (res) => {
        this.list.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('organizations.loadError'));
        this.loading.set(false);
      }
    });
  }

  toggleStatus(org: Organization): void {
    const nextStatus = org.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.api.updateStatus(org.id, nextStatus).subscribe({
      next: (updated) => {
        this.list.update(items => items.map(item => item.id === org.id ? updated : item));
      },
      error: () => {
        this.error.set(this.i18n.t('organizations.statusError'));
      }
    });
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((visible) => !visible);
    if (this.showCreateForm()) {
      return;
    }
    this.resetForm();
  }

  cancelCreate(): void {
    this.showCreateForm.set(false);
    this.resetForm();
  }

  submit(): void {
    this.formError.set(null);
    if (!this.name() || !this.email() || !this.city()) {
      this.formError.set(this.i18n.t('organizations.requiredFields'));
      return;
    }

    this.formLoading.set(true);
    this.api.create({
      name: this.name(),
      email: this.email(),
      phone: this.phone(),
      address: this.address(),
      city: this.city()
    }).subscribe({
      next: (res) => {
        this.list.update(items => [...items, res]);
        this.formLoading.set(false);
        this.showCreateForm.set(false);
        this.resetForm();
      },
      error: (err) => {
        if (err.status === 409) {
          this.formError.set(this.i18n.t('organizations.duplicateEmail'));
        } else if (err && err.status === 401) {
          this.formError.set(this.i18n.t('common.error.unauthorized'));
        } else if (err && err.status === 403) {
          this.formError.set(this.i18n.t('common.error.forbidden'));
        } else if (err && err.status >= 500) {
          this.formError.set(this.i18n.t('common.error.server'));
        } else if (err && err.error && err.error.detail) {
          this.formError.set(err.error.detail);
        } else {
          this.formError.set(this.i18n.t('organizations.saveError'));
        }
        this.formLoading.set(false);
      }
    });
  }

  resetForm(): void {
    this.name.set('');
    this.email.set('');
    this.phone.set('');
    this.address.set('');
    this.city.set('');
    this.formError.set(null);
  }
}

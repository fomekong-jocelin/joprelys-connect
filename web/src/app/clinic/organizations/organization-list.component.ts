import { Component, computed, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { FileDragDropComponent } from '../../shared/ui/file-drag-drop.component';
import { OrganizationApiService } from './organization-api.service';
import { OrganizationFormComponent, OrganizationFormLabels } from './organization-form.component';
import { OrganizationTableComponent, OrganizationTableLabels } from './organization-table.component';
import { CreateClinicAdminResponse, Organization, ApiKey } from './organizations.models';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-organization-list',
  templateUrl: './organization-list.component.html',
  imports: [
    AlertComponent,
    AppShellComponent,
    ButtonComponent,
    OrganizationFormComponent,
    OrganizationTableComponent,
    PageHeaderComponent,
    FileDragDropComponent,
    FormsModule
  ],
})
export class OrganizationListComponent implements OnInit {
  private readonly api = inject(OrganizationApiService);
  private readonly route = inject(ActivatedRoute, { optional: true });
  readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly list = signal<Organization[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  // --- Formulaire création/édition clinique ---
  readonly name = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly address = signal('');
  readonly city = signal('');
  readonly country = signal('Cameroun');
  readonly type = signal('CLINIC');
  readonly responsibleName = signal('');
  readonly apiEnabled = signal(true);
  readonly logoPath = signal<string | null>(null);

  readonly logoViewUrl = computed(() => this.logoPath() ? `/api/public/files/view?path=${this.logoPath()}` : null);

  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly showCreateForm = signal(false);

  // --- Organisation Sélectionnée (Détails & Édition) ---
  readonly selectedOrg = signal<Organization | null>(null);
  readonly isEditingSelectedOrg = signal(false);

  // --- Clés API ---
  readonly apiKeys = signal<ApiKey[]>([]);
  readonly loadingApiKeys = signal(false);
  readonly newKeyName = signal('');
  readonly generatedKey = signal<ApiKey | null>(null);
  readonly apiKeyCopied = signal(false);

  // --- Formulaire création admin clinique ---
  readonly adminTargetOrg = signal<Organization | null>(null);
  readonly adminDisplayName = signal('');
  readonly adminEmail = signal('');
  readonly adminFormLoading = signal(false);
  readonly adminFormError = signal<string | null>(null);
  readonly adminCreatedResult = signal<CreateClinicAdminResponse | null>(null);
  readonly adminPasswordCopied = signal(false);

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
    emailPlaceholder: this.i18n.t('organizations.emailPlaceholder'),
    phone: this.i18n.t('organizations.phone'),
    phonePlaceholder: this.i18n.t('organizations.phonePlaceholder'),
    city: this.i18n.t('organizations.city'),
    cityPlaceholder: this.i18n.t('organizations.cityPlaceholder'),
    address: this.i18n.t('organizations.address'),
    addressPlaceholder: this.i18n.t('organizations.addressPlaceholder'),
    country: this.i18n.t('organizations.country'),
    countryPlaceholder: this.i18n.t('organizations.countryPlaceholder'),
    type: this.i18n.t('organizations.type'),
    responsibleName: this.i18n.t('organizations.responsibleName'),
    responsibleNamePlaceholder: this.i18n.t('organizations.responsibleNamePlaceholder'),
    apiEnabled: this.i18n.t('organizations.apiEnabled'),
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
    assignAdmin: this.i18n.t('organizations.assignAdmin'),
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
        const selected = res.find(org => org.id === this.route?.snapshot.queryParamMap.get('organizationId'));
        if (selected) this.selectOrg(selected);
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
        const fullUpdated: Organization = {
          ...updated,
          adminEmail: org.adminEmail,
          adminDisplayName: org.adminDisplayName
        };
        this.list.update(items => items.map(item => item.id === org.id ? fullUpdated : item));
        if (this.selectedOrg()?.id === org.id) {
          this.selectedOrg.set(fullUpdated);
        }
      },
      error: () => {
        this.error.set(this.i18n.t('organizations.statusError'));
      }
    });
  }

  toggleCreateForm(): void {
    this.showCreateForm.update((visible) => !visible);
    this.selectedOrg.set(null);
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
    if (!this.name().trim() || !this.email().trim() || !this.city().trim() || !this.country().trim() || !this.responsibleName().trim()) {
      this.formError.set(this.i18n.t('organizations.requiredFields'));
      return;
    }

    this.formLoading.set(true);
    this.api.create({
      name: this.name().trim(),
      email: this.email().trim(),
      phone: this.phone().trim(),
      address: this.address().trim(),
      city: this.city().trim(),
      country: this.country().trim(),
      type: this.type().trim(),
      responsibleName: this.responsibleName().trim(),
      apiEnabled: this.apiEnabled(),
      logoPath: this.logoPath() || undefined
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
    this.country.set('Cameroun');
    this.type.set('CLINIC');
    this.responsibleName.set('');
    this.apiEnabled.set(true);
    this.logoPath.set(null);
    this.formError.set(null);
  }

  // --- Sélection clinique (Tiroir) ---

  selectOrg(org: Organization): void {
    this.selectedOrg.set(org);
    this.isEditingSelectedOrg.set(false);
    this.formError.set(null);
    this.showCreateForm.set(false);
    this.generatedKey.set(null);
    this.newKeyName.set('');
    this.loadApiKeys(org.id);
  }

  closeDrawer(): void {
    this.selectedOrg.set(null);
    this.isEditingSelectedOrg.set(false);
    this.formError.set(null);
    this.generatedKey.set(null);
  }

  startEditing(): void {
    const org = this.selectedOrg();
    if (!org) return;
    this.name.set(org.name);
    this.email.set(org.email);
    this.phone.set(org.phone || '');
    this.address.set(org.address || '');
    this.city.set(org.city);
    this.country.set(org.country || 'Cameroun');
    this.type.set(org.type || 'CLINIC');
    this.responsibleName.set(org.responsibleName || '');
    this.apiEnabled.set(org.apiEnabled !== undefined ? org.apiEnabled : true);
    this.logoPath.set(org.logoPath || null);
    this.isEditingSelectedOrg.set(true);
    this.formError.set(null);
  }

  cancelEditing(): void {
    this.isEditingSelectedOrg.set(false);
    this.formError.set(null);
  }

  submitUpdate(): void {
    this.formError.set(null);
    const org = this.selectedOrg();
    if (!org) return;

    if (!this.name().trim() || !this.email().trim() || !this.city().trim() || !this.country().trim() || !this.responsibleName().trim()) {
      this.formError.set(this.i18n.t('organizations.requiredFields'));
      return;
    }

    this.formLoading.set(true);
    this.api.update(org.id, {
      name: this.name().trim(),
      email: this.email().trim(),
      phone: this.phone().trim(),
      address: this.address().trim(),
      city: this.city().trim(),
      country: this.country().trim(),
      type: this.type().trim(),
      responsibleName: this.responsibleName().trim(),
      apiEnabled: this.apiEnabled(),
      logoPath: this.logoPath() || undefined
    }).subscribe({
      next: (res) => {
        const updated: Organization = {
          ...res,
          adminEmail: org.adminEmail,
          adminDisplayName: org.adminDisplayName
        };
        this.list.update(items => items.map(item => item.id === org.id ? updated : item));
        this.selectedOrg.set(updated);
        this.formLoading.set(false);
        this.isEditingSelectedOrg.set(false);
      },
      error: (err) => {
        this.formLoading.set(false);
        if (err.status === 409) {
          this.formError.set(this.i18n.t('organizations.duplicateEmail'));
        } else {
          this.formError.set(err.error?.detail || "Erreur lors de la mise à jour");
        }
      }
    });
  }

  // --- Clés API ---

  loadApiKeys(orgId: string): void {
    this.loadingApiKeys.set(true);
    this.api.listApiKeys(orgId).subscribe({
      next: (keys) => {
        this.apiKeys.set(keys);
        this.loadingApiKeys.set(false);
      },
      error: () => {
        this.loadingApiKeys.set(false);
      }
    });
  }

  generateKey(): void {
    const org = this.selectedOrg();
    if (!org || !this.newKeyName().trim()) return;

    this.api.generateApiKey(org.id, { name: this.newKeyName().trim() }).subscribe({
      next: (key) => {
        this.apiKeys.update(keys => [...keys, key]);
        this.generatedKey.set(key);
        this.newKeyName.set('');
      }
    });
  }

  revokeKey(keyId: string): void {
    const org = this.selectedOrg();
    if (!org) return;

    this.api.revokeApiKey(org.id, keyId).subscribe({
      next: () => {
        this.apiKeys.update(keys =>
          keys.map(k => k.id === keyId ? { ...k, status: 'REVOKED', revokedAt: new Date().toISOString() } : k)
        );
      }
    });
  }

  copyRawKey(): void {
    const key = this.generatedKey();
    if (!key || !key.rawKey) return;

    navigator.clipboard.writeText(key.rawKey).then(() => {
      this.apiKeyCopied.set(true);
      setTimeout(() => this.apiKeyCopied.set(false), 2000);
    });
  }

  // --- Gestion du formulaire Admin Clinique ---

  openAdminForm(org: Organization): void {
    this.adminTargetOrg.set(org);
    this.adminDisplayName.set('');
    this.adminEmail.set('');
    this.adminFormError.set(null);
    this.adminCreatedResult.set(null);
    this.adminPasswordCopied.set(false);
    this.showCreateForm.set(false);
  }

  closeAdminForm(): void {
    this.adminTargetOrg.set(null);
    this.adminDisplayName.set('');
    this.adminEmail.set('');
    this.adminFormError.set(null);
    this.adminCreatedResult.set(null);
    this.adminPasswordCopied.set(false);
  }

  submitAdmin(): void {
    this.adminFormError.set(null);
    const org = this.adminTargetOrg();
    if (!org) return;

    if (!this.adminDisplayName().trim() || !this.adminEmail().trim()) {
      this.adminFormError.set(this.i18n.t('organizations.adminRequiredFields'));
      return;
    }

    this.adminFormLoading.set(true);
    this.api.createClinicAdmin(org.id, {
      displayName: this.adminDisplayName().trim(),
      email: this.adminEmail().trim(),
    }).subscribe({
      next: (res) => {
        this.adminCreatedResult.set(res);
        this.adminFormLoading.set(false);

        const updatedOrg: Organization = {
          ...org,
          adminEmail: res.email,
          adminDisplayName: res.displayName
        };
        this.list.update(items => items.map(item => item.id === org.id ? updatedOrg : item));

        if (this.selectedOrg()?.id === org.id) {
          this.selectedOrg.set(updatedOrg);
        }
      },
      error: (err) => {
        if (err.status === 409 || err.status === 400) {
          this.adminFormError.set(this.i18n.t('organizations.adminDuplicateEmail'));
        } else if (err && err.status === 404) {
          this.adminFormError.set(this.i18n.t('organizations.adminOrgNotFound'));
        } else if (this.apiErrorMessage(err)) {
          this.adminFormError.set(this.apiErrorMessage(err));
        } else {
          this.adminFormError.set(this.i18n.t('organizations.adminSaveError'));
        }
        this.adminFormLoading.set(false);
      }
    });
  }

  onLogoSelected(file: File, uploader: FileDragDropComponent): void {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', 'logo');
    this.http.post<any>('/api/files/upload', formData).subscribe({
      next: (res) => {
        this.logoPath.set(res.filePath);
        uploader.setPreviewUrl(res.viewUrl, file.name);
      },
      error: (err) => {
        this.formError.set(this.apiErrorMessage(err) || this.i18n.t('organizations.logoUploadError'));
      }
    });
  }

  private apiErrorMessage(err: unknown): string | null {
    if (!err || typeof err !== 'object') return null;
    const response = err as { error?: { detail?: unknown; error?: { message?: unknown } } };
    const message = response.error?.error?.message ?? response.error?.detail;
    return typeof message === 'string' && message.trim() ? message : null;
  }
}

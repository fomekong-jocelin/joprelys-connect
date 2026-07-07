import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { FileDragDropComponent } from '../../shared/ui/file-drag-drop.component';
import { StaffApiService } from './staff-api.service';
import { StaffMember, StaffRole } from './staff.models';
import { StaffTableComponent, StaffTableLabels } from './staff-table.component';

const STAFF_ROLES: readonly StaffRole[] = ['MEDECIN', 'INFIRMIER', 'AGENT_ACCUEIL', 'PHARMACIEN', 'BIOLOGISTE'];

@Component({
  selector: 'app-staff-management',
  standalone: true,
  imports: [
    AlertComponent,
    AppShellComponent,
    ButtonComponent,
    CardComponent,
    PageHeaderComponent,
    StaffTableComponent,
    FileDragDropComponent,
  ],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('staff.title')"
        [subtitle]="t('staff.subtitle')"
        backLink="/dashboard"
        [backLabel]="t('common.back')"
      >
        <app-ui-button class="w-full sm:w-auto" (pressed)="toggleInviteForm()">
          {{ showForm() ? t('common.cancel') : t('staff.invite') }}
        </app-ui-button>
      </app-page-header>

      <div class="app-container space-y-6 pb-10">
        @if (pageError(); as error) {
          <app-ui-alert tone="error">{{ error }}</app-ui-alert>
        }

        @if (temporaryPassword(); as password) {
          <app-ui-alert>
            <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p class="font-extrabold">{{ t('staff.temporaryPasswordTitle') }}</p>
                <p class="mt-1 text-sm">{{ t('staff.temporaryPasswordHelp') }}</p>
                <code class="mt-2 inline-flex rounded-md bg-white/80 px-3 py-1 font-mono text-base font-extrabold text-cyan-900">
                  {{ password }}
                </code>
              </div>
              <app-ui-button variant="secondary" (pressed)="copyTemporaryPassword()">
                {{ copyLabel() }}
              </app-ui-button>
            </div>
          </app-ui-alert>
        }

        @if (showForm()) {
          <app-ui-card [title]="formTitle()">
            <form class="space-y-5" (submit)="$event.preventDefault(); submitForm()">
              @if (formError(); as error) {
                <app-ui-alert tone="error">{{ error }}</app-ui-alert>
              }

              <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <label class="space-y-1.5">
                  <span class="ui-label">{{ t('staff.displayName') }} <span class="text-[var(--brand-danger)]">*</span></span>
                  <input
                    class="ui-input"
                    [value]="displayName()"
                    [placeholder]="t('staff.displayNamePlaceholder')"
                    [disabled]="formLoading()"
                    (input)="displayName.set($any($event.target).value)"
                  />
                </label>

                <label class="space-y-1.5">
                  <span class="ui-label">{{ t('staff.email') }} <span class="text-[var(--brand-danger)]">*</span></span>
                  <input
                    class="ui-input"
                    type="email"
                    [value]="email()"
                    [placeholder]="t('staff.emailPlaceholder')"
                    [disabled]="formLoading() || editingStaff() !== null"
                    (input)="email.set($any($event.target).value)"
                  />
                </label>

                <div class="space-y-2 sm:col-span-2">
                  <span class="ui-label block">{{ t('staff.role') }} <span class="text-[var(--brand-danger)]">*</span></span>
                  <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 mt-2">
                    @for (option of roles; track option) {
                      <label class="inline-flex items-center gap-2 select-none cursor-pointer">
                        <input
                          type="checkbox"
                          class="ui-checkbox"
                          [checked]="hasSelectedRole(option)"
                          [disabled]="formLoading()"
                          (change)="toggleSelectedRole(option)"
                        />
                        <span class="text-sm font-semibold" style="color: var(--text-primary)">{{ roleLabel(option) }}</span>
                      </label>
                    }
                  </div>
                </div>
              </div>

              @if (editingStaff() !== null) {
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2 mt-4 pt-4 border-t border-[var(--app-border)]">
                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('profile.phone') }}</span>
                    <input
                      class="ui-input"
                      type="tel"
                      [value]="phone()"
                      [placeholder]="t('profile.phonePlaceholder')"
                      [disabled]="formLoading()"
                      (input)="phone.set($any($event.target).value)"
                    />
                  </label>

                  <div class="space-y-1.5">
                    <span class="ui-label">{{ t('profile.department') }}</span>
                    <select
                      class="ui-select focus:border-brand-primary transition-colors"
                      [value]="selectedDept()"
                      [disabled]="formLoading()"
                      (change)="onDeptChange($any($event.target).value)"
                    >
                      <option value="">{{ t('profile.departmentPlaceholder') || 'Choisir un service...' }}</option>
                      @for (dept of departments; track dept) {
                        <option [value]="dept">{{ dept }}</option>
                      }
                      <option value="Autre">Autre (Saisir...)</option>
                    </select>

                    @if (selectedDept() === 'Autre') {
                      <input
                        class="ui-input mt-2 focus:border-brand-primary transition-colors"
                        [value]="customDept()"
                        placeholder="Saisir le nom du service..."
                        [disabled]="formLoading()"
                        (input)="onCustomDeptInput($any($event.target).value)"
                      />
                    }
                  </div>

                  @if (isDoctorSelected()) {
                    <label class="space-y-1.5">
                      <span class="ui-label">{{ t('profile.specialty') }}</span>
                      <input
                        class="ui-input"
                        [value]="specialty()"
                        [placeholder]="t('profile.specialtyPlaceholder')"
                        [disabled]="formLoading()"
                        (input)="specialty.set($any($event.target).value)"
                      />
                    </label>

                    <label class="space-y-1.5">
                      <span class="ui-label">{{ t('profile.registrationNumber') }}</span>
                      <input
                        class="ui-input"
                        [value]="registrationNumber()"
                        [placeholder]="t('profile.registrationNumberPlaceholder')"
                        [disabled]="formLoading()"
                        (input)="registrationNumber.set($any($event.target).value)"
                      />
                    </label>
                  }

                  <div class="sm:col-span-2">
                    <label class="space-y-1.5 block">
                      <span class="ui-label">{{ t('profile.bio') }}</span>
                      <textarea
                        class="ui-input h-20 resize-y py-2"
                        [value]="bio()"
                        [placeholder]="t('profile.bioPlaceholder')"
                        [disabled]="formLoading()"
                        (input)="bio.set($any($event.target).value)"
                      ></textarea>
                    </label>
                  </div>

                  <div class="sm:col-span-2 grid grid-cols-1 md:grid-cols-3 gap-4 mt-2">
                    <app-file-drag-drop
                      #photoUploader
                      [label]="t('profile.photoLabel')"
                      [previewUrl]="photoViewUrl()"
                      (fileSelected)="onFileSelected($event, 'photo', photoUploader)"
                      (fileRemoved)="photoPath.set(null)"
                    >
                    </app-file-drag-drop>

                    @if (isDoctorSelected()) {
                      <app-file-drag-drop
                        #sigUploader
                        [label]="t('profile.signatureLabel')"
                        [previewUrl]="signatureViewUrl()"
                        (fileSelected)="onFileSelected($event, 'signature', sigUploader)"
                        (fileRemoved)="signaturePath.set(null)"
                      >
                      </app-file-drag-drop>

                      <app-file-drag-drop
                        #stampUploader
                        [label]="t('profile.stampLabel')"
                        [previewUrl]="stampViewUrl()"
                        (fileSelected)="onFileSelected($event, 'stamp', stampUploader)"
                        (fileRemoved)="stampPath.set(null)"
                      >
                      </app-file-drag-drop>
                    }
                  </div>
                </div>
              }

              <div class="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <app-ui-button variant="secondary" class="w-full sm:w-auto" (pressed)="cancelForm()">
                  {{ t('common.cancel') }}
                </app-ui-button>
                <app-ui-button type="submit" class="w-full sm:w-auto" [disabled]="formLoading()">
                  {{ formLoading() ? t('common.saving') : submitLabel() }}
                </app-ui-button>
              </div>
            </form>
          </app-ui-card>
        }

        <app-staff-table
          [staff]="staff()"
          [loading]="loading()"
          [labels]="tableLabels()"
          (editRequested)="startEdit($event)"
          (statusToggled)="toggleStatus($event)"
        />
      </div>
    </app-shell>
  `,
})
export class StaffManagementComponent implements OnInit {
  private readonly api = inject(StaffApiService);
  private readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly roles = STAFF_ROLES;
  readonly staff = signal<StaffMember[]>([]);
  readonly loading = signal(false);
  readonly pageError = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly editingStaff = signal<StaffMember | null>(null);
  readonly temporaryPassword = signal<string | null>(null);
  readonly passwordCopied = signal(false);

  readonly displayName = signal('');
  readonly email = signal('');
  readonly selectedRoles = signal<string[]>(['MEDECIN']);

  readonly phone = signal('');
  readonly specialty = signal('');
  readonly registrationNumber = signal('');
  readonly department = signal('');
  readonly bio = signal('');

  readonly departments = ['Médecine générale', 'Pédiatrie', 'Gynécologie', 'Urgences', 'Pharmacie', 'Laboratoire', 'Cardiologie'];
  readonly selectedDept = signal('');
  readonly customDept = signal('');
  
  readonly photoPath = signal<string | null>(null);
  readonly signaturePath = signal<string | null>(null);
  readonly stampPath = signal<string | null>(null);

  readonly photoViewUrl = computed(() => this.photoPath() ? `/api/public/files/view?path=${this.photoPath()}` : null);
  readonly signatureViewUrl = computed(() => this.signaturePath() ? `/api/public/files/view?path=${this.signaturePath()}` : null);
  readonly stampViewUrl = computed(() => this.stampPath() ? `/api/public/files/view?path=${this.stampPath()}` : null);

  readonly isDoctorSelected = computed(() => this.selectedRoles().includes('MEDECIN'));

  readonly formTitle = computed(() => this.editingStaff() ? this.t('staff.editTitle') : this.t('staff.inviteTitle'));
  readonly submitLabel = computed(() => this.editingStaff() ? this.t('staff.update') : this.t('staff.create'));
  readonly copyLabel = computed(() => this.passwordCopied() ? this.t('staff.copied') : this.t('staff.copyPassword'));
  readonly tableLabels = computed<StaffTableLabels>(() => ({
    title: this.t('staff.tableTitle'),
    loading: this.t('common.loading'),
    empty: this.t('staff.empty'),
    member: this.t('staff.member'),
    role: this.t('staff.role'),
    status: this.t('staff.status'),
    createdAt: this.t('staff.createdAt'),
    actions: this.t('common.actions'),
    edit: this.t('staff.edit'),
    activate: this.t('common.activate'),
    deactivate: this.t('common.deactivate'),
    active: this.t('common.active'),
    inactive: this.t('common.inactive'),
    roleLabels: {
      MEDECIN: this.t('staff.roles.MEDECIN'),
      INFIRMIER: this.t('staff.roles.INFIRMIER'),
      AGENT_ACCUEIL: this.t('staff.roles.AGENT_ACCUEIL'),
      PHARMACIEN: this.t('staff.roles.PHARMACIEN'),
      BIOLOGISTE: this.t('staff.roles.BIOLOGISTE'),
    },
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    this.api.list().subscribe({
      next: (members) => {
        this.staff.set(members);
        this.loading.set(false);
      },
      error: () => {
        this.pageError.set(this.t('staff.loadError'));
        this.loading.set(false);
      },
    });
  }

  toggleInviteForm(): void {
    this.temporaryPassword.set(null);
    if (this.showForm() && !this.editingStaff()) {
      this.cancelForm();
      return;
    }
    this.resetForm();
    this.showForm.set(true);
  }

  startEdit(member: StaffMember): void {
    this.temporaryPassword.set(null);
    this.editingStaff.set(member);
    this.displayName.set(member.displayName);
    this.email.set(member.email);
    this.selectedRoles.set(member.role ? member.role.split(',').map((r) => r.trim()) : ['MEDECIN']);
    this.phone.set(member.phone || '');
    this.specialty.set(member.specialty || '');
    this.registrationNumber.set(member.registrationNumber || '');
    const dept = member.department || '';
    this.department.set(dept);
    if (this.departments.includes(dept)) {
      this.selectedDept.set(dept);
      this.customDept.set('');
    } else if (dept) {
      this.selectedDept.set('Autre');
      this.customDept.set(dept);
    } else {
      this.selectedDept.set('');
      this.customDept.set('');
    }

    this.bio.set(member.bio || '');
    this.photoPath.set(member.photoPath || null);
    this.signaturePath.set(member.signaturePath || null);
    this.stampPath.set(member.stampPath || null);
    this.formError.set(null);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.resetForm();
  }

  submitForm(): void {
    this.formError.set(null);
    if (!this.displayName().trim() || !this.email().trim() || this.selectedRoles().length === 0) {
      this.formError.set(this.t('staff.requiredFields'));
      return;
    }

    const current = this.editingStaff();
    this.formLoading.set(true);
    if (current) {
      this.updateStaff(current);
    } else {
      this.inviteStaff();
    }
  }

  toggleStatus(member: StaffMember): void {
    this.pageError.set(null);
    this.api.toggleStatus(member.id).subscribe({
      next: (updated) => this.replaceMember(updated),
      error: (err) => this.pageError.set(this.errorMessage(err, this.t('staff.statusError'))),
    });
  }

  hasSelectedRole(option: string): boolean {
    return this.selectedRoles().includes(option);
  }

  toggleSelectedRole(option: string): void {
    this.selectedRoles.update((current) => {
      if (current.includes(option)) {
        if (current.length === 1) return current; // Enforce at least one role
        return current.filter((r) => r !== option);
      } else {
        return [...current, option];
      }
    });
  }

  roleLabel(role: string): string {
    return this.tableLabels().roleLabels[role as StaffRole] || role;
  }

  copyTemporaryPassword(): void {
    const password = this.temporaryPassword();
    if (!password || !navigator.clipboard) {
      return;
    }
    navigator.clipboard.writeText(password).then(() => this.passwordCopied.set(true));
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private inviteStaff(): void {
    this.api.invite({
      displayName: this.displayName().trim(),
      email: this.email().trim(),
      role: this.selectedRoles().join(','),
    }).subscribe({
      next: (created) => {
        this.staff.update((items) => [...items, created]);
        this.temporaryPassword.set(created.temporaryPassword);
        this.passwordCopied.set(false);
        this.formLoading.set(false);
        this.showForm.set(false);
        this.resetForm();
      },
      error: (err) => {
        this.formError.set(this.errorMessage(err, this.t('staff.saveError')));
        this.formLoading.set(false);
      },
    });
  }

  private updateStaff(current: StaffMember): void {
    this.api.update(current.id, {
      displayName: this.displayName().trim(),
      role: this.selectedRoles().join(','),
      photoPath: this.photoPath() || undefined,
      signaturePath: this.signaturePath() || undefined,
      stampPath: this.stampPath() || undefined,
      phone: this.phone().trim() || undefined,
      specialty: this.specialty().trim() || undefined,
      registrationNumber: this.registrationNumber().trim() || undefined,
      department: this.department().trim() || undefined,
      bio: this.bio().trim() || undefined,
    }).subscribe({
      next: (updated) => {
        this.replaceMember(updated);
        this.formLoading.set(false);
        this.showForm.set(false);
        this.resetForm();
      },
      error: (err) => {
        this.formError.set(this.errorMessage(err, this.t('staff.saveError')));
        this.formLoading.set(false);
      },
    });
  }

  private replaceMember(updated: StaffMember): void {
    this.staff.update((items) => items.map((item) => item.id === updated.id ? updated : item));
  }

  private resetForm(): void {
    this.editingStaff.set(null);
    this.displayName.set('');
    this.email.set('');
    this.selectedRoles.set(['MEDECIN']);
    this.phone.set('');
    this.specialty.set('');
    this.registrationNumber.set('');
    this.department.set('');
    this.selectedDept.set('');
    this.customDept.set('');
    this.bio.set('');
    this.photoPath.set(null);
    this.signaturePath.set(null);
    this.stampPath.set(null);
    this.formError.set(null);
    this.formLoading.set(false);
  }

  onDeptChange(val: string): void {
    this.selectedDept.set(val);
    if (val !== 'Autre') {
      this.department.set(val);
      this.customDept.set('');
    } else {
      this.department.set(this.customDept());
    }
  }

  onCustomDeptInput(val: string): void {
    this.customDept.set(val);
    this.department.set(val);
  }

  onFileSelected(file: File, type: 'photo' | 'signature' | 'stamp', component: FileDragDropComponent): void {
    this.formError.set(null);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    this.http.post<any>('/api/files/upload', formData).subscribe({
      next: (res) => {
        if (type === 'photo') this.photoPath.set(res.filePath);
        if (type === 'signature') this.signaturePath.set(res.filePath);
        if (type === 'stamp') this.stampPath.set(res.filePath);
        component.setPreviewUrl(res.viewUrl, file.name);
      },
      error: (err) => {
        console.error(err);
        this.formError.set(this.t('profile.uploadError') || "Erreur lors du chargement de l'image.");
      }
    });
  }

  private errorMessage(err: { status?: number; error?: { detail?: string } }, fallback: string): string {
    if (err.status === 401) {
      return this.t('common.error.unauthorized');
    }
    if (err.status === 403) {
      return this.t('common.error.forbidden');
    }
    if (err.status && err.status >= 500) {
      return this.t('common.error.server');
    }
    return err.error?.detail ?? fallback;
  }
}

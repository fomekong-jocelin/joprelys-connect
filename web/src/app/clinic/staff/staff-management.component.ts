import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
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
                  <span class="ui-label">{{ t('staff.displayName') }} <span class="text-red-500">*</span></span>
                  <input
                    class="ui-input"
                    [value]="displayName()"
                    [placeholder]="t('staff.displayNamePlaceholder')"
                    [disabled]="formLoading()"
                    (input)="displayName.set($any($event.target).value)"
                  />
                </label>

                <label class="space-y-1.5">
                  <span class="ui-label">{{ t('staff.email') }} <span class="text-red-500">*</span></span>
                  <input
                    class="ui-input"
                    type="email"
                    [value]="email()"
                    [placeholder]="t('staff.emailPlaceholder')"
                    [disabled]="formLoading() || editingStaff() !== null"
                    (input)="email.set($any($event.target).value)"
                  />
                </label>

                <label class="space-y-1.5 sm:col-span-2">
                  <span class="ui-label">{{ t('staff.role') }} <span class="text-red-500">*</span></span>
                  <select
                    class="ui-select"
                    [value]="role()"
                    [disabled]="formLoading()"
                    (change)="setRole($any($event.target).value)"
                  >
                    @for (option of roles; track option) {
                      <option [value]="option">{{ roleLabel(option) }}</option>
                    }
                  </select>
                </label>
              </div>

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
  readonly role = signal<StaffRole>('MEDECIN');

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
    this.role.set(member.role);
    this.formError.set(null);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.resetForm();
  }

  submitForm(): void {
    this.formError.set(null);
    if (!this.displayName().trim() || !this.email().trim() || !this.role()) {
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

  setRole(value: string): void {
    if (this.roles.includes(value as StaffRole)) {
      this.role.set(value as StaffRole);
    }
  }

  roleLabel(role: StaffRole): string {
    return this.tableLabels().roleLabels[role];
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
      role: this.role(),
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
      role: this.role(),
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
    this.role.set('MEDECIN');
    this.formError.set(null);
    this.formLoading.set(false);
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

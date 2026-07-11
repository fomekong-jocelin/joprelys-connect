import { HttpClient } from '@angular/common/http';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { FileDragDropComponent } from '../../shared/ui/file-drag-drop.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { RbacApiService } from '../rbac/rbac-api.service';
import { RbacRole } from '../rbac/rbac.models';
import { StaffApiService } from './staff-api.service';
import { StaffMember, StaffRole } from './staff.models';
import { StaffTableComponent, StaffTableLabels } from './staff-table.component';

const NON_STAFF_ROLE_CODES = new Set(['SUPER_ADMIN', 'ADMIN_JOPRELYS', 'ADMIN_CLINIQUE', 'PATIENT']);

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
  templateUrl: './staff-management.component.html',
})
export class StaffManagementComponent implements OnInit {
  private readonly api = inject(StaffApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly roles = signal<RbacRole[]>([]);
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
  readonly selectedRoles = signal<string[]>([]);

  readonly phone = signal('');
  readonly specialty = signal('');
  readonly registrationNumber = signal('');
  readonly department = signal('');
  readonly bio = signal('');

  readonly departments = [
    { value: 'Médecine générale', labelKey: 'staff.departments.general' },
    { value: 'Pédiatrie', labelKey: 'staff.departments.pediatrics' },
    { value: 'Gynécologie', labelKey: 'staff.departments.gynecology' },
    { value: 'Urgences', labelKey: 'staff.departments.emergency' },
    { value: 'Pharmacie', labelKey: 'staff.departments.pharmacy' },
    { value: 'Laboratoire', labelKey: 'staff.departments.laboratory' },
    { value: 'Cardiologie', labelKey: 'staff.departments.cardiology' },
  ];
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
    roleLabels: Object.fromEntries(this.roles().map((role) => [role.code, role.name])) as Record<StaffRole, string>,
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.pageError.set(null);
    forkJoin({
      staff: this.api.list(),
      roles: this.rbacApi.listRoles(),
    }).subscribe({
      next: ({ staff, roles }) => {
        this.staff.set(staff);
        this.roles.set(roles
          .filter((role) => role.assignable && role.enabled && !NON_STAFF_ROLE_CODES.has(role.code))
          .sort((left, right) => {
            if (left.systemRole !== right.systemRole) return left.systemRole ? -1 : 1;
            return left.name.localeCompare(right.name);
          }));
        if (this.selectedRoles().length === 0) {
          this.selectedRoles.set(this.defaultSelectedRoles());
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.pageError.set(this.errorMessage(error, this.t('staff.loadError')));
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
    this.selectedRoles.set(this.parseRoleCodes(member.role));
    this.phone.set(member.phone || '');
    this.specialty.set(member.specialty || '');
    this.registrationNumber.set(member.registrationNumber || '');
    const dept = member.department || '';
    this.department.set(dept);
    if (this.departments.some((item) => item.value === dept)) {
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
      error: (error) => this.pageError.set(this.errorMessage(error, this.t('staff.statusError'))),
    });
  }

  hasSelectedRole(roleCode: string): boolean {
    return this.selectedRoles().includes(roleCode);
  }

  toggleRole(roleCode: string): void {
    this.selectedRoles.update((current) =>
      current.includes(roleCode)
        ? current.filter((code) => code !== roleCode)
        : [...current, roleCode],
    );
  }

  roleLabel(roleCode: string): string {
    return this.roles().find((role) => role.code === roleCode)?.name ?? roleCode;
  }

  copyTemporaryPassword(): void {
    const password = this.temporaryPassword();
    if (!password || !navigator.clipboard) return;
    navigator.clipboard.writeText(password).then(() => this.passwordCopied.set(true));
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  onDeptChange(value: string): void {
    this.selectedDept.set(value);
    if (value !== 'Autre') {
      this.department.set(value);
      this.customDept.set('');
    } else {
      this.department.set(this.customDept());
    }
  }

  onCustomDeptInput(value: string): void {
    this.customDept.set(value);
    this.department.set(value);
  }

  onFileSelected(file: File, type: 'photo' | 'signature' | 'stamp', component: FileDragDropComponent): void {
    this.formError.set(null);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    this.http.post<{ filePath: string; viewUrl: string }>('/api/files/upload', formData).subscribe({
      next: (response) => {
        if (type === 'photo') this.photoPath.set(response.filePath);
        if (type === 'signature') this.signaturePath.set(response.filePath);
        if (type === 'stamp') this.stampPath.set(response.filePath);
        component.setPreviewUrl(response.viewUrl, file.name);
      },
      error: (error) => {
        console.error(error);
        this.formError.set(this.t('profile.uploadError', "Erreur lors du chargement de l'image."));
      },
    });
  }

  private inviteStaff(): void {
    this.api.invite({
      displayName: this.displayName().trim(),
      email: this.email().trim(),
      roles: [...this.selectedRoles()],
    }).subscribe({
      next: (created) => {
        this.staff.update((items) => [...items, created]);
        this.temporaryPassword.set(created.temporaryPassword);
        this.passwordCopied.set(false);
        this.formLoading.set(false);
        this.showForm.set(false);
        this.resetForm();
      },
      error: (error) => {
        this.formError.set(this.errorMessage(error, this.t('staff.saveError')));
        this.formLoading.set(false);
      },
    });
  }

  private updateStaff(current: StaffMember): void {
    this.api.update(current.id, {
      displayName: this.displayName().trim(),
      roles: [...this.selectedRoles()],
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
      error: (error) => {
        this.formError.set(this.errorMessage(error, this.t('staff.saveError')));
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
    this.selectedRoles.set(this.defaultSelectedRoles());
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

  private defaultSelectedRoles(): string[] {
    const preferred = this.roles().find((role) => role.code === 'MEDECIN');
    const fallback = preferred ?? this.roles()[0];
    return fallback ? [fallback.code] : [];
  }

  private parseRoleCodes(rawRoles: string): string[] {
    return rawRoles
      ? [...new Set(rawRoles.split(',').map((role) => role.trim()).filter(Boolean))]
      : this.defaultSelectedRoles();
  }

  private errorMessage(error: { status?: number; error?: { detail?: string } }, fallback: string): string {
    if (error.status === 401) return this.t('common.error.unauthorized');
    if (error.status === 403) return this.t('common.error.forbidden');
    if (error.status && error.status >= 500) return this.t('common.error.server');
    return error.error?.detail ?? fallback;
  }
}

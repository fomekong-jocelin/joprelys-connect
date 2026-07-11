import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { CardComponent } from '../../shared/ui/card.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { FileDragDropComponent } from '../../shared/ui/file-drag-drop.component';
import { StaffApiService } from './staff-api.service';
import {
  StaffMember,
  StaffRole,
  StaffRoleCategory,
  StaffRoleDefinition,
} from './staff.models';
import { StaffTableComponent, StaffTableLabels } from './staff-table.component';

interface UploadResponse {
  readonly filePath: string;
  readonly viewUrl: string;
}

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
  styleUrl: './staff-management.component.css',
})
export class StaffManagementComponent implements OnInit {
  private readonly api = inject(StaffApiService);
  private readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly staff = signal<StaffMember[]>([]);
  readonly roleDefinitions = signal<StaffRoleDefinition[]>([]);
  readonly loading = signal(false);
  readonly rolesLoading = signal(false);
  readonly pageError = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly editingStaff = signal<StaffMember | null>(null);
  readonly temporaryPassword = signal<string | null>(null);
  readonly passwordCopied = signal(false);

  readonly displayName = signal('');
  readonly email = signal('');
  readonly selectedRoles = signal<StaffRole[]>(['MEDECIN']);
  readonly phone = signal('');
  readonly specialty = signal('');
  readonly registrationNumber = signal('');
  readonly department = signal('');
  readonly bio = signal('');
  readonly selectedDept = signal('');
  readonly customDept = signal('');
  readonly photoPath = signal<string | null>(null);
  readonly signaturePath = signal<string | null>(null);
  readonly stampPath = signal<string | null>(null);

  readonly departments = [
    { value: 'Médecine générale', labelKey: 'staff.departments.general' },
    { value: 'Pédiatrie', labelKey: 'staff.departments.pediatrics' },
    { value: 'Gynécologie', labelKey: 'staff.departments.gynecology' },
    { value: 'Urgences', labelKey: 'staff.departments.emergency' },
    { value: 'Pharmacie', labelKey: 'staff.departments.pharmacy' },
    { value: 'Laboratoire', labelKey: 'staff.departments.laboratory' },
    { value: 'Cardiologie', labelKey: 'staff.departments.cardiology' },
  ];

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
    roleLabels: this.roleDefinitions().reduce((labels, role) => {
      labels[role.code] = this.t(role.labelKey);
      return labels;
    }, {} as Record<StaffRole, string>),
  }));

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.rolesLoading.set(true);
    this.pageError.set(null);

    forkJoin({
      members: this.api.list(),
      roles: this.api.listRoles(),
    }).subscribe({
      next: ({ members, roles }) => {
        this.staff.set(members);
        this.roleDefinitions.set(roles);
        this.loading.set(false);
        this.rolesLoading.set(false);
      },
      error: () => {
        this.pageError.set(this.t('staff.loadError'));
        this.loading.set(false);
        this.rolesLoading.set(false);
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

    const assignedRoles = member.role
      .split(',')
      .map((role) => role.trim())
      .filter((role): role is StaffRole => this.isKnownRole(role));
    this.selectedRoles.set(assignedRoles.length > 0 ? assignedRoles : ['MEDECIN']);

    this.phone.set(member.phone || '');
    this.specialty.set(member.specialty || '');
    this.registrationNumber.set(member.registrationNumber || '');
    this.configureDepartment(member.department || '');
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

  hasSelectedRole(role: StaffRole): boolean {
    return this.selectedRoles().includes(role);
  }

  toggleRole(role: StaffRole): void {
    const selected = new Set(this.selectedRoles());
    if (selected.has(role)) {
      selected.delete(role);
    } else {
      selected.add(role);
    }

    const orderedSelection = this.roleDefinitions()
      .map((definition) => definition.code)
      .filter((code) => selected.has(code));
    this.selectedRoles.set(orderedSelection);
  }

  roleLabel(role: StaffRole): string {
    const definition = this.roleDefinitions().find((candidate) => candidate.code === role);
    return definition ? this.t(definition.labelKey) : role;
  }

  categoryLabel(category: StaffRoleCategory): string {
    return this.t(`staff.roleCategories.${category}`);
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

    this.http.post<UploadResponse>('/api/files/upload', formData).subscribe({
      next: (response) => {
        if (type === 'photo') this.photoPath.set(response.filePath);
        if (type === 'signature') this.signaturePath.set(response.filePath);
        if (type === 'stamp') this.stampPath.set(response.filePath);
        component.setPreviewUrl(response.viewUrl, file.name);
      },
      error: () => this.formError.set(this.t('profile.uploadError')),
    });
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
      error: (error) => {
        this.formError.set(this.errorMessage(error, this.t('staff.saveError')));
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

  private configureDepartment(department: string): void {
    this.department.set(department);
    if (this.departments.some((candidate) => candidate.value === department)) {
      this.selectedDept.set(department);
      this.customDept.set('');
    } else if (department) {
      this.selectedDept.set('Autre');
      this.customDept.set(department);
    } else {
      this.selectedDept.set('');
      this.customDept.set('');
    }
  }

  private isKnownRole(role: string): role is StaffRole {
    return this.roleDefinitions().some((definition) => definition.code === role);
  }

  private errorMessage(
    error: { status?: number; error?: { detail?: string } },
    fallback: string,
  ): string {
    if (error.status === 401) return this.t('common.error.unauthorized');
    if (error.status === 403) return this.t('common.error.forbidden');
    if (error.status && error.status >= 500) return this.t('common.error.server');
    return error.error?.detail ?? fallback;
  }
}

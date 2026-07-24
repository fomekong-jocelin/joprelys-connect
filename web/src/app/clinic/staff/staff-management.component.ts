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
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import {
  HospitalServiceCatalogEntry,
  MedicalSpecialtyCatalogEntry,
  OrganizationalUnit,
} from '../hospital-organization/hospital-organization.models';
import { RbacApiService } from '../rbac/rbac-api.service';
import { RbacRole } from '../rbac/rbac.models';
import { StaffApiService } from './staff-api.service';
import { StaffAssignmentEditorComponent } from './staff-assignment-editor.component';
import { StaffAssignmentRole, StaffMember, StaffRole } from './staff.models';
import { StaffTableComponent, StaffTableLabels } from './staff-table.component';

const NON_STAFF_ROLE_CODES = new Set(['SUPER_ADMIN', 'ADMIN_JOPRELYS', 'ADMIN_CLINIQUE', 'PATIENT']);

interface ApiErrorPayload {
  detail?: string;
  message?: string;
  error?: {
    code?: string;
    message?: string;
    trace_id?: string;
  };
}

interface HttpErrorLike {
  status?: number;
  error?: ApiErrorPayload;
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
    StaffAssignmentEditorComponent,
    FileDragDropComponent,
  ],
  templateUrl: './staff-management.component.html',
})
export class StaffManagementComponent implements OnInit {
  private readonly api = inject(StaffApiService);
  private readonly organizationApi = inject(HospitalOrganizationApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  private readonly http = inject(HttpClient);

  readonly roles = signal<RbacRole[]>([]);
  readonly specialties = signal<MedicalSpecialtyCatalogEntry[]>([]);
  readonly serviceCatalog = signal<HospitalServiceCatalogEntry[]>([]);
  readonly units = signal<OrganizationalUnit[]>([]);
  readonly assignmentRoles = signal<StaffAssignmentRole[]>([]);
  readonly staff = signal<StaffMember[]>([]);
  readonly loading = signal(false);
  readonly pageError = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly formLoading = signal(false);
  readonly formError = signal<string | null>(null);
  readonly editingStaff = signal<StaffMember | null>(null);

  readonly displayName = signal('');
  readonly email = signal('');
  readonly selectedRoles = signal<string[]>([]);
  readonly phone = signal('');
  readonly registrationNumber = signal('');
  readonly bio = signal('');

  readonly initialSpecialtyCode = signal('');
  readonly initialUnitId = signal('');
  readonly initialAssignmentRoleCode = signal('');
  readonly initialAssignmentFrom = signal(this.localDateTimeNow());

  readonly photoPath = signal<string | null>(null);
  readonly signaturePath = signal<string | null>(null);
  readonly stampPath = signal<string | null>(null);

  readonly photoViewUrl = computed(() => this.photoPath() ? `/api/public/files/view?path=${this.photoPath()}` : null);
  readonly signatureViewUrl = computed(() => this.signaturePath() ? `/api/public/files/view?path=${this.signaturePath()}` : null);
  readonly stampViewUrl = computed(() => this.stampPath() ? `/api/public/files/view?path=${this.stampPath()}` : null);

  readonly isDoctorSelected = computed(() => this.selectedRoles().includes('MEDECIN'));

  readonly formTitle = computed(() => this.editingStaff() ? this.t('staff.editTitle') : this.t('staff.inviteTitle'));
  readonly submitLabel = computed(() => this.editingStaff() ? this.t('staff.update') : this.t('staff.create'));
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
      specialties: this.organizationApi.listSpecialtyCatalog(),
      services: this.organizationApi.listServiceCatalog(),
      units: this.organizationApi.listUnits(undefined, false),
      assignmentRoles: this.api.listAssignmentRoles(),
    }).subscribe({
      next: ({ staff, roles, specialties, services, units, assignmentRoles }) => {
        this.staff.set(staff);
        this.roles.set(roles
          .filter((role) => role.assignable && role.enabled && !NON_STAFF_ROLE_CODES.has(role.code))
          .sort((left, right) => {
            if (left.systemRole !== right.systemRole) return left.systemRole ? -1 : 1;
            return left.name.localeCompare(right.name);
          }));
        this.specialties.set(specialties);
        this.serviceCatalog.set(services);
        this.units.set(units.filter((unit) => unit.active));
        this.assignmentRoles.set(assignmentRoles);
        if (this.selectedRoles().length === 0) {
          this.selectedRoles.set(this.defaultSelectedRoles());
        }
        this.applySuggestedAssignmentRole();
        this.loading.set(false);
      },
      error: (error) => {
        this.pageError.set(this.errorMessage(error, this.t('staff.loadError')));
        this.loading.set(false);
      },
    });
  }

  toggleInviteForm(): void {
    if (this.showForm() && !this.editingStaff()) {
      this.cancelForm();
      return;
    }
    this.resetForm();
    this.showForm.set(true);
  }

  startEdit(member: StaffMember): void {
    this.editingStaff.set(member);
    this.displayName.set(member.displayName);
    this.email.set(member.email);
    this.selectedRoles.set(this.parseRoleCodes(member.role));
    this.phone.set(member.phone || '');
    this.registrationNumber.set(member.registrationNumber || '');
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
    if (this.isDoctorSelected() && !this.registrationNumber().trim()) {
      this.formError.set(this.t('staff.onboarding.registrationRequired', "Le numéro d'inscription à l'Ordre est obligatoire pour un médecin."));
      return;
    }
    if (!this.editingStaff() && ((this.initialUnitId() && !this.initialAssignmentRoleCode()) || (!this.initialUnitId() && this.initialAssignmentRoleCode()))) {
      this.formError.set(this.t('staff.onboarding.unitRoleRequired', "Sélectionnez à la fois l'unité et le rôle contextuel."));
      return;
    }
    if (!this.editingStaff() && (this.initialSpecialtyCode() || this.initialUnitId()) && !this.toIso(this.initialAssignmentFrom())) {
      this.formError.set(this.t('staff.onboarding.startRequired', "La date de début de l'affectation est obligatoire."));
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
    this.applySuggestedAssignmentRole();
  }

  roleLabel(roleCode: string): string {
    return this.roles().find((role) => role.code === roleCode)?.name ?? roleCode;
  }

  specialtyLabel(item: MedicalSpecialtyCatalogEntry): string {
    return this.localized(item.nameFr, item.nameEn);
  }

  unitLabel(unit: OrganizationalUnit): string {
    if (unit.name) return `${unit.name} · ${unit.code}`;
    const service = this.serviceCatalog().find((candidate) => candidate.code === unit.serviceCatalogCode);
    return `${service ? this.localized(service.nameFr, service.nameEn) : (unit.serviceCatalogCode ?? unit.code)} · ${unit.code}`;
  }

  assignmentRoleLabel(item: StaffAssignmentRole): string {
    return this.localized(item.nameFr, item.nameEn);
  }

  localized(fr: string, en: string): string {
    return this.i18n.currentLanguage() === 'en' ? en : fr;
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
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
    const validFrom = this.toIso(this.initialAssignmentFrom()) ?? new Date().toISOString();
    const specialtyAssignments = this.initialSpecialtyCode()
      ? [{
          specialtyCode: this.initialSpecialtyCode(),
          primary: true,
          validFrom,
        }]
      : undefined;
    const unitAssignments = this.initialUnitId() && this.initialAssignmentRoleCode()
      ? [{
          organizationalUnitId: this.initialUnitId(),
          assignmentRoleCode: this.initialAssignmentRoleCode(),
          primary: true,
          validFrom,
        }]
      : undefined;

    this.api.invite({
      displayName: this.displayName().trim(),
      email: this.email().trim(),
      roles: [...this.selectedRoles()],
      phone: this.phone().trim() || undefined,
      registrationNumber: this.registrationNumber().trim() || undefined,
      bio: this.bio().trim() || undefined,
      specialtyAssignments,
      unitAssignments,
    }).subscribe({
      next: () => {
        this.formLoading.set(false);
        this.showForm.set(false);
        this.resetForm();
        this.load();
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
      registrationNumber: this.registrationNumber().trim() || undefined,
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
    this.registrationNumber.set('');
    this.bio.set('');
    this.initialSpecialtyCode.set('');
    this.initialUnitId.set('');
    this.initialAssignmentRoleCode.set('');
    this.initialAssignmentFrom.set(this.localDateTimeNow());
    this.photoPath.set(null);
    this.signaturePath.set(null);
    this.stampPath.set(null);
    this.formError.set(null);
    this.formLoading.set(false);
    this.applySuggestedAssignmentRole();
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

  private applySuggestedAssignmentRole(): void {
    if (this.editingStaff() || this.initialAssignmentRoleCode()) return;
    const suggestions: Array<[string, string]> = [
      ['MEDECIN', 'PRACTITIONER'],
      ['INFIRMIER', 'NURSE'],
      ['PHARMACIEN', 'PHARMACIST'],
      ['BIOLOGISTE', 'LAB_TECHNICIAN'],
      ['RESPONSABLE_HOSPITALISATION', 'UNIT_MANAGER'],
      ['AGENT_ACCUEIL', 'ADMINISTRATIVE_SUPPORT'],
      ['CAISSIER', 'ADMINISTRATIVE_SUPPORT'],
      ['DAF', 'ADMINISTRATIVE_SUPPORT'],
      ['SECRETAIRE_COMPTABLE', 'ADMINISTRATIVE_SUPPORT'],
    ];
    const suggested = suggestions.find(([rbacRole]) => this.selectedRoles().includes(rbacRole))?.[1];
    if (suggested && this.assignmentRoles().some((role) => role.code === suggested)) {
      this.initialAssignmentRoleCode.set(suggested);
    }
  }

  private toIso(value: string): string | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date.toISOString();
  }

  private localDateTimeNow(): string {
    const date = new Date();
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
    return local.toISOString().slice(0, 16);
  }

  private errorMessage(error: HttpErrorLike, fallback: string): string {
    const code = error.error?.error?.code;
    if (code === 'MAIL_RECIPIENT_REJECTED') {
      return this.t('staff.errors.mailRecipientRejected');
    }
    if (code === 'MAIL_DELIVERY_UNAVAILABLE') {
      return this.t('staff.errors.mailDeliveryUnavailable');
    }
    if (error.status === 401) return this.t('common.error.unauthorized');
    if (error.status === 403) return this.t('common.error.forbidden');

    const apiMessage = error.error?.error?.message ?? error.error?.detail ?? error.error?.message;
    if (error.status && error.status >= 500) return this.t('common.error.server');
    return apiMessage ?? fallback;
  }
}

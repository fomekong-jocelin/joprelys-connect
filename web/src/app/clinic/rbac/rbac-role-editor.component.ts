import { Component, computed, inject, input, output, signal } from '@angular/core';
import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { isValidUnitCode, normalizeUnitCode, suggestUnitCode } from '../hospital-organization/org-unit-code';
import { CreateRbacRoleRequest, RbacPermission, RbacRole, UpdateRbacRoleRequest } from './rbac.models';
import { groupPermissions } from './rbac-view';

export type RoleSaveEvent =
  | { readonly id: null; readonly request: CreateRbacRoleRequest }
  | { readonly id: string; readonly request: UpdateRbacRoleRequest; readonly permissionCodes: string[] };

interface RoleDraft {
  readonly name: string;
  readonly code: string;
  readonly codeEdited: boolean;
  readonly description: string;
  readonly assignable: boolean;
  readonly enabled: boolean;
  readonly permissionCodes: readonly string[];
}

const BLANK: RoleDraft = { name: '', code: '', codeEdited: false, description: '', assignable: true, enabled: true, permissionCodes: [] };

/** Onglet « Rôles » : un rôle système est un modèle à dupliquer, un rôle personnalisé se modifie. */
@Component({
  selector: 'app-rbac-role-editor',
  standalone: true,
  templateUrl: './rbac-role-editor.component.html',
})
export class RbacRoleEditorComponent {
  private readonly i18n = inject(I18nService);

  readonly roles = input.required<readonly RbacRole[]>();
  readonly permissions = input.required<readonly RbacPermission[]>();
  readonly saving = input(false);
  readonly save = output<RoleSaveEvent>();
  /** Message à afficher par la page (copie prête, etc.). */
  readonly notice = output<string>();

  readonly t = (key: string) => this.i18n.t(key);
  private readonly selectedRoleId = signal<string | null>(null);
  private readonly creating = signal(false);
  private readonly draft = signal<RoleDraft | null>(null);
  readonly submitted = signal(false);

  readonly selectedRole = computed(() => this.creating()
    ? null
    : this.roles().find((role) => role.id === this.selectedRoleId()) ?? this.roles()[0] ?? null);
  readonly readOnly = computed(() => this.selectedRole()?.systemRole ?? false);
  readonly form = computed<RoleDraft>(() => this.draft() ?? this.fromRole(this.selectedRole()));
  readonly code = computed(() => {
    const form = this.form();
    return form.codeEdited ? form.code : suggestUnitCode(form.name, this.takenCodes());
  });
  readonly nameError = computed(() => this.form().name.trim() ? '' : 'rbac.roles.validation.nameRequired');
  readonly codeError = computed(() => isValidUnitCode(this.code()) ? '' : 'rbac.roles.validation.codeFormat');
  readonly groups = computed(() => groupPermissions(this.permissions()));
  /** Modifications en attente sur un rôle existant (un nouveau rôle est par nature non enregistré). */
  readonly dirty = computed(() => this.draft() !== null && this.selectedRole() !== null);

  private readonly takenCodes = computed(() => new Set(this.roles()
    .filter((role) => role.id !== this.selectedRole()?.id)
    .map((role) => role.code.toUpperCase())));

  select(role: RbacRole): void {
    this.creating.set(false);
    this.selectedRoleId.set(role.id);
    this.draft.set(null);
    this.submitted.set(false);
  }

  startNew(): void {
    this.creating.set(true);
    this.draft.set(BLANK);
    this.submitted.set(false);
  }

  duplicate(): void {
    const source = this.selectedRole();
    if (!source) return;
    this.creating.set(true);
    this.submitted.set(false);
    this.draft.set({
      ...BLANK,
      name: interpolate(this.t('rbac.roles.copyName'), { name: source.name }),
      description: source.description ?? '',
      permissionCodes: [...source.permissions],
    });
    this.notice.emit(this.t('rbac.roles.duplicateReady'));
  }

  patch(changes: Partial<RoleDraft>): void {
    if (this.readOnly()) return;
    this.draft.set({ ...this.form(), ...changes });
  }

  editCode(value: string): void {
    this.patch({ code: value, codeEdited: true });
  }

  togglePermission(code: string): void {
    const current = this.form().permissionCodes;
    this.patch({ permissionCodes: current.includes(code) ? current.filter((item) => item !== code) : [...current, code] });
  }

  checkedCount(items: readonly RbacPermission[]): number {
    const codes = this.form().permissionCodes;
    return items.filter((item) => codes.includes(item.code)).length;
  }

  domainLabel(domain: string): string {
    return this.i18n.t(`rbac.domain.${domain}`, domain);
  }

  submit(): void {
    this.submitted.set(true);
    if (this.readOnly() || this.saving() || this.nameError() || this.codeError()) return;
    const form = this.form();
    const base = {
      code: normalizeUnitCode(this.code()),
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      assignable: form.assignable,
    };
    const role = this.selectedRole();
    if (!role) {
      this.save.emit({ id: null, request: { ...base, permissionCodes: [...form.permissionCodes] } });
      return;
    }
    this.save.emit({ id: role.id, request: { ...base, enabled: form.enabled }, permissionCodes: [...form.permissionCodes] });
  }

  private fromRole(role: RbacRole | null): RoleDraft {
    if (!role) return BLANK;
    return {
      name: role.name,
      code: role.code,
      codeEdited: true,
      description: role.description ?? '',
      assignable: role.assignable,
      enabled: role.enabled,
      permissionCodes: role.permissions,
    };
  }
}

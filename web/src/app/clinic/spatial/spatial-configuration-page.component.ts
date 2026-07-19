import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { OrganizationApiService } from '../organizations/organization-api.service';
import { RbacApiService } from '../rbac/rbac-api.service';
import { Organization } from '../organizations/organizations.models';
import { SpatialApiService } from '../../patient/spatial-api.service';
import { Bed } from '../../patient/patient.models';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { ConfirmationDialogComponent } from '../../shared/ui/confirmation-dialog.component';
import { IconComponent } from '../../shared/ui/icon.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import {
  HOSPITAL_SERVICE_TYPES,
  HospitalServiceType,
  RoomConfiguration,
  SpatialConfiguration,
  WardConfiguration,
} from './spatial-configuration.models';

type EditorKind = 'ward' | 'room' | 'bed';
type DeleteKind = EditorKind;

interface EditorState {
  kind: EditorKind;
  id: string | null;
  parentId: string | null;
  name: string;
  serviceType: HospitalServiceType | '';
  hasRooms: boolean;
  capacity: number;
  comfortLevel: string;
}

interface DeleteTarget {
  readonly kind: DeleteKind;
  readonly id: string;
  readonly label: string;
}

@Component({
  selector: 'app-spatial-configuration-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AppShellComponent,
    PageHeaderComponent,
    IconComponent,
    ConfirmationDialogComponent,
  ],
  templateUrl: './spatial-configuration-page.component.html',
})
export class SpatialConfigurationPageComponent implements OnInit {
  private readonly spatialApi = inject(SpatialApiService);
  private readonly i18n = inject(I18nService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly organizationApi = inject(OrganizationApiService);

  readonly serviceTypes = HOSPITAL_SERVICE_TYPES;
  readonly configuration = signal<SpatialConfiguration>({ wards: [] });
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly errorMessage = signal('');
  readonly successMessage = signal('');
  readonly editor = signal<EditorState | null>(null);
  readonly deleteTarget = signal<DeleteTarget | null>(null);
  readonly platformAdministrator = computed(() => this.rbacApi.hasPermission('ORGANIZATION_MANAGE'));
  readonly organizations = signal<Organization[]>([]);
  readonly selectedOrganizationId = signal('');
  readonly t = (key: string) => this.i18n.t(key);

  ngOnInit(): void {
    if (this.platformAdministrator()) {
      this.loadOrganizations();
      return;
    }
    this.loadConfiguration();
  }

  loadConfiguration(): void {
    this.loading.set(true);
    if (!this.canManageScope()) {
      this.loading.set(false);
      return;
    }
    this.spatialApi.getConfiguration(this.scopeOrganizationId()).subscribe({
      next: (configuration) => {
        this.configuration.set(configuration);
        this.loading.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
        this.loading.set(false);
      },
    });
  }

  onOrganizationChange(): void {
    this.configuration.set({ wards: [] });
    this.clearMessages();
    this.loadConfiguration();
  }

  canManageScope(): boolean {
    return !this.platformAdministrator() || this.selectedOrganizationId().length > 0;
  }

  openWardEditor(ward?: WardConfiguration): void {
    this.openEditor({
      kind: 'ward',
      id: ward?.id ?? null,
      parentId: null,
      name: ward?.name ?? '',
      serviceType: ward?.serviceType ?? '',
      hasRooms: (ward?.rooms.length ?? 0) > 0,
      capacity: 1,
      comfortLevel: 'STANDARD',
    });
  }

  openRoomEditor(ward: WardConfiguration, room?: RoomConfiguration): void {
    if (!ward.allowsRooms) {
      this.errorMessage.set(this.t('spatial.services.roomsForbidden'));
      return;
    }
    this.openEditor({
      kind: 'room',
      id: room?.id ?? null,
      parentId: ward.id,
      name: room?.roomNumber ?? '',
      serviceType: '',
      hasRooms: false,
      capacity: room?.capacity ?? 1,
      comfortLevel: room?.comfortLevel ?? 'STANDARD',
    });
  }

  openBedEditor(roomId: string, bed?: Bed): void {
    this.openEditor({
      kind: 'bed',
      id: bed?.id ?? null,
      parentId: roomId,
      name: bed?.bedNumber ?? '',
      serviceType: '',
      hasRooms: false,
      capacity: 1,
      comfortLevel: 'STANDARD',
    });
  }

  closeEditor(): void {
    if (!this.busy()) {
      this.editor.set(null);
    }
  }

  canSubmitEditor(state: EditorState): boolean {
    if (!state.name.trim() || (!state.parentId && state.kind !== 'ward')) {
      return false;
    }
    if (state.kind === 'ward') {
      return state.serviceType !== '';
    }
    if (state.kind === 'room') {
      return state.capacity >= 1;
    }
    return true;
  }

  serviceTypeAllowsRooms(serviceType: HospitalServiceType): boolean {
    return serviceType === 'HOSPITALIZATION' || serviceType === 'EMERGENCY';
  }

  submitEditor(): void {
    const state = this.editor();
    if (!state || !this.canSubmitEditor(state)) {
      return;
    }

    this.busy.set(true);
    this.clearMessages();
    this.saveRequest(state).subscribe({
      next: () => this.afterSave(),
      error: (error: HttpErrorResponse) => this.handleOperationError(error),
    });
  }

  askDelete(kind: DeleteKind, id: string, label: string): void {
    this.deleteTarget.set({ kind, id, label });
  }

  cancelDelete(): void {
    if (!this.busy()) {
      this.deleteTarget.set(null);
    }
  }

  confirmDelete(): void {
    const target = this.deleteTarget();
    if (!target) {
      return;
    }
    this.busy.set(true);
    this.clearMessages();
    this.deleteRequest(target).subscribe({
      next: () => {
        this.deleteTarget.set(null);
        this.successMessage.set(this.t('spatial.config.deleteSuccess'));
        this.busy.set(false);
        this.loadConfiguration();
      },
      error: (error: HttpErrorResponse) => this.handleOperationError(error),
    });
  }

  editorTitle(state: EditorState): string {
    const action = state.id ? 'edit' : 'create';
    return this.t(`spatial.config.${action}.${state.kind}`);
  }

  serviceTypeLabel(serviceType: HospitalServiceType): string {
    return this.t(`spatial.services.type.${serviceType}.label`);
  }

  serviceTypeDescription(serviceType: HospitalServiceType): string {
    return this.t(`spatial.services.type.${serviceType}.description`);
  }

  deleteMessage(): string {
    const target = this.deleteTarget();
    return target ? `${this.t('spatial.config.deleteMessage')} « ${target.label} » ?` : '';
  }

  private openEditor(state: EditorState): void {
    this.clearMessages();
    this.editor.set(state);
  }

  private saveRequest(state: EditorState): Observable<unknown> {
    if (state.kind === 'ward') {
      if (state.serviceType === '') {
        throw new Error('A hospital service type is required before submitting the form.');
      }
      const payload = {
        name: state.name.trim(),
        serviceType: state.serviceType,
      };
      return state.id
        ? this.spatialApi.updateWard(state.id, payload, this.scopeOrganizationId())
        : this.spatialApi.createWard(payload, this.scopeOrganizationId());
    }
    if (state.kind === 'room') {
      const payload = {
        wardId: state.parentId!,
        roomNumber: state.name.trim(),
        capacity: state.capacity,
        comfortLevel: state.comfortLevel,
      };
      return state.id
        ? this.spatialApi.updateRoom(state.id, payload, this.scopeOrganizationId())
        : this.spatialApi.createRoom(payload, this.scopeOrganizationId());
    }
    const payload = { roomId: state.parentId!, bedNumber: state.name.trim() };
    return state.id
      ? this.spatialApi.updateBed(state.id, payload, this.scopeOrganizationId())
      : this.spatialApi.createBed(payload, this.scopeOrganizationId());
  }

  private deleteRequest(target: DeleteTarget): Observable<void> {
    if (target.kind === 'ward') {
      return this.spatialApi.deleteWard(target.id, this.scopeOrganizationId());
    }
    if (target.kind === 'room') {
      return this.spatialApi.deleteRoom(target.id, this.scopeOrganizationId());
    }
    return this.spatialApi.deleteBed(target.id, this.scopeOrganizationId());
  }

  private afterSave(): void {
    this.editor.set(null);
    this.successMessage.set(this.t('spatial.config.saveSuccess'));
    this.busy.set(false);
    this.loadConfiguration();
  }

  private handleOperationError(error: HttpErrorResponse): void {
    this.errorMessage.set(this.extractError(error));
    this.busy.set(false);
  }

  private clearMessages(): void {
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  private extractError(error: HttpErrorResponse): string {
    const body = error.error as { detail?: string; error?: { message?: string } } | null;
    return body?.error?.message ?? body?.detail ?? this.t('spatial.config.genericError');
  }

  private loadOrganizations(): void {
    this.loading.set(true);
    this.organizationApi.list().subscribe({
      next: (organizations) => {
        this.organizations.set(organizations);
        const preferred = organizations.find((organization) => organization.status === 'ACTIVE') ?? organizations[0];
        this.selectedOrganizationId.set(preferred?.id ?? '');
        if (preferred) {
          this.loadConfiguration();
        } else {
          this.loading.set(false);
        }
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(this.extractError(error));
        this.loading.set(false);
      },
    });
  }

  private scopeOrganizationId(): string | undefined {
    return this.platformAdministrator() ? this.selectedOrganizationId() || undefined : undefined;
  }
}

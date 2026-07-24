import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { HospitalOrganizationApiService } from '../hospital-organization/hospital-organization-api.service';
import {
  HospitalServiceCatalogEntry,
  MedicalSpecialtyCatalogEntry,
  OrganizationalUnit,
} from '../hospital-organization/hospital-organization.models';
import { StaffApiService } from './staff-api.service';
import {
  StaffAssignmentRole,
  StaffAssignmentStructure,
  StaffSpecialtyAssignment,
  StaffUnitAssignment,
} from './staff.models';

const EMPTY_STRUCTURE: StaffAssignmentStructure = { specialties: [], unitAssignments: [] };

@Component({
  selector: 'app-staff-assignment-editor',
  standalone: true,
  imports: [AlertComponent, ButtonComponent, DatePipe],
  template: `
    <section class="space-y-5 rounded-md border border-[var(--app-border)] p-4 sm:col-span-2">
      <div>
        <h3 class="font-extrabold text-[var(--text-primary)]">
          {{ t('staff.assignments.title', 'Affectations hospitalières structurées') }}
        </h3>
        <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
          {{ t('staff.assignments.help', 'Les unités, spécialités et périodes sont contrôlées. Les affectations terminées restent dans l’historique.') }}
        </p>
      </div>

      @if (error(); as message) {
        <app-ui-alert tone="error">{{ message }}</app-ui-alert>
      }

      @if (loading()) {
        <p class="py-4 text-sm font-semibold text-[var(--text-secondary)]">{{ t('common.loading') }}</p>
      } @else {
        <div class="grid grid-cols-1 gap-5 xl:grid-cols-2">
          <!-- Section Spécialités -->
          <section class="space-y-4 rounded-md bg-[var(--app-surface-muted)] p-4">
            <div>
              <h4 class="font-bold text-[var(--text-primary)]">{{ t('staff.assignments.specialties', 'Spécialités') }}</h4>
              <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('staff.assignments.specialtiesHelp', 'Sélection depuis le catalogue médical HOS-ORG.') }}</p>
            </div>

            <!-- Liste des cartes enregistrées d'abord -->
            <div class="space-y-3">
              @for (item of structure().specialties; track item.id) {
                <article class="rounded-md border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-xs">
                  <div class="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p class="font-bold text-[var(--text-primary)]">{{ specialtyLabel(item.specialtyCode) }}</p>
                      <p class="mt-1 text-xs text-[var(--text-muted)]">
                        {{ item.validFrom | date:'dd/MM/yyyy HH:mm' }} → {{ item.validTo ? (item.validTo | date:'dd/MM/yyyy HH:mm') : t('staff.assignments.openEnded', 'sans fin') }}
                      </p>
                    </div>
                    <div class="flex flex-wrap gap-2">
                      @if (item.primary) {
                        <span class="rounded bg-[var(--app-surface-muted)] px-2 py-1 text-[10px] font-bold uppercase text-[var(--text-secondary)]">{{ t('staff.assignments.primary', 'Principale') }}</span>
                      }
                      <span class="rounded px-2 py-1 text-[10px] font-bold uppercase" [class.text-brand-primary]="item.active" [class.text-[var(--text-muted)]]="!item.active">
                        {{ item.active ? t('common.active') : t('staff.assignments.historical', 'Historique') }}
                      </span>
                    </div>
                  </div>
                  @if (item.active) {
                    <div class="mt-3 border-t border-[var(--app-border)] pt-2">
                      <app-ui-button variant="secondary" [disabled]="saving()" (pressed)="closeSpecialty(item)">
                        {{ t('staff.assignments.close', 'Clôturer maintenant') }}
                      </app-ui-button>
                    </div>
                  }
                </article>
              } @empty {
                <p class="text-sm text-[var(--text-muted)]">{{ t('staff.assignments.noSpecialty', 'Aucune spécialité structurée.') }}</p>
              }
            </div>

            <!-- Bouton pour ouvrir / fermer le formulaire d'ajout -->
            <div class="pt-2">
              <app-ui-button variant="secondary" class="w-full sm:w-auto" [disabled]="saving()" (pressed)="toggleSpecialtyForm()">
                {{ showSpecialtyForm() ? t('common.cancel', 'Annuler') : ('+ ' + t('staff.assignments.addSpecialtyToggle', 'Ajouter une spécialité')) }}
              </app-ui-button>
            </div>

            <!-- Formulaire d'ajout (affiché uniquement sur demande) -->
            @if (showSpecialtyForm()) {
              <form class="mt-3 space-y-4 rounded-md border border-[var(--app-border)] bg-[var(--app-surface)] p-4 shadow-xs" (submit)="$event.preventDefault(); addSpecialty()">
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <label class="space-y-1.5 sm:col-span-2">
                    <span class="ui-label">{{ t('profile.specialty') }}</span>
                    <select class="ui-select" [value]="specialtyCode()" [disabled]="saving()" (change)="specialtyCode.set($any($event.target).value)">
                      <option value="">{{ t('staff.assignments.chooseSpecialty', 'Choisir une spécialité...') }}</option>
                      @for (item of specialties(); track item.code) {
                        <option [value]="item.code">{{ localized(item.nameFr, item.nameEn) }}</option>
                      }
                    </select>
                  </label>

                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('staff.assignments.validFrom', 'Début') }}</span>
                    <input class="ui-input" type="datetime-local" [value]="specialtyFrom()" [disabled]="saving()" (input)="specialtyFrom.set($any($event.target).value)" />
                  </label>

                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('staff.assignments.validTo', 'Fin facultative') }}</span>
                    <input class="ui-input" type="datetime-local" [value]="specialtyTo()" [disabled]="saving()" (input)="specialtyTo.set($any($event.target).value)" />
                  </label>
                </div>

                <label class="flex items-center gap-2 text-sm font-semibold text-[var(--text-secondary)]">
                  <input type="checkbox" class="ui-checkbox" [checked]="specialtyPrimary()" [disabled]="saving()" (change)="specialtyPrimary.set($any($event.target).checked)" />
                  {{ t('staff.assignments.primarySpecialty', 'Spécialité principale sur cette période') }}
                </label>

                <div class="flex flex-wrap items-center gap-3 pt-2">
                  <app-ui-button type="submit" [disabled]="saving() || !specialtyCode() || !specialtyFrom()">
                    {{ t('staff.assignments.addSpecialty', 'Enregistrer la spécialité') }}
                  </app-ui-button>
                  <app-ui-button variant="secondary" [disabled]="saving()" (pressed)="toggleSpecialtyForm()">
                    {{ t('common.cancel', 'Annuler') }}
                  </app-ui-button>
                </div>
              </form>
            }
          </section>

          <!-- Section Unités organisationnelles -->
          <section class="space-y-4 rounded-md bg-[var(--app-surface-muted)] p-4">
            <div>
              <h4 class="font-bold text-[var(--text-primary)]">{{ t('staff.assignments.units', 'Unités organisationnelles') }}</h4>
              <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('staff.assignments.unitsHelp', 'Le rôle d’affectation est contextuel et distinct des rôles RBAC.') }}</p>
            </div>

            <!-- Liste des cartes enregistrées d'abord -->
            <div class="space-y-3">
              @for (item of structure().unitAssignments; track item.id) {
                <article class="rounded-md border border-[var(--app-border)] bg-[var(--app-surface)] p-3 shadow-xs">
                  <div class="flex flex-wrap items-start justify-between gap-2">
                    <div>
                      <p class="font-bold text-[var(--text-primary)]">{{ unitAssignmentLabel(item) }}</p>
                      <p class="mt-1 text-xs text-[var(--text-muted)]">{{ assignmentRoleLabel(item.assignmentRoleCode) }}</p>
                      <p class="mt-1 text-xs text-[var(--text-muted)]">
                        {{ item.validFrom | date:'dd/MM/yyyy HH:mm' }} → {{ item.validTo ? (item.validTo | date:'dd/MM/yyyy HH:mm') : t('staff.assignments.openEnded', 'sans fin') }}
                      </p>
                    </div>
                    <div class="flex flex-wrap gap-2">
                      @if (item.primary) {
                        <span class="rounded bg-[var(--app-surface-muted)] px-2 py-1 text-[10px] font-bold uppercase text-[var(--text-secondary)]">{{ t('staff.assignments.primary', 'Principale') }}</span>
                      }
                      <span class="rounded px-2 py-1 text-[10px] font-bold uppercase" [class.text-brand-primary]="item.active" [class.text-[var(--text-muted)]]="!item.active">
                        {{ item.active ? t('common.active') : t('staff.assignments.historical', 'Historique') }}
                      </span>
                    </div>
                  </div>
                  @if (item.active) {
                    <div class="mt-3 border-t border-[var(--app-border)] pt-2">
                      <app-ui-button variant="secondary" [disabled]="saving()" (pressed)="closeUnit(item)">
                        {{ t('staff.assignments.close', 'Clôturer maintenant') }}
                      </app-ui-button>
                    </div>
                  }
                </article>
              } @empty {
                <p class="text-sm text-[var(--text-muted)]">{{ t('staff.assignments.noUnit', 'Aucune affectation d’unité.') }}</p>
              }
            </div>

            <!-- Bouton pour ouvrir / fermer le formulaire d'ajout -->
            <div class="pt-2">
              <app-ui-button variant="secondary" class="w-full sm:w-auto" [disabled]="saving()" (pressed)="toggleUnitForm()">
                {{ showUnitForm() ? t('common.cancel', 'Annuler') : ('+ ' + t('staff.assignments.addUnitToggle', 'Ajouter une affectation')) }}
              </app-ui-button>
            </div>

            <!-- Formulaire d'ajout (affiché uniquement sur demande) -->
            @if (showUnitForm()) {
              <form class="mt-3 space-y-4 rounded-md border border-[var(--app-border)] bg-[var(--app-surface)] p-4 shadow-xs" (submit)="$event.preventDefault(); addUnit()">
                <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
                  <label class="space-y-1.5 sm:col-span-2">
                    <span class="ui-label">{{ t('staff.assignments.unit', 'Unité') }}</span>
                    <select class="ui-select" [value]="unitId()" [disabled]="saving()" (change)="unitId.set($any($event.target).value)">
                      <option value="">{{ t('staff.assignments.chooseUnit', 'Choisir une unité...') }}</option>
                      @for (item of units(); track item.id) {
                        <option [value]="item.id">{{ unitLabel(item) }}</option>
                      }
                    </select>
                  </label>

                  <label class="space-y-1.5 sm:col-span-2">
                    <span class="ui-label">{{ t('staff.assignments.assignmentRole', 'Rôle dans l’unité') }}</span>
                    <select class="ui-select" [value]="unitRoleCode()" [disabled]="saving()" (change)="unitRoleCode.set($any($event.target).value)">
                      <option value="">{{ t('staff.assignments.chooseRole', 'Choisir un rôle...') }}</option>
                      @for (item of assignmentRoles(); track item.code) {
                        <option [value]="item.code">{{ localized(item.nameFr, item.nameEn) }}</option>
                      }
                    </select>
                  </label>

                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('staff.assignments.validFrom', 'Début') }}</span>
                    <input class="ui-input" type="datetime-local" [value]="unitFrom()" [disabled]="saving()" (input)="unitFrom.set($any($event.target).value)" />
                  </label>

                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('staff.assignments.validTo', 'Fin facultative') }}</span>
                    <input class="ui-input" type="datetime-local" [value]="unitTo()" [disabled]="saving()" (input)="unitTo.set($any($event.target).value)" />
                  </label>
                </div>

                <label class="flex items-center gap-2 text-sm font-semibold text-[var(--text-secondary)]">
                  <input type="checkbox" class="ui-checkbox" [checked]="unitPrimary()" [disabled]="saving()" (change)="unitPrimary.set($any($event.target).checked)" />
                  {{ t('staff.assignments.primaryUnit', 'Unité principale sur cette période') }}
                </label>

                <div class="flex flex-wrap items-center gap-3 pt-2">
                  <app-ui-button type="submit" [disabled]="saving() || !unitId() || !unitRoleCode() || !unitFrom()">
                    {{ t('staff.assignments.addUnit', 'Enregistrer l’affectation') }}
                  </app-ui-button>
                  <app-ui-button variant="secondary" [disabled]="saving()" (pressed)="toggleUnitForm()">
                    {{ t('common.cancel', 'Annuler') }}
                  </app-ui-button>
                </div>
              </form>
            }
          </section>
        </div>
      }
    </section>
  `,
})
export class StaffAssignmentEditorComponent {
  readonly staffId = input.required<string>();

  private readonly api = inject(StaffApiService);
  private readonly organizationApi = inject(HospitalOrganizationApiService);
  private readonly i18n = inject(I18nService);

  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showSpecialtyForm = signal(false);
  readonly showUnitForm = signal(false);
  readonly specialties = signal<MedicalSpecialtyCatalogEntry[]>([]);
  readonly services = signal<HospitalServiceCatalogEntry[]>([]);
  readonly units = signal<OrganizationalUnit[]>([]);
  readonly assignmentRoles = signal<StaffAssignmentRole[]>([]);
  readonly structure = signal<StaffAssignmentStructure>(EMPTY_STRUCTURE);

  readonly specialtyCode = signal('');
  readonly specialtyPrimary = signal(false);
  readonly specialtyFrom = signal(this.localDateTimeNow());
  readonly specialtyTo = signal('');

  readonly unitId = signal('');
  readonly unitRoleCode = signal('');
  readonly unitPrimary = signal(false);
  readonly unitFrom = signal(this.localDateTimeNow());
  readonly unitTo = signal('');

  constructor() {
    effect(() => {
      const id = this.staffId();
      if (id) this.load(id);
    });
  }

  toggleSpecialtyForm(): void {
    this.showSpecialtyForm.update((value) => !value);
  }

  toggleUnitForm(): void {
    this.showUnitForm.update((value) => !value);
  }

  addSpecialty(): void {
    const staffId = this.staffId();
    const specialtyCode = this.specialtyCode();
    const validFrom = this.toIso(this.specialtyFrom());
    if (!staffId || !specialtyCode || !validFrom) return;

    this.saving.set(true);
    this.error.set(null);
    this.api.createSpecialtyAssignment(staffId, {
      specialtyCode,
      primary: this.specialtyPrimary(),
      validFrom,
      validTo: this.toIso(this.specialtyTo()) || undefined,
    }).subscribe({
      next: () => {
        this.specialtyCode.set('');
        this.specialtyPrimary.set(false);
        this.specialtyFrom.set(this.localDateTimeNow());
        this.specialtyTo.set('');
        this.showSpecialtyForm.set(false);
        this.saving.set(false);
        this.loadStructure(staffId);
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  addUnit(): void {
    const staffId = this.staffId();
    const organizationalUnitId = this.unitId();
    const assignmentRoleCode = this.unitRoleCode();
    const validFrom = this.toIso(this.unitFrom());
    if (!staffId || !organizationalUnitId || !assignmentRoleCode || !validFrom) return;

    this.saving.set(true);
    this.error.set(null);
    this.api.createUnitAssignment(staffId, {
      organizationalUnitId,
      assignmentRoleCode,
      primary: this.unitPrimary(),
      validFrom,
      validTo: this.toIso(this.unitTo()) || undefined,
    }).subscribe({
      next: () => {
        this.unitId.set('');
        this.unitRoleCode.set('');
        this.unitPrimary.set(false);
        this.unitFrom.set(this.localDateTimeNow());
        this.unitTo.set('');
        this.showUnitForm.set(false);
        this.saving.set(false);
        this.loadStructure(staffId);
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  closeSpecialty(item: StaffSpecialtyAssignment): void {
    this.saving.set(true);
    this.error.set(null);
    this.api.closeSpecialtyAssignment(this.staffId(), item.id, { closedAt: new Date().toISOString() }).subscribe({
      next: () => {
        this.saving.set(false);
        this.loadStructure(this.staffId());
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  closeUnit(item: StaffUnitAssignment): void {
    this.saving.set(true);
    this.error.set(null);
    this.api.closeUnitAssignment(this.staffId(), item.id, { closedAt: new Date().toISOString() }).subscribe({
      next: () => {
        this.saving.set(false);
        this.loadStructure(this.staffId());
      },
      error: (error) => this.handleSaveError(error),
    });
  }

  specialtyLabel(code: string): string {
    const item = this.specialties().find((candidate) => candidate.code === code);
    return item ? this.localized(item.nameFr, item.nameEn) : code;
  }

  unitAssignmentLabel(item: StaffUnitAssignment): string {
    const unit = this.units().find((candidate) => candidate.id === item.organizationalUnitId);
    return unit ? this.unitLabel(unit) : item.organizationalUnitId;
  }

  assignmentRoleLabel(code: string): string {
    const item = this.assignmentRoles().find((candidate) => candidate.code === code);
    return item ? this.localized(item.nameFr, item.nameEn) : code;
  }

  unitLabel(unit: OrganizationalUnit): string {
    if (unit.name) return `${unit.name} · ${unit.code}`;
    const service = this.services().find((candidate) => candidate.code === unit.serviceCatalogCode);
    return `${service ? this.localized(service.nameFr, service.nameEn) : (unit.serviceCatalogCode ?? unit.code)} · ${unit.code}`;
  }

  localized(fr: string, en: string): string {
    return this.i18n.currentLanguage() === 'en' ? en : fr;
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  private load(staffId: string): void {
    this.loading.set(true);
    this.error.set(null);
    forkJoin({
      specialties: this.organizationApi.listSpecialtyCatalog(),
      services: this.organizationApi.listServiceCatalog(),
      units: this.organizationApi.listUnits(undefined, false),
      roles: this.api.listAssignmentRoles(),
      structure: this.api.getAssignments(staffId),
    }).subscribe({
      next: ({ specialties, services, units, roles, structure }) => {
        this.specialties.set(specialties);
        this.services.set(services);
        this.units.set(units.filter((item) => item.active));
        this.assignmentRoles.set(roles);
        this.structure.set(structure);
        this.loading.set(false);
      },
      error: (error) => {
        this.error.set(this.apiError(error, this.t('staff.assignments.loadError', 'Impossible de charger les affectations.')));
        this.loading.set(false);
      },
    });
  }

  private loadStructure(staffId: string): void {
    this.api.getAssignments(staffId).subscribe({
      next: (structure) => this.structure.set(structure),
      error: (error) => this.error.set(this.apiError(error, this.t('staff.assignments.loadError', 'Impossible de charger les affectations.'))),
    });
  }

  private handleSaveError(error: unknown): void {
    this.saving.set(false);
    this.error.set(this.apiError(error, this.t('staff.assignments.saveError', 'Impossible d’enregistrer l’affectation.')));
  }

  private apiError(error: unknown, fallback: string): string {
    const value = error as { error?: { detail?: string; message?: string; error?: { message?: string } } };
    return value?.error?.error?.message ?? value?.error?.detail ?? value?.error?.message ?? fallback;
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
}

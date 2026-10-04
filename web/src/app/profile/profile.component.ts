import { CommonModule } from '@angular/common';
import { ProfileApiService } from './profile-api.service';
import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { catchError, forkJoin, of } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { StaffApiService } from '../clinic/staff/staff-api.service';
import { StaffProfileActiveStructure } from '../clinic/staff/staff.models';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { FileDragDropComponent } from '../shared/ui/file-drag-drop.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';

const EMPTY_ASSIGNMENTS: StaffProfileActiveStructure = { specialties: [], unitAssignments: [] };

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    AlertComponent,
    AppShellComponent,
    ButtonComponent,
    CardComponent,
    PageHeaderComponent,
    FileDragDropComponent,
  ],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('profile.title')"
        [subtitle]="t('profile.subtitle')"
        backLink="/dashboard"
        [backLabel]="t('common.back')"
      />

      <div class="app-container space-y-6 pb-12">
        @if (error(); as err) {
          <app-ui-alert tone="error">{{ err }}</app-ui-alert>
        }
        @if (assignmentsWarning(); as warning) {
          <app-ui-alert tone="info">{{ warning }}</app-ui-alert>
        }
        @if (success(); as msg) {
          <app-ui-alert tone="info">{{ msg }}</app-ui-alert>
        }

        <div class="grid grid-cols-1 gap-6 lg:grid-cols-3">
          <div class="space-y-6 lg:col-span-1">
            <app-ui-card [title]="t('profile.avatarTitle')">
              <div class="flex flex-col items-center justify-center p-4">
                <app-file-drag-drop
                  #photoUploader
                  [label]="t('profile.photoLabel')"
                  [previewUrl]="photoViewUrl()"
                  (fileSelected)="onFileSelected($event, 'photo', photoUploader)"
                  (fileRemoved)="onFileRemoved('photo')"
                  class="w-full"
                />
              </div>
            </app-ui-card>

            @if (isDoctor()) {
              <app-ui-card [title]="t('profile.medicalAssetsTitle')">
                <div class="space-y-6 p-2">
                  <app-file-drag-drop
                    #sigUploader
                    [label]="t('profile.signatureLabel')"
                    accept="image/png, image/jpeg"
                    [previewUrl]="signatureViewUrl()"
                    (fileSelected)="onFileSelected($event, 'signature', sigUploader)"
                    (fileRemoved)="onFileRemoved('signature')"
                  />
                  <p class="text-xs text-[var(--text-muted)]">{{ t('profile.signaturePngHint') }}</p>
                  <app-file-drag-drop
                    #stampUploader
                    [label]="t('profile.stampLabel')"
                    [previewUrl]="stampViewUrl()"
                    (fileSelected)="onFileSelected($event, 'stamp', stampUploader)"
                    (fileRemoved)="onFileRemoved('stamp')"
                  />
                </div>
              </app-ui-card>
            }
          </div>

          <div class="space-y-6 lg:col-span-2">
            <app-ui-card [title]="t('profile.detailsTitle')">
              <form class="space-y-5" (submit)="$event.preventDefault(); saveProfile()">
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('profile.displayName') }} <span class="text-[var(--brand-danger)]">*</span></span>
                    <input
                      class="ui-input"
                      [value]="displayName()"
                      [placeholder]="t('profile.displayNamePlaceholder')"
                      [disabled]="loading()"
                      (input)="displayName.set($any($event.target).value)"
                      required
                    />
                  </label>

                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('profile.email') }}</span>
                    <input class="ui-input cursor-not-allowed bg-slate-100/70 dark:bg-[var(--bg-input)]/70" [value]="email()" disabled />
                  </label>

                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('profile.phone') }}</span>
                    <input
                      class="ui-input"
                      type="tel"
                      [value]="phone()"
                      [placeholder]="t('profile.phonePlaceholder')"
                      [disabled]="loading()"
                      (input)="phone.set($any($event.target).value)"
                    />
                  </label>

                  @if (isDoctor()) {
                    <label class="space-y-1.5">
                      <span class="ui-label">{{ t('profile.registrationNumber') }} <span class="text-[var(--brand-danger)]">*</span></span>
                      <input
                        class="ui-input"
                        [value]="registrationNumber()"
                        [placeholder]="t('profile.registrationNumberPlaceholder')"
                        [disabled]="loading()"
                        (input)="registrationNumber.set($any($event.target).value)"
                        required
                      />
                    </label>
                  }
                </div>

                <label class="block space-y-1.5">
                  <span class="ui-label">{{ t('profile.bio') }}</span>
                  <textarea
                    class="ui-input h-28 resize-y py-2"
                    [value]="bio()"
                    [placeholder]="t('profile.bioPlaceholder')"
                    [disabled]="loading()"
                    (input)="bio.set($any($event.target).value)"
                  ></textarea>
                </label>

                <div class="flex justify-end border-t border-[var(--app-border)] pt-3">
                  <app-ui-button type="submit" [disabled]="loading()">
                    {{ loading() ? t('common.saving') : t('common.save') }}
                  </app-ui-button>
                </div>
              </form>
            </app-ui-card>

            <app-ui-card [title]="t('profile.professionalContext.title', 'Mon exercice professionnel')">
              <div class="space-y-5">
                <div class="flex flex-wrap gap-2">
                  @for (roleCode of roleCodes(); track roleCode) {
                    <span class="rounded-md border border-[var(--app-border)] bg-[var(--app-surface-muted)] px-3 py-1.5 text-xs font-bold text-[var(--text-secondary)]">
                      {{ roleLabel(roleCode) }}
                    </span>
                  }
                </div>

                <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
                  <section class="rounded-md border border-[var(--app-border)] p-4">
                    <p class="text-[10px] font-extrabold uppercase tracking-[0.14em] text-brand-cyan">
                      {{ t('profile.professionalContext.specialtiesEyebrow', 'Spécialités') }}
                    </p>
                    <h3 class="mt-1 font-extrabold text-[var(--text-primary)]">
                      {{ t('profile.professionalContext.specialtiesTitle', 'Spécialités actives') }}
                    </h3>
                    <div class="mt-3 space-y-2">
                      @for (item of assignments().specialties; track item.specialtyCode) {
                        <div class="rounded-md bg-[var(--app-surface-muted)] p-3">
                          <div class="flex flex-wrap items-center justify-between gap-2">
                            <strong class="text-sm text-[var(--text-primary)]">{{ localized(item.nameFr, item.nameEn) }}</strong>
                            @if (item.primary) {
                              <span class="rounded bg-[var(--app-surface)] px-2 py-1 text-[10px] font-bold uppercase text-[var(--text-secondary)]">
                                {{ t('profile.professionalContext.primary', 'Principale') }}
                              </span>
                            }
                          </div>
                        </div>
                      } @empty {
                        <p class="text-sm text-[var(--text-muted)]">{{ t('profile.professionalContext.noSpecialty', 'Aucune spécialité active.') }}</p>
                      }
                    </div>
                  </section>

                  <section class="rounded-md border border-[var(--app-border)] p-4">
                    <p class="text-[10px] font-extrabold uppercase tracking-[0.14em] text-brand-cyan">
                      {{ t('profile.professionalContext.unitsEyebrow', 'Organisation') }}
                    </p>
                    <h3 class="mt-1 font-extrabold text-[var(--text-primary)]">
                      {{ t('profile.professionalContext.unitsTitle', 'Unités d’exercice actives') }}
                    </h3>
                    <div class="mt-3 space-y-2">
                      @for (item of assignments().unitAssignments; track item.organizationalUnitId) {
                        <div class="rounded-md bg-[var(--app-surface-muted)] p-3">
                          <div class="flex flex-wrap items-start justify-between gap-2">
                            <div>
                              <strong class="text-sm text-[var(--text-primary)]">{{ localized(item.nameFr, item.nameEn) }}</strong>
                              <p class="mt-1 text-xs text-[var(--text-muted)]">{{ localized(item.assignmentRoleNameFr, item.assignmentRoleNameEn) }}</p>
                            </div>
                            @if (item.primary) {
                              <span class="rounded bg-[var(--app-surface)] px-2 py-1 text-[10px] font-bold uppercase text-[var(--text-secondary)]">
                                {{ t('profile.professionalContext.primary', 'Principale') }}
                              </span>
                            }
                          </div>
                        </div>
                      } @empty {
                        <p class="text-sm text-[var(--text-muted)]">{{ t('profile.professionalContext.noUnit', 'Aucune unité active.') }}</p>
                      }
                    </div>
                  </section>
                </div>

                <app-ui-alert tone="info">
                  {{ t('profile.assignmentsManaged', 'Ces affectations sont administrées par l’établissement afin de préserver leur historique et leur cohérence organisationnelle.') }}
                </app-ui-alert>
              </div>
            </app-ui-card>
          </div>
        </div>
      </div>
    </app-shell>
  `,
})
export class ProfileComponent implements OnInit {
  private readonly api = inject(ProfileApiService);
  private readonly staffApi = inject(StaffApiService);
  private readonly i18n = inject(I18nService);

  @ViewChild('photoUploader') photoUploader!: FileDragDropComponent;
  @ViewChild('sigUploader') sigUploader!: FileDragDropComponent;
  @ViewChild('stampUploader') stampUploader!: FileDragDropComponent;

  readonly displayName = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly registrationNumber = signal('');
  readonly bio = signal('');
  readonly role = signal('');
  readonly assignments = signal<StaffProfileActiveStructure>(EMPTY_ASSIGNMENTS);

  readonly photoPath = signal<string | null>(null);
  readonly signaturePath = signal<string | null>(null);
  readonly stampPath = signal<string | null>(null);

  readonly photoViewUrl = computed(() => this.photoPath() ? `/api/public/files/view?path=${this.photoPath()}` : null);
  readonly signatureViewUrl = computed(() => this.signaturePath() ? `/api/public/files/view?path=${this.signaturePath()}` : null);
  readonly stampViewUrl = computed(() => this.stampPath() ? `/api/public/files/view?path=${this.stampPath()}` : null);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly assignmentsWarning = signal<string | null>(null);
  readonly roleCodes = computed(() => this.role().split(',').map((value) => value.trim()).filter(Boolean));
  readonly isDoctor = computed(() => this.roleCodes().includes('MEDECIN'));

  ngOnInit(): void {
    this.loadProfile();
  }

  t(key: string, fallback?: string): string {
    return this.i18n.t(key, fallback);
  }

  localized(fr: string, en: string): string {
    return this.i18n.currentLanguage() === 'en' ? en : fr;
  }

  roleLabel(code: string): string {
    return this.t(`staff.roles.${code}`, code.replaceAll('_', ' '));
  }

  loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);
    this.assignmentsWarning.set(null);
    forkJoin({
      profile: this.api.load(),
      assignments: this.staffApi.getOwnActiveAssignments().pipe(catchError(() => {
        this.assignmentsWarning.set(this.t('profile.assignmentsUnavailable'));
        return of(EMPTY_ASSIGNMENTS);
      })),
    }).subscribe({
      next: ({ profile, assignments }) => {
        this.displayName.set(profile.displayName || '');
        this.email.set(profile.email || '');
        this.phone.set(profile.phone || '');
        this.registrationNumber.set(profile.registrationNumber || '');
        this.bio.set(profile.bio || '');
        this.role.set(profile.role || '');
        this.photoPath.set(profile.photoPath || null);
        this.signaturePath.set(profile.signaturePath || null);
        this.stampPath.set(profile.stampPath || null);
        this.assignments.set(assignments);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.t('profile.loadError', 'Erreur de chargement du profil.'));
        this.loading.set(false);
      },
    });
  }

  onFileSelected(file: File, type: 'photo' | 'signature' | 'stamp', component: FileDragDropComponent): void {
    this.error.set(null);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', type);

    this.api.upload(formData).subscribe({
      next: (response) => {
        if (type === 'photo') this.photoPath.set(response.filePath);
        if (type === 'signature') this.signaturePath.set(response.filePath);
        if (type === 'stamp') this.stampPath.set(response.filePath);
        component.setPreviewUrl(response.viewUrl, file.name);
      },
      error: (error) => {
        console.error(error);
        this.error.set(this.t('profile.uploadError', "Erreur lors du chargement de l'image."));
      },
    });
  }

  onFileRemoved(type: 'photo' | 'signature' | 'stamp'): void {
    if (type === 'photo') this.photoPath.set(null);
    if (type === 'signature') this.signaturePath.set(null);
    if (type === 'stamp') this.stampPath.set(null);
  }

  saveProfile(): void {
    this.error.set(null);
    this.success.set(null);

    if (!this.displayName().trim()) {
      this.error.set(this.t('profile.requiredName', "Le nom d'affichage est requis."));
      return;
    }
    if (this.isDoctor() && !this.registrationNumber().trim()) {
      this.error.set(this.t('profile.requiredRegistrationNumber', "Le numéro d'ordre est requis."));
      return;
    }

    this.loading.set(true);
    const body = {
      displayName: this.displayName().trim(),
      phone: this.phone().trim() || null,
      photoPath: this.photoPath(),
      signaturePath: this.signaturePath(),
      stampPath: this.stampPath(),
      registrationNumber: this.registrationNumber().trim() || null,
      bio: this.bio().trim() || null,
    };

    this.api.save(body).subscribe({
      next: () => {
        this.success.set(this.t('profile.saveSuccess', 'Profil enregistré avec succès !'));
        this.loading.set(false);
      },
      error: (error) => {
        console.error(error);
        this.error.set(error.error?.detail || this.t('profile.saveError', 'Impossible de sauvegarder le profil.'));
        this.loading.set(false);
      },
    });
  }
}

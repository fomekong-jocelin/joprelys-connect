import { Component, computed, inject, OnInit, signal, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { AlertComponent } from '../shared/ui/alert.component';
import { ButtonComponent } from '../shared/ui/button.component';
import { CardComponent } from '../shared/ui/card.component';
import { PageHeaderComponent } from '../shared/ui/page-header.component';
import { FileDragDropComponent } from '../shared/ui/file-drag-drop.component';

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
      >
      </app-page-header>

      <div class="app-container space-y-6 pb-12">
        @if (error(); as err) {
          <app-ui-alert tone="error">{{ err }}</app-ui-alert>
        }
        @if (success(); as msg) {
          <app-ui-alert tone="info">{{ msg }}</app-ui-alert>
        }

        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          <!-- Section 1 : Photo de profil & Informations de base -->
          <div class="lg:col-span-1 space-y-6">
            <app-ui-card [title]="t('profile.avatarTitle')">
              <div class="flex flex-col items-center justify-center p-4">
                <app-file-drag-drop
                  #photoUploader
                  [label]="t('profile.photoLabel')"
                  [previewUrl]="photoViewUrl()"
                  (fileSelected)="onFileSelected($event, 'photo', photoUploader)"
                  (fileRemoved)="onFileRemoved('photo')"
                  class="w-full"
                >
                </app-file-drag-drop>
              </div>
            </app-ui-card>

            @if (isDoctor()) {
              <!-- Cachet & Signature (Médecins uniquement) -->
              <app-ui-card [title]="t('profile.medicalAssetsTitle')">
                <div class="space-y-6 p-2">
                  <app-file-drag-drop
                    #sigUploader
                    [label]="t('profile.signatureLabel')"
                    [previewUrl]="signatureViewUrl()"
                    (fileSelected)="onFileSelected($event, 'signature', sigUploader)"
                    (fileRemoved)="onFileRemoved('signature')"
                  >
                  </app-file-drag-drop>

                  <app-file-drag-drop
                    #stampUploader
                    [label]="t('profile.stampLabel')"
                    [previewUrl]="stampViewUrl()"
                    (fileSelected)="onFileSelected($event, 'stamp', stampUploader)"
                    (fileRemoved)="onFileRemoved('stamp')"
                  >
                  </app-file-drag-drop>
                </div>
              </app-ui-card>
            }
          </div>

          <!-- Section 2 : Détails du profil & Paramètres -->
          <div class="lg:col-span-2">
            <app-ui-card [title]="t('profile.detailsTitle')">
              <form class="space-y-5" (submit)="$event.preventDefault(); saveProfile()">
                <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
                  
                  <label class="space-y-1.5">
                    <span class="ui-label">{{ t('profile.displayName') }} <span class="text-red-500">*</span></span>
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
                    <input
                      class="ui-input bg-slate-100/70 dark:bg-slate-800/70 cursor-not-allowed"
                      [value]="email()"
                      disabled
                    />
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

                  <div class="space-y-1.5">
                    <label class="ui-label">{{ t('profile.department') }}</label>
                    <select
                      class="ui-select focus:border-brand-primary transition-colors"
                      [value]="selectedDept()"
                      [disabled]="loading()"
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
                        [disabled]="loading()"
                        (input)="onCustomDeptInput($any($event.target).value)"
                      />
                    }
                  </div>

                  @if (isDoctor()) {
                    <label class="space-y-1.5">
                      <span class="ui-label">{{ t('profile.specialty') }} <span class="text-red-500">*</span></span>
                      <input
                        class="ui-input"
                        [value]="specialty()"
                        [placeholder]="t('profile.specialtyPlaceholder')"
                        [disabled]="loading()"
                        (input)="specialty.set($any($event.target).value)"
                        required
                      />
                    </label>

                    <label class="space-y-1.5">
                      <span class="ui-label">{{ t('profile.registrationNumber') }} <span class="text-red-500">*</span></span>
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

                <label class="space-y-1.5 block">
                  <span class="ui-label">{{ t('profile.bio') }}</span>
                  <textarea
                    class="ui-input h-28 resize-y py-2"
                    [value]="bio()"
                    [placeholder]="t('profile.bioPlaceholder')"
                    [disabled]="loading()"
                    (input)="bio.set($any($event.target).value)"
                  ></textarea>
                </label>

                <div class="flex justify-end pt-3 border-t border-[var(--app-border)]">
                  <app-ui-button type="submit" [disabled]="loading()">
                    {{ loading() ? t('common.saving') : t('common.save') }}
                  </app-ui-button>
                </div>
              </form>
            </app-ui-card>
          </div>

        </div>
      </div>
    </app-shell>
  `
})
export class ProfileComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly i18n = inject(I18nService);

  @ViewChild('photoUploader') photoUploader!: FileDragDropComponent;
  @ViewChild('sigUploader') sigUploader!: FileDragDropComponent;
  @ViewChild('stampUploader') stampUploader!: FileDragDropComponent;

  readonly displayName = signal('');
  readonly email = signal('');
  readonly phone = signal('');
  readonly department = signal('');
  readonly specialty = signal('');
  readonly registrationNumber = signal('');
  readonly bio = signal('');
  readonly role = signal('');

  readonly departments = ['Médecine générale', 'Pédiatrie', 'Gynécologie', 'Urgences', 'Pharmacie', 'Laboratoire', 'Cardiologie'];
  readonly selectedDept = signal('');
  readonly customDept = signal('');

  readonly photoPath = signal<string | null>(null);
  readonly signaturePath = signal<string | null>(null);
  readonly stampPath = signal<string | null>(null);

  readonly photoViewUrl = computed(() => this.photoPath() ? `/api/public/files/view?path=${this.photoPath()}` : null);
  readonly signatureViewUrl = computed(() => this.signaturePath() ? `/api/public/files/view?path=${this.signaturePath()}` : null);
  readonly stampViewUrl = computed(() => this.stampPath() ? `/api/public/files/view?path=${this.stampPath()}` : null);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly isDoctor = computed(() => this.role().split(',').map(r => r.trim()).includes('MEDECIN'));

  ngOnInit(): void {
    this.loadProfile();
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  loadProfile(): void {
    this.loading.set(true);
    this.error.set(null);
    this.http.get<any>('/api/profile').subscribe({
      next: (data) => {
        this.displayName.set(data.displayName || '');
        this.email.set(data.email || '');
        this.phone.set(data.phone || '');
        
        const dept = data.department || '';
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

        this.specialty.set(data.specialty || '');
        this.registrationNumber.set(data.registrationNumber || '');
        this.bio.set(data.bio || '');
        this.role.set(data.role || '');
        this.photoPath.set(data.photoPath);
        this.signaturePath.set(data.signaturePath);
        this.stampPath.set(data.stampPath);

        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(this.t('profile.loadError') || "Erreur de chargement du profil.");
        this.loading.set(false);
      }
    });
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
    this.error.set(null);
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
        this.error.set(this.t('profile.uploadError') || "Erreur lors du chargement de l'image.");
      }
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
      this.error.set(this.t('profile.requiredName') || "Le nom d'affichage est requis.");
      return;
    }

    if (this.isDoctor()) {
      if (!this.specialty().trim() || !this.registrationNumber().trim()) {
        this.error.set(this.t('profile.requiredMedical') || "La spécialité et le numéro d'ordre sont requis.");
        return;
      }
    }

    this.loading.set(true);

    const body = {
      displayName: this.displayName().trim(),
      phone: this.phone().trim() || null,
      photoPath: this.photoPath(),
      signaturePath: this.signaturePath(),
      stampPath: this.stampPath(),
      specialty: this.specialty().trim() || null,
      registrationNumber: this.registrationNumber().trim() || null,
      department: this.department().trim() || null,
      bio: this.bio().trim() || null,
    };

    this.http.put<any>('/api/profile', body).subscribe({
      next: () => {
        this.success.set(this.t('profile.saveSuccess') || "Profil enregistré avec succès !");
        this.loading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.error.set(err.error?.detail || this.t('profile.saveError') || "Impossible de sauvegarder le profil.");
        this.loading.set(false);
      }
    });
  }
}

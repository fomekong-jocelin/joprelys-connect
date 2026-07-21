import { DatePipe } from '@angular/common';
import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import {
  EmergencyDocument,
  EmergencyDocumentApiService,
} from './emergency-document-api.service';

@Component({
  selector: 'app-emergency-documents-panel',
  standalone: true,
  imports: [DatePipe, AlertComponent, ButtonComponent],
  template: `
    <section class="space-y-4">
      <div class="ui-card-subtle p-4">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
          <div>
            <p class="text-[10px] font-extrabold uppercase tracking-[0.16em] text-brand-primary">
              {{ t('emergency.documents.eyebrow', 'Continuité documentaire') }}
            </p>
            <h3 class="mt-1 font-display text-base font-extrabold text-[var(--text-primary)]">
              {{ t('emergency.documents.title', 'Documents vérifiables de l’urgence') }}
            </h3>
            <p class="mt-1 text-sm leading-6 text-[var(--text-muted)]">
              {{ t('emergency.documents.description', 'Les documents conservent leur numéro, leur version, leur empreinte et leur patient d’origine après le rapprochement.') }}
            </p>
          </div>
          <app-ui-button variant="secondary" (pressed)="generate()" [disabled]="loading() || generating()">
            {{ generating()
              ? t('emergency.documents.generating', 'Génération…')
              : t('emergency.documents.generate', 'Générer ou vérifier le lot') }}
          </app-ui-button>
        </div>

        @if (error(); as message) {
          <app-ui-alert tone="error" class="mt-4">{{ message }}</app-ui-alert>
        }

        @if (loading()) {
          <p class="mt-4 text-sm font-semibold text-[var(--text-muted)]">
            {{ t('common.loading', 'Chargement…') }}
          </p>
        } @else if (documents().length === 0) {
          <div class="mt-4 rounded-sm border border-dashed border-[var(--app-border)] bg-[var(--app-surface)] px-4 py-8 text-center">
            <p class="text-sm font-bold text-[var(--text-primary)]">
              {{ t('emergency.documents.emptyTitle', 'Aucun document généré') }}
            </p>
            <p class="mt-1 text-xs leading-5 text-[var(--text-muted)]">
              {{ t('emergency.documents.emptyDescription', 'Le lot peut être généré avant ou après l’hospitalisation dès qu’une visite est associée à l’urgence.') }}
            </p>
          </div>
        } @else {
          <div class="mt-4 grid gap-3 sm:grid-cols-2">
            @for (document of documents(); track document.id) {
              <article class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
                <div class="flex items-start justify-between gap-3">
                  <div class="min-w-0">
                    <p class="text-xs font-extrabold text-[var(--text-primary)]">
                      {{ documentLabel(document.documentType) }}
                    </p>
                    <p class="mt-1 truncate font-mono text-[10px] font-bold text-brand-primary" [title]="document.documentNumber">
                      {{ document.documentNumber }}
                    </p>
                  </div>
                  <span class="rounded-sm bg-[var(--brand-success-subtle)] px-2 py-0.5 text-[10px] font-extrabold text-[var(--brand-success-text)]">
                    v{{ document.version }}
                  </span>
                </div>
                <dl class="mt-3 space-y-2 text-xs">
                  <div>
                    <dt class="ui-label">{{ t('emergency.documents.hash', 'Empreinte') }}</dt>
                    <dd class="mt-0.5 truncate font-mono text-[10px] text-[var(--text-secondary)]" [title]="document.hash">
                      {{ document.hash }}
                    </dd>
                  </div>
                  <div class="flex items-center justify-between gap-2">
                    <span class="text-[var(--text-muted)]">{{ document.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                    <span class="font-bold text-[var(--text-secondary)]">
                      {{ t('emergency.documents.origin', 'Origine conservée') }}
                    </span>
                  </div>
                </dl>
              </article>
            }
          </div>
        }
      </div>

      <div class="rounded-lg border border-[var(--brand-info-border)] bg-[var(--brand-info-subtle)] p-4">
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h3 class="font-display text-base font-extrabold text-[var(--text-primary)]">
              {{ t('emergency.documents.hospitalizationTitle', 'Continuer le parcours de soins') }}
            </h3>
            <p class="mt-1 text-sm leading-6 text-[var(--text-secondary)]">
              {{ t('emergency.documents.hospitalizationDescription', 'Sélectionnez un service, un lit libre et un médecin responsable sans perdre l’urgence d’origine.') }}
            </p>
          </div>
          <app-ui-button variant="primary" (pressed)="hospitalizationRequested.emit()">
            {{ t('emergency.documents.hospitalizationAction', 'Poursuivre vers l’hospitalisation') }}
          </app-ui-button>
        </div>
      </div>
    </section>
  `,
})
export class EmergencyDocumentsPanelComponent implements OnInit {
  private readonly api = inject(EmergencyDocumentApiService);
  private readonly i18n = inject(I18nService);

  readonly emergencyId = input.required<string>();
  readonly hospitalizationRequested = output<void>();
  readonly documents = signal<EmergencyDocument[]>([]);
  readonly loading = signal(false);
  readonly generating = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  t(key: string, fallback: string): string {
    return this.i18n.t(key, fallback);
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listByEmergency(this.emergencyId()).pipe(
      finalize(() => this.loading.set(false)),
    ).subscribe({
      next: (documents) => this.documents.set(documents),
      error: () => this.error.set(this.t(
        'emergency.documents.loadError',
        'Les documents d’urgence n’ont pas pu être chargés.',
      )),
    });
  }

  generate(): void {
    if (this.generating()) return;
    this.generating.set(true);
    this.error.set(null);
    this.api.generateBundle(this.emergencyId()).pipe(
      finalize(() => this.generating.set(false)),
    ).subscribe({
      next: (documents) => this.documents.set(documents),
      error: (error) => this.error.set(
        error?.error?.detail
        || error?.error?.title
        || this.t(
          'emergency.documents.generateError',
          'Le lot documentaire ne peut pas encore être généré. Associez d’abord une visite ou une hospitalisation.',
        ),
      ),
    });
  }

  documentLabel(type: EmergencyDocument['documentType']): string {
    return this.t(`emergency.documents.type.${type.toLowerCase()}`, type);
  }
}

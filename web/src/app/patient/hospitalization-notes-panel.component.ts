import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n/i18n.service';
import { RbacApiService } from '../clinic/rbac/rbac-api.service';
import { PatientApiService } from './patient-api.service';
import { HospitalizationNote } from './patient.models';

@Component({
  selector: 'app-hospitalization-notes-panel',
  standalone: true,
  imports: [DatePipe, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="space-y-4" role="tabpanel" aria-label="Notes d'évolution">
      <div class="flex items-center justify-between gap-3">
        <div>
          <h3 class="text-sm font-bold text-[var(--text-primary)]">{{ t('patients.hospitalization.notes', 'Notes d’évolution journalières') }}</h3>
          <p class="mt-1 text-xs text-[var(--text-muted)]">{{ t('patients.hospitalization.notesPanelHint', 'Les transmissions sont horodatées et attribuées à leur auteur.') }}</p>
        </div>
        <button type="button" class="ui-button ui-button-secondary" (click)="reload()" [disabled]="loading()">{{ t('common.refresh', 'Actualiser') }}</button>
      </div>

      @if (canModify()) {
        <form class="border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-4" (submit)="save($event)">
          <label for="stay-note" class="text-xs font-semibold text-[var(--text-secondary)]">{{ t('patients.hospitalization.notes.add', 'Ajouter une observation') }}</label>
          <div class="mt-2 flex flex-col gap-2 sm:flex-row">
            <input id="stay-note" class="ui-input flex-1" [ngModel]="noteContent()" (ngModelChange)="noteContent.set($event)" name="note" required [placeholder]="t('patients.hospitalization.notes.add', 'Ajouter une observation')" />
            <button type="submit" class="ui-button ui-button-primary" [disabled]="!noteContent().trim() || saving()">{{ t('common.save', 'Enregistrer') }}</button>
          </div>
          @if (error()) { <p class="mt-2 text-xs font-semibold text-[var(--brand-danger-text)]" role="alert">{{ error() }}</p> }
        </form>
      }

      @if (loading()) {
        <p class="py-6 text-center text-sm text-[var(--text-muted)]">{{ t('common.loading', 'Chargement…') }}</p>
      } @else if (notes().length === 0) {
        <div class="border border-dashed border-[var(--app-border)] p-6 text-center text-sm text-[var(--text-muted)]">{{ t('patients.hospitalization.notes.empty', 'Aucune note d’évolution pour ce séjour.') }}</div>
      } @else {
        <ol class="space-y-3 border-l border-[var(--app-border)] pl-5">
          @for (note of notes(); track note.id) {
            <li class="relative">
              <span class="absolute -left-[25px] top-1.5 h-2 w-2 border-2 border-[var(--app-surface)] bg-[var(--brand-primary)]"></span>
              <article class="border border-[var(--app-border)] bg-[var(--app-surface)] p-3">
                <div class="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <strong class="text-[var(--text-primary)]">{{ note.authorName }}</strong>
                  <time class="text-[var(--text-muted)]">{{ note.createdAt | date:'dd/MM/yyyy HH:mm' }}</time>
                </div>
                <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-[var(--text-secondary)]">{{ note.noteContent }}</p>
              </article>
            </li>
          }
        </ol>
      }
    </section>
  `,
})
export class HospitalizationNotesPanelComponent {
  readonly hospitalizationId = input.required<string>();
  readonly parentCanModify = input(false, { alias: 'canModify' });
  readonly notes = signal<HospitalizationNote[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly noteContent = signal('');

  private readonly patientApi = inject(PatientApiService);
  private readonly rbacApi = inject(RbacApiService);
  private readonly i18n = inject(I18nService);
  readonly t = (key: string, fallback: string) => this.i18n.t(key, fallback);

  constructor() {
    effect(() => this.load(this.hospitalizationId()));
  }

  canModify(): boolean {
    return this.rbacApi.hasPermission('HOSPITALIZATION_NOTE_WRITE');
  }

  reload(): void {
    this.load(this.hospitalizationId());
  }

  save(event: Event): void {
    event.preventDefault();
    if (!this.canModify()) return;
    const content = this.noteContent().trim();
    if (!content) return;

    this.saving.set(true);
    this.error.set(null);
    this.patientApi.addHospitalizationNote(this.hospitalizationId(), content).subscribe({
      next: () => {
        this.noteContent.set('');
        this.saving.set(false);
        this.reload();
      },
      error: () => {
        this.saving.set(false);
        this.error.set(this.t('common.error.server', 'Une erreur est survenue.'));
      },
    });
  }

  private load(hospitalizationId: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.patientApi.getHospitalizationNotes(hospitalizationId).subscribe({
      next: (notes) => {
        this.notes.set(notes);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(this.t('common.error.server', 'Une erreur est survenue.'));
      },
    });
  }
}

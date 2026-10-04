import { DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { I18nService } from '../core/i18n/i18n.service';
import { PatientApiService } from './patient-api.service';
import { Patient } from './patient.models';

const MIN_QUERY_LENGTH = 2;
const MAX_RESULTS = 10;

/**
 * Recherche d'un patient existant (nom, téléphone, DPU) avec les éléments qui distinguent
 * les homonymes : date de naissance, numéro de dossier et téléphone.
 */
@Component({
  selector: 'app-patient-search-picker',
  standalone: true,
  imports: [DatePipe],
  template: `
    @if (selected(); as patient) {
      <div class="flex items-start justify-between gap-3 rounded-[6px] border border-[var(--brand-primary)] bg-[var(--brand-primary-subtle)] p-3">
        <div class="min-w-0 text-sm">
          <p class="font-bold text-[var(--text-primary)]">{{ displayName(patient) }}</p>
          <p class="text-xs text-[var(--text-secondary)]">{{ patient.globalPatientNumber }}@if (patient.birthDate) { · {{ patient.birthDate | date: 'dd/MM/yyyy' }}}@if (patient.phone) { · {{ patient.phone }}}</p>
        </div>
        <button type="button" class="ui-link shrink-0 text-xs font-bold" (click)="clear()">
          {{ i18n.t('patientSearch.change') }}
        </button>
      </div>
    } @else {
      <label class="ui-label mb-1.5" [attr.for]="inputId()">{{ i18n.t('patientSearch.label') }}</label>
      <input
        [id]="inputId()"
        type="search"
        class="ui-input"
        autocomplete="off"
        [placeholder]="i18n.t('patientSearch.placeholder')"
        (input)="onQuery($any($event.target).value)"
      />
      @if (isSearching()) {
        <p class="mt-2 text-xs text-[var(--text-muted)]" aria-live="polite">{{ i18n.t('patientSearch.searching') }}</p>
      } @else if (error()) {
        <p class="mt-2 text-xs font-semibold text-[var(--brand-danger-text)]">{{ i18n.t('patientSearch.error') }}</p>
      } @else if (searched() && results().length === 0) {
        <p class="mt-2 text-xs text-[var(--text-muted)]">{{ i18n.t('patientSearch.empty') }}</p>
      }
      @if (results().length > 0) {
        <ul class="mt-2 max-h-64 divide-y divide-[var(--app-border)] overflow-y-auto rounded-[6px] border border-[var(--app-border)]">
          @for (patient of results(); track patient.id) {
            <li>
              <button
                type="button"
                class="w-full px-3 py-2 text-left text-sm hover:bg-[var(--app-surface-muted)] cursor-pointer"
                (click)="choose(patient)"
              >
                <span class="block font-bold text-[var(--text-primary)]">{{ displayName(patient) }}</span>
                <span class="block text-xs text-[var(--text-secondary)]">{{ patient.globalPatientNumber }}@if (patient.birthDate) { · {{ patient.birthDate | date: 'dd/MM/yyyy' }}}@if (patient.phone) { · {{ patient.phone }}}</span>
              </button>
            </li>
          }
        </ul>
      }
    }
  `,
})
export class PatientSearchPickerComponent implements OnInit {
  readonly i18n = inject(I18nService);
  private readonly patientApi = inject(PatientApiService);
  private readonly destroyRef = inject(DestroyRef);

  readonly inputId = input('patient-search');
  readonly selected = input<Patient | null>(null);
  readonly patientSelected = output<Patient | null>();

  readonly results = signal<Patient[]>([]);
  readonly isSearching = signal(false);
  readonly searched = signal(false);
  readonly error = signal(false);

  private readonly queries = new Subject<string>();

  ngOnInit(): void {
    this.queries.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      tap(() => this.error.set(false)),
      switchMap((query) => {
        if (query.length < MIN_QUERY_LENGTH) {
          this.searched.set(false);
          return of([] as Patient[]);
        }
        this.isSearching.set(true);
        return this.patientApi.list(query).pipe(
          catchError(() => {
            this.error.set(true);
            return of([] as Patient[]);
          }),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((patients) => {
      this.isSearching.set(false);
      this.searched.set(true);
      this.results.set(patients.slice(0, MAX_RESULTS));
    });
  }

  onQuery(value: string): void {
    this.queries.next(value.trim());
  }

  choose(patient: Patient): void {
    this.results.set([]);
    this.patientSelected.emit(patient);
  }

  clear(): void {
    this.searched.set(false);
    this.patientSelected.emit(null);
  }

  displayName(patient: Patient): string {
    return patient.displayName || patient.fullName || patient.temporaryPatientNumber || patient.globalPatientNumber;
  }
}

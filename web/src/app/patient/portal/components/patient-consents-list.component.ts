import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PatientConsent, PatientPortalService } from '../services/patient-portal.service';

@Component({
  selector: 'app-patient-consents-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ui-card p-6 flex flex-col gap-6">
      <div>
        <h3 class="font-display font-extrabold text-lg text-[var(--text-primary)]">
          Gestion des consentements d'accès
        </h3>
        <p class="text-xs text-[var(--text-secondary)] mt-1">
          Sélectionnez les cliniques autorisées à consulter votre Dossier Patient Unique (DPU). 
          L'établissement créateur de votre dossier dispose d'un accès permanent pour assurer la continuité des soins.
        </p>
      </div>

      @if (isLoading()) {
        <div class="flex items-center justify-center py-12">
          <div class="w-8 h-8 border-4 border-[var(--brand-primary)] border-t-transparent rounded-full animate-spin"></div>
        </div>
      } @else if (error()) {
        <div class="p-4 rounded-[var(--radius-brand-md)] bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800/40 text-rose-700 dark:text-rose-300 text-sm font-semibold">
          {{ error() }}
        </div>
      } @else {
        <div class="flex flex-col gap-4">
          @for (consent of consents(); track consent.organizationId) {
            <div class="flex items-center justify-between p-4 rounded-[var(--radius-brand-md)] border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
              <div class="flex items-center gap-4">
                <div class="p-2.5 rounded-[var(--radius-brand-md)] bg-slate-200 dark:bg-slate-800 text-[var(--text-primary)]">
                  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="2" stroke="currentColor" class="w-5 h-5">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 21v-8.25M15.75 21v-8.25M8.25 21v-8.25M3 9l9-6 9 6m-1.5 12V10.33l-7.5-5-7.5 5V21" />
                  </svg>
                </div>
                <div>
                  <h4 class="font-display font-bold text-sm text-[var(--text-primary)]">
                    {{ consent.organizationName }}
                  </h4>
                  @if (consent.isCreator) {
                    <span class="inline-flex items-center gap-1.5 text-[10px] font-bold text-emerald-600 dark:text-emerald-400 mt-0.5">
                      <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
                      Établissement Créateur
                    </span>
                  } @else {
                    <span class="text-[10px] text-[var(--text-secondary)]">
                      {{ consent.status === 'ACTIVE' ? 'Accès autorisé' : 'Accès révoqué' }}
                    </span>
                  }
                </div>
              </div>

              <div>
                @if (consent.isCreator) {
                  <span class="text-xs font-semibold px-2.5 py-1 rounded-[var(--radius-brand-xs)] bg-emerald-100 dark:bg-emerald-950/30 text-emerald-800 dark:text-emerald-300">
                    Actif
                  </span>
                } @else {
                  <button
                    type="button"
                    [class]="consent.status === 'ACTIVE' ? 'bg-[var(--brand-primary)]' : 'bg-slate-300 dark:bg-slate-700'"
                    class="relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none"
                    [attr.aria-label]="'Autoriser ' + consent.organizationName"
                    (click)="toggleConsent(consent)"
                  >
                    <span
                      [class]="consent.status === 'ACTIVE' ? 'translate-x-5' : 'translate-x-0'"
                      class="pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out"
                    ></span>
                  </button>
                }
              </div>
            </div>
          } @empty {
            <p class="text-sm text-[var(--text-secondary)] text-center py-6">
              Aucun établissement trouvé.
            </p>
          }
        </div>
      }
    </div>
  `
})
export class PatientConsentsListComponent implements OnInit {
  private readonly portalService = inject(PatientPortalService);

  readonly consents = signal<PatientConsent[]>([]);
  readonly isLoading = signal<boolean>(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadConsents();
  }

  loadConsents(): void {
    this.isLoading.set(true);
    this.portalService.getConsents().subscribe({
      next: (data) => {
        this.consents.set(data);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger la liste des consentements.");
        this.isLoading.set(false);
      }
    });
  }

  toggleConsent(consent: PatientConsent): void {
    const nextStatus = consent.status === 'ACTIVE' ? 'REVOKED' : 'ACTIVE';
    this.portalService.updateConsent(consent.organizationId, nextStatus).subscribe({
      next: () => {
        // Mettre à jour localement l'état du consentement
        this.consents.update((list) =>
          list.map((c) =>
            c.organizationId === consent.organizationId
              ? { ...c, status: nextStatus }
              : c
          )
        );
      },
      error: () => {
        alert("Une erreur est survenue lors de la mise à jour du consentement.");
      }
    });
  }
}

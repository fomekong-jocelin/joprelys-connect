import { DatePipe } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { EmergencyApiService } from '../../emergency/emergency-api.service';
import { EmergencyRecord } from '../../emergency/emergency.models';

@Component({
  selector: 'app-patient-emergency-context',
  standalone: true,
  imports: [DatePipe],
  template: `
    <section class="ui-card p-4 sm:p-5">
      <div class="flex items-start justify-between gap-3 border-b border-[var(--divider-subtle)] pb-3">
        <div>
          <h3 class="font-display text-sm font-black uppercase tracking-wider text-[var(--text-primary)]">
            Contexte d'urgence et accompagnant
          </h3>
          <p class="mt-1 text-xs text-[var(--text-muted)]">
            Ces informations restent attachées au dossier patient après stabilisation.
          </p>
        </div>
        <span class="rounded-sm bg-[var(--brand-warning-subtle)] px-2 py-1 text-[10px] font-black uppercase tracking-wider text-[var(--brand-warning-text)]">
          Historique conservé
        </span>
      </div>

      @if (loading()) {
        <p class="py-6 text-center text-sm text-[var(--text-muted)]">Chargement de l'historique d'urgence…</p>
      } @else if (error()) {
        <p class="py-4 text-sm font-semibold text-[var(--brand-danger-text)]">{{ error() }}</p>
      } @else if (emergencies().length === 0) {
        <p class="py-5 text-sm italic text-[var(--text-muted)]">Aucun passage aux urgences enregistré.</p>
      } @else {
        <div class="mt-4 space-y-4">
          @for (emergency of emergencies(); track emergency.id) {
            <article class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface-muted)] p-3 sm:p-4">
              <header class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                <div>
                  <div class="flex flex-wrap items-center gap-2">
                    <strong class="text-sm text-[var(--text-primary)]">{{ emergency.createdAt | date:'dd/MM/yyyy HH:mm' }}</strong>
                    <span class="rounded-sm px-2 py-0.5 text-[10px] font-black uppercase"
                      [class.bg-[var(--brand-danger-subtle)]]="emergency.triageLevel === 'RED'"
                      [class.text-[var(--brand-danger-text)]]="emergency.triageLevel === 'RED'"
                      [class.bg-[var(--brand-warning-subtle)]]="emergency.triageLevel !== 'RED'"
                      [class.text-[var(--brand-warning-text)]]="emergency.triageLevel !== 'RED'">
                      Triage {{ emergency.triageLevel }}
                    </span>
                    @if (emergency.stabilizedAt) {
                      <span class="rounded-sm bg-[var(--brand-success-subtle)] px-2 py-0.5 text-[10px] font-black uppercase text-[var(--brand-success-text)]">
                        Stabilisé — {{ emergency.orientation || 'orientation non renseignée' }}
                      </span>
                    } @else {
                      <span class="rounded-sm bg-[var(--brand-danger-subtle)] px-2 py-0.5 text-[10px] font-black uppercase text-[var(--brand-danger-text)]">Actif</span>
                    }
                  </div>
                  <p class="mt-2 text-sm font-semibold text-[var(--text-secondary)]">{{ emergency.chiefComplaint }}</p>
                </div>
                <div class="text-xs text-[var(--text-muted)]">
                  <div><strong>Arrivée :</strong> {{ arrivalLabel(emergency.arrivalMode) }}</div>
                  @if (emergency.stabilizedAt) {
                    <div><strong>Stabilisé le :</strong> {{ emergency.stabilizedAt | date:'dd/MM/yyyy HH:mm' }}</div>
                  }
                </div>
              </header>

              <div class="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <span class="ui-label block">Tension</span>
                  <strong class="text-sm text-[var(--text-primary)]">{{ emergency.initialBpSystolic || '—' }}/{{ emergency.initialBpDiastolic || '—' }} mmHg</strong>
                </div>
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <span class="ui-label block">Pouls</span>
                  <strong class="text-sm text-[var(--text-primary)]">{{ emergency.initialHr || '—' }} bpm</strong>
                </div>
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <span class="ui-label block">Température</span>
                  <strong class="text-sm text-[var(--text-primary)]">{{ emergency.initialTemp || '—' }} °C</strong>
                </div>
                <div class="rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2">
                  <span class="ui-label block">Hémodynamique</span>
                  <strong class="text-sm text-[var(--text-primary)]">{{ emergency.hemodynamicStatus }}</strong>
                </div>
              </div>

              @if (emergency.thirdPartyName) {
                <div class="mt-3 rounded-sm border-l-2 border-[var(--brand-primary)] bg-[var(--app-surface)] p-3">
                  <h4 class="text-xs font-black uppercase tracking-wider text-[var(--text-primary)]">Personne ayant amené ou renseigné le patient</h4>
                  <dl class="mt-2 grid grid-cols-1 gap-2 text-xs sm:grid-cols-2">
                    <div><dt class="ui-label">Nom</dt><dd class="font-bold text-[var(--text-primary)]">{{ emergency.thirdPartyName }}</dd></div>
                    <div><dt class="ui-label">Téléphone</dt><dd class="font-bold text-[var(--text-primary)]">{{ emergency.thirdPartyPhone || 'Non renseigné' }}</dd></div>
                    <div><dt class="ui-label">Lien / qualité</dt><dd class="font-bold text-[var(--text-primary)]">{{ relationshipLabel(emergency.thirdPartyRelationship) }}</dd></div>
                    <div><dt class="ui-label">Pièce / référence</dt><dd class="font-bold text-[var(--text-primary)]">{{ emergency.thirdPartyIdDocument || 'Non renseignée' }}</dd></div>
                  </dl>
                  @if (emergency.thirdPartyCircumstances) {
                    <p class="mt-2 text-xs text-[var(--text-secondary)]"><strong>Circonstances :</strong> {{ emergency.thirdPartyCircumstances }}</p>
                  }
                  <p class="mt-2 text-[10px] font-semibold text-[var(--text-muted)]">
                    Contact autorisé : {{ emergency.thirdPartyConsentToContact ? 'Oui' : 'Non' }}. Cette personne n'est pas automatiquement un représentant légal.
                  </p>
                </div>
              }

              @if (emergency.resuscitationLogs?.length) {
                <div class="mt-3 border-t border-[var(--divider-subtle)] pt-3">
                  <h4 class="ui-label mb-2">Chronologie des soins d'urgence</h4>
                  <div class="space-y-2">
                    @for (log of emergency.resuscitationLogs; track log.id) {
                      <div class="flex items-start justify-between gap-3 rounded-sm border border-[var(--app-border)] bg-[var(--app-surface)] p-2 text-xs">
                        <div>
                          <strong class="text-[var(--text-primary)]">{{ careLabel(log.actionType) }} — {{ log.description }}</strong>
                          @if (log.quantity) {
                            <span class="ml-1 font-bold text-[var(--brand-primary)]">({{ log.quantity }} {{ log.unit }})</span>
                          }
                        </div>
                        <time class="shrink-0 text-[var(--text-muted)]">{{ log.administeredAt | date:'dd/MM HH:mm' }}</time>
                      </div>
                    }
                  </div>
                </div>
              }
            </article>
          }
        </div>
      }
    </section>
  `,
})
export class PatientEmergencyContextComponent implements OnInit {
  @Input({ required: true }) patientId!: string;

  private readonly emergencyApi = inject(EmergencyApiService);
  readonly emergencies = signal<EmergencyRecord[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loading.set(true);
    this.emergencyApi.getPatientEmergencies(this.patientId).subscribe({
      next: records => {
        this.emergencies.set([...records].sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
        this.loading.set(false);
      },
      error: () => {
        this.error.set("Impossible de charger l'historique complet des urgences.");
        this.loading.set(false);
      },
    });
  }

  arrivalLabel(value?: string): string {
    const labels: Record<string, string> = {
      AMBULANCE: 'Ambulance',
      ACCOMPANIED: 'Amené par un tiers',
      WALK_IN: 'Arrivée autonome',
      POLICE: 'Police / forces de l’ordre',
      FIREFIGHTERS: 'Sapeurs-pompiers',
      TRANSFER: 'Transfert médical',
    };
    return value ? labels[value] || value : 'Non renseigné';
  }

  relationshipLabel(value?: string): string {
    const labels: Record<string, string> = {
      FAMILY: 'Famille',
      PARENT: 'Parent',
      SPOUSE: 'Conjoint(e)',
      FRIEND: 'Ami(e)',
      WITNESS: 'Témoin',
      TRANSPORTER: 'Transporteur',
      POLICE: 'Police',
      OTHER: 'Autre',
    };
    return value ? labels[value] || value : 'Non renseigné';
  }

  careLabel(value: string): string {
    const labels: Record<string, string> = {
      VASCULAR_ACCESS: 'Voie veineuse',
      FLUID_BOLUS: 'Remplissage',
      MEDICATION: 'Médication',
    };
    return labels[value] || value;
  }
}

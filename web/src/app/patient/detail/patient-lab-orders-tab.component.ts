import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PatientDetailComponent } from '../patient-detail.component';
import { PatientApiService } from '../patient-api.service';
import { LabOrder, LabResult } from '../patient.models';

@Component({
  selector: 'app-patient-lab-orders-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="space-y-6 animate-fade-in text-xs text-[var(--text-secondary)]">
      @if (isLoadingLab()) {
        <div class="py-12 text-center">
          <div class="inline-block w-6 h-6 rounded-full border-2 border-indigo-200 border-t-indigo-600 animate-spin"></div>
          <p class="mt-2 text-xs font-bold text-[var(--text-muted)]">Chargement des données de laboratoire...</p>
        </div>
      } @else {
        <!-- 1. Graphique d'évolution si des résultats existent -->
        @if (analyteNames().length > 0) {
          <div class="p-4 bg-[var(--app-surface)] border border-[var(--app-border)]/80 rounded-xl space-y-4 shadow-xs">
            <div class="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 pb-3 border-b border-[var(--app-border)]/40">
              <div>
                <h4 class="font-extrabold text-sm text-[var(--text-primary)]">Évolution des Marqueurs Biologiques</h4>
                <p class="text-[10px] text-[var(--text-muted)]">Sélectionnez un analyte pour afficher sa courbe de tendance.</p>
              </div>
              <select
                [ngModel]="selectedAnalyte()"
                (ngModelChange)="selectedAnalyte.set($event)"
                class="ui-select max-w-xs focus:border-brand-primary transition-colors text-xs py-1.5"
              >
                @for (name of analyteNames(); track name) {
                  <option [value]="name">{{ name }}</option>
                }
              </select>
            </div>

            <!-- Tracé SVG réactif -->
            @if (filteredResults().length > 0) {
              <div class="space-y-2">
                <div class="overflow-x-auto">
                  <svg viewBox="0 0 500 200" class="w-full min-w-[400px] h-48 rounded-[var(--radius-brand-lg)] border border-[var(--app-border)] p-2" style="background:var(--app-surface-muted)">
                    <!-- Grille -->
                    <line x1="30" y1="30" x2="470" y2="30" stroke="var(--chart-grid)" stroke-dasharray="3 3" stroke-width="0.5" />
                    <line x1="30" y1="100" x2="470" y2="100" stroke="var(--chart-grid)" stroke-dasharray="3 3" stroke-width="0.5" />
                    <line x1="30" y1="170" x2="470" y2="170" stroke="var(--chart-grid)" stroke-dasharray="3 3" stroke-width="0.5" />

                    <!-- Ligne de tendance -->
                    @if (chartPoints().length > 1) {
                      <polyline
                        fill="none"
                        stroke="var(--chart-curve)"
                        stroke-width="2.5"
                        [attr.points]="getPolylinePoints()"
                      />
                    }

                    <!-- Points -->
                    @for (p of chartPoints(); track p.x) {
                      <circle
                        [attr.cx]="p.x"
                        [attr.cy]="p.y"
                        r="5"
                        [attr.fill]="getInterpretationColor(p.interpretation)"
                        stroke="var(--app-surface)"
                        stroke-width="1.5"
                        class="cursor-pointer"
                      />
                      <!-- Valeur -->
                      <text
                        [attr.x]="p.x"
                        [attr.y]="p.y - 10"
                        text-anchor="middle"
                        class="text-[9px] font-extrabold" style="fill:var(--text-primary)"
                      >
                        {{ p.val }}
                      </text>
                      <!-- Date -->
                      <text
                        [attr.x]="p.x"
                        y="192"
                        text-anchor="middle"
                        class="text-[8px] font-bold" style="fill:var(--text-muted)"
                      >
                        {{ p.date }}
                      </text>
                    }
                  </svg>
                </div>

                <!-- Légende et interprétations -->
                <div class="flex flex-wrap items-center gap-x-4 gap-y-1.5 text-[10px]" style="color:var(--text-muted)">
                  <span class="font-bold">Interprétations :</span>
                  <span class="flex items-center gap-1 font-semibold" style="color:var(--text-secondary)">
                    <span class="w-2.5 h-2.5 rounded" style="background:var(--chart-normal)"></span> Normal
                  </span>
                  <span class="flex items-center gap-1 font-semibold" style="color:var(--text-secondary)">
                    <span class="w-2.5 h-2.5 rounded" style="background:var(--chart-low)"></span> Bas
                  </span>
                  <span class="flex items-center gap-1 font-semibold" style="color:var(--text-secondary)">
                    <span class="w-2.5 h-2.5 rounded" style="background:var(--chart-high)"></span> Élevé
                  </span>
                  <span class="flex items-center gap-1 font-semibold" style="color:var(--text-secondary)">
                    <span class="w-2.5 h-2.5 rounded" style="background:var(--chart-critical)"></span> Critique
                  </span>
                </div>
              </div>
            }
          </div>
        }

        <!-- 2. Liste des résultats structurés détaillés -->
        @if (labResults().length > 0) {
          <div class="space-y-3">
            <h4 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)] mb-2">Historique des Analyses</h4>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              @for (res of labResults(); track res.id) {
                <div class="p-4 rounded-xl bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/20 border border-[var(--app-border)]/60 space-y-3 shadow-xs">
                  <div class="flex justify-between items-start">
                    <div>
                      <span class="inline-flex items-center px-1.5 py-0.5 rounded text-[9px] font-bold bg-[var(--brand-info-subtle)] text-[var(--brand-info-text)]  dark:text-brand-cyan uppercase tracking-wider">
                        {{ res.resultNumber }}
                      </span>
                      <h5 class="text-sm font-bold text-[var(--text-primary)] mt-1">{{ res.analyteName }}</h5>
                    </div>
                    
                    <!-- Badge interpretation -->
                    <span
                      class="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-extrabold uppercase tracking-wider"
                      [style.background-color]="getInterpretationColor(res.interpretation) + '15'"
                      [style.color]="getInterpretationColor(res.interpretation)"
                    >
                      {{ res.interpretation }}
                    </span>
                  </div>

                  <div class="grid grid-cols-2 gap-2 text-xs">
                    <div class="p-2 rounded bg-[var(--app-surface)] border border-[var(--app-border)]/40">
                      <span class="block text-[9px] font-bold text-[var(--text-muted)] uppercase">Valeur mesurée</span>
                      <span class="font-black text-sm text-[var(--text-primary)]">{{ res.value }} {{ res.unit || '' }}</span>
                    </div>
                    <div class="p-2 rounded bg-[var(--app-surface)] border border-[var(--app-border)]/40">
                      <span class="block text-[9px] font-bold text-[var(--text-muted)] uppercase">Valeurs de référence</span>
                      <span class="font-bold text-[var(--text-secondary)]">{{ res.referenceRange || 'N/A' }}</span>
                    </div>
                  </div>

                  @if (res.comment) {
                    <p class="text-xs italic text-[var(--text-muted)] leading-relaxed">
                      <strong>Note :</strong> {{ res.comment }}
                    </p>
                  }

                  <div class="flex justify-between items-center text-[10px] text-[var(--text-muted)] pt-2 border-t border-[var(--app-border)]/40">
                    <span>Validé par : <strong>{{ res.validatorName }}</strong></span>
                    <span>Le : <strong>{{ (res.validatedAt || res.createdAt) | slice:0:10 }}</strong></span>
                  </div>
                </div>
              }
            </div>
          </div>
        }

        <!-- 3. Demandes d'analyses (LabOrders) -->
        <div class="space-y-3">
          <h4 class="text-xs font-black uppercase tracking-wider text-[var(--text-muted)] mb-2">Demandes d'Examens Biologiques</h4>

          @if (labOrders().length === 0) {
            <div class="p-8 text-center border border-dashed border-[var(--app-border)]/80 rounded-xl">
              <p class="text-sm font-semibold text-[var(--text-muted)]">Aucune demande d'examen enregistrée.</p>
            </div>
          } @else {
            <div class="space-y-2">
              @for (order of labOrders(); track order.id) {
                <div class="rounded-xl bg-[var(--app-surface-muted)] dark:bg-[var(--bg-input)]/20 border border-[var(--app-border)]/60 overflow-hidden">
                  <!-- En-tête cliquable -->
                  <div
                     (click)="toggleLabOrder(order.id)"
                     class="p-4 flex items-center justify-between cursor-pointer hover:bg-slate-100/30 dark:hover:bg-slate-800/40 select-none transition-colors"
                  >
                    <div class="flex flex-wrap items-center gap-3">
                      <span class="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-extrabold bg-[var(--brand-info-subtle)] text-[var(--brand-info-text)]  dark:text-brand-cyan uppercase tracking-wider">
                        {{ order.examRequestNumber }}
                      </span>
                      <span class="text-xs font-semibold text-[var(--text-muted)]">
                        {{ order.createdAt | slice:0:10 }}
                      </span>
                      <span
                        class="inline-flex items-center px-2 py-0.5 rounded text-[9px] font-extrabold uppercase tracking-wider"
                        [class]="order.status === 'VALIDATED' 
                          ? 'bg-[var(--brand-success-subtle)] text-[var(--brand-success-text)] dark:bg-green-950/20 dark:text-green-300'
                          : 'bg-[var(--brand-warning-subtle)] text-[var(--brand-warning-text)] dark:bg-amber-950/20 dark:text-amber-300'"
                      >
                        {{ order.status }}
                      </span>
                    </div>

                    <div class="flex items-center gap-3">
                      <span class="text-xs font-bold text-[var(--text-muted)]">Prescrit par : Dr. {{ order.requesterPractitionerName }}</span>
                      <!-- Chevron -->
                      <svg
                        class="w-4 h-4 text-[var(--text-muted)] transform transition-transform duration-200"
                        [class.rotate-180]="expandedLabOrders()[order.id]"
                        fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5"
                      >
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19 9l-7 7-7-7" />
                      </svg>
                    </div>
                  </div>

                  <!-- Corps -->
                  @if (expandedLabOrders()[order.id]) {
                    <div class="px-4 pb-4 pt-2 border-t border-[var(--app-border)]/40 space-y-3 animate-fade-in text-xs text-[var(--text-secondary)]">
                      <div>
                        <span class="block text-[9px] font-bold text-[var(--text-muted)] uppercase">Analyses demandées</span>
                        <div class="flex flex-wrap gap-1.5 mt-1">
                          @for (exam of order.exams; track exam) {
                            <span class="px-2 py-1 rounded bg-[var(--app-surface)] border border-[var(--app-border)]/40 font-semibold text-[var(--text-secondary)]">{{ exam }}</span>
                          }
                        </div>
                      </div>
                      
                      @if (order.reason) {
                        <div>
                          <span class="block text-[9px] font-bold text-[var(--text-muted)] uppercase">Indication clinique</span>
                          <p class="text-xs mt-0.5 font-semibold text-[var(--text-primary)]">{{ order.reason }}</p>
                        </div>
                      }
                      
                      <div>
                        <span class="block text-[9px] font-bold text-[var(--text-muted)] uppercase">Priorité</span>
                        <span class="inline-flex items-center gap-1.5 text-xs mt-0.5 font-bold" [class]="order.priority === 'URGENTE' ? 'text-[var(--brand-danger)]' : 'text-[var(--text-secondary)]'">
                          ⚠️ {{ order.priority }}
                        </span>
                      </div>
                    </div>
                  }
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `
})
export class PatientLabOrdersTabComponent implements OnInit {
  readonly parent = inject(PatientDetailComponent);
  private readonly patientApi = inject(PatientApiService);

  readonly labOrders = signal<LabOrder[]>([]);
  readonly labResults = signal<LabResult[]>([]);
  readonly isLoadingLab = signal<boolean>(false);
  readonly selectedAnalyte = signal<string>('');
  readonly expandedLabOrders = signal<Record<string, boolean>>({});

  readonly analyteNames = computed(() => {
    const results = this.labResults();
    return Array.from(new Set(results.map(r => r.analyteName)));
  });

  readonly filteredResults = computed(() => {
    const results = this.labResults();
    const analyte = this.selectedAnalyte();
    if (!analyte) return [];
    return results
      .filter(r => r.analyteName === analyte)
      .sort((a, b) => {
        const dateA = new Date(a.validatedAt || a.createdAt).getTime();
        const dateB = new Date(b.validatedAt || b.createdAt).getTime();
        return dateA - dateB;
      });
  });

  readonly chartPoints = computed(() => {
    const results = this.filteredResults();
    if (results.length === 0) return [];

    const values = results.map(r => {
      const v = parseFloat(r.value);
      return isNaN(v) ? 0 : v;
    });
    const minVal = Math.min(...values) * 0.9;
    const maxVal = Math.max(...values) * 1.1;
    const valRange = maxVal - minVal === 0 ? 1 : maxVal - minVal;

    const width = 500;
    const height = 200;
    const padding = 30;

    return results.map((r, i) => {
      const v = parseFloat(r.value);
      const val = isNaN(v) ? 0 : v;
      const x = padding + (i * (width - 2 * padding)) / (results.length === 1 ? 1 : results.length - 1);
      const y = height - padding - ((val - minVal) / valRange) * (height - 2 * padding);

      return {
        x,
        y,
        val,
        date: r.validatedAt ? r.validatedAt.slice(0, 10) : r.createdAt.slice(0, 10),
        interpretation: r.interpretation
      };
    });
  });

  ngOnInit(): void {
    this.loadLab();
  }

  loadLab(): void {
    const patient = this.parent.patient();
    if (!patient) return;

    this.isLoadingLab.set(true);
    this.patientApi.getPatientLabOrders(patient.id).subscribe({
      next: (orders) => {
        this.labOrders.set(orders);
      }
    });

    this.patientApi.getPatientLabResults(patient.id).subscribe({
      next: (results) => {
        this.labResults.set(results);
        this.isLoadingLab.set(false);

        const analytes = Array.from(new Set(results.map(r => r.analyteName)));
        if (analytes.length > 0) {
          this.selectedAnalyte.set(analytes[0]);
        }
      },
      error: () => {
        this.isLoadingLab.set(false);
      }
    });
  }

  toggleLabOrder(id: string): void {
    this.expandedLabOrders.update(prev => ({ ...prev, [id]: !prev[id] }));
  }

  getPolylinePoints(): string {
    return this.chartPoints().map(p => `${p.x},${p.y}`).join(' ');
  }

  getInterpretationColor(interpretation: string): string {
    // Lire les variables CSS dynamiques selon le thème actif (light/dark)
    const root = document.documentElement;
    const style = getComputedStyle(root);
    switch (interpretation) {
      case 'ELEVE':
        return style.getPropertyValue('--chart-high').trim() || '#d97706';
      case 'CRITIQUE':
        return style.getPropertyValue('--chart-critical').trim() || '#dc2626';
      case 'BAS':
        return style.getPropertyValue('--chart-low').trim() || '#0b91b2';
      default:
        return style.getPropertyValue('--chart-normal').trim() || '#009730';
    }
  }
}

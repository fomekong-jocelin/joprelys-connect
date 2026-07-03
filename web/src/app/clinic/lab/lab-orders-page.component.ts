import { DatePipe } from '@angular/common';
import { Component, inject, signal, computed } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../core/i18n/i18n.service';
import { AppShellComponent } from '../../shared/layout/app-shell.component';
import { PageHeaderComponent } from '../../shared/ui/page-header.component';
import { ButtonComponent } from '../../shared/ui/button.component';
import { LabOrderApiService } from './lab-api.service';
import { LabOrder, LabResult } from './lab.models';

@Component({
  selector: 'app-lab-orders-page',
  standalone: true,
  imports: [ReactiveFormsModule, DatePipe, AppShellComponent, PageHeaderComponent, ButtonComponent],
  template: `
    <app-shell>
      <app-page-header
        [title]="t('lab.title')"
        [subtitle]="t('lab.portal')"
        backLink="/dashboard"
        [backLabel]="t('common.back')"
      />

      <div class="app-container-wide pb-10">

          <div class="grid gap-5 xl:grid-cols-[420px_1fr]">
            <section class="ui-card-subtle p-5 md:p-6">
              <div class="mb-4 flex items-center justify-between gap-3">
                <div>
                  <p class="ui-label">{{ t('lab.requests') }}</p>
                  <h2 class="font-display text-lg font-extrabold" style="color: var(--text-primary)">
                    {{ filteredOrders().length }} {{ t('lab.requestsCount') }}
                  </h2>
                </div>
                <app-ui-button variant="secondary" (pressed)="loadOrders()">{{ t('lab.refresh') }}</app-ui-button>
              </div>

              <!-- Filtres de recherche -->
              <div class="mb-5 space-y-3 border-b pb-4" style="border-color: var(--app-border);">
                <!-- Recherche texte -->
                <div>
                  <input
                    type="text"
                    [placeholder]="t('lab.searchPlaceholder')"
                    class="w-full border px-3 py-2 text-sm bg-transparent"
                    style="border-radius: var(--radius-brand-sm); border-color: var(--app-border); color: var(--text-primary);"
                    [value]="searchQuery()"
                    (input)="updateSearchQuery($event)"
                  />
                </div>
                <!-- Sélecteurs Priorité & Statut -->
                <div class="grid grid-cols-2 gap-2">
                  <select
                    class="border px-2 py-1.5 text-xs bg-transparent"
                    style="border-radius: var(--radius-brand-sm); border-color: var(--app-border); color: var(--text-primary);"
                    [value]="filterPriority()"
                    (change)="updateFilterPriority($event)"
                  >
                    <option value="" style="background: var(--app-surface);">{{ t('lab.filterPriorityAll') }}</option>
                    <option value="NORMALE" style="background: var(--app-surface);">NORMALE</option>
                    <option value="URGENTE" style="background: var(--app-surface);">URGENTE</option>
                  </select>

                  <select
                    class="border px-2 py-1.5 text-xs bg-transparent"
                    style="border-radius: var(--radius-brand-sm); border-color: var(--app-border); color: var(--text-primary);"
                    [value]="filterStatus()"
                    (change)="updateFilterStatus($event)"
                  >
                    <option value="" style="background: var(--app-surface);">{{ t('lab.filterStatusAll') }}</option>
                    @for (st of allowedStatuses; track st) {
                      <option [value]="st" style="background: var(--app-surface);">{{ statusLabel(st) }}</option>
                    }
                  </select>
                </div>
              </div>

              @if (isLoading()) {
                <p class="py-8 text-center text-sm font-bold" style="color: var(--text-muted)">{{ t('common.loading') }}</p>
              } @else if (loadError()) {
                <p class="border p-3 text-sm font-semibold"
                   style="border-radius: var(--radius-brand-md); border-color: color-mix(in srgb, var(--brand-danger) 34%, transparent); color: var(--brand-danger);">
                  {{ loadError() }}
                </p>
              } @else if (filteredOrders().length === 0) {
                <p class="py-8 text-center text-sm" style="color: var(--text-secondary)">{{ t('lab.empty') }}</p>
              } @else {
                <div class="space-y-3">
                  @for (order of filteredOrders(); track order.id) {
                    <button
                      type="button"
                      class="w-full border p-4 text-left transition"
                      [style.border-color]="selectedOrder()?.id === order.id ? 'var(--brand-primary)' : 'var(--app-border)'"
                      [style.background]="selectedOrder()?.id === order.id ? 'color-mix(in srgb, var(--brand-primary) 7%, var(--app-surface))' : 'var(--app-surface)'"
                      style="border-radius: var(--radius-brand-md);"
                      (click)="selectOrder(order)"
                    >
                      <div class="flex items-start justify-between gap-3">
                        <div>
                          <p class="font-mono text-sm font-black" style="color: var(--text-primary)">{{ order.examRequestNumber }}</p>
                          <p class="mt-1 text-xs font-bold" style="color: var(--text-secondary)">{{ order.patientName }}</p>
                        </div>
                        <span class="border px-2 py-1 text-[11px] font-black uppercase"
                              [style.color]="statusColor(order.status)"
                              style="border-radius: var(--radius-brand-sm); border-color: var(--app-border);">
                          {{ statusLabel(order.status) }}
                        </span>
                      </div>
                      <p class="mt-2 text-xs" style="color: var(--text-muted)">
                        {{ order.examType }} · {{ order.priority }} · {{ order.createdAt | date:'short' }}
                      </p>
                    </button>
                  }
                </div>
              }
            </section>

            <section class="ui-card-subtle min-h-[520px] p-5 md:p-6">
              @if (selectedOrder(); as order) {
                <div class="space-y-5">
                  <div class="border-b pb-5" style="border-color: var(--app-border)">
                    <p class="ui-label">{{ t('lab.detail') }}</p>
                    <h2 class="mt-1 font-mono text-xl font-black" style="color: var(--text-primary)">{{ order.examRequestNumber }}</h2>
                    <p class="mt-2 text-sm" style="color: var(--text-secondary)">
                      {{ order.patientName }} · {{ t('lab.requestedBy') }} {{ order.requesterPractitionerName }}
                    </p>
                  </div>

                  <div class="grid gap-3 md:grid-cols-3">
                    <div class="ui-card-muted p-4">
                      <span class="ui-label">{{ t('lab.examType') }}</span>
                      <p class="mt-1 font-bold" style="color: var(--text-primary)">{{ order.examType }}</p>
                    </div>
                    <div class="ui-card-muted p-4">
                      <span class="ui-label">{{ t('lab.priority') }}</span>
                      <p class="mt-1 font-bold" style="color: var(--text-primary)">{{ order.priority }}</p>
                    </div>
                    <div class="ui-card-muted p-4">
                      <span class="ui-label">{{ t('lab.createdAt') }}</span>
                      <p class="mt-1 font-bold" style="color: var(--text-primary)">{{ order.createdAt | date:'mediumDate' }}</p>
                    </div>
                  </div>

                  <div class="ui-card-muted p-4">
                    <p class="ui-label">{{ t('lab.statusChange') }}</p>
                    <div class="mt-3 flex flex-col gap-3 md:flex-row md:items-end">
                      <label class="block md:w-72">
                        <span class="ui-label">{{ t('patients.status') }}</span>
                        <select class="ui-input lab-input mt-1" [value]="statusDraft()" (change)="statusDraft.set($any($event.target).value)">
                          @for (status of allowedStatuses; track status) {
                            <option [value]="status">{{ statusLabel(status) }}</option>
                          }
                        </select>
                      </label>
                      <app-ui-button type="button" variant="secondary" [disabled]="isUpdatingStatus()" (pressed)="updateStatus(order)">
                        {{ isUpdatingStatus() ? t('common.saving') : t('lab.updateStatus') }}
                      </app-ui-button>
                    </div>
                    @if (statusError()) {
                      <p class="mt-3 text-sm font-semibold" style="color: var(--brand-danger)">{{ statusError() }}</p>
                    }
                    @if (statusSuccess()) {
                      <p class="mt-3 text-sm font-semibold" style="color: var(--brand-success)">{{ statusSuccess() }}</p>
                    }
                  </div>

                  <div class="ui-card-muted p-4">
                    <p class="ui-label">{{ t('lab.exams') }}</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      @for (exam of order.exams; track exam) {
                        <span class="border px-2.5 py-1 text-xs font-bold"
                              style="border-radius: var(--radius-brand-sm); border-color: var(--app-border); color: var(--text-primary);">
                          {{ exam }}
                        </span>
                      }
                    </div>
                    @if (order.reason) {
                      <p class="mt-3 text-sm" style="color: var(--text-secondary)">{{ order.reason }}</p>
                    }
                  </div>

                  <form class="ui-card-muted space-y-4 p-4" [formGroup]="resultForm" (ngSubmit)="submitResults(order)">
                    <div>
                      <p class="ui-label">{{ t('lab.resultEntry') }}</p>
                      <h3 class="font-display text-base font-extrabold" style="color: var(--text-primary)">
                        {{ t('lab.resultEntryTitle') }}
                      </h3>
                    </div>

                    <div class="grid gap-3 md:grid-cols-2">
                      <label class="block">
                        <span class="ui-label">{{ t('lab.apiKey') }}</span>
                        <input class="ui-input lab-input mt-1" type="password" formControlName="apiKey" />
                      </label>
                      <label class="block">
                        <span class="ui-label">{{ t('lab.validatorName') }}</span>
                        <input class="ui-input lab-input mt-1" formControlName="validatorName" />
                      </label>
                      <label class="block">
                        <span class="ui-label">{{ t('lab.sampleCollectedAt') }}</span>
                        <input class="ui-input lab-input mt-1" type="datetime-local" formControlName="sampleCollectedAt" />
                      </label>
                      <label class="block">
                        <span class="ui-label">{{ t('lab.validatedAt') }}</span>
                        <input class="ui-input lab-input mt-1" type="datetime-local" formControlName="validatedAt" />
                      </label>
                    </div>

                    <div class="space-y-3" formArrayName="results">
                      @for (group of results.controls; track $index) {
                        <div class="grid gap-3 border p-3 md:grid-cols-6" [formGroupName]="$index" style="border-radius: var(--radius-brand-md); border-color: var(--app-border);">
                          <input class="ui-input lab-input md:col-span-2" formControlName="analyteName" [placeholder]="t('lab.analyteName')" />
                          <input class="ui-input lab-input" formControlName="value" [placeholder]="t('lab.value')" />
                          <input class="ui-input lab-input" formControlName="unit" [placeholder]="t('lab.unit')" />
                          <input class="ui-input lab-input" formControlName="referenceRange" [placeholder]="t('lab.referenceRange')" />
                          <select class="ui-input lab-input" formControlName="interpretation">
                            <option value="NORMAL">{{ t('lab.interpretation.NORMAL') }}</option>
                            <option value="LOW">{{ t('lab.interpretation.LOW') }}</option>
                            <option value="HIGH">{{ t('lab.interpretation.HIGH') }}</option>
                            <option value="CRITICAL">{{ t('lab.interpretation.CRITICAL') }}</option>
                          </select>
                        </div>
                      }
                    </div>

                    <div class="flex flex-wrap gap-3">
                      <button type="button" class="ui-button ui-button-secondary" (click)="addResultLine()">{{ t('lab.addLine') }}</button>
                      @if (results.length > 1) {
                        <button type="button" class="ui-button ui-button-secondary" (click)="removeResultLine()">{{ t('lab.removeLine') }}</button>
                      }
                    </div>

                    <label class="block">
                      <span class="ui-label">{{ t('lab.pdf') }}</span>
                      <input class="ui-input lab-input mt-1" type="file" accept="application/pdf" (change)="onPdfSelected($event)" />
                    </label>

                    @if (submitError()) {
                      <p class="text-sm font-semibold" style="color: var(--brand-danger)">{{ submitError() }}</p>
                    }
                    @if (submitSuccess()) {
                      <p class="text-sm font-semibold" style="color: var(--brand-success)">{{ submitSuccess() }}</p>
                    }

                    <div class="flex justify-end">
                      <app-ui-button type="submit" [disabled]="resultForm.invalid || isSubmitting()">
                        {{ isSubmitting() ? t('common.saving') : t('lab.validateButton') }}
                      </app-ui-button>
                    </div>
                  </form>

                  <div class="ui-card-muted p-4">
                    <div class="mb-3">
                      <p class="ui-label">{{ t('lab.history') }}</p>
                      <h3 class="font-display text-base font-extrabold" style="color: var(--text-primary)">
                        {{ filteredResults(order).length }} {{ t('lab.historyCount') }}
                      </h3>
                    </div>
                    @if (isLoadingResults()) {
                      <p class="text-sm font-bold" style="color: var(--text-muted)">{{ t('common.loading') }}</p>
                    } @else if (resultsError()) {
                      <p class="text-sm font-semibold" style="color: var(--brand-danger)">{{ resultsError() }}</p>
                    } @else if (filteredResults(order).length === 0) {
                      <p class="text-sm" style="color: var(--text-secondary)">{{ t('lab.historyEmpty') }}</p>
                    } @else {
                      <div class="overflow-x-auto">
                        <table class="ui-table min-w-[720px]">
                          <thead>
                            <tr>
                              <th>{{ t('lab.analyteName') }}</th>
                              <th>{{ t('lab.value') }}</th>
                              <th>{{ t('lab.referenceRange') }}</th>
                              <th>{{ t('lab.validatorName') }}</th>
                              <th>{{ t('lab.validatedAt') }}</th>
                            </tr>
                          </thead>
                          <tbody>
                            @for (result of filteredResults(order); track result.id) {
                              <tr>
                                <td>{{ result.analyteName }}</td>
                                <td><strong style="color: var(--text-primary)">{{ result.value }}</strong> {{ result.unit || '' }}</td>
                                <td>{{ result.referenceRange || '-' }}</td>
                                <td>{{ result.validatorName }}</td>
                                <td>{{ (result.validatedAt || result.createdAt) | date:'short' }}</td>
                              </tr>
                            }
                          </tbody>
                        </table>
                      </div>
                    }
                  </div>
                </div>
              } @else {
                <div class="flex min-h-[440px] items-center justify-center text-center">
                  <div>
                    <h2 class="ui-title text-xl">{{ t('lab.noSelectionTitle') }}</h2>
                    <p class="mt-2 max-w-md text-sm" style="color: var(--text-secondary)">{{ t('lab.noSelectionText') }}</p>
                  </div>
                </div>
              }
            </section>
          </div>
      </div>
    </app-shell>
  `,
  styles: [`.lab-input { border-radius: var(--radius-brand-sm); }`],
})
export class LabOrdersPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly labApi = inject(LabOrderApiService);
  readonly i18n = inject(I18nService);

  readonly searchQuery = signal('');
  readonly filterPriority = signal('');
  readonly filterStatus = signal('');

  readonly filteredOrders = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const priority = this.filterPriority();
    const status = this.filterStatus();

    return this.orders().filter((order) => {
      const matchesQuery = !query ||
        order.examRequestNumber.toLowerCase().includes(query) ||
        order.patientName.toLowerCase().includes(query) ||
        (order.patientId && order.patientId.toLowerCase().includes(query));

      const matchesPriority = !priority || order.priority === priority;
      const matchesStatus = !status || order.status === status;

      return matchesQuery && matchesPriority && matchesStatus;
    });
  });

  updateSearchQuery(event: Event): void {
    const value = event.target instanceof HTMLInputElement ? event.target.value : '';
    this.searchQuery.set(value);
  }

  updateFilterPriority(event: Event): void {
    const value = event.target instanceof HTMLSelectElement ? event.target.value : '';
    this.filterPriority.set(value);
  }

  updateFilterStatus(event: Event): void {
    const value = event.target instanceof HTMLSelectElement ? event.target.value : '';
    this.filterStatus.set(value);
  }

  readonly orders = signal<LabOrder[]>([]);
  readonly selectedOrder = signal<LabOrder | null>(null);
  readonly isLoading = signal(false);
  readonly isLoadingResults = signal(false);
  readonly isSubmitting = signal(false);
  readonly isUpdatingStatus = signal(false);
  readonly loadError = signal('');
  readonly resultsError = signal('');
  readonly submitError = signal('');
  readonly submitSuccess = signal('');
  readonly statusError = signal('');
  readonly statusSuccess = signal('');
  readonly pdfBase64 = signal('');
  readonly patientResults = signal<LabResult[]>([]);
  readonly statusDraft = signal('REQUESTED');
  readonly allowedStatuses = ['REQUESTED', 'SAMPLE_COLLECTED', 'IN_PROGRESS', 'RESULT_AVAILABLE', 'VALIDATED', 'CANCELLED'];

  readonly resultForm = this.fb.nonNullable.group({
    apiKey: ['', Validators.required],
    validatorName: ['', Validators.required],
    sampleCollectedAt: [''],
    validatedAt: [''],
    conclusion: [''],
    results: this.fb.array([this.createResultGroup()]),
  });

  constructor() {
    this.loadOrders();
  }

  get results(): FormArray {
    return this.resultForm.controls.results;
  }

  loadOrders(): void {
    this.isLoading.set(true);
    this.loadError.set('');
    this.labApi.getLabOrders().subscribe({
      next: (orders) => {
        this.orders.set(orders);
        const selected = this.selectedOrder() ?? orders[0] ?? null;
        this.selectedOrder.set(selected);
        if (selected) {
          this.statusDraft.set(selected.status);
          this.loadResults(selected.patientId);
        }
        this.isLoading.set(false);
      },
      error: (error) => {
        this.loadError.set(error.error?.detail || error.error?.title || this.t('lab.loadError'));
        this.isLoading.set(false);
      },
    });
  }

  selectOrder(order: LabOrder): void {
    this.selectedOrder.set(order);
    this.statusDraft.set(order.status);
    this.submitError.set('');
    this.submitSuccess.set('');
    this.statusError.set('');
    this.statusSuccess.set('');
    this.loadResults(order.patientId);
  }

  addResultLine(): void {
    this.results.push(this.createResultGroup());
  }

  removeResultLine(): void {
    if (this.results.length > 1) this.results.removeAt(this.results.length - 1);
  }

  submitResults(order: LabOrder): void {
    if (this.resultForm.invalid || this.isSubmitting()) {
      this.resultForm.markAllAsTouched();
      return;
    }

    const raw = this.resultForm.getRawValue();
    this.isSubmitting.set(true);
    this.submitError.set('');
    this.submitSuccess.set('');

    this.labApi.uploadResults({
      examRequestNumber: order.examRequestNumber,
      validatorName: raw.validatorName.trim(),
      sampleCollectedAt: this.toIso(raw.sampleCollectedAt),
      resultAt: this.toIso(raw.validatedAt),
      validatedAt: this.toIso(raw.validatedAt),
      conclusion: raw.conclusion.trim() || undefined,
      results: raw.results.map((item) => ({
        analyteName: item.analyteName.trim(),
        value: item.value.trim(),
        unit: item.unit.trim() || undefined,
        referenceRange: item.referenceRange.trim() || undefined,
        interpretation: item.interpretation,
        comment: item.comment?.trim() || undefined,
      })),
      pdfBase64: this.pdfBase64() || undefined,
    }, raw.apiKey.trim()).subscribe({
      next: () => {
        this.submitSuccess.set(this.t('lab.submitSuccess'));
        this.isSubmitting.set(false);
        this.loadOrders();
        this.loadResults(order.patientId);
      },
      error: (error) => {
        this.submitError.set(error.error?.detail || error.error?.title || this.t('lab.submitError'));
        this.isSubmitting.set(false);
      },
    });
  }

  updateStatus(order: LabOrder): void {
    this.isUpdatingStatus.set(true);
    this.statusError.set('');
    this.statusSuccess.set('');
    this.labApi.updateStatus(order.id, this.statusDraft()).subscribe({
      next: (updated) => {
        this.replaceOrder(updated);
        this.selectedOrder.set(updated);
        this.statusSuccess.set(this.t('lab.statusSuccess'));
        this.isUpdatingStatus.set(false);
      },
      error: (error) => {
        this.statusError.set(error.error?.detail || error.error?.title || this.t('lab.statusError'));
        this.isUpdatingStatus.set(false);
      },
    });
  }

  filteredResults(order: LabOrder): LabResult[] {
    return this.patientResults().filter((result) => result.examRequestNumber === order.examRequestNumber);
  }

  onPdfSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) {
      this.pdfBase64.set('');
      return;
    }
    const reader = new FileReader();
    reader.onload = () => this.pdfBase64.set(String(reader.result).split(',')[1] ?? '');
    reader.readAsDataURL(file);
  }

  statusLabel(status: string): string {
    const key = `lab.status.${status}`;
    return this.t(key) === key ? status : this.t(key);
  }

  statusColor(status: string): string {
    if (status === 'VALIDATED' || status === 'RESULT_AVAILABLE') return 'var(--brand-success)';
    if (status === 'CANCELLED') return 'var(--brand-danger)';
    return 'var(--brand-primary)';
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private createResultGroup() {
    return this.fb.nonNullable.group({
      analyteName: ['', Validators.required],
      value: ['', Validators.required],
      unit: [''],
      referenceRange: [''],
      interpretation: ['NORMAL'],
      comment: [''],
    });
  }

  private loadResults(patientId: string): void {
    this.isLoadingResults.set(true);
    this.resultsError.set('');
    this.labApi.getPatientResults(patientId).subscribe({
      next: (results) => {
        this.patientResults.set(results);
        this.isLoadingResults.set(false);
      },
      error: (error) => {
        this.resultsError.set(error.error?.detail || error.error?.title || this.t('lab.resultsError'));
        this.isLoadingResults.set(false);
      },
    });
  }

  private replaceOrder(order: LabOrder): void {
    this.orders.update((orders) => orders.map((current) => current.id === order.id ? order : current));
  }

  private toIso(value?: string): string | undefined {
    return value ? new Date(value).toISOString() : undefined;
  }
}

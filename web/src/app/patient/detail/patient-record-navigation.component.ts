import { Component, OnDestroy, OnInit, computed, inject, input, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Subscription, filter } from 'rxjs';
import { I18nService } from '../../core/i18n/i18n.service';

interface PatientRecordSection {
  segment: 'profile' | 'consultations' | 'lab-orders' | 'hospitalizations' | 'audit-trail';
  labelKey: string;
}

@Component({
  selector: 'app-patient-record-navigation',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  template: `
    <!-- Mobile: one explicit current step, then a compact section picker. -->
    <div class="md:hidden">
      <button
        type="button"
        class="ui-card flex min-h-12 w-full items-center justify-between gap-3 px-4 py-3 text-left"
        [attr.aria-expanded]="mobileOpen()"
        [attr.aria-label]="currentLabel()"
        (click)="mobileOpen.update(value => !value)"
      >
        <span class="min-w-0 truncate text-sm font-extrabold text-[var(--text-primary)]">{{ currentLabel() }}</span>
        <svg
          class="h-4 w-4 shrink-0 text-[var(--text-muted)] transition-transform"
          [class.rotate-180]="mobileOpen()"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          aria-hidden="true"
        >
          <path stroke-linecap="round" stroke-linejoin="round" d="m6 9 6 6 6-6" />
        </svg>
      </button>

      @if (mobileOpen()) {
        <nav class="ui-card mt-2 grid gap-1 p-2 animate-fade-in" [attr.aria-label]="currentLabel()">
          @for (section of sections(); track section.segment) {
            <a
              [routerLink]="['/patients', patientId(), section.segment]"
              routerLinkActive="border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] text-[var(--brand-primary)] font-extrabold"
              [routerLinkActiveOptions]="{ exact: true }"
              class="flex min-h-12 items-center justify-between rounded-md border border-transparent px-3 py-2.5 text-sm font-semibold text-[var(--text-secondary)] no-underline transition-colors hover:bg-[var(--app-surface-muted)]"
              (click)="closeMobile()"
            >
              <span>{{ i18n.t(section.labelKey) }}</span>
              <svg class="h-4 w-4 text-[var(--text-muted)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
                <path stroke-linecap="round" stroke-linejoin="round" d="m9 18 6-6-6-6" />
              </svg>
            </a>
          }
        </nav>
      }
    </div>

    <!-- Desktop/tablet landscape: persistent vertical navigation, no horizontal tab rail. -->
    <nav class="ui-card sticky top-4 hidden gap-1 p-2 md:grid" [attr.aria-label]="currentLabel()">
      @for (section of sections(); track section.segment) {
        <a
          [routerLink]="['/patients', patientId(), section.segment]"
          routerLinkActive="border-[var(--brand-primary-border)] bg-[var(--brand-primary-subtle)] text-[var(--brand-primary)] font-extrabold"
          [routerLinkActiveOptions]="{ exact: true }"
          class="flex min-h-11 items-center justify-between rounded-md border border-transparent px-3 py-2.5 text-sm font-semibold text-[var(--text-secondary)] no-underline transition-colors hover:bg-[var(--app-surface-muted)]"
        >
          <span>{{ i18n.t(section.labelKey) }}</span>
          <svg class="h-4 w-4 text-[var(--text-muted)]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
            <path stroke-linecap="round" stroke-linejoin="round" d="m9 18 6-6-6-6" />
          </svg>
        </a>
      }
    </nav>
  `,
})
export class PatientRecordNavigationComponent implements OnInit, OnDestroy {
  readonly patientId = input.required<string>();
  readonly canViewConsultations = input(false);
  readonly canViewLabOrders = input(false);
  readonly canViewHospitalizations = input(false);
  readonly canViewAudit = input(false);

  readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private routerSubscription?: Subscription;

  readonly mobileOpen = signal(false);
  readonly currentSegment = signal<PatientRecordSection['segment']>('profile');

  readonly sections = computed<PatientRecordSection[]>(() => {
    const items: PatientRecordSection[] = [
      { segment: 'profile', labelKey: 'menu.patientDetail.profile' },
    ];

    if (this.canViewConsultations()) {
      items.push({ segment: 'consultations', labelKey: 'menu.patientDetail.consultations' });
    }
    if (this.canViewLabOrders()) {
      items.push({ segment: 'lab-orders', labelKey: 'menu.patientDetail.labOrders' });
    }
    if (this.canViewHospitalizations()) {
      items.push({ segment: 'hospitalizations', labelKey: 'menu.patientDetail.hospitalization' });
    }
    if (this.canViewAudit()) {
      items.push({ segment: 'audit-trail', labelKey: 'menu.patientDetail.audit' });
    }

    return items;
  });

  readonly currentLabel = computed(() => {
    const active = this.sections().find(section => section.segment === this.currentSegment()) ?? this.sections()[0];
    return active ? this.i18n.t(active.labelKey) : this.i18n.t('menu.patientDetail.profile');
  });

  ngOnInit(): void {
    this.syncFromUrl(this.router.url);
    this.routerSubscription = this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    ).subscribe(event => {
      this.syncFromUrl(event.urlAfterRedirects);
      this.closeMobile();
    });
  }

  ngOnDestroy(): void {
    this.routerSubscription?.unsubscribe();
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  private syncFromUrl(url: string): void {
    const available = this.sections();
    const active = available.find(section => url.includes(`/patients/${this.patientId()}/${section.segment}`));
    this.currentSegment.set(active?.segment ?? 'profile');
  }
}

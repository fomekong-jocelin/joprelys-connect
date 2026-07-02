import { Component, computed, inject, OnInit } from '@angular/core';
import { AuthTokenStorageService } from '../auth/auth-token-storage.service';
import { RouterLink } from '@angular/router';
import { I18nService } from '../core/i18n/i18n.service';
import { AppShellComponent } from '../shared/layout/app-shell.component';
import { VisitApiService } from '../visit/visit-api.service';
import { Visit } from '../visit/visit.models';
import { DatePipe } from '@angular/common';
import { EmptyStateComponent } from '../shared/ui/empty-state.component';
import { ButtonComponent } from '../shared/ui/button.component';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  imports: [AppShellComponent, RouterLink, DatePipe, EmptyStateComponent, ButtonComponent]
})
export class DashboardComponent implements OnInit {
  private readonly tokenStorage = inject(AuthTokenStorageService);
  private readonly i18n = inject(I18nService);
  private readonly visitApi = inject(VisitApiService);

  readonly session = this.tokenStorage.session;
  readonly welcomeLabel = computed(() => this.i18n.t('dashboard.welcome'));
  readonly authorizedLabel = computed(() => this.i18n.t('dashboard.authorized'));
  readonly roleLabel = computed(() => this.i18n.t('dashboard.role'));

  activeVisits: Visit[] = [];
  isLoadingQueue = false;
  queueError = '';

  readonly isClinicalRole = computed(() => {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'AGENT_ACCUEIL' || role === 'INFIRMIER' || role === 'ADMIN_CLINIQUE';
  });

  readonly canCloseVisit = computed(() => {
    const role = this.session()?.role;
    return role === 'MEDECIN' || role === 'ADMIN_CLINIQUE';
  });

  ngOnInit(): void {
    if (this.isClinicalRole()) {
      this.loadQueue();
    }
  }

  loadQueue(): void {
    this.isLoadingQueue = true;
    this.queueError = '';
    this.visitApi.getActiveVisits().subscribe({
      next: (data) => {
        this.activeVisits = data;
        this.isLoadingQueue = false;
      },
      error: (err) => {
        this.isLoadingQueue = false;
        this.queueError = 'Impossible de charger la file d\'attente active.';
      }
    });
  }

  closeVisit(visitId: string): void {
    if (!confirm('Voulez-vous vraiment clôturer cette visite ?')) {
      return;
    }

    this.visitApi.closeVisit(visitId).subscribe({
      next: () => {
        this.loadQueue();
      },
      error: (err) => {
        alert(err.error?.detail || 'Erreur lors de la clôtures de la visite.');
      }
    });
  }
}

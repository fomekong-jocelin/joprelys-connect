import { CommonModule } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { Observable, finalize } from 'rxjs';
import { ApiErrorI18nService } from '../../core/i18n/api-error-i18n.service';
import { I18nService } from '../../core/i18n/i18n.service';
import { AlertComponent } from '../../shared/ui/alert.component';
import { EmergencyBelongingsSectionComponent, BelongingTransferCommand } from './emergency-belongings-section.component';
import { EmergencyCapacitySectionComponent } from './emergency-capacity-section.component';
import { EmergencyLegalBasisSectionComponent } from './emergency-legal-basis-section.component';
import { EmergencyThirdPartiesSectionComponent } from './emergency-third-parties-section.component';
import { EmergencyMedicoLegalApiService } from './emergency-medico-legal-api.service';
import {
  CreateEmergencyBelongingRequest,
  CreateEmergencyLegalBasisRequest,
  CreateEmergencyThirdPartyRequest,
  EmergencyMedicoLegalDossier,
  RecordEmergencyCapacityRequest,
} from './emergency-medico-legal.models';

type MedicoLegalSection = 'THIRD_PARTIES' | 'CAPACITY' | 'LEGAL_BASIS' | 'BELONGINGS';

@Component({
  selector: 'app-emergency-medico-legal-panel',
  standalone: true,
  imports: [
    CommonModule,
    AlertComponent,
    EmergencyThirdPartiesSectionComponent,
    EmergencyCapacitySectionComponent,
    EmergencyLegalBasisSectionComponent,
    EmergencyBelongingsSectionComponent,
  ],
  templateUrl: './emergency-medico-legal-panel.component.html',
})
export class EmergencyMedicoLegalPanelComponent implements OnInit {
  private readonly api = inject(EmergencyMedicoLegalApiService);
  private readonly apiErrors = inject(ApiErrorI18nService);
  readonly i18n = inject(I18nService);

  readonly emergencyId = input.required<string>();
  readonly dossier = signal<EmergencyMedicoLegalDossier | null>(null);
  readonly activeSection = signal<MedicoLegalSection>('THIRD_PARTIES');
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  setSection(section: MedicoLegalSection): void {
    this.activeSection.set(section);
    this.error.set(null);
  }

  addThirdParty(request: CreateEmergencyThirdPartyRequest): void {
    this.execute(this.api.addThirdParty(this.emergencyId(), request));
  }

  recordCapacity(request: RecordEmergencyCapacityRequest): void {
    this.execute(this.api.recordCapacity(this.emergencyId(), request));
  }

  addLegalBasis(request: CreateEmergencyLegalBasisRequest): void {
    this.execute(this.api.addLegalBasis(this.emergencyId(), request));
  }

  addBelonging(request: CreateEmergencyBelongingRequest): void {
    this.execute(this.api.addBelonging(this.emergencyId(), request));
  }

  transferBelonging(command: BelongingTransferCommand): void {
    this.execute(this.api.transferBelonging(
      this.emergencyId(),
      command.belongingId,
      command.request,
    ));
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private load(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.api.getDossier(this.emergencyId()).pipe(
      finalize(() => this.isLoading.set(false)),
    ).subscribe({
      next: (dossier) => this.dossier.set(dossier),
      error: (error) => this.error.set(this.apiErrors.message(
        error,
        'medicoLegal.error',
        'medicoLegal.error.load',
      )),
    });
  }

  private execute(operation: Observable<EmergencyMedicoLegalDossier>): void {
    if (this.isSaving()) return;
    this.isSaving.set(true);
    this.error.set(null);
    operation.pipe(finalize(() => this.isSaving.set(false))).subscribe({
      next: (dossier) => this.dossier.set(dossier),
      error: (error) => this.error.set(this.apiErrors.message(
        error,
        'medicoLegal.error',
        'medicoLegal.error.save',
      )),
    });
  }
}

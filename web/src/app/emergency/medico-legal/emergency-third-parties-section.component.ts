import { CommonModule } from '@angular/common';
import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { I18nService } from '../../core/i18n/i18n.service';
import { ButtonComponent } from '../../shared/ui/button.component';
import {
  CreateEmergencyThirdPartyRequest,
  EmergencyIdentityStatement,
  EmergencyIdentityStatementRequest,
  EmergencyThirdParty,
  EmergencyThirdPartyQuality,
  IdentityConfidenceLevel,
} from './emergency-medico-legal.models';

const QUALITY_OPTIONS: EmergencyThirdPartyQuality[] = [
  'ACCOMPANYING_PERSON',
  'DECLARANT',
  'WITNESS',
  'TRANSPORTER',
  'PRESUMED_REPRESENTATIVE',
  'GUARANTOR',
  'POLICE_OR_AUTHORITY',
  'OTHER',
];

@Component({
  selector: 'app-emergency-third-parties-section',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ButtonComponent],
  templateUrl: './emergency-third-parties-section.component.html',
})
export class EmergencyThirdPartiesSectionComponent {
  private readonly fb = inject(FormBuilder);
  readonly i18n = inject(I18nService);

  readonly thirdParties = input.required<EmergencyThirdParty[]>();
  readonly identityStatements = input.required<EmergencyIdentityStatement[]>();
  readonly disabled = input(false);
  readonly submitted = output<CreateEmergencyThirdPartyRequest>();

  readonly showForm = signal(false);
  readonly selectedQualities = signal<EmergencyThirdPartyQuality[]>(['ACCOMPANYING_PERSON']);
  readonly statements = signal<EmergencyIdentityStatementRequest[]>([]);
  readonly qualityOptions = QUALITY_OPTIONS;

  readonly form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(160)]],
    phone: ['', Validators.maxLength(40)],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    idDocument: ['', Validators.maxLength(120)],
    relationshipToPatient: ['', Validators.maxLength(80)],
    circumstances: ['', Validators.maxLength(1000)],
    consentToContact: [false],
    legalRepresentativeClaimed: [false],
    sourceType: ['ACCOMPANYING_PERSON'],
    confidenceLevel: ['LOW'],
    proofReference: ['', Validators.maxLength(255)],
    statementField: [''],
    statementValue: [''],
    statementConfidence: ['LOW'],
    statementProof: [''],
  });

  toggleForm(): void {
    this.showForm.update(value => !value);
  }

  toggleQuality(quality: EmergencyThirdPartyQuality): void {
    this.selectedQualities.update(current => current.includes(quality)
      ? current.filter(item => item !== quality)
      : [...current, quality]);
  }

  isQualitySelected(quality: EmergencyThirdPartyQuality): boolean {
    return this.selectedQualities().includes(quality);
  }

  addStatement(): void {
    const value = this.form.getRawValue();
    const fieldName = value.statementField.trim();
    const statementValue = value.statementValue.trim();
    if (!fieldName || !statementValue) return;

    this.statements.update(current => [...current, {
      fieldName,
      value: statementValue,
      confidenceLevel: value.statementConfidence as IdentityConfidenceLevel,
      proofReference: this.optional(value.statementProof),
    }]);
    this.form.patchValue({
      statementField: '',
      statementValue: '',
      statementConfidence: 'LOW',
      statementProof: '',
    });
  }

  removeStatement(index: number): void {
    this.statements.update(current => current.filter((_, itemIndex) => itemIndex !== index));
  }

  submit(): void {
    if (this.form.invalid || this.selectedQualities().length === 0 || this.disabled()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const qualities = [...this.selectedQualities()];
    if (value.legalRepresentativeClaimed && !qualities.includes('PRESUMED_REPRESENTATIVE')) {
      qualities.push('PRESUMED_REPRESENTATIVE');
    }

    this.submitted.emit({
      fullName: value.fullName.trim(),
      phone: this.optional(value.phone),
      email: this.optional(value.email),
      idDocument: this.optional(value.idDocument),
      relationshipToPatient: this.optional(value.relationshipToPatient),
      circumstances: this.optional(value.circumstances),
      consentToContact: value.consentToContact,
      legalRepresentativeClaimed: value.legalRepresentativeClaimed,
      sourceType: value.sourceType as CreateEmergencyThirdPartyRequest['sourceType'],
      confidenceLevel: value.confidenceLevel as IdentityConfidenceLevel,
      proofReference: this.optional(value.proofReference),
      qualities,
      identityStatements: this.statements(),
    });
    this.reset();
  }

  statementsFor(thirdPartyId: string): EmergencyIdentityStatement[] {
    return this.identityStatements().filter(item => item.thirdPartyId === thirdPartyId);
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  private reset(): void {
    this.form.reset({
      fullName: '', phone: '', email: '', idDocument: '', relationshipToPatient: '',
      circumstances: '', consentToContact: false, legalRepresentativeClaimed: false,
      sourceType: 'ACCOMPANYING_PERSON', confidenceLevel: 'LOW', proofReference: '',
      statementField: '', statementValue: '', statementConfidence: 'LOW', statementProof: '',
    });
    this.selectedQualities.set(['ACCOMPANYING_PERSON']);
    this.statements.set([]);
    this.showForm.set(false);
  }

  private optional(value: string): string | undefined {
    const normalized = value.trim();
    return normalized || undefined;
  }
}

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiProposalDecisionRequest,
  AiProposalPanelComponent,
} from './ai-proposal-panel.component';

class I18nStub {
  private readonly translations: Record<string, string> = {
    'consultation.ai.revisionTitle': 'Modifications à valider',
    'consultation.ai.revisionGovernanceHelp': 'Le copilote propose uniquement.',
    'consultation.ai.uncertaintyLow': 'Confiance élevée',
    'consultation.ai.previousValue': 'Avant',
    'consultation.ai.proposedValue': 'Après',
    'consultation.ai.emptyValue': 'Vide',
    'consultation.ai.clearValue': 'Supprimer cette valeur',
    'consultation.ai.acceptProposal': 'Accepter ce champ',
    'consultation.ai.rejectProposal': 'Rejeter ce champ',
    'consultation.ai.acceptAll': 'Tout accepter',
    'consultation.ai.rejectAll': 'Tout rejeter',
    'consultation.symptoms.label': 'Symptômes',
    'consultation.diagnosis.label': 'Diagnostic',
  };

  t(key: string, defaultValue?: string): string {
    return this.translations[key] ?? defaultValue ?? key;
  }
}

describe('AiProposalPanelComponent', () => {
  let fixture: ComponentFixture<AiProposalPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiProposalPanelComponent],
      providers: [{ provide: I18nService, useClass: I18nStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiProposalPanelComponent);
    fixture.componentRef.setInput('revisions', [
      {
        id: 'revision-1',
        sequence: 1,
        status: 'PENDING',
        createdAt: '2026-07-17T20:00:00Z',
        proposals: [
          {
            id: 'proposal-1',
            field: 'symptoms',
            operation: 'SET',
            previousValue: 'Fièvre',
            proposedValue: 'Fièvre avec céphalées',
            reason: 'Symptôme ajouté par le médecin.',
            uncertainty: 'LOW',
            status: 'PENDING',
            createdAt: '2026-07-17T20:00:00Z',
            decidedAt: null,
          },
          {
            id: 'proposal-2',
            field: 'diagnosis',
            operation: 'CLEAR',
            previousValue: 'Diagnostic provisoire',
            proposedValue: null,
            reason: 'Suppression demandée.',
            uncertainty: 'LOW',
            status: 'PENDING',
            createdAt: '2026-07-17T20:00:00Z',
            decidedAt: null,
          },
        ],
      },
    ]);
    fixture.detectChanges();
  });

  it('affiche les valeurs avant et après sans modifier le brouillon', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Fièvre');
    expect(text).toContain('Fièvre avec céphalées');
    expect(text).toContain('Supprimer cette valeur');
    expect(text).toContain('Symptômes');
  });

  it('affiche tous les détails utiles d’une prescription avant validation', () => {
    const formatted = fixture.componentInstance.formatValue(
      'prescription',
      JSON.stringify([{
        drugName: 'Paracétamol',
        dosage: '1000 mg',
        form: 'comprimé',
        posology: '2 comprimés par prise',
        frequency: 'matin midi soir',
        route: 'voie orale',
        duration: '4 jours',
        quantity: '24 comprimés',
        instructions: 'après repas',
      }]),
    );

    expect(formatted).toContain('Paracétamol');
    expect(formatted).toContain('1000 mg');
    expect(formatted).toContain('2 comprimés par prise');
    expect(formatted).toContain('matin midi soir');
    expect(formatted).toContain('voie orale');
    expect(formatted).toContain('4 jours');
    expect(formatted).toContain('24 comprimés');
    expect(formatted).toContain('après repas');
  });

  it('émet une décision liée à la révision et à la proposition', () => {
    let emitted: AiProposalDecisionRequest | undefined;
    fixture.componentInstance.decided.subscribe(value => emitted = value);

    fixture.componentInstance.decideProposal(
      'revision-1',
      'proposal-1',
      'ACCEPT',
    );

    expect(emitted).toEqual({
      scope: 'PROPOSAL',
      revisionId: 'revision-1',
      proposalId: 'proposal-1',
      decision: 'ACCEPT',
    });
  });

  it('émet une décision globale pour les propositions en attente', () => {
    let emitted: AiProposalDecisionRequest | undefined;
    fixture.componentInstance.decided.subscribe(value => emitted = value);

    fixture.componentInstance.decideRevision('revision-1', 'REJECT');

    expect(emitted).toEqual({
      scope: 'REVISION',
      revisionId: 'revision-1',
      decision: 'REJECT',
    });
  });
});

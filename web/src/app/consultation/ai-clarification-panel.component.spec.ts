import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import {
  AiClarificationAnswer,
  AiClarificationPanelComponent,
} from './ai-clarification-panel.component';

class I18nStub {
  t(_key: string, defaultValue?: string): string {
    return defaultValue ?? _key;
  }
}

describe('AiClarificationPanelComponent', () => {
  let fixture: ComponentFixture<AiClarificationPanelComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiClarificationPanelComponent],
      providers: [{ provide: I18nService, useClass: I18nStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiClarificationPanelComponent);
    fixture.componentRef.setInput('clarifications', [
      {
        id: 'clarification-1',
        field: 'symptoms',
        question: 'Depuis combien de temps ?',
        status: 'PENDING',
        options: ['Un jour', 'Deux jours'],
        createdAt: '2026-07-17T20:00:00Z',
        answer: null,
        resolvedAt: null,
      },
    ]);
    fixture.detectChanges();
  });

  it('émet la réponse avec l identifiant de clarification', () => {
    let emitted: AiClarificationAnswer | undefined;
    fixture.componentInstance.answered.subscribe(value => emitted = value);

    fixture.componentInstance.selectOption('Deux jours');
    fixture.componentInstance.submitAnswer('clarification-1');

    expect(emitted).toEqual({
      clarificationId: 'clarification-1',
      answer: 'Deux jours',
    });
  });

  it('affiche le champ et la question ciblés', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Depuis combien de temps ?');
    expect(text).toContain('Symptômes');
    expect(text).toContain('Deux jours');
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { I18nService } from '../core/i18n/i18n.service';
import { AiConversationThreadComponent } from './ai-conversation-thread.component';

class I18nStub {
  t(_key: string, defaultValue?: string): string {
    return defaultValue ?? _key;
  }
}

describe('AiConversationThreadComponent', () => {
  let fixture: ComponentFixture<AiConversationThreadComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiConversationThreadComponent],
      providers: [{ provide: I18nService, useClass: I18nStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiConversationThreadComponent);
  });

  it('affiche les messages médecin et assistant dans l ordre', () => {
    fixture.componentRef.setInput('messages', [
      {
        id: '1',
        role: 'USER',
        content: 'La douleur est à droite.',
        source: 'TEXT',
        createdAt: '2026-07-17T20:00:00Z',
        needsClarification: false,
      },
      {
        id: '2',
        role: 'ASSISTANT',
        content: 'Depuis combien de temps ?',
        source: 'AI',
        createdAt: '2026-07-17T20:00:01Z',
        needsClarification: true,
      },
    ]);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('La douleur est à droite.');
    expect(text).toContain('Depuis combien de temps ?');
    expect(text).toContain('Précision attendue');
    expect(text.indexOf('La douleur est à droite.'))
      .toBeLessThan(text.indexOf('Depuis combien de temps ?'));
  });
});

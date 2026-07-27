import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { I18nService } from '../core/i18n/i18n.service';
import { ClinicalRealtimeVoiceBridgeService } from './clinical-realtime-voice-bridge.service';

class I18nStub {
  currentLanguage(): string {
    return 'fr';
  }

  t(_key: string, fallback?: string): string {
    return fallback ?? _key;
  }
}

describe('ClinicalRealtimeVoiceBridgeService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        ClinicalRealtimeVoiceBridgeService,
        { provide: I18nService, useClass: I18nStub },
      ],
    });
  });

  it('never starts unsolicited assistant audio during continuous clinical capture', () => {
    const bridge = TestBed.inject(ClinicalRealtimeVoiceBridgeService);

    expect(bridge.speakApproved('Veuillez préciser la durée.')).toBe(false);
  });
});

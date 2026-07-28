import { TestBed } from '@angular/core/testing';
import { NEVER } from 'rxjs';
import { AiConsultationApiService } from './ai-consultation-api.service';
import { ClinicalVoicePlaybackService } from './clinical-voice-playback.service';

describe('ClinicalVoicePlaybackService', () => {
  let synthesizeSpeech: ReturnType<typeof vi.fn>;
  let service: ClinicalVoicePlaybackService;

  beforeEach(() => {
    synthesizeSpeech = vi.fn().mockReturnValue(NEVER);
    TestBed.configureTestingModule({
      providers: [
        ClinicalVoicePlaybackService,
        {
          provide: AiConsultationApiService,
          useValue: { synthesizeSpeech },
        },
      ],
    });
    service = TestBed.inject(ClinicalVoicePlaybackService);
  });

  afterEach(() => {
    service.ngOnDestroy();
    TestBed.resetTestingModule();
  });

  it('uses the dedicated backend TTS once for the same approved realtime question', () => {
    const question = 'Confirmez-vous la dose de 500 mg ?';

    service.play(question);
    service.play(question);

    expect(synthesizeSpeech).toHaveBeenCalledTimes(1);
    expect(synthesizeSpeech).toHaveBeenCalledWith(question);
    expect('speakApproved' in service).toBe(false);
  });

  it('does not call TTS for an empty assistant message', () => {
    service.play('   ');

    expect(synthesizeSpeech).not.toHaveBeenCalled();
  });
});

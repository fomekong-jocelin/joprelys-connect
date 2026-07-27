import { Injectable } from '@angular/core';
import { RealtimeVoiceBridgeService } from './realtime-voice-bridge.service';

/**
 * Clinical safety policy for ambient consultation.
 *
 * During continuous clinical capture the assistant must not start unsolicited
 * audio output. The base bridge historically marks that output as
 * `assistantSpeaking`, and the controller then removes the outbound microphone
 * track to avoid echo. A clinician who keeps speaking during that interval is
 * therefore no longer sent to Realtime and words can disappear before any
 * durable transcript event exists.
 *
 * Clarifications remain visible in the consultation UI. Spoken feedback can be
 * reintroduced later only as an explicit clinician action with verified
 * full-duplex/barge-in behavior on physical mobile devices.
 */
@Injectable()
export class ClinicalRealtimeVoiceBridgeService extends RealtimeVoiceBridgeService {
  override speakApproved(_message: string): boolean {
    return false;
  }
}

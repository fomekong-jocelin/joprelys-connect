import 'dart:async';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_realtime_transcription_bridge.dart';
import 'package:joprelys_mobile/features/dashboard/application/clinical_speech_service.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_ai_api.dart';

void main() {
  test('completed turns appear and are persisted before Terminer', () async {
    final gateway = _FakeGateway();
    final transport = _FakeTransport();
    final service = ClinicalSpeechService(
      gateway: gateway,
      visitId: 'visit-1',
      initialDraft: const <String, String>{},
      locale: 'fr',
      transport: transport,
    );
    addTearDown(service.dispose);

    await service.restoreOrStart();
    expect(service.value.status, SpeechStatus.listening);

    transport.emit(const ClinicalRealtimeEvent(
      ClinicalRealtimeEventType.transcriptDelta,
      text: 'Le patient signale ',
    ));
    expect(service.value.transcript, 'Le patient signale');

    transport.emit(const ClinicalRealtimeEvent(
      ClinicalRealtimeEventType.transcriptCompleted,
      text: 'Le patient signale une fièvre depuis quatre jours.',
      itemId: 'item-1',
    ));
    await _flushAsyncWork();

    expect(
      service.value.transcript,
      'Le patient signale une fièvre depuis quatre jours.',
    );
    expect(gateway.liveTranscript, service.value.transcript);
    expect(gateway.liveSequence, 1);

    transport.emit(const ClinicalRealtimeEvent(
      ClinicalRealtimeEventType.transcriptCompleted,
      text: 'Il présente aussi une toux sèche.',
      itemId: 'item-2',
    ));
    await _flushAsyncWork();

    expect(
      service.value.transcript,
      'Le patient signale une fièvre depuis quatre jours.\n'
      'Il présente aussi une toux sèche.',
    );
    expect(gateway.liveSequence, 2);
  });

  test('Terminer stages text without uploading a final audio file', () async {
    final gateway = _FakeGateway();
    final transport = _FakeTransport();
    final service = ClinicalSpeechService(
      gateway: gateway,
      visitId: 'visit-2',
      initialDraft: const <String, String>{},
      locale: 'fr',
      transport: transport,
    );
    addTearDown(service.dispose);

    await service.restoreOrStart();
    transport.emit(const ClinicalRealtimeEvent(
      ClinicalRealtimeEventType.transcriptCompleted,
      text: 'La température est de trente-huit virgule cinq degrés.',
      itemId: 'item-vitals',
    ));
    await _flushAsyncWork();

    await service.stopListening();

    expect(transport.finalizeCalls, 1);
    expect(gateway.transcribeAudioCalls, 0);
    expect(gateway.stagedTranscript, contains('trente-huit virgule cinq'));
    expect(gateway.liveTranscript, isNull);
    expect(service.value.status, SpeechStatus.transcriptReview);
  });

  test('a silent live session never stages or analyzes clinical content', () async {
    final gateway = _FakeGateway();
    final transport = _FakeTransport();
    final service = ClinicalSpeechService(
      gateway: gateway,
      visitId: 'visit-3',
      initialDraft: const <String, String>{},
      locale: 'fr',
      transport: transport,
    );
    addTearDown(service.dispose);

    await service.restoreOrStart();
    await service.stopListening();

    expect(gateway.stagedTranscript, isNull);
    expect(gateway.analyzeCalls, 0);
    expect(service.value.status, SpeechStatus.idle);
    expect(service.value.errorMessage, contains('Aucune parole'));
  });

  test('a live transcript is restored after closing the mobile sheet', () async {
    final gateway = _FakeGateway()
      ..liveTranscript = 'Céphalées et fatigue depuis lundi.'
      ..liveSequence = 4;
    final transport = _FakeTransport();
    final service = ClinicalSpeechService(
      gateway: gateway,
      visitId: 'visit-4',
      initialDraft: const <String, String>{},
      locale: 'fr',
      transport: transport,
    );
    addTearDown(service.dispose);

    await service.restoreOrStart();

    expect(service.value.status, SpeechStatus.transcriptReview);
    expect(service.value.transcript, gateway.liveTranscript);
    expect(transport.connectCalls, 0);
  });

  test('duplicate completed items are not appended twice', () async {
    final gateway = _FakeGateway();
    final transport = _FakeTransport();
    final service = ClinicalSpeechService(
      gateway: gateway,
      visitId: 'visit-5',
      initialDraft: const <String, String>{},
      locale: 'fr',
      transport: transport,
    );
    addTearDown(service.dispose);

    await service.restoreOrStart();
    const event = ClinicalRealtimeEvent(
      ClinicalRealtimeEventType.transcriptCompleted,
      text: 'Pas de douleur thoracique.',
      itemId: 'same-item',
    );
    transport.emit(event);
    transport.emit(event);
    await _flushAsyncWork();

    expect(service.value.transcript, 'Pas de douleur thoracique.');
    expect(gateway.liveSequence, 1);
  });
}

Future<void> _flushAsyncWork() async {
  await Future<void>.delayed(Duration.zero);
  await Future<void>.delayed(Duration.zero);
}

final class _FakeTransport
    implements ClinicalRealtimeTranscriptionTransport {
  final StreamController<ClinicalRealtimeEvent> _controller =
      StreamController<ClinicalRealtimeEvent>.broadcast(sync: true);

  int connectCalls = 0;
  int finalizeCalls = 0;

  @override
  Stream<ClinicalRealtimeEvent> get events => _controller.stream;

  void emit(ClinicalRealtimeEvent event) => _controller.add(event);

  @override
  Future<void> connect({required String visitId, required String locale}) async {
    connectCalls++;
    emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.connected));
  }

  @override
  Future<void> finalize() async {
    finalizeCalls++;
  }

  @override
  Future<void> disconnect() async {}

  @override
  Future<void> dispose() => _controller.close();
}

final class _FakeGateway implements ClinicalVoiceAiGateway {
  String? liveTranscript;
  int liveSequence = 0;
  String? stagedTranscript;
  int transcribeAudioCalls = 0;
  int analyzeCalls = 0;

  ClinicalAiState get _emptyState => const ClinicalAiState(
    draft: <String, String>{},
    revisions: <ClinicalAiRevision>[],
    transcript: null,
    assistantMessage: null,
    needsClarification: false,
  );

  @override
  Future<ClinicalAiState?> getSession(String visitId) async => null;

  @override
  Future<ClinicalAiState> startSession(
    String visitId,
    Map<String, String> draft, {
    required String locale,
  }) async => _emptyState;

  @override
  Future<ClinicalLiveTranscript?> getLiveTranscript(String visitId) async {
    final transcript = liveTranscript;
    if (transcript == null) return null;
    return ClinicalLiveTranscript(
      transcript: transcript,
      sequence: liveSequence,
      eventId: 'restored-event',
    );
  }

  @override
  Future<void> upsertLiveTranscript(
    String visitId, {
    required String transcript,
    required int sequence,
    required String eventId,
  }) async {
    liveTranscript = transcript;
    liveSequence = sequence;
  }

  @override
  Future<void> clearLiveTranscript(String visitId) async {
    liveTranscript = null;
    liveSequence = 0;
  }

  @override
  Future<void> stageRealtimeTranscript(
    String visitId,
    String transcript,
  ) async {
    stagedTranscript = transcript;
  }

  @override
  Future<String> transcribeAudio(String visitId, Uint8List audioBytes) async {
    transcribeAudioCalls++;
    return 'legacy';
  }

  @override
  Future<ClinicalAiState> analyzeTranscript(
    String visitId,
    String transcript,
  ) async {
    analyzeCalls++;
    return _emptyState;
  }

  @override
  Future<void> discardPendingTranscript(String visitId) async {}

  @override
  Future<String> createRealtimeCall(
    String visitId,
    String locale,
    String sdpOffer,
  ) async => 'answer';

  @override
  Future<ClinicalAiState> decideProposal(
    String visitId,
    String revisionId,
    String proposalId,
    ClinicalAiDecision decision,
  ) async => _emptyState;

  @override
  Future<ClinicalAiState> decideRevision(
    String visitId,
    String revisionId,
    ClinicalAiDecision decision,
  ) async => _emptyState;
}

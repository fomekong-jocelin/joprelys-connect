from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"Expected block not found in {path}: {old[:180]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


# 1) Android WebRTC: avoid native constraint crashes and reconnect storms.
bridge = "mobile/lib/features/dashboard/application/clinical_realtime_transcription_bridge.dart"
replace_once(
    bridge,
    "import 'package:flutter_webrtc/flutter_webrtc.dart';\nimport 'package:permission_handler/permission_handler.dart';\n",
    "import 'package:flutter_webrtc/flutter_webrtc.dart';\n",
)
replace_once(
    bridge,
    "  bool _shouldStayConnected = false;\n  bool _connecting = false;\n  bool _disposed = false;",
    "  bool _shouldStayConnected = false;\n  bool _connecting = false;\n  bool _hasConnectedOnce = false;\n  bool _tearingDown = false;\n  bool _disposed = false;",
)
replace_once(
    bridge,
    "    _shouldStayConnected = true;\n    _reconnectAttempts = 0;\n    _segmentedItemIds.clear();",
    "    _shouldStayConnected = true;\n    _reconnectAttempts = 0;\n    _hasConnectedOnce = false;\n    _segmentedItemIds.clear();",
)
replace_once(
    bridge,
    "    try {\n      final permission = await Permission.microphone.request();\n      if (!permission.isGranted) {\n        throw StateError('MICROPHONE_PERMISSION_DENIED');\n      }\n\n      final stream = await navigator.mediaDevices.getUserMedia(const <\n        String,\n        dynamic\n      >{\n        'audio': <String, dynamic>{\n          'channelCount': 1,\n          // Le serveur applique déjà un filtre far_field adapté à la\n          // consultation en salle. Le double traitement Android supprimait des\n          // syllabes et les voix jouées à distance pendant la recette.\n          'echoCancellation': false,\n          'noiseSuppression': false,\n          'autoGainControl': false,\n        },\n        'video': false,\n      });",
    "    try {\n      // Sur Android, flutter_webrtc délègue lui-même la permission micro.\n      // Des contraintes booléennes avancées provoquent des crashes natifs sur\n      // certains constructeurs ; on demande donc une piste audio standard.\n      final stream = await navigator.mediaDevices.getUserMedia(\n        const <String, dynamic>{'audio': true, 'video': false},\n      );",
)
replace_once(
    bridge,
    "        } else if (state == RTCDataChannelState.RTCDataChannelClosed &&\n            _shouldStayConnected) {\n          _scheduleReconnect();",
    "        } else if (state == RTCDataChannelState.RTCDataChannelClosed &&\n            _shouldStayConnected &&\n            _hasConnectedOnce &&\n            !_tearingDown) {\n          _scheduleReconnect();",
)
replace_once(
    bridge,
    "      _reconnectAttempts = 0;\n      _emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.connected));",
    "      _reconnectAttempts = 0;\n      _hasConnectedOnce = true;\n      _emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.connected));",
)
replace_once(
    bridge,
    "      if (_shouldStayConnected) {\n        _emit(\n          ClinicalRealtimeEvent(ClinicalRealtimeEventType.error, error: error),\n        );\n        _scheduleReconnect();\n      }\n      rethrow;",
    "      if (_shouldStayConnected) {\n        _emit(\n          ClinicalRealtimeEvent(ClinicalRealtimeEventType.error, error: error),\n        );\n        // Une première négociation qui échoue doit revenir à l'interface.\n        // Relancer immédiatement flutter_webrtc en boucle peut faire tomber le\n        // processus Android. La reconnexion automatique reste réservée aux\n        // connexions qui ont déjà été établies au moins une fois.\n        if (_hasConnectedOnce) {\n          _scheduleReconnect();\n        } else {\n          _shouldStayConnected = false;\n        }\n      }\n      rethrow;",
)
replace_once(
    bridge,
    "  void _handleConnectionState(RTCPeerConnectionState state) {\n    if (!_shouldStayConnected) return;",
    "  void _handleConnectionState(RTCPeerConnectionState state) {\n    if (!_shouldStayConnected || !_hasConnectedOnce || _tearingDown) return;",
)
replace_once(
    bridge,
    "  void _handleIceConnectionState(RTCIceConnectionState state) {\n    if (!_shouldStayConnected) return;",
    "  void _handleIceConnectionState(RTCIceConnectionState state) {\n    if (!_shouldStayConnected || !_hasConnectedOnce || _tearingDown) return;",
)
replace_once(
    bridge,
    "  void _scheduleReconnect() {\n    if (_disposed || !_shouldStayConnected || _reconnectTimer != null) return;",
    "  void _scheduleReconnect() {\n    if (_disposed ||\n        !_shouldStayConnected ||\n        !_hasConnectedOnce ||\n        _tearingDown ||\n        _reconnectTimer != null) {\n      return;\n    }",
)
replace_once(
    bridge,
    "  Future<void> _teardownTransport() async {\n    final channel = _dataChannel;",
    "  Future<void> _teardownTransport() async {\n    if (_tearingDown) return;\n    _tearingDown = true;\n    final channel = _dataChannel;",
)
replace_once(
    bridge,
    "    if (stream != null) await _disposeStream(stream);\n  }",
    "    if (stream != null) await _disposeStream(stream);\n    _tearingDown = false;\n  }",
)

# 2) Service: stop failed transport and make deletion robust.
service = "mobile/lib/features/dashboard/application/clinical_speech_service.dart"
replace_once(
    service,
    "      assistantMessage: _isFrench\n          ? 'La dictée précédente a été restaurée. Relisez-la ou reprenez l’enregistrement.'\n          : 'The previous dictation was restored. Review it or resume recording.',",
    "      assistantMessage: _isFrench\n          ? 'La dictée précédente a été restaurée. Relisez-la ou supprimez-la avant de reprendre l’enregistrement.'\n          : 'The previous dictation was restored. Review or delete it before recording again.',",
)
replace_once(
    service,
    "    } catch (error) {\n      _setError(error);\n    }\n  }\n\n  void _handleRealtimeEvent",
    "    } catch (error) {\n      // Coupe toute tentative de reconnexion en arrière-plan après un échec de\n      // démarrage. Le prochain essai doit venir d'une action explicite.\n      await _transport.disconnect();\n      _setError(error, fallbackStatus: SpeechStatus.idle);\n    }\n  }\n\n  void _handleRealtimeEvent",
)
replace_once(
    service,
    "  Future<void> discardCurrentCapture() async {\n    if (_disposed) return;\n    try {\n      await _transport.disconnect();\n      await _gateway.clearLiveTranscript(_visitId);\n      if (_serverHasPendingTranscript) {\n        await _gateway.discardPendingTranscript(_visitId);\n      }\n    } catch (_) {\n      // La suppression explicite reste best effort et n'applique aucune donnée.\n    }\n    _serverHasPendingTranscript = false;\n    _confirmedTranscript = '';\n    _partialTranscript = '';\n    _partialItemId = null;\n    _liveSequence = 0;\n    _completedTurnIds.clear();\n    _lastPersistenceError = null;\n    value = const RealtimeSpeechState(\n      status: SpeechStatus.idle,\n      transcript: '',\n      vitals: PatientVitals(),\n      note: ConsultationNote(),\n      revisions: <ClinicalAiRevision>[],\n    );\n  }",
    "  Future<void> discardCurrentCapture() async {\n    if (_disposed || value.status == SpeechStatus.processing) return;\n    final previous = value;\n    value = value.copyWith(\n      status: SpeechStatus.processing,\n      soundLevel: 0,\n      clearError: true,\n      assistantMessage: _isFrench\n          ? 'Suppression de la transcription…'\n          : 'Deleting the transcript…',\n    );\n\n    Object? deletionError;\n    try {\n      await _transport.disconnect();\n    } catch (error) {\n      deletionError = error;\n    }\n    // Les deux suppressions sont indépendantes : l'échec ou l'absence du tampon\n    // live ne doit jamais empêcher la suppression de la transcription en attente.\n    try {\n      await _gateway.clearLiveTranscript(_visitId);\n    } catch (error) {\n      deletionError ??= error;\n    }\n    try {\n      await _gateway.discardPendingTranscript(_visitId);\n    } catch (error) {\n      deletionError ??= error;\n    }\n\n    if (deletionError != null) {\n      value = previous.copyWith(\n        status: SpeechStatus.transcriptReview,\n        errorMessage: _isFrench\n            ? 'La transcription n’a pas pu être supprimée du serveur. Réessayez.'\n            : 'The transcript could not be deleted from the server. Try again.',\n      );\n      return;\n    }\n\n    _serverHasPendingTranscript = false;\n    _confirmedTranscript = '';\n    _partialTranscript = '';\n    _partialItemId = null;\n    _liveSequence = 0;\n    _completedTurnIds.clear();\n    _lastPersistenceError = null;\n    value = const RealtimeSpeechState(\n      status: SpeechStatus.idle,\n      transcript: '',\n      vitals: PatientVitals(),\n      note: ConsultationNote(),\n      revisions: <ClinicalAiRevision>[],\n    );\n  }",
)

# 3) Mobile UI: expose an explicit destructive action.
sheet = "mobile/lib/features/dashboard/presentation/widgets/clinical_voice_assistant_sheet.dart"
replace_once(
    sheet,
    "  Future<void> _close() async {\n    final safeToClose = await _speechService.prepareForClose();\n    if (safeToClose && mounted) Navigator.of(context).pop();\n  }\n\n  Future<void> _applyAcceptedResult() async {",
    "  Future<void> _close() async {\n    final safeToClose = await _speechService.prepareForClose();\n    if (safeToClose && mounted) Navigator.of(context).pop();\n  }\n\n  Future<void> _discardTranscript() async {\n    final confirmed = await showDialog<bool>(\n      context: context,\n      builder: (dialogContext) => AlertDialog(\n        title: Text(\n          _isFrench ? 'Supprimer cette transcription ?' : 'Delete this transcript?',\n        ),\n        content: Text(\n          _isFrench\n              ? 'La transcription enregistrée pour cette consultation sera supprimée. Cette action est irréversible.'\n              : 'The saved transcript for this consultation will be deleted. This action cannot be undone.',\n        ),\n        actions: [\n          TextButton(\n            onPressed: () => Navigator.of(dialogContext).pop(false),\n            child: Text(_isFrench ? 'Annuler' : 'Cancel'),\n          ),\n          FilledButton(\n            onPressed: () => Navigator.of(dialogContext).pop(true),\n            child: Text(_isFrench ? 'Supprimer' : 'Delete'),\n          ),\n        ],\n      ),\n    );\n    if (confirmed == true) await _speechService.discardCurrentCapture();\n  }\n\n  Future<void> _applyAcceptedResult() async {",
)
replace_once(
    sheet,
    "                        ClinicalTranscriptCard(\n                          transcript: state.transcript,\n                          controller: _transcriptController,\n                          onChanged: _speechService.updateTranscript,\n                          label: _isFrench\n                              ? 'Transcription à vérifier'\n                              : 'Transcript to review',\n                          hint: l10n.assistantDictationHint,\n                          editLabel: _isFrench ? 'Corriger' : 'Edit',\n                          doneLabel: _isFrench\n                              ? 'Terminer la correction'\n                              : 'Finish editing',\n                          editable:\n                              state.status == SpeechStatus.transcriptReview,\n                        ),\n                      ],",
    "                        ClinicalTranscriptCard(\n                          transcript: state.transcript,\n                          controller: _transcriptController,\n                          onChanged: _speechService.updateTranscript,\n                          label: _isFrench\n                              ? 'Transcription à vérifier'\n                              : 'Transcript to review',\n                          hint: l10n.assistantDictationHint,\n                          editLabel: _isFrench ? 'Corriger' : 'Edit',\n                          doneLabel: _isFrench\n                              ? 'Terminer la correction'\n                              : 'Finish editing',\n                          editable:\n                              state.status == SpeechStatus.transcriptReview,\n                        ),\n                        if (!listening && !processing) ...[\n                          const SizedBox(height: 6),\n                          Align(\n                            alignment: Alignment.centerRight,\n                            child: TextButton.icon(\n                              onPressed: _discardTranscript,\n                              icon: const Icon(Icons.delete_outline_rounded),\n                              label: Text(\n                                _isFrench\n                                    ? 'Supprimer cette transcription'\n                                    : 'Delete this transcript',\n                              ),\n                              style: TextButton.styleFrom(\n                                foregroundColor: colors.error,\n                              ),\n                            ),\n                          ),\n                        ],\n                      ],",
)

# 4) Extraction completeness: retain adherence/stress/negations/vitals in symptoms.
prompt = "backend/src/main/java/com/joprelys/backend/ai/application/AiClinicalCapturePrompt.java"
replace_once(
    prompt,
    "            - Symptoms include the chief complaint, duration, chronology, severity, associated or denied symptoms, treatment adherence, functional impact and any subjective history explicitly stated by the patient or clinician.\n",
    "            - Symptoms include the chief complaint, duration, chronology, severity, associated or denied symptoms, treatment adherence, missed doses, stress or exposure triggers, functional impact and any subjective history explicitly stated by the patient or clinician.\n            - Never reduce symptoms to the chief complaint when the transcript also contains non-adherence, stress context, denied symptoms or chronology. Combine every supported subjective fact into the single symptoms change.\n            - A spoken vital sign such as 15/9 must also produce a vitals change; do not leave it only inside narrative text.\n",
)

# 5) Regression tests.
test = "mobile/test/clinical_realtime_speech_service_test.dart"
replace_once(
    test,
    "  test('duplicate completed items are not appended twice', () async {",
    "  test('restored transcript can be deleted before starting a new recording', () async {\n    final gateway = _FakeGateway()\n      ..sessionPendingTranscript = 'Ancienne transcription à supprimer.'\n      ..liveTranscript = 'Ancienne transcription à supprimer.';\n    final transport = _FakeTransport();\n    final service = ClinicalSpeechService(\n      gateway: gateway,\n      visitId: 'visit-delete',\n      initialDraft: const <String, String>{},\n      locale: 'fr',\n      transport: transport,\n    );\n    addTearDown(service.dispose);\n\n    await service.restoreOrStart();\n    expect(service.value.status, SpeechStatus.transcriptReview);\n\n    await service.discardCurrentCapture();\n\n    expect(gateway.clearLiveCalls, 1);\n    expect(gateway.discardPendingCalls, 1);\n    expect(service.value.status, SpeechStatus.idle);\n    expect(service.value.transcript, isEmpty);\n\n    await service.startRealtimeListening();\n    expect(service.value.status, SpeechStatus.listening);\n    expect(transport.connectCalls, 1);\n  });\n\n  test('failed initial realtime connection is disconnected without retry loop', () async {\n    final gateway = _FakeGateway();\n    final transport = _FakeTransport()\n      ..connectError = StateError('AI_REALTIME_CHANNEL_TIMEOUT');\n    final service = ClinicalSpeechService(\n      gateway: gateway,\n      visitId: 'visit-start-failure',\n      initialDraft: const <String, String>{},\n      locale: 'fr',\n      transport: transport,\n    );\n    addTearDown(service.dispose);\n\n    await service.restoreOrStart();\n\n    expect(transport.connectCalls, 1);\n    expect(transport.disconnectCalls, 1);\n    expect(service.value.status, SpeechStatus.idle);\n    expect(service.value.errorMessage, contains('temps réel'));\n  });\n\n  test('duplicate completed items are not appended twice', () async {",
)
replace_once(
    test,
    "  int connectCalls = 0;\n  int finalizeCalls = 0;",
    "  int connectCalls = 0;\n  int disconnectCalls = 0;\n  int finalizeCalls = 0;\n  Object? connectError;",
)
replace_once(
    test,
    "  }) async {\n    connectCalls++;\n    emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.connected));\n  }\n\n  @override\n  Future<void> finalize() async {",
    "  }) async {\n    connectCalls++;\n    final error = connectError;\n    if (error != null) throw error;\n    emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.connected));\n  }\n\n  @override\n  Future<void> finalize() async {",
)
replace_once(
    test,
    "  @override\n  Future<void> disconnect() async {}",
    "  @override\n  Future<void> disconnect() async {\n    disconnectCalls++;\n  }",
)
replace_once(
    test,
    "  ApiException? liveTranscriptError;\n",
    "  ApiException? liveTranscriptError;\n  String? sessionPendingTranscript;\n  int clearLiveCalls = 0;\n  int discardPendingCalls = 0;\n",
)
replace_once(
    test,
    "  @override\n  Future<ClinicalAiState?> getSession(String visitId) async => null;",
    "  @override\n  Future<ClinicalAiState?> getSession(String visitId) async {\n    final pending = sessionPendingTranscript;\n    if (pending == null) return null;\n    return ClinicalAiState(\n      draft: const <String, String>{},\n      revisions: const <ClinicalAiRevision>[],\n      transcript: pending,\n      assistantMessage: null,\n      needsClarification: false,\n      pendingTranscript: pending,\n      transcriptStatus: 'PENDING_REVIEW',\n    );\n  }",
)
replace_once(
    test,
    "  Future<void> clearLiveTranscript(String visitId) async {\n    liveTranscript = null;\n    liveSequence = 0;\n  }",
    "  Future<void> clearLiveTranscript(String visitId) async {\n    clearLiveCalls++;\n    liveTranscript = null;\n    liveSequence = 0;\n  }",
)
replace_once(
    test,
    "  Future<void> discardPendingTranscript(String visitId) async {}",
    "  Future<void> discardPendingTranscript(String visitId) async {\n    discardPendingCalls++;\n    sessionPendingTranscript = null;\n    stagedTranscript = null;\n  }",
)

print("Mobile voice blockers patch applied")

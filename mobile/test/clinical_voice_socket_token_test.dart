import 'dart:async';
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:record/record.dart';
import 'package:web_socket_channel/web_socket_channel.dart';

import 'package:joprelys_mobile/core/network/api_exception.dart';
import 'package:joprelys_mobile/features/dashboard/application/cloud_speech_streaming_service.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_transcript_draft_store.dart';
import 'package:joprelys_mobile/features/dashboard/data/clinical_voice_ai_api.dart';

void main() {
  late List<Uri> connectedUris;
  late List<_FakeSocket> sockets;

  CloudSpeechStreamingService buildService(
    ClinicalVoiceAccessTokenProvider accessTokenProvider,
  ) {
    final service = CloudSpeechStreamingService(
      gateway: _UnusedAiGateway(),
      visitId: 'visit-fixture',
      initialDraft: const <String, String>{},
      locale: 'fr',
      apiBaseUri: Uri.parse('https://api.test'),
      accessTokenProvider: accessTokenProvider,
      draftStore: _MemoryDraftStore(),
      recorder: _FakeRecorder(),
      channelFactory: (uri) {
        connectedUris.add(uri);
        final socket = _FakeSocket();
        sockets.add(socket);
        return socket;
      },
    );
    addTearDown(service.dispose);
    return service;
  }

  setUp(() {
    connectedUris = <Uri>[];
    sockets = <_FakeSocket>[];
  });

  test('a reconnection uses the current token, not the initial one', () async {
    var tokenCalls = 0;
    final service = buildService(() async {
      tokenCalls += 1;
      return tokenCalls == 1 ? 'token-before-refresh' : 'token-after-refresh';
    });

    await service.startRealtimeListening();
    await _until(() => sockets.length == 1 && sockets.single.started);

    await sockets.single.closeFromServer();
    await _until(() => connectedUris.length == 2);

    expect(
      connectedUris.first.queryParameters['token'],
      'token-before-refresh',
    );
    expect(connectedUris.last.queryParameters['token'], 'token-after-refresh');
    expect(tokenCalls, 2);
  });

  test('stops with a session message when no token is available', () async {
    final service = buildService(() async => null);

    await service.startRealtimeListening();
    await _until(() => service.value.status == SpeechStatus.error);

    expect(connectedUris, isEmpty);
    expect(service.value.errorMessage, contains('session a expiré'));
  });

  test('retries when the token refresh hits a network timeout', () async {
    var tokenCalls = 0;
    final service = buildService(() async {
      tokenCalls += 1;
      if (tokenCalls == 1) {
        throw const ApiException(
          kind: ApiFailureKind.timeout,
          code: 'REQUEST_TIMEOUT',
          message: 'REQUEST_TIMEOUT',
          retryable: true,
        );
      }
      return 'token-after-retry';
    });

    await service.startRealtimeListening();
    await _until(() => connectedUris.length == 1);

    expect(connectedUris.single.queryParameters['token'], 'token-after-retry');
    expect(service.value.status, isNot(SpeechStatus.error));
  });
}

Future<void> _until(bool Function() condition) async {
  final deadline = DateTime.now().add(const Duration(seconds: 5));
  while (!condition()) {
    if (DateTime.now().isAfter(deadline)) {
      fail('Condition not reached before timeout');
    }
    await Future<void>.delayed(const Duration(milliseconds: 10));
  }
}

final class _FakeSocket extends Fake implements WebSocketChannel {
  final StreamController<dynamic> _incoming = StreamController<dynamic>();
  late final _FakeSink _sink = _FakeSink(this);
  bool started = false;

  @override
  Stream<dynamic> get stream => _incoming.stream;

  @override
  Future<void> get ready => Future<void>.value();

  @override
  WebSocketSink get sink => _sink;

  void receive(Map<String, Object?> message) {
    if (!_incoming.isClosed) _incoming.add(jsonEncode(message));
  }

  Future<void> closeFromServer() async {
    if (!_incoming.isClosed) await _incoming.close();
  }
}

final class _FakeSink extends Fake implements WebSocketSink {
  _FakeSink(this._socket);

  final _FakeSocket _socket;

  @override
  void add(Object? data) {
    final decoded = jsonDecode(data! as String) as Map<String, dynamic>;
    if (decoded['type'] == 'start') {
      _socket.started = true;
      scheduleMicrotask(
        () => _socket.receive(const <String, Object?>{
          'type': 'ack',
          'status': 'STREAMING_STARTED',
        }),
      );
    }
  }

  @override
  Future<void> close([int? closeCode, String? closeReason]) async {}
}

final class _FakeRecorder extends Fake implements AudioRecorder {
  @override
  Future<bool> hasPermission({bool request = true}) async => true;

  @override
  Future<bool> isEncoderSupported(AudioEncoder encoder) async => true;

  @override
  Future<Stream<Uint8List>> startStream(RecordConfig config) async {
    // A fresh open stream per capture (re)start, like the platform recorder.
    return StreamController<Uint8List>().stream;
  }

  @override
  Future<Amplitude> getAmplitude() async => Amplitude(current: -30, max: 0);

  @override
  Future<String?> stop() async => null;

  @override
  Future<void> dispose() async {}
}

final class _MemoryDraftStore implements ClinicalTranscriptDraftGateway {
  final Map<String, ClinicalTranscriptDraft> _drafts =
      <String, ClinicalTranscriptDraft>{};

  @override
  Future<void> delete(String visitId) async => _drafts.remove(visitId);

  @override
  Future<ClinicalTranscriptDraft?> read(String visitId) async {
    return _drafts[visitId];
  }

  @override
  Future<void> write(String visitId, ClinicalTranscriptDraft draft) async {
    _drafts[visitId] = draft;
  }
}

final class _UnusedAiGateway extends Fake implements ClinicalVoiceAiGateway {}

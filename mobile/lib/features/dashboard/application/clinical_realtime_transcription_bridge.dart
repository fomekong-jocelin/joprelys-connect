import 'dart:async';
import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter_webrtc/flutter_webrtc.dart';

import '../data/clinical_voice_ai_api.dart';

enum ClinicalRealtimeEventType {
  connected,
  speechStarted,
  speechStopped,
  transcriptDelta,
  transcriptCompleted,
  reconnecting,
  error,
}

final class ClinicalRealtimeEvent {
  const ClinicalRealtimeEvent(
    this.type, {
    this.text,
    this.eventId,
    this.itemId,
    this.confidence,
    this.error,
  });

  final ClinicalRealtimeEventType type;
  final String? text;
  final String? eventId;
  final String? itemId;
  final double? confidence;
  final Object? error;
}

abstract interface class ClinicalRealtimeTranscriptionTransport {
  Stream<ClinicalRealtimeEvent> get events;

  Future<void> connect({required String visitId, required String locale});

  Future<void> finalize();

  Future<void> disconnect();

  Future<void> dispose();
}

/// Transport audio temps réel du copilote clinique mobile.
///
/// Le microphone est envoyé directement par WebRTC. Le backend Joprelys reste
/// l'autorité de négociation SDP et ne transmet jamais la clé fournisseur au
/// mobile. Le canal de données ne transporte que les événements de transcription.
final class WebRtcClinicalRealtimeTranscriptionTransport
    implements ClinicalRealtimeTranscriptionTransport {
  WebRtcClinicalRealtimeTranscriptionTransport(this._gateway);

  static const Duration _iceTimeout = Duration(seconds: 10);
  static const Duration _channelTimeout = Duration(seconds: 10);
  static const Duration _finalTranscriptTimeout = Duration(milliseconds: 2800);
  static const int _maximumReconnectAttempts = 5;

  final ClinicalVoiceAiGateway _gateway;
  final StreamController<ClinicalRealtimeEvent> _eventsController =
      StreamController<ClinicalRealtimeEvent>.broadcast(sync: true);

  RTCPeerConnection? _peerConnection;
  RTCDataChannel? _dataChannel;
  MediaStream? _mediaStream;
  Timer? _reconnectTimer;
  Completer<void>? _dataChannelOpenCompleter;
  Completer<void>? _finalTranscriptCompleter;

  String _visitId = '';
  String _locale = 'fr';
  bool _shouldStayConnected = false;
  bool _connecting = false;
  bool _disposed = false;
  int _reconnectAttempts = 0;

  @override
  Stream<ClinicalRealtimeEvent> get events => _eventsController.stream;

  @override
  Future<void> connect({required String visitId, required String locale}) async {
    if (_disposed) throw StateError('AI_REALTIME_TRANSPORT_DISPOSED');
    _visitId = visitId;
    _locale = locale == 'en' ? 'en' : 'fr';
    _shouldStayConnected = true;
    _reconnectAttempts = 0;
    _reconnectTimer?.cancel();
    await _establishConnection();
  }

  Future<void> _establishConnection() async {
    if (_disposed || !_shouldStayConnected || _connecting) return;
    _connecting = true;
    await _teardownTransport();

    try {
      final stream = await navigator.mediaDevices.getUserMedia(
        const <String, dynamic>{
          'audio': <String, dynamic>{
            'channelCount': 1,
            'echoCancellation': true,
            'noiseSuppression': true,
            'autoGainControl': true,
          },
          'video': false,
        },
      );
      if (!_shouldStayConnected) {
        await _disposeStream(stream);
        return;
      }
      final audioTracks = stream.getAudioTracks();
      if (audioTracks.isEmpty) {
        await _disposeStream(stream);
        throw StateError('AI_REALTIME_MICROPHONE_TRACK_MISSING');
      }
      _mediaStream = stream;

      final peerConnection = await createPeerConnection(
        const <String, dynamic>{'sdpSemantics': 'unified-plan'},
      );
      _peerConnection = peerConnection;
      peerConnection.onConnectionState = _handleConnectionState;
      peerConnection.onIceConnectionState = _handleIceConnectionState;

      await peerConnection.addTrack(audioTracks.first, stream);

      final dataChannel = await peerConnection.createDataChannel(
        'oai-events',
        RTCDataChannelInit()..ordered = true,
      );
      _dataChannel = dataChannel;
      _dataChannelOpenCompleter = Completer<void>();
      dataChannel.onMessage = _handleDataChannelMessage;
      dataChannel.onDataChannelState = (state) {
        if (state == RTCDataChannelState.RTCDataChannelOpen) {
          final completer = _dataChannelOpenCompleter;
          if (completer != null && !completer.isCompleted) completer.complete();
        } else if (state == RTCDataChannelState.RTCDataChannelClosed &&
            _shouldStayConnected) {
          _scheduleReconnect();
        }
      };

      final offer = await peerConnection.createOffer(
        const <String, dynamic>{
          'offerToReceiveAudio': false,
          'offerToReceiveVideo': false,
        },
      );
      await peerConnection.setLocalDescription(offer);
      await _waitForIceGathering(peerConnection);
      final localDescription = await peerConnection.getLocalDescription();
      final localSdp = localDescription?.sdp?.trim();
      if (localSdp == null || localSdp.isEmpty) {
        throw StateError('AI_REALTIME_SDP_MISSING');
      }

      final answer = await _gateway.createRealtimeCall(
        _visitId,
        _locale,
        localSdp,
      );
      await peerConnection.setRemoteDescription(
        RTCSessionDescription(answer, 'answer'),
      );
      await _waitForDataChannel();

      _reconnectAttempts = 0;
      _emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.connected));
    } catch (error) {
      await _teardownTransport();
      if (_shouldStayConnected) {
        _emit(ClinicalRealtimeEvent(
          ClinicalRealtimeEventType.error,
          error: error,
        ));
        _scheduleReconnect();
      }
      rethrow;
    } finally {
      _connecting = false;
    }
  }

  Future<void> _waitForIceGathering(RTCPeerConnection peerConnection) async {
    final current = await peerConnection.getIceGatheringState();
    if (current == RTCIceGatheringState.RTCIceGatheringStateComplete) return;

    final completer = Completer<void>();
    peerConnection.onIceGatheringState = (state) {
      if (state == RTCIceGatheringState.RTCIceGatheringStateComplete &&
          !completer.isCompleted) {
        completer.complete();
      }
    };
    await completer.future.timeout(
      _iceTimeout,
      onTimeout: () => throw TimeoutException('AI_REALTIME_ICE_TIMEOUT'),
    );
  }

  Future<void> _waitForDataChannel() async {
    if (_dataChannel?.state == RTCDataChannelState.RTCDataChannelOpen) return;
    final completer = _dataChannelOpenCompleter ??= Completer<void>();
    await completer.future.timeout(
      _channelTimeout,
      onTimeout: () => throw TimeoutException('AI_REALTIME_CHANNEL_TIMEOUT'),
    );
  }

  void _handleConnectionState(RTCPeerConnectionState state) {
    if (!_shouldStayConnected) return;
    if (state == RTCPeerConnectionState.RTCPeerConnectionStateFailed ||
        state == RTCPeerConnectionState.RTCPeerConnectionStateDisconnected ||
        state == RTCPeerConnectionState.RTCPeerConnectionStateClosed) {
      _scheduleReconnect();
    }
  }

  void _handleIceConnectionState(RTCIceConnectionState state) {
    if (!_shouldStayConnected) return;
    if (state == RTCIceConnectionState.RTCIceConnectionStateFailed ||
        state == RTCIceConnectionState.RTCIceConnectionStateDisconnected ||
        state == RTCIceConnectionState.RTCIceConnectionStateClosed) {
      _scheduleReconnect();
    }
  }

  void _scheduleReconnect() {
    if (_disposed || !_shouldStayConnected || _reconnectTimer != null) return;
    if (_reconnectAttempts >= _maximumReconnectAttempts) {
      _emit(ClinicalRealtimeEvent(
        ClinicalRealtimeEventType.error,
        error: StateError('AI_REALTIME_RECONNECT_EXHAUSTED'),
      ));
      return;
    }
    final exponent = _reconnectAttempts.clamp(0, 3).toInt();
    final delaySeconds = 1 << exponent;
    _reconnectAttempts++;
    _emit(const ClinicalRealtimeEvent(ClinicalRealtimeEventType.reconnecting));
    _reconnectTimer = Timer(Duration(seconds: delaySeconds), () async {
      _reconnectTimer = null;
      if (!_shouldStayConnected || _disposed) return;
      try {
        await _establishConnection();
      } catch (_) {
        // _establishConnection publie l'erreur et planifie la tentative suivante.
      }
    });
  }

  void _handleDataChannelMessage(RTCDataChannelMessage message) {
    if (message.isBinary) return;
    Map<String, dynamic> event;
    try {
      final decoded = jsonDecode(message.text);
      if (decoded is! Map) return;
      event = Map<String, dynamic>.from(decoded);
    } catch (_) {
      return;
    }

    final type = event['type']?.toString();
    switch (type) {
      case 'input_audio_buffer.speech_started':
        _emit(const ClinicalRealtimeEvent(
          ClinicalRealtimeEventType.speechStarted,
        ));
        break;
      case 'input_audio_buffer.speech_stopped':
        _emit(const ClinicalRealtimeEvent(
          ClinicalRealtimeEventType.speechStopped,
        ));
        break;
      case 'conversation.item.input_audio_transcription.delta':
        final delta = event['delta']?.toString();
        if (delta != null && delta.isNotEmpty) {
          _emit(ClinicalRealtimeEvent(
            ClinicalRealtimeEventType.transcriptDelta,
            text: delta,
            eventId: event['event_id']?.toString(),
            itemId: event['item_id']?.toString(),
          ));
        }
        break;
      case 'conversation.item.input_audio_transcription.completed':
        final transcript = event['transcript']?.toString().trim();
        if (transcript != null && transcript.isNotEmpty) {
          _emit(ClinicalRealtimeEvent(
            ClinicalRealtimeEventType.transcriptCompleted,
            text: transcript,
            eventId: event['event_id']?.toString(),
            itemId: event['item_id']?.toString(),
            confidence: _confidence(event['logprobs']),
          ));
        }
        final completer = _finalTranscriptCompleter;
        if (completer != null && !completer.isCompleted) completer.complete();
        break;
      case 'error':
        final rawError = event['error'];
        final message = rawError is Map
            ? rawError['message']?.toString()
            : event['message']?.toString();
        _emit(ClinicalRealtimeEvent(
          ClinicalRealtimeEventType.error,
          error: StateError(message ?? 'AI_REALTIME_ERROR'),
        ));
        break;
    }
  }

  double? _confidence(Object? rawLogprobs) {
    if (rawLogprobs is! List) return null;
    final probabilities = <double>[];
    for (final rawEntry in rawLogprobs) {
      if (rawEntry is! Map) continue;
      final raw = rawEntry['logprob'];
      if (raw is! num || !raw.toDouble().isFinite) continue;
      final bounded = raw.toDouble().clamp(-20.0, 0.0);
      probabilities.add(math.exp(bounded));
    }
    if (probabilities.isEmpty) return null;
    probabilities.sort();
    return probabilities[((probabilities.length - 1) * 0.2).floor()];
  }

  @override
  Future<void> finalize() async {
    _shouldStayConnected = false;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;

    final channel = _dataChannel;
    if (channel?.state == RTCDataChannelState.RTCDataChannelOpen) {
      _finalTranscriptCompleter = Completer<void>();
      await channel!.send(
        RTCDataChannelMessage(jsonEncode(<String, dynamic>{
          'type': 'input_audio_buffer.commit',
        })),
      );
      try {
        await _finalTranscriptCompleter!.future.timeout(
          _finalTranscriptTimeout,
        );
      } on TimeoutException {
        // Un tampon silencieux ne produit volontairement aucun tour final.
      }
    }
    await _teardownTransport();
  }

  @override
  Future<void> disconnect() async {
    _shouldStayConnected = false;
    _reconnectTimer?.cancel();
    _reconnectTimer = null;
    await _teardownTransport();
  }

  Future<void> _teardownTransport() async {
    final channel = _dataChannel;
    final peerConnection = _peerConnection;
    final stream = _mediaStream;
    _dataChannel = null;
    _peerConnection = null;
    _mediaStream = null;
    _dataChannelOpenCompleter = null;
    _finalTranscriptCompleter = null;

    try {
      await channel?.close();
    } catch (_) {
      // Fermeture best effort.
    }
    try {
      await peerConnection?.close();
      await peerConnection?.dispose();
    } catch (_) {
      // Fermeture best effort.
    }
    if (stream != null) await _disposeStream(stream);
  }

  Future<void> _disposeStream(MediaStream stream) async {
    for (final track in stream.getTracks()) {
      try {
        await track.stop();
      } catch (_) {
        // Fermeture best effort.
      }
    }
    try {
      await stream.dispose();
    } catch (_) {
      // Fermeture best effort.
    }
  }

  void _emit(ClinicalRealtimeEvent event) {
    if (!_disposed && !_eventsController.isClosed) {
      _eventsController.add(event);
    }
  }

  @override
  Future<void> dispose() async {
    if (_disposed) return;
    await disconnect();
    _disposed = true;
    await _eventsController.close();
  }
}

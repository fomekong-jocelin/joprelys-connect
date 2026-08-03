import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_client_providers.dart';
import '../domain/consultation_note.dart';
import '../domain/patient_vitals.dart';

final clinicalVoiceAiApiProvider = Provider<ClinicalVoiceAiGateway>((ref) {
  return ClinicalVoiceAiApi(ref.watch(apiClientProvider));
});

abstract interface class ClinicalVoiceAiGateway {
  Future<ClinicalAiState?> getSession(String visitId);

  Future<ClinicalAiState> startSession(
    String visitId,
    Map<String, String> draft, {
    required String locale,
  });

  Future<String> transcribeAudio(String visitId, Uint8List audioBytes);

  Future<void> savePendingTranscript(String visitId, String transcript);

  Future<ClinicalAiState> analyzeTranscript(String visitId, String transcript);

  Future<ClinicalAiState> decideProposal(
    String visitId,
    String revisionId,
    String proposalId,
    ClinicalAiDecision decision,
  );

  Future<ClinicalAiState> decideRevision(
    String visitId,
    String revisionId,
    ClinicalAiDecision decision,
  );

  Future<void> discardPendingTranscript(String visitId);
}

enum ClinicalAiDecision { accept, reject }

extension ClinicalAiDecisionWire on ClinicalAiDecision {
  String get wireValue => switch (this) {
    ClinicalAiDecision.accept => 'ACCEPT',
    ClinicalAiDecision.reject => 'REJECT',
  };
}

@immutable
final class ClinicalAiFieldProposal {
  const ClinicalAiFieldProposal({
    required this.id,
    required this.field,
    required this.operation,
    required this.previousValue,
    required this.proposedValue,
    required this.reason,
    required this.uncertainty,
    required this.status,
  });

  factory ClinicalAiFieldProposal.fromJson(Map<String, dynamic> json) {
    return ClinicalAiFieldProposal(
      id: json['id']?.toString() ?? '',
      field: json['field']?.toString() ?? '',
      operation: json['operation']?.toString() ?? 'SET',
      previousValue: json['previousValue']?.toString(),
      proposedValue: json['proposedValue']?.toString(),
      reason: json['reason']?.toString() ?? '',
      uncertainty: json['uncertainty']?.toString() ?? 'UNKNOWN',
      status: json['status']?.toString() ?? 'PENDING',
    );
  }

  final String id;
  final String field;
  final String operation;
  final String? previousValue;
  final String? proposedValue;
  final String reason;
  final String uncertainty;
  final String status;

  bool get isPending => status == 'PENDING';
  bool get isAccepted => status == 'ACCEPTED';
}

@immutable
final class ClinicalAiRevision {
  const ClinicalAiRevision({
    required this.id,
    required this.sequence,
    required this.status,
    required this.proposals,
  });

  factory ClinicalAiRevision.fromJson(Map<String, dynamic> json) {
    final rawProposals = json['proposals'];
    return ClinicalAiRevision(
      id: json['id']?.toString() ?? '',
      sequence: (json['sequence'] as num?)?.toInt() ?? 0,
      status: json['status']?.toString() ?? 'PENDING',
      proposals: rawProposals is List
          ? rawProposals
                .whereType<Map>()
                .map(
                  (item) => ClinicalAiFieldProposal.fromJson(
                    Map<String, dynamic>.from(item),
                  ),
                )
                .toList(growable: false)
          : const <ClinicalAiFieldProposal>[],
    );
  }

  final String id;
  final int sequence;
  final String status;
  final List<ClinicalAiFieldProposal> proposals;

  bool get isPending => status == 'PENDING';
  bool get hasPendingProposals => proposals.any((item) => item.isPending);
  bool get hasAcceptedProposals => proposals.any((item) => item.isAccepted);
}

@immutable
final class ClinicalAiState {
  const ClinicalAiState({
    required this.draft,
    required this.revisions,
    required this.transcript,
    required this.assistantMessage,
    required this.needsClarification,
    this.sessionId,
    this.visitId,
    this.sessionStatus,
    this.expiresAt,
    this.pendingTranscript,
    this.transcriptStatus = 'NONE',
  });

  factory ClinicalAiState.fromJson(Map<String, dynamic> json) {
    final rawDraft = json['draft'];
    final rawRevisions = json['revisions'];
    return ClinicalAiState(
      sessionId: json['sessionId']?.toString(),
      visitId: json['visitId']?.toString(),
      sessionStatus: json['status']?.toString(),
      expiresAt: json['expiresAt']?.toString(),
      draft: rawDraft is Map
          ? rawDraft.map(
              (key, value) => MapEntry(key.toString(), value?.toString() ?? ''),
            )
          : const <String, String>{},
      revisions: rawRevisions is List
          ? rawRevisions
                .whereType<Map>()
                .map(
                  (item) => ClinicalAiRevision.fromJson(
                    Map<String, dynamic>.from(item),
                  ),
                )
                .toList(growable: false)
          : const <ClinicalAiRevision>[],
      transcript: json['transcript']?.toString(),
      assistantMessage: json['assistantMessage']?.toString(),
      needsClarification: json['needsClarification'] == true,
      pendingTranscript: json['pendingTranscript']?.toString(),
      transcriptStatus: json['transcriptStatus']?.toString() ?? 'NONE',
    );
  }

  final String? sessionId;
  final String? visitId;
  final String? sessionStatus;
  final String? expiresAt;
  final Map<String, String> draft;
  final List<ClinicalAiRevision> revisions;
  final String? transcript;
  final String? assistantMessage;
  final bool needsClarification;
  final String? pendingTranscript;
  final String transcriptStatus;

  List<ClinicalAiRevision> get pendingRevisions =>
      revisions.where((revision) => revision.isPending).toList(growable: false);

  bool get hasPendingProposals =>
      pendingRevisions.any((revision) => revision.hasPendingProposals);

  bool get hasAcceptedChanges =>
      revisions.any((revision) => revision.hasAcceptedProposals);

  Map<String, String> get proposedDraft {
    final result = Map<String, String>.from(draft);
    for (final revision in pendingRevisions) {
      for (final proposal in revision.proposals.where(
        (item) => item.isPending,
      )) {
        if (proposal.operation == 'CLEAR') {
          result.remove(proposal.field);
        } else {
          final value = proposal.proposedValue?.trim();
          if (value != null && value.isNotEmpty) {
            result[proposal.field] = value;
          }
        }
      }
    }
    return result;
  }

  ConsultationNote noteFrom({bool includePending = false}) {
    final source = includePending ? proposedDraft : draft;
    return ConsultationNote(
      symptoms: _nonBlank(source['symptoms']),
      clinicalExam: _nonBlank(source['clinicalExam']),
      diagnosis: _nonBlank(source['diagnosis']),
      conclusion: _nonBlank(source['conclusion']),
      advice: _nonBlank(source['advice']),
      followUp: _nonBlank(source['followUp']),
      prescriptions: _nonBlank(source['prescription']),
      labOrders: _nonBlank(source['labOrders']),
    );
  }

  PatientVitals vitalsFrom({bool includePending = false}) {
    final source = includePending ? proposedDraft : draft;
    final raw = source['vitals'];
    if (raw == null || raw.trim().isEmpty) {
      return const PatientVitals();
    }
    try {
      final decoded = jsonDecode(raw);
      if (decoded is Map) {
        return PatientVitals.fromJson(Map<String, dynamic>.from(decoded));
      }
    } catch (_) {
      return const PatientVitals();
    }
    return const PatientVitals();
  }

  static String? _nonBlank(String? value) {
    final trimmed = value?.trim();
    return trimmed == null || trimmed.isEmpty ? null : trimmed;
  }
}

final class ClinicalVoiceAiApi implements ClinicalVoiceAiGateway {
  const ClinicalVoiceAiApi(this._client);

  final ApiClient _client;

  @override
  Future<ClinicalAiState?> getSession(String visitId) async {
    final response = await _client.get<dynamic>(
      '/api/ai/consultations/$visitId/session',
    );
    if (response.statusCode == 204) return null;

    // Une ouverture de l'assistant correspond à une nouvelle capture clinique.
    // On valide la réponse puis on supprime toute session précédente afin que le
    // brouillon SOAP courant soit la seule base du prochain POST /sessions.
    final existing = _optionalObjectFrom(
      response.data,
      'Invalid AI session response',
    );
    if (existing == null) return null;
    await _client.delete<void>('/api/ai/consultations/$visitId/session');
    return null;
  }

  @override
  Future<ClinicalAiState> startSession(
    String visitId,
    Map<String, String> draft, {
    required String locale,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/sessions',
      data: <String, dynamic>{'draft': draft, 'locale': locale},
    );
    return _stateFrom(response.data, 'Invalid AI session response');
  }

  @override
  Future<String> transcribeAudio(String visitId, Uint8List audioBytes) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/transcriptions/audio',
      data: audioBytes,
      headers: const <String, dynamic>{'Content-Type': 'audio/wav'},
    );
    final data = _objectFrom(
      response.data,
      'Invalid AI transcription response',
    );
    final transcript = data['transcript']?.toString().trim();
    if (transcript == null || transcript.isEmpty) {
      throw const FormatException('Empty AI transcription response');
    }
    return transcript;
  }

  @override
  Future<void> savePendingTranscript(String visitId, String transcript) async {
    final normalized = transcript.trim();
    if (normalized.isEmpty) {
      await discardPendingTranscript(visitId);
      return;
    }

    final response = await _client.put<dynamic>(
      '/api/ai/consultations/$visitId/transcriptions/pending',
      data: <String, dynamic>{'transcript': normalized},
    );
    _objectFrom(response.data, 'Invalid AI transcript save response');
  }

  @override
  Future<ClinicalAiState> analyzeTranscript(
    String visitId,
    String transcript,
  ) async {
    final normalized = transcript.trim();
    if (normalized.isEmpty) {
      throw const FormatException('Empty AI transcript analysis request');
    }

    // Le flux final doit reconstruire un diff SOAP depuis la transcription relue,
    // pas l'ajouter comme un nouveau message realtime à un ancien brouillon.
    await savePendingTranscript(visitId, normalized);
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/transcriptions/analyze',
      data: <String, dynamic>{'transcript': normalized},
    );
    return _stateFrom(response.data, 'Invalid AI analysis response');
  }

  @override
  Future<ClinicalAiState> decideProposal(
    String visitId,
    String revisionId,
    String proposalId,
    ClinicalAiDecision decision,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/revisions/$revisionId/proposals/$proposalId/decision',
      data: <String, dynamic>{'decision': decision.wireValue},
    );
    return _stateFrom(response.data, 'Invalid AI proposal decision response');
  }

  @override
  Future<ClinicalAiState> decideRevision(
    String visitId,
    String revisionId,
    ClinicalAiDecision decision,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/revisions/$revisionId/decision',
      data: <String, dynamic>{'decision': decision.wireValue},
    );
    return _stateFrom(response.data, 'Invalid AI revision decision response');
  }

  @override
  Future<void> discardPendingTranscript(String visitId) async {
    await _client.delete<void>(
      '/api/ai/consultations/$visitId/transcriptions/pending',
    );
  }

  ClinicalAiState _stateFrom(Object? data, String message) {
    return ClinicalAiState.fromJson(_objectFrom(data, message));
  }

  Map<String, dynamic> _objectFrom(Object? data, String message) {
    final decoded = _decodeResponse(data, message, allowEmpty: false);
    if (decoded is! Map) throw FormatException(message);
    return Map<String, dynamic>.from(decoded);
  }

  Map<String, dynamic>? _optionalObjectFrom(Object? data, String message) {
    final decoded = _decodeResponse(data, message, allowEmpty: true);
    if (decoded == null) return null;
    if (decoded is! Map) throw FormatException(message);
    return Map<String, dynamic>.from(decoded);
  }

  Object? _decodeResponse(
    Object? data,
    String message, {
    required bool allowEmpty,
  }) {
    if (data == null) {
      if (allowEmpty) return null;
      throw FormatException(message);
    }
    if (data is! String) return data;

    final body = data.trim();
    if (body.isEmpty) {
      if (allowEmpty) return null;
      throw FormatException(message);
    }

    try {
      final decoded = jsonDecode(body);
      if (decoded == null && allowEmpty) return null;
      return decoded;
    } on FormatException {
      throw FormatException(message);
    }
  }
}

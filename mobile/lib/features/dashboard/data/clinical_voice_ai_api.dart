import 'dart:convert';
import 'dart:typed_data';

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
  Future<ClinicalAiState> startSession(
    String visitId,
    Map<String, String> draft, {
    required String locale,
  });

  Future<String> transcribeAudio(String visitId, Uint8List audioBytes);

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
  });

  factory ClinicalAiState.fromJson(Map<String, dynamic> json) {
    final rawDraft = json['draft'];
    final rawRevisions = json['revisions'];
    return ClinicalAiState(
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
    );
  }

  final Map<String, String> draft;
  final List<ClinicalAiRevision> revisions;
  final String? transcript;
  final String? assistantMessage;
  final bool needsClarification;

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
    final data = response.data;
    if (data is! Map) {
      throw const FormatException('Invalid AI transcription response');
    }
    final transcript = data['transcript']?.toString().trim();
    if (transcript == null || transcript.isEmpty) {
      throw const FormatException('Empty AI transcription response');
    }
    return transcript;
  }

  @override
  Future<ClinicalAiState> analyzeTranscript(
    String visitId,
    String transcript,
  ) async {
    final response = await _client.post<dynamic>(
      '/api/ai/consultations/$visitId/transcriptions/analyze',
      data: <String, dynamic>{'transcript': transcript},
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
    if (data is! Map) {
      throw FormatException(message);
    }
    return ClinicalAiState.fromJson(Map<String, dynamic>.from(data));
  }
}

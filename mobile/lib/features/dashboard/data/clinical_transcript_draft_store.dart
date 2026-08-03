import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../application/clinical_voice_state.dart';

const int clinicalTranscriptDraftVersion = 3;

@immutable
final class ClinicalTranscriptDraft {
  const ClinicalTranscriptDraft({
    required this.segments,
    required this.partialTranscript,
    required this.partialOffset,
    required this.explicitlyCleared,
  });

  final List<ClinicalTranscriptSegment> segments;
  final String partialTranscript;
  final Duration partialOffset;
  final bool explicitlyCleared;

  bool get hasContent =>
      segments.any((segment) => segment.text.trim().isNotEmpty) ||
      partialTranscript.trim().isNotEmpty;

  Map<String, Object?> toJson() => <String, Object?>{
    'version': clinicalTranscriptDraftVersion,
    'explicitlyCleared': explicitlyCleared,
    'partialTranscript': partialTranscript,
    'partialOffsetMs': partialOffset.inMilliseconds,
    'segments': segments
        .map(
          (segment) => <String, Object?>{
            'id': segment.id,
            'offsetMs': segment.offset.inMilliseconds,
            'text': segment.text,
          },
        )
        .toList(growable: false),
  };

  factory ClinicalTranscriptDraft.fromJson(Map<String, dynamic> json) {
    final version = int.tryParse(json['version']?.toString() ?? '');
    if (version != clinicalTranscriptDraftVersion) {
      throw const FormatException('Unsupported clinical transcript draft version');
    }

    final rawSegments = json['segments'];
    final segments = <ClinicalTranscriptSegment>[];
    if (rawSegments is List) {
      for (var index = 0; index < rawSegments.length; index++) {
        final raw = rawSegments[index];
        if (raw is! Map) continue;
        final map = Map<String, dynamic>.from(raw);
        final text = map['text']?.toString().trim() ?? '';
        if (text.isEmpty) continue;
        final offsetMs = int.tryParse(map['offsetMs']?.toString() ?? '') ?? 0;
        final id = map['id']?.toString().trim();
        segments.add(
          ClinicalTranscriptSegment(
            id: id == null || id.isEmpty ? 'restored-local-$index' : id,
            offset: Duration(
              milliseconds: offsetMs.clamp(0, 359999000).toInt(),
            ),
            text: text,
          ),
        );
      }
    }

    final partialOffsetMs =
        int.tryParse(json['partialOffsetMs']?.toString() ?? '') ?? 0;
    return ClinicalTranscriptDraft(
      segments: List<ClinicalTranscriptSegment>.unmodifiable(segments),
      partialTranscript: json['partialTranscript']?.toString().trim() ?? '',
      partialOffset: Duration(
        milliseconds: partialOffsetMs.clamp(0, 359999000).toInt(),
      ),
      explicitlyCleared: json['explicitlyCleared'] == true,
    );
  }
}

String encodeClinicalTranscriptDraft(ClinicalTranscriptDraft draft) {
  return jsonEncode(draft.toJson());
}

ClinicalTranscriptDraft decodeClinicalTranscriptDraft(String encoded) {
  final decoded = jsonDecode(encoded);
  if (decoded is! Map) {
    throw const FormatException('Invalid clinical transcript draft');
  }
  return ClinicalTranscriptDraft.fromJson(Map<String, dynamic>.from(decoded));
}

abstract interface class ClinicalTranscriptDraftGateway {
  Future<ClinicalTranscriptDraft?> read(String visitId);
  Future<void> write(String visitId, ClinicalTranscriptDraft draft);
  Future<void> delete(String visitId);
}

final class SecureClinicalTranscriptDraftStore
    implements ClinicalTranscriptDraftGateway {
  const SecureClinicalTranscriptDraftStore({
    this._storage = const FlutterSecureStorage(),
  });

  static const String _keyPrefix = 'joprelys.clinical-voice.draft.v3.';
  static const String _legacyKeyPrefix = 'joprelys.clinical-voice.draft.v2.';
  final FlutterSecureStorage _storage;

  String _key(String visitId) => '$_keyPrefix${visitId.trim()}';
  String _legacyKey(String visitId) => '$_legacyKeyPrefix${visitId.trim()}';

  @override
  Future<ClinicalTranscriptDraft?> read(String visitId) async {
    // v2 drafts were written by the former stop-then-analyze lifecycle and may
    // already represent an applied or rejected dictation. They are deliberately
    // invalidated instead of being re-injected into the new durable v3 pipeline.
    try {
      await _storage.delete(key: _legacyKey(visitId));
    } catch (_) {
      // Legacy cleanup is best-effort; current-draft recovery remains available.
    }

    final encoded = await _storage.read(key: _key(visitId));
    if (encoded == null || encoded.trim().isEmpty) return null;
    try {
      return decodeClinicalTranscriptDraft(encoded);
    } on FormatException {
      await delete(visitId);
      return null;
    }
  }

  @override
  Future<void> write(String visitId, ClinicalTranscriptDraft draft) {
    return _storage.write(
      key: _key(visitId),
      value: encodeClinicalTranscriptDraft(draft),
    );
  }

  @override
  Future<void> delete(String visitId) {
    return _storage.delete(key: _key(visitId));
  }
}

import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import '../application/clinical_voice_state.dart';

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
    'version': 2,
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
            offset: Duration(milliseconds: offsetMs.clamp(0, 359999000)),
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
        milliseconds: partialOffsetMs.clamp(0, 359999000),
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
    FlutterSecureStorage storage = const FlutterSecureStorage(),
  }) : _storage = storage;

  static const String _keyPrefix = 'joprelys.clinical-voice.draft.v2.';
  final FlutterSecureStorage _storage;

  String _key(String visitId) => '$_keyPrefix${visitId.trim()}';

  @override
  Future<ClinicalTranscriptDraft?> read(String visitId) async {
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

import 'package:flutter/foundation.dart';

@immutable
final class ClinicalSpeechHypothesisMerge {
  const ClinicalSpeechHypothesisMerge({
    required this.text,
    required this.startsNewSegment,
  });

  final String text;
  final bool startsNewSegment;
}

/// Reconciles two speech-recognition hypotheses.
///
/// While [currentFinalized] is false, a radically different result is treated as
/// either a small recognizer correction or a reset to the latest words inside the
/// same listening window. Existing words are never discarded during such a reset.
/// Once a result has been finalized by the platform, a genuinely different result
/// starts the next spoken passage. Progressive results that extend, shorten or
/// overlap the current text always remain in the same passage.
ClinicalSpeechHypothesisMerge mergeClinicalSpeechHypothesis(
  String current,
  String incoming, {
  bool currentFinalized = true,
}) {
  final currentText = _normalizeWhitespace(current);
  final incomingText = _normalizeWhitespace(incoming);

  if (currentText.isEmpty) {
    return ClinicalSpeechHypothesisMerge(
      text: incomingText,
      startsNewSegment: false,
    );
  }
  if (incomingText.isEmpty) {
    return ClinicalSpeechHypothesisMerge(
      text: currentText,
      startsNewSegment: false,
    );
  }

  final currentFolded = _fold(currentText);
  final incomingFolded = _fold(incomingText);
  if (currentFolded == incomingFolded) {
    return ClinicalSpeechHypothesisMerge(
      text: incomingText,
      startsNewSegment: false,
    );
  }
  if (incomingFolded.startsWith(currentFolded)) {
    return ClinicalSpeechHypothesisMerge(
      text: incomingText,
      startsNewSegment: false,
    );
  }
  if (currentFolded.startsWith(incomingFolded)) {
    return ClinicalSpeechHypothesisMerge(
      text: currentText,
      startsNewSegment: false,
    );
  }

  final currentTokens = _tokens(currentText);
  final incomingTokens = _tokens(incomingText);
  final currentKeys = currentTokens.map(_fold).toList(growable: false);
  final incomingKeys = incomingTokens.map(_fold).toList(growable: false);
  final shortest = currentKeys.length < incomingKeys.length
      ? currentKeys.length
      : incomingKeys.length;

  var commonPrefix = 0;
  while (commonPrefix < shortest &&
      currentKeys[commonPrefix] == incomingKeys[commonPrefix]) {
    commonPrefix++;
  }
  final prefixRatio = shortest == 0 ? 0.0 : commonPrefix / shortest;
  if (commonPrefix >= 2 || prefixRatio >= 0.6) {
    return ClinicalSpeechHypothesisMerge(
      text: incomingTokens.length >= currentTokens.length
          ? incomingText
          : currentText,
      startsNewSegment: false,
    );
  }

  final overlap = _suffixPrefixOverlap(currentKeys, incomingKeys);
  final minimumUsefulOverlap = incomingKeys.length <= 3 ? 1 : 2;
  if (overlap >= minimumUsefulOverlap) {
    final merged = <String>[
      ...currentTokens,
      ...incomingTokens.skip(overlap),
    ].join(' ');
    return ClinicalSpeechHypothesisMerge(text: merged, startsNewSegment: false);
  }

  final approximateOverlap = _approximateSuffixPrefixOverlap(
    currentKeys,
    incomingKeys,
  );
  if (approximateOverlap > 0) {
    final merged = <String>[
      ...currentTokens,
      ...incomingTokens.skip(approximateOverlap),
    ].join(' ');
    return ClinicalSpeechHypothesisMerge(text: merged, startsNewSegment: false);
  }

  if (!currentFinalized) {
    if (currentTokens.length <= 2 && incomingTokens.length <= 2) {
      return ClinicalSpeechHypothesisMerge(
        text: incomingText,
        startsNewSegment: false,
      );
    }
    return ClinicalSpeechHypothesisMerge(
      text: '$currentText $incomingText',
      startsNewSegment: false,
    );
  }

  return ClinicalSpeechHypothesisMerge(
    text: incomingText,
    startsNewSegment: true,
  );
}

String stripCommittedClinicalTranscriptPrefix(
  String incoming,
  String committed, {
  bool allowRecentReplay = false,
}
) {
  final incomingText = _normalizeWhitespace(incoming);
  final committedText = _normalizeWhitespace(committed);
  if (incomingText.isEmpty || committedText.isEmpty) return incomingText;

  final incomingTokens = _tokens(incomingText);
  final committedTokens = _tokens(committedText);
  final incomingKeys = incomingTokens.map(_fold).toList(growable: false);
  final committedKeys = committedTokens.map(_fold).toList(growable: false);

  if (incomingTokens.length >= committedTokens.length) {
    var completePrefix = true;
    for (var index = 0; index < committedTokens.length; index++) {
      if (incomingKeys[index] != committedKeys[index]) {
        completePrefix = false;
        break;
      }
    }
    if (completePrefix) {
      return incomingTokens.skip(committedTokens.length).join(' ').trim();
    }
  }

  if (allowRecentReplay) {
    final exactOverlap = _suffixPrefixOverlap(committedKeys, incomingKeys);
    if (exactOverlap >= 3) {
      return incomingTokens.skip(exactOverlap).join(' ').trim();
    }

    final approximateOverlap = _approximateSuffixPrefixOverlap(
      committedKeys,
      incomingKeys,
    );
    if (approximateOverlap > 0) {
      return incomingTokens.skip(approximateOverlap).join(' ').trim();
    }
  }
  return incomingText;
}

String _normalizeWhitespace(String value) {
  return value.trim().replaceAll(RegExp(r'\s+'), ' ');
}

String _fold(String value) {
  return value
      .toLowerCase()
      .replaceAll(RegExp(r"[^\p{L}\p{N}']+", unicode: true), ' ')
      .trim()
      .replaceAll(RegExp(r'\s+'), ' ');
}

List<String> _tokens(String value) {
  final normalized = _normalizeWhitespace(value);
  return normalized.isEmpty
      ? const <String>[]
      : normalized.split(RegExp(r'\s+'));
}

int _suffixPrefixOverlap(List<String> left, List<String> right) {
  final limit = left.length < right.length ? left.length : right.length;
  for (var size = limit; size > 0; size--) {
    var matches = true;
    for (var index = 0; index < size; index++) {
      if (left[left.length - size + index] != right[index]) {
        matches = false;
        break;
      }
    }
    if (matches) return size;
  }
  return 0;
}

int _approximateSuffixPrefixOverlap(List<String> left, List<String> right) {
  final limit = left.length < right.length ? left.length : right.length;
  for (var size = limit; size >= 5; size--) {
    var matches = 0;
    for (var index = 0; index < size; index++) {
      if (left[left.length - size + index] == right[index]) matches++;
    }
    if (matches / size >= 0.8) return size;
  }
  return 0;
}

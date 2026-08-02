import '../../../core/network/api_exception.dart';

String clinicalVoiceUserMessage(Object error, {required String locale}) {
  final isFrench = locale.toLowerCase().startsWith('fr');

  if (error is ApiException) {
    return switch (error.kind) {
      ApiFailureKind.unauthenticated =>
        isFrench
            ? 'Votre session a expiré. Reconnectez-vous pour reprendre la dictée.'
            : 'Your session has expired. Sign in again to resume dictation.',
      ApiFailureKind.forbidden =>
        isFrench
            ? 'Vous n’avez pas l’autorisation d’utiliser l’assistant vocal pour cette consultation.'
            : 'You are not allowed to use the voice assistant for this consultation.',
      ApiFailureKind.notFound =>
        isFrench
            ? 'La consultation ou la session vocale n’est plus disponible.'
            : 'The consultation or voice session is no longer available.',
      ApiFailureKind.rateLimited =>
        isFrench
            ? 'L’assistant vocal est momentanément très sollicité. Réessayez dans un instant.'
            : 'The voice assistant is temporarily busy. Try again in a moment.',
      ApiFailureKind.timeout =>
        isFrench
            ? 'L’assistant vocal met trop de temps à répondre. Réessayez.'
            : 'The voice assistant is taking too long to respond. Try again.',
      ApiFailureKind.noConnection =>
        isFrench
            ? 'Connexion indisponible. Vérifiez votre réseau puis réessayez.'
            : 'No connection. Check your network and try again.',
      ApiFailureKind.server =>
        isFrench
            ? 'L’assistant vocal est momentanément indisponible. Réessayez.'
            : 'The voice assistant is temporarily unavailable. Try again.',
      ApiFailureKind.malformedResponse =>
        isFrench
            ? 'La réponse de l’assistant vocal est temporairement illisible. Réessayez.'
            : 'The voice assistant returned an unreadable response. Try again.',
      ApiFailureKind.validation || ApiFailureKind.conflict =>
        isFrench
            ? 'La demande vocale n’a pas pu être traitée. Vérifiez la consultation puis réessayez.'
            : 'The voice request could not be processed. Check the consultation and try again.',
      ApiFailureKind.cancelled =>
        isFrench
            ? 'L’opération vocale a été interrompue.'
            : 'The voice operation was interrupted.',
      ApiFailureKind.unknown =>
        isFrench
            ? 'Impossible de poursuivre la dictée pour le moment. Réessayez.'
            : 'Dictation cannot continue right now. Try again.',
    };
  }

  if (error is FormatException) {
    return isFrench
        ? 'La réponse de l’assistant vocal est temporairement illisible. Réessayez.'
        : 'The voice assistant returned an unreadable response. Try again.';
  }

  final raw = error.toString();
  if (raw.contains('MICROPHONE_PERMISSION_DENIED') ||
      raw.contains('error_permission')) {
    return isFrench
        ? 'Autorisez l’accès au microphone pour démarrer la dictée.'
        : 'Allow microphone access to start dictation.';
  }
  if (raw.contains('SPEECH_RECOGNITION_UNAVAILABLE')) {
    return isFrench
        ? 'La reconnaissance vocale n’est pas disponible sur cet appareil.'
        : 'Speech recognition is not available on this device.';
  }
  if (raw.contains('AI_AUDIO_SILENCE') ||
      raw.contains('error_speech_timeout') ||
      raw.contains('error_no_match')) {
    return isFrench
        ? 'Aucune parole exploitable n’a été détectée. Reprenez la dictée près du microphone.'
        : 'No usable speech was detected. Resume dictation near the microphone.';
  }
  if (raw.contains('AI_TRANSCRIPT_REVIEW_REQUIRED')) {
    return isFrench
        ? 'Une transcription est déjà en attente. Relisez-la avant de reprendre le micro.'
        : 'A transcript is already waiting. Review it before recording again.';
  }

  return isFrench
      ? 'Impossible de poursuivre la dictée pour le moment. Réessayez.'
      : 'Dictation cannot continue right now. Try again.';
}

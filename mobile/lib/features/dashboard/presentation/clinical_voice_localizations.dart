import '../../../l10n/app_localizations.dart';

extension ClinicalVoiceLocalizations on AppLocalizations {
  bool get _voiceIsFrench => localeName.startsWith('fr');

  String get voiceCaptureStep => _voiceIsFrench ? '1. Dictée' : '1. Dictation';
  String get voiceReviewStep =>
      _voiceIsFrench ? '2. Synthèse clinique' : '2. Clinical summary';

  String get voiceCaptureBadge =>
      _voiceIsFrench ? 'Écoute clinique' : 'Clinical listening';
  String get voiceCaptureStatus => _voiceIsFrench
      ? 'Parlez naturellement, la transcription apparaît immédiatement.'
      : 'Speak naturally. The transcript appears immediately.';
  String get voiceCapturePausedStatus => _voiceIsFrench
      ? 'Relisez les segments avant de lancer l’analyse.'
      : 'Review the segments before starting the analysis.';
  String get voiceCaptureTip => _voiceIsFrench
      ? 'L’écran reste actif pendant l’écoute. Chaque pause crée un segment horodaté.'
      : 'The screen stays awake while listening. Each pause creates a timestamped segment.';

  String get voiceSegmentsTitle =>
      _voiceIsFrench ? 'Transcription segmentée' : 'Segmented transcript';
  String get voiceSegmentsSubtitle => _voiceIsFrench
      ? 'Corrigez ou supprimez chaque passage avant l’analyse.'
      : 'Edit or delete each passage before analysis.';
  String get voiceSegmentsEmptyTitle => _voiceIsFrench
      ? 'La dictée apparaîtra ici'
      : 'Your dictation will appear here';
  String get voiceSegmentsEmptyBody => _voiceIsFrench
      ? 'Commencez à parler. Les phrases seront séparées automatiquement selon vos pauses.'
      : 'Start speaking. Sentences will be separated automatically when you pause.';
  String get voiceLiveSegment =>
      _voiceIsFrench ? 'En cours de transcription' : 'Transcribing now';
  String voiceSegmentLabel(int index) =>
      _voiceIsFrench ? 'Segment $index' : 'Segment $index';
  String get voiceEditSegment => _voiceIsFrench ? 'Corriger' : 'Edit';
  String get voiceSaveSegment =>
      _voiceIsFrench ? 'Enregistrer' : 'Save changes';
  String get voiceDeleteSegment => _voiceIsFrench ? 'Supprimer' : 'Delete';

  String get voiceClearAll => _voiceIsFrench ? 'Tout supprimer' : 'Delete all';
  String get voiceClearConfirmTitle => _voiceIsFrench
      ? 'Supprimer toute la transcription ?'
      : 'Delete the full transcript?';
  String get voiceClearConfirmBody => _voiceIsFrench
      ? 'Tous les segments saisis seront supprimés. Cette action est irréversible.'
      : 'All captured segments will be deleted. This action cannot be undone.';
  String get voiceCancel => _voiceIsFrench ? 'Annuler' : 'Cancel';
  String get voiceConfirmDelete => _voiceIsFrench ? 'Supprimer' : 'Delete';
  String get voiceAnalyzeAction => _voiceIsFrench
      ? 'Analyser la transcription relue'
      : 'Analyze reviewed transcript';
  String get voiceAnalyzingTitle => _voiceIsFrench
      ? 'Reformulation et extraction en cours'
      : 'Rewriting and extraction in progress';
  String get voiceAnalyzingBody => _voiceIsFrench
      ? 'L’IA reformule uniquement si nécessaire, puis classe les informations dans les champs cliniques adaptés.'
      : 'AI rewrites only when needed, then places information in the appropriate clinical fields.';

  String get voiceReviewTitle => _voiceIsFrench
      ? 'Synthèse clinique proposée'
      : 'Proposed clinical summary';
  String get voiceReviewSubtitle => _voiceIsFrench
      ? 'Validez les propositions avant tout remplissage de la consultation.'
      : 'Review the proposals before anything is applied to the consultation.';
  String get voiceReviewBadge =>
      _voiceIsFrench ? 'Revue clinique' : 'Clinical review';
  String get voiceDecisionRequired =>
      _voiceIsFrench ? 'Décision requise' : 'Decision required';
  String get voiceReviewComplete =>
      _voiceIsFrench ? 'Synthèse vérifiée' : 'Summary reviewed';
  String get voiceClarificationRequired => _voiceIsFrench
      ? 'La synthèse reste incomplète. Corrigez la transcription puis relancez l’analyse.'
      : 'The summary is incomplete. Correct the transcript and run the analysis again.';

  String get voiceApplyTitle => _voiceIsFrench
      ? 'Appliquer les éléments vérifiés ?'
      : 'Apply reviewed items?';
  String get voiceApplyBody => _voiceIsFrench
      ? 'Seuls les éléments relus et confirmés seront placés dans les champs correspondants. Les autres données resteront inchangées.'
      : 'Only reviewed and confirmed items will be placed in the matching fields. Other data will remain unchanged.';
  String get voiceApplyAction => _voiceIsFrench
      ? 'Appliquer aux bonnes rubriques'
      : 'Apply to matching fields';
  String get voiceApplyConfirm => _voiceIsFrench ? 'Appliquer' : 'Apply';

  String get voiceSecureProcessing =>
      _voiceIsFrench ? 'Traitement sécurisé' : 'Secure processing';
  String get voiceTranscriptReady =>
      _voiceIsFrench ? 'Transcription à vérifier' : 'Transcript to review';
  String get voiceIdleBadge =>
      _voiceIsFrench ? 'Assistant en pause' : 'Assistant paused';
  String get voiceStartNewDictation => _voiceIsFrench
      ? 'Démarrez une nouvelle dictée clinique.'
      : 'Start a new clinical dictation.';
  String get voicePrivacyNotice => _voiceIsFrench
      ? 'Aucune donnée clinique n’est appliquée sans validation explicite.'
      : 'No clinical data is applied without explicit approval.';
}

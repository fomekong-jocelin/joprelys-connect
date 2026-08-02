import '../../../l10n/app_localizations.dart';

extension ClinicalVoiceLocalizations on AppLocalizations {
  bool get _voiceIsFrench => localeName.startsWith('fr');

  String get voiceCaptureStep => _voiceIsFrench ? '1. Dictée' : '1. Dictation';
  String get voiceReviewStep =>
      _voiceIsFrench ? '2. Synthèse clinique' : '2. Clinical summary';

  String get voiceCaptureBadge =>
      _voiceIsFrench ? 'Écoute clinique' : 'Clinical listening';
  String get voiceCaptureStatus => _voiceIsFrench
      ? 'Parlez naturellement, à une distance normale du téléphone.'
      : 'Speak naturally at a normal distance from the phone.';
  String get voiceCapturePausedStatus => _voiceIsFrench
      ? 'Transcription prête : corrigez, reprenez ou lancez l’analyse.'
      : 'Transcript ready: edit it, resume, or start the analysis.';
  String get voiceCaptureTip => _voiceIsFrench
      ? 'Le passage en cours reste unique pendant que le moteur affine les mots. Il est conservé localement sans requête réseau.'
      : 'The current passage stays unique while speech recognition refines the words. It is kept locally without a network request.';
  String get voiceResumeDictation => _voiceIsFrench ? 'Reprendre' : 'Resume';

  String get voiceSegmentsTitle =>
      _voiceIsFrench ? 'Transcription' : 'Transcript';
  String get voiceSegmentsSubtitle => _voiceIsFrench
      ? 'Chaque passage peut être relu, corrigé ou supprimé avant l’analyse.'
      : 'Each passage can be reviewed, edited, or deleted before analysis.';
  String get voiceSegmentsEmptyTitle => _voiceIsFrench
      ? 'La dictée apparaîtra ici'
      : 'Your dictation will appear here';
  String get voiceSegmentsEmptyBody => _voiceIsFrench
      ? 'Commencez à parler. Le passage en cours sera mis à jour sans créer de doublons.'
      : 'Start speaking. The current passage will be updated without creating duplicates.';
  String get voiceLiveSegment =>
      _voiceIsFrench ? 'Paroles en cours' : 'Current speech';
  String voiceSegmentLabel(int index) =>
      _voiceIsFrench ? 'Passage $index' : 'Passage $index';
  String get voiceEditSegment => _voiceIsFrench ? 'Corriger' : 'Edit';
  String get voiceSaveSegment =>
      _voiceIsFrench ? 'Enregistrer' : 'Save changes';
  String get voiceDeleteSegment => _voiceIsFrench ? 'Supprimer' : 'Delete';

  String get voiceClearAll => _voiceIsFrench ? 'Tout supprimer' : 'Delete all';
  String get voiceClearConfirmTitle => _voiceIsFrench
      ? 'Supprimer toute la transcription ?'
      : 'Delete the full transcript?';
  String get voiceClearConfirmBody => _voiceIsFrench
      ? 'Tous les passages seront supprimés du stockage sécurisé de cet appareil. Ils ne réapparaîtront pas à la réouverture.'
      : 'All passages will be removed from secure storage on this device. They will not return when reopened.';
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

  String get voiceTranscriptSaving => _voiceIsFrench
      ? 'Enregistrement sécurisé du brouillon…'
      : 'Saving the draft securely…';
  String get voiceTranscriptSaved => _voiceIsFrench
      ? 'Brouillon enregistré sur cet appareil.'
      : 'Draft saved on this device.';
  String get voiceTranscriptSaveFailed => _voiceIsFrench
      ? 'Le brouillon local n’a pas pu être enregistré. Il reste affiché : réessayez avant de fermer.'
      : 'The local draft could not be saved. It remains visible: retry before closing.';
  String get voiceRetrySave =>
      _voiceIsFrench ? 'Réessayer l’enregistrement' : 'Retry saving';

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
      ? 'Démarrez ou reprenez la dictée clinique.'
      : 'Start or resume the clinical dictation.';
  String get voicePrivacyNotice => _voiceIsFrench
      ? 'Aucune donnée clinique n’est appliquée sans validation explicite.'
      : 'No clinical data is applied without explicit approval.';
}

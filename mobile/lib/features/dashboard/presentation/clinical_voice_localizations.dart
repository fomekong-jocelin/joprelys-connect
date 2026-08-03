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
      ? 'Dictée enregistrée : relisez-la avant la reconstruction finale.'
      : 'Dictation saved: review it before the final rebuild.';
  String get voiceCaptureTip => _voiceIsFrench
      ? 'Le micro reste actif pendant que les passages finalisés sont enregistrés puis structurés en arrière-plan.'
      : 'The microphone stays active while finalized passages are saved and structured in the background.';
  String get voiceResumeDictation => _voiceIsFrench ? 'Reprendre' : 'Resume';
  String get voiceSaveDictation =>
      _voiceIsFrench ? 'Enregistrer la dictée' : 'Save dictation';

  String get voiceSegmentsTitle =>
      _voiceIsFrench ? 'Transcription' : 'Transcript';
  String get voiceSegmentsSubtitle => _voiceIsFrench
      ? 'Chaque passage peut être relu, corrigé ou supprimé avant la reconstruction finale.'
      : 'Each passage can be reviewed, edited, or deleted before the final rebuild.';
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
      ? 'Tous les passages seront retirés du stockage sécurisé de cet appareil et du brouillon serveur récupérable. Ils ne réapparaîtront pas à la réouverture.'
      : 'All passages will be removed from secure storage on this device and from the recoverable server draft. They will not return when reopened.';
  String get voiceCancel => _voiceIsFrench ? 'Annuler' : 'Cancel';
  String get voiceConfirmDelete => _voiceIsFrench ? 'Supprimer' : 'Delete';
  String get voiceAnalyzeAction => _voiceIsFrench
      ? 'Reconstruire la synthèse finale'
      : 'Build final clinical summary';
  String get voiceAnalyzingTitle => _voiceIsFrench
      ? 'Reformulation et extraction en cours'
      : 'Rewriting and extraction in progress';
  String get voiceAnalyzingBody => _voiceIsFrench
      ? 'L’IA reformule de façon contrôlée, sans inventer de fait, puis classe les informations dans les champs cliniques adaptés.'
      : 'AI performs controlled rewriting without inventing facts, then places information in the appropriate clinical fields.';
  String get voiceProgressivePreviewTitle => _voiceIsFrench
      ? 'Synthèse actualisée pendant l’écoute'
      : 'Summary updated while listening';
  String get voiceProgressivePreviewBody => _voiceIsFrench
      ? 'Les passages déjà finalisés sont sécurisés et structurés sans couper le microphone.'
      : 'Finalized passages are secured and structured without stopping the microphone.';

  String get voiceTranscriptSaving => _voiceIsFrench
      ? 'Synchronisation sécurisée du brouillon…'
      : 'Securely synchronizing the draft…';
  String get voiceTranscriptSaved => _voiceIsFrench
      ? 'Brouillon sécurisé sur l’appareil et synchronisé avec le serveur.'
      : 'Draft secured on the device and synchronized with the server.';
  String get voiceTranscriptSaveFailed => _voiceIsFrench
      ? 'La synchronisation n’a pas abouti. La dictée reste disponible localement : réessayez avant de fermer.'
      : 'Synchronization did not complete. The dictation remains available locally: retry before closing.';
  String get voiceRetrySave =>
      _voiceIsFrench ? 'Réessayer la synchronisation' : 'Retry synchronization';

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
      ? 'La synthèse reste incomplète. Corrigez la transcription puis relancez la reconstruction.'
      : 'The summary is incomplete. Correct the transcript and run the rebuild again.';

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

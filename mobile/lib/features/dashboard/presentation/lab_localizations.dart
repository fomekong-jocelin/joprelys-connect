import '../../../l10n/app_localizations.dart';

extension LabLocalizations on AppLocalizations {
  bool get _labFrench => localeName.startsWith('fr');

  String get labSectionTitle => _labFrench ? 'Examens' : 'Examinations';
  String get labCreateOrder =>
      _labFrench ? 'Prescrire un examen' : 'Request examination';
  String get labCreateTitle =>
      _labFrench ? 'Nouvelle demande d’examens' : 'New examination request';
  String get labCreateSubmit => _labFrench ? 'Prescrire' : 'Request';
  String get labCreateSuccess => _labFrench
      ? 'Demande d’examens enregistrée'
      : 'Examination request saved';
  String get labCreateError => _labFrench
      ? 'La demande ne peut pas être enregistrée pour le moment.'
      : 'The request cannot be saved right now.';
  String get labEmptyTitle => _labFrench ? 'Aucun examen' : 'No examinations';
  String get labEmptyBody => _labFrench
      ? 'Aucune demande d’examen n’est enregistrée pour ce patient.'
      : 'No examination request is recorded for this patient.';
  String get labActive => _labFrench ? 'En cours' : 'In progress';
  String get labHistory => _labFrench ? 'Historique' : 'History';
  String get labResults => _labFrench ? 'Résultats' : 'Results';
  String get labResult => _labFrench ? 'Résultat' : 'Result';
  String get labRequestLevelResults => _labFrench
      ? 'Résultats non rattachés à un examen précis'
      : 'Results not linked to a specific examination';

  String labResultsCount(int count) {
    if (_labFrench) {
      return count <= 1 ? '$count résultat' : '$count résultats';
    }
    return count == 1 ? '1 result' : '$count results';
  }

  String labExamsCount(int count) {
    if (_labFrench) {
      return count <= 1 ? '$count examen' : '$count examens';
    }
    return count == 1 ? '1 examination' : '$count examinations';
  }

  String labItemProgress(int completed, int total) {
    if (_labFrench) {
      return '$completed/$total finalisés';
    }
    return '$completed/$total completed';
  }

  String get labOrderNumber => _labFrench ? 'Demande' : 'Request';
  String get labExamType => _labFrench ? 'Type d’examen' : 'Examination type';
  String get labExamTypeRequired => _labFrench
      ? 'Sélectionnez un type d’examen.'
      : 'Select an examination type.';
  String get labRequestedExams =>
      _labFrench ? 'Examens prescrits' : 'Requested examinations';
  String get labRequestedExamsHint => _labFrench
      ? 'Un examen par ligne, ex. NFS\nCRP'
      : 'One examination per line, e.g. CBC\nCRP';
  String get labRequestedExamsRequired => _labFrench
      ? 'Ajoutez au moins un examen.'
      : 'Add at least one examination.';
  String get labReason => _labFrench ? 'Motif' : 'Reason';
  String get labReasonHint => _labFrench
      ? 'Contexte clinique utile au laboratoire'
      : 'Clinical context useful to the laboratory';
  String get labPriority => _labFrench ? 'Priorité' : 'Priority';
  String get labPriorityNormal => _labFrench ? 'Normale' : 'Normal';
  String get labPriorityUrgent => _labFrench ? 'Urgente' : 'Urgent';
  String get labPractitioner => _labFrench ? 'Prescripteur' : 'Requester';
  String get labRequestedAt => _labFrench ? 'Prescrit le' : 'Requested on';
  String get labDetails => _labFrench ? 'Voir le détail' : 'View details';
  String get labHideDetails => _labFrench ? 'Réduire' : 'Collapse';
  String get labClinicalDetails =>
      _labFrench ? 'Détails cliniques' : 'Clinical details';
  String get labTimeline => _labFrench ? 'Suivi' : 'Progress';
  String get labReferenceRange =>
      _labFrench ? 'Valeurs de référence' : 'Reference range';
  String get labInterpretation =>
      _labFrench ? 'Interprétation' : 'Interpretation';
  String get labConclusion => _labFrench ? 'Conclusion' : 'Conclusion';
  String get labComment => _labFrench ? 'Commentaire' : 'Comment';
  String get labValidator => _labFrench ? 'Validé par' : 'Validated by';
  String get labSampleCollectedAt =>
      _labFrench ? 'Prélèvement' : 'Sample collected';
  String get labResultAt =>
      _labFrench ? 'Résultat disponible' : 'Result available';
  String get labValidatedAt => _labFrench ? 'Validation' : 'Validation';
  String get labDownloadPdf =>
      _labFrench ? 'Télécharger le PDF' : 'Download PDF';
  String get labPdfSaved => _labFrench
      ? 'PDF enregistré dans les fichiers de l’application.'
      : 'PDF saved in the app files.';
  String get labPdfError => _labFrench
      ? 'Le PDF ne peut pas être téléchargé pour le moment.'
      : 'The PDF cannot be downloaded right now.';
  String get labStatusAction =>
      _labFrench ? 'Mettre à jour le suivi' : 'Update progress';
  String get labMarkSampleCollected =>
      _labFrench ? 'Marquer prélevé' : 'Mark sample collected';
  String get labStartProcessing =>
      _labFrench ? 'Démarrer l’analyse' : 'Start processing';
  String get labCancelOrder =>
      _labFrench ? 'Annuler la demande' : 'Cancel request';
  String get labCancelExam => _labFrench ? 'Annuler' : 'Cancel';
  String labCancelExamTitle(String examName) =>
      _labFrench ? 'Annuler « $examName » ?' : 'Cancel “$examName”?';
  String get labCancelExamBody => _labFrench
      ? 'Seul cet examen sera annulé. Les autres examens de la demande continueront leur parcours.'
      : 'Only this examination will be cancelled. The other examinations in the request will continue.';
  String get labCancelTitle => _labFrench
      ? 'Annuler cette demande d’examens ?'
      : 'Cancel this examination request?';
  String get labCancelBody => _labFrench
      ? 'La demande restera visible dans l’historique avec le statut annulé.'
      : 'The request will remain visible in history with a cancelled status.';
  String get labConfirm => _labFrench ? 'Confirmer' : 'Confirm';
  String get labBack => _labFrench ? 'Retour' : 'Back';
  String get labStatusUpdateError => _labFrench
      ? 'Le statut ne peut pas être mis à jour pour le moment.'
      : 'The status cannot be updated right now.';
  String get labStatusUpdated =>
      _labFrench ? 'Suivi mis à jour' : 'Progress updated';
  String get labUnknown => _labFrench ? 'Non renseigné' : 'Not provided';
  String get labNoStructuredResults => _labFrench
      ? 'Le résultat est disponible mais ne contient pas de valeur structurée.'
      : 'The result is available but contains no structured value.';
  String get labOrphanResult =>
      _labFrench ? 'Résultat non rattaché' : 'Unlinked result';
  String get labPaymentPending =>
      _labFrench ? 'Paiement en attente' : 'Awaiting payment';
  String get labPaid => _labFrench ? 'Payé' : 'Paid';
  String get labStepRequested => _labFrench ? 'Prescrit' : 'Requested';
  String get labStepSample => _labFrench ? 'Prélevé' : 'Collected';
  String get labStepProcessing => _labFrench ? 'Analyse' : 'Processing';
  String get labStepResult => _labFrench ? 'Résultat' : 'Result';
  String get labStepValidated => _labFrench ? 'Validé' : 'Validated';

  String labStatusLabel(String raw) {
    return switch (raw.trim().toUpperCase()) {
      'REQUESTED' => _labFrench ? 'Prescrit' : 'Requested',
      'AWAITING_PAYMENT' =>
        _labFrench ? 'Paiement en attente' : 'Awaiting payment',
      'PAID' => _labFrench ? 'Payé' : 'Paid',
      'SAMPLE_COLLECTED' => _labFrench ? 'Prélevé' : 'Sample collected',
      'IN_PROGRESS' => _labFrench ? 'En analyse' : 'In progress',
      'RESULT_AVAILABLE' =>
        _labFrench ? 'Résultat disponible' : 'Result available',
      'VALIDATED' => _labFrench ? 'Validé' : 'Validated',
      'CANCELLED' || 'CANCELED' => _labFrench ? 'Annulé' : 'Cancelled',
      'DRAFT' => _labFrench ? 'Brouillon' : 'Draft',
      final value when value.isNotEmpty => value,
      _ => _labFrench ? 'Statut inconnu' : 'Unknown status',
    };
  }

  String labExamTypeLabel(String raw) {
    return switch (raw.trim().toUpperCase()) {
      'LABORATOIRE' => _labFrench ? 'Laboratoire' : 'Laboratory',
      'IMAGERIE' => _labFrench ? 'Imagerie' : 'Imaging',
      'CARDIOLOGIE' => _labFrench ? 'Cardiologie' : 'Cardiology',
      'ORL' => _labFrench ? 'ORL' : 'ENT',
      'OPHTALMOLOGIE' => _labFrench ? 'Ophtalmologie' : 'Ophthalmology',
      'AUTRE' => _labFrench ? 'Autre' : 'Other',
      final value when value.isNotEmpty => value,
      _ => labUnknown,
    };
  }
}

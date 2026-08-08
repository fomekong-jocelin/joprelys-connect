import '../../../l10n/app_localizations.dart';

extension PrescriptionLocalizations on AppLocalizations {
  bool get _prescriptionFrench => localeName.startsWith('fr');

  String get prescriptionTitle =>
      _prescriptionFrench ? 'Ordonnance' : 'Prescription';
  String get prescriptionCreate =>
      _prescriptionFrench ? 'Créer une ordonnance' : 'Create prescription';
  String get prescriptionEmptyTitle =>
      _prescriptionFrench ? 'Aucune ordonnance' : 'No prescription';
  String get prescriptionEmptyBody => _prescriptionFrench
      ? 'Aucun traitement n’est associé à cette consultation.'
      : 'No treatment is attached to this consultation.';
  String get prescriptionAddMedication =>
      _prescriptionFrench ? 'Ajouter un médicament' : 'Add medication';
  String get prescriptionMedication =>
      _prescriptionFrench ? 'Médicament' : 'Medication';
  String prescriptionMedicationIndex(int index) =>
      _prescriptionFrench ? 'Médicament $index' : 'Medication $index';
  String get prescriptionDrugName =>
      _prescriptionFrench ? 'Nom du médicament' : 'Medication name';
  String get prescriptionDrugNameHint =>
      _prescriptionFrench ? 'Ex. Amoxicilline' : 'e.g. Amoxicillin';
  String get prescriptionDosage => _prescriptionFrench ? 'Dosage' : 'Dosage';
  String get prescriptionDosageHint =>
      _prescriptionFrench ? 'Ex. 500 mg' : 'e.g. 500 mg';
  String get prescriptionPosology =>
      _prescriptionFrench ? 'Posologie' : 'Directions';
  String get prescriptionPosologyHint => _prescriptionFrench
      ? 'Ex. 1 comprimé matin et soir'
      : 'e.g. 1 tablet morning and evening';
  String get prescriptionDuration => _prescriptionFrench ? 'Durée' : 'Duration';
  String get prescriptionQuantity =>
      _prescriptionFrench ? 'Quantité' : 'Quantity';
  String get prescriptionMoreDetails =>
      _prescriptionFrench ? 'Détails complémentaires' : 'Additional details';
  String get prescriptionForm => _prescriptionFrench ? 'Forme' : 'Form';
  String get prescriptionRoute => _prescriptionFrench ? 'Voie' : 'Route';
  String get prescriptionFrequency =>
      _prescriptionFrench ? 'Fréquence' : 'Frequency';
  String get prescriptionInstructions =>
      _prescriptionFrench ? 'Instructions' : 'Instructions';
  String get prescriptionSubstitutionAllowed =>
      _prescriptionFrench ? 'Substitution autorisée' : 'Substitution allowed';
  String get prescriptionRemoveMedication =>
      _prescriptionFrench ? 'Retirer' : 'Remove';
  String get prescriptionSaveDraft =>
      _prescriptionFrench ? 'Enregistrer' : 'Save draft';
  String get prescriptionFinalize =>
      _prescriptionFrench ? 'Finaliser' : 'Finalize';
  String get prescriptionTransmit =>
      _prescriptionFrench ? 'Transmettre' : 'Transmit';
  String get prescriptionCancel => _prescriptionFrench ? 'Annuler' : 'Cancel';
  String get prescriptionClose => _prescriptionFrench ? 'Fermer' : 'Close';
  String get prescriptionDraftSaved =>
      _prescriptionFrench ? 'Brouillon enregistré' : 'Draft saved';
  String get prescriptionFinalized =>
      _prescriptionFrench ? 'Ordonnance finalisée' : 'Prescription finalized';
  String get prescriptionCancelled =>
      _prescriptionFrench ? 'Ordonnance annulée' : 'Prescription cancelled';
  String get prescriptionTransmitted =>
      _prescriptionFrench ? 'Ordonnance transmise' : 'Prescription transmitted';
  String get prescriptionDrugRequired => _prescriptionFrench
      ? 'Indiquez le nom du médicament.'
      : 'Enter the medication name.';
  String get prescriptionDosageRequiredToFinalize => _prescriptionFrench
      ? 'Complétez le dosage avant de finaliser.'
      : 'Complete the dosage before finalizing.';
  String get prescriptionAtLeastOneMedication => _prescriptionFrench
      ? 'Ajoutez au moins un médicament.'
      : 'Add at least one medication.';
  String get prescriptionLoadError => _prescriptionFrench
      ? 'L’ordonnance ne peut pas être chargée pour le moment.'
      : 'The prescription cannot be loaded right now.';
  String get prescriptionSaveError => _prescriptionFrench
      ? 'L’ordonnance ne peut pas être enregistrée pour le moment.'
      : 'The prescription cannot be saved right now.';
  String get prescriptionFinalizeTitle => _prescriptionFrench
      ? 'Finaliser l’ordonnance ?'
      : 'Finalize prescription?';
  String get prescriptionFinalizeBody => _prescriptionFrench
      ? 'Après finalisation, les médicaments ne pourront plus être modifiés.'
      : 'After finalization, medications can no longer be edited.';
  String get prescriptionCancelTitle => _prescriptionFrench
      ? 'Annuler cette ordonnance ?'
      : 'Cancel this prescription?';
  String get prescriptionCancelBody => _prescriptionFrench
      ? 'L’ordonnance restera visible dans l’historique avec le statut annulé.'
      : 'The prescription will remain in history with a cancelled status.';
  String get prescriptionConfirm =>
      _prescriptionFrench ? 'Confirmer' : 'Confirm';
  String get prescriptionBack => _prescriptionFrench ? 'Retour' : 'Back';
  String get prescriptionStatusDraft =>
      _prescriptionFrench ? 'Brouillon' : 'Draft';
  String get prescriptionStatusActive =>
      _prescriptionFrench ? 'Active' : 'Active';
  String get prescriptionStatusCancelled =>
      _prescriptionFrench ? 'Annulée' : 'Cancelled';
  String get prescriptionStatusExpired =>
      _prescriptionFrench ? 'Expirée' : 'Expired';
  String get prescriptionStatusUnknown =>
      _prescriptionFrench ? 'Statut inconnu' : 'Unknown status';
  String get prescriptionTransmissionPending =>
      _prescriptionFrench ? 'Transmission en cours' : 'Transmission pending';
  String get prescriptionTransmissionSent =>
      _prescriptionFrench ? 'Transmise' : 'Transmitted';
  String get prescriptionTransmissionFailed =>
      _prescriptionFrench ? 'Transmission échouée' : 'Transmission failed';
  String get prescriptionReadOnly => _prescriptionFrench
      ? 'Cette ordonnance est finalisée et ne peut plus être modifiée.'
      : 'This prescription is finalized and can no longer be edited.';
  String get prescriptionConsultationRequired => _prescriptionFrench
      ? 'Enregistrez d’abord la note de consultation pour créer une ordonnance.'
      : 'Save the consultation note before creating a prescription.';
  String get prescriptionSectionTitle =>
      _prescriptionFrench ? 'Ordonnances' : 'Prescriptions';
  String get prescriptionHistoryEmpty => _prescriptionFrench
      ? 'Aucune ordonnance enregistrée dans l’historique.'
      : 'No prescription recorded in history.';
  String get prescriptionOpen => _prescriptionFrench ? 'Ouvrir' : 'Open';
  String prescriptionItemsCount(int count) {
    if (_prescriptionFrench) {
      return count <= 1 ? '$count médicament' : '$count médicaments';
    }
    return count == 1 ? '1 medication' : '$count medications';
  }
}

import '../../../l10n/app_localizations.dart';

extension MobileWorkspaceLocalizations on AppLocalizations {
  bool get _workspaceFrench => localeName.startsWith('fr');

  String get navHome => _workspaceFrench ? 'Accueil' : 'Home';
  String get navPatients => _workspaceFrench ? 'Patients' : 'Patients';
  String get navProfile => _workspaceFrench ? 'Profil' : 'Profile';
  String get navMore => _workspaceFrench ? 'Plus' : 'More';

  String get patientsTitle =>
      _workspaceFrench ? 'Dossiers patients' : 'Patient records';
  String get patientsSubtitle => _workspaceFrench
      ? 'Recherchez un patient et ouvrez son dossier selon vos autorisations.'
      : 'Find a patient and open the record allowed by your permissions.';
  String get patientsSearchHint => _workspaceFrench
      ? 'Nom, téléphone ou numéro DPU'
      : 'Name, phone or patient number';
  String get patientsEmptyTitle =>
      _workspaceFrench ? 'Aucun patient trouvé' : 'No patient found';
  String get patientsEmptyBody => _workspaceFrench
      ? 'Modifiez votre recherche ou actualisez l’annuaire.'
      : 'Change your search or refresh the directory.';
  String get patientsOpenRecord =>
      _workspaceFrench ? 'Ouvrir le dossier' : 'Open record';
  String get patientsDirectoryForbidden => _workspaceFrench
      ? 'Vous n’êtes pas autorisé à consulter l’annuaire des patients.'
      : 'You are not allowed to view the patient directory.';

  String get recordTitle =>
      _workspaceFrench ? 'Dossier patient' : 'Patient record';
  String get recordOverview => _workspaceFrench ? 'Aperçu' : 'Overview';
  String get recordMedical =>
      _workspaceFrench ? 'Informations médicales' : 'Medical information';
  String get recordConsultations =>
      _workspaceFrench ? 'Consultations' : 'Consultations';
  String get recordLaboratory =>
      _workspaceFrench ? 'Laboratoire' : 'Laboratory';
  String get recordHospitalizations =>
      _workspaceFrench ? 'Hospitalisations' : 'Hospitalizations';
  String get recordAudit => _workspaceFrench ? 'Traçabilité' : 'Audit trail';
  String get recordPatientIdentity =>
      _workspaceFrench ? 'Identité du patient' : 'Patient identity';
  String get recordSafetySummary =>
      _workspaceFrench ? 'Sécurité clinique' : 'Clinical safety';
  String get recordCriticalAllergies =>
      _workspaceFrench ? 'Allergies à surveiller' : 'Allergies to monitor';
  String get recordNoCriticalAllergies => _workspaceFrench
      ? 'Aucune allergie critique déclarée.'
      : 'No critical allergy declared.';
  String get recordKnownAntecedents =>
      _workspaceFrench ? 'Antécédents connus' : 'Known history';
  String get recordDeclaredAllergies =>
      _workspaceFrench ? 'Allergies déclarées' : 'Declared allergies';
  String get recordNoAntecedents => _workspaceFrench
      ? 'Aucun antécédent répertorié.'
      : 'No medical history recorded.';
  String get recordNoAllergies =>
      _workspaceFrench ? 'Aucune allergie connue.' : 'No known allergy.';
  String get recordNoConsultations => _workspaceFrench
      ? 'Aucune consultation enregistrée.'
      : 'No consultation recorded.';
  String get recordNoLabOrders => _workspaceFrench
      ? 'Aucune donnée de laboratoire disponible.'
      : 'No laboratory data available.';
  String get recordNoHospitalizations => _workspaceFrench
      ? 'Aucune hospitalisation enregistrée.'
      : 'No hospitalization recorded.';
  String get recordNoAudit => _workspaceFrench
      ? 'Aucun événement de traçabilité disponible.'
      : 'No audit event available.';
  String get recordLoading =>
      _workspaceFrench ? 'Chargement du dossier…' : 'Loading patient record…';
  String get recordLoadError => _workspaceFrench
      ? 'Le dossier patient ne peut pas être chargé pour le moment.'
      : 'The patient record cannot be loaded right now.';
  String get recordAccessDeniedTitle =>
      _workspaceFrench ? 'Accès au dossier protégé' : 'Protected record access';
  String get recordAccessDeniedBody => _workspaceFrench
      ? 'Le consentement ou une autorisation supplémentaire est nécessaire pour consulter ce dossier.'
      : 'Consent or an additional authorization is required to view this record.';
  String get recordEmergencyReasonLabel => _workspaceFrench
      ? 'Justification de l’accès d’urgence'
      : 'Emergency access justification';
  String get recordEmergencyReasonHint => _workspaceFrench
      ? 'Décrivez la situation clinique urgente…'
      : 'Describe the urgent clinical situation…';
  String get recordEmergencyAccess =>
      _workspaceFrench ? 'Activer le Break-Glass' : 'Activate Break-Glass';
  String get recordEmergencyAccessError => _workspaceFrench
      ? 'L’accès d’urgence n’a pas pu être activé.'
      : 'Emergency access could not be activated.';
  String get recordPermissionUnavailable => _workspaceFrench
      ? 'Cette section n’est pas disponible avec vos autorisations.'
      : 'This section is not available with your permissions.';
  String get recordRefresh => _workspaceFrench ? 'Actualiser' : 'Refresh';
  String get recordClose => _workspaceFrench ? 'Fermer' : 'Close';
  String get recordUnknown => _workspaceFrench ? 'Non renseigné' : 'Not provided';
  String get recordDateOfBirth =>
      _workspaceFrench ? 'Date de naissance' : 'Date of birth';
  String get recordGender => _workspaceFrench ? 'Sexe' : 'Gender';
  String get recordPhone => _workspaceFrench ? 'Téléphone' : 'Phone';
  String get recordBloodGroup =>
      _workspaceFrench ? 'Groupe sanguin' : 'Blood group';
  String get recordStatus => _workspaceFrench ? 'Statut' : 'Status';
  String get recordPractitioner =>
      _workspaceFrench ? 'Praticien' : 'Practitioner';
  String get recordReason => _workspaceFrench ? 'Motif' : 'Reason';
  String get recordTemperature =>
      _workspaceFrench ? 'Température' : 'Temperature';
  String get recordBloodPressure =>
      _workspaceFrench ? 'Tension' : 'Blood pressure';
  String get recordPulse => _workspaceFrench ? 'Pouls' : 'Pulse';
  String get recordSeverityLow => _workspaceFrench ? 'Faible' : 'Low';
  String get recordSeverityModerate =>
      _workspaceFrench ? 'Modérée' : 'Moderate';
  String get recordSeveritySevere =>
      _workspaceFrench ? 'Sévère' : 'Severe';
  String get recordLabOrders =>
      _workspaceFrench ? 'Demandes d’examens' : 'Lab orders';
  String get recordLabResults =>
      _workspaceFrench ? 'Résultats biologiques' : 'Lab results';
  String get recordHospitalizationStatus =>
      _workspaceFrench ? 'État du séjour' : 'Stay status';
  String get recordAuditActor =>
      _workspaceFrench ? 'Utilisateur' : 'User';
  String get recordAuditAction =>
      _workspaceFrench ? 'Action' : 'Action';
  String get recordAuditDate => _workspaceFrench ? 'Date' : 'Date';

  String get profileWorkspaceTitle =>
      _workspaceFrench ? 'Mon espace' : 'My workspace';
  String get profileWorkspaceSubtitle => _workspaceFrench
      ? 'Compte, sécurité, langue et préférences.'
      : 'Account, security, language, and preferences.';
  String get profilePermissionsTitle =>
      _workspaceFrench ? 'Autorisations actives' : 'Active permissions';
  String get profilePermissionsEmpty => _workspaceFrench
      ? 'Aucune autorisation métier chargée.'
      : 'No business permission loaded.';
}

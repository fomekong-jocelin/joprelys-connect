import 'package:intl/intl.dart' as intl;

import '../../../l10n/app_localizations.dart';

extension DashboardLocalizations on AppLocalizations {
  bool get _isFrench => localeName.startsWith('fr');

  String dashboardGreeting(String name) {
    return _isFrench ? 'Ravi de vous revoir, $name' : 'Good to see you, $name';
  }

  String dashboardOverviewSubtitle(int count) {
    if (_isFrench) {
      return intl.Intl.pluralLogic(
        count,
        locale: localeName,
        zero: 'La file est vide. Vous êtes à jour.',
        one: '1 patient attend votre prise en charge.',
        other: '$count patients attendent votre prise en charge.',
      );
    }
    return intl.Intl.pluralLogic(
      count,
      locale: localeName,
      zero: 'The queue is clear. You are up to date.',
      one: '1 patient is waiting for your care.',
      other: '$count patients are waiting for your care.',
    );
  }

  String get dashboardWorkspaceLabel =>
      _isFrench ? 'ESPACE CLINIQUE' : 'CLINICAL WORKSPACE';

  String get dashboardProfileTooltip =>
      _isFrench ? 'Compte et sécurité' : 'Account and security';

  String get dashboardProfileTitle =>
      _isFrench ? 'Mon espace professionnel' : 'My professional workspace';

  String get dashboardClose => _isFrench ? 'Fermer' : 'Close';

  String get dashboardQueueTitle =>
      _isFrench ? 'Patients en attente' : 'Patients waiting';

  String get dashboardQueueSubtitleLoading => _isFrench
      ? 'Actualisation de la file…'
      : 'Refreshing the queue…';

  String dashboardQueueSubtitle(int count) {
    if (_isFrench) {
      return intl.Intl.pluralLogic(
        count,
        locale: localeName,
        zero: 'Aucun patient dans la file',
        one: '1 patient dans la file',
        other: '$count patients dans la file',
      );
    }
    return intl.Intl.pluralLogic(
      count,
      locale: localeName,
      zero: 'No patients in the queue',
      one: '1 patient in the queue',
      other: '$count patients in the queue',
    );
  }

  String get dashboardQueueRefresh => _isFrench
      ? 'Actualiser la file d’attente'
      : 'Refresh the waiting queue';

  String get dashboardQueueTotal =>
      _isFrench ? 'Patients actifs' : 'Active patients';

  String get dashboardQueueTotalCompact =>
      _isFrench ? 'En attente' : 'Waiting';

  String get dashboardQueueWithVitals =>
      _isFrench ? 'Constantes saisies' : 'Vitals recorded';

  String get dashboardQueueWithVitalsCompact =>
      _isFrench ? 'Prêts' : 'Ready';

  String get dashboardQueueWithoutVitals =>
      _isFrench ? 'À évaluer' : 'To assess';

  String get dashboardQueueEmptyTitle =>
      _isFrench ? 'Tout est à jour' : 'Everything is up to date';

  String get dashboardQueueEmptyBody => _isFrench
      ? 'Aucun patient n’attend actuellement une prise en charge.'
      : 'No patient is currently waiting for care.';

  String get dashboardQueueLoadError => _isFrench
      ? 'La file d’attente ne peut pas être chargée pour le moment.'
      : 'The waiting queue cannot be loaded right now.';

  String get dashboardQueueRetry => _isFrench ? 'Réessayer' : 'Retry';

  String get dashboardQueueVitalsReady =>
      _isFrench ? 'Constantes OK' : 'Vitals ready';

  String get dashboardQueueVitalsReadyCompact =>
      _isFrench ? 'Prêt' : 'Ready';

  String get dashboardQueueVitalsPending =>
      _isFrench ? 'À évaluer' : 'To assess';

  String dashboardQueueVisitReference(String visitNumber, String patientDpu) {
    var cleaned = patientDpu.trim();
    cleaned = cleaned.replaceAll(RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false), 'DPU-');
    if (!cleaned.toUpperCase().startsWith('DPU-')) {
      cleaned = 'DPU-$cleaned';
    }
    return '$visitNumber · $cleaned';
  }

  String dashboardQueueArrivedAt(String time) {
    return _isFrench ? 'Arrivée $time' : 'Arrived $time';
  }

  String get vitalsTitle =>
      _isFrench ? 'Constantes vitales' : 'Vital signs';

  String get vitalsSaveButton =>
      _isFrench ? 'Enregistrer les constantes' : 'Save vital signs';

  String get vitalsBmiLabel =>
      _isFrench ? 'Indice de Masse Corporelle (IMC)' : 'Body Mass Index (BMI)';

  String get vitalsTemperatureLabel =>
      _isFrench ? 'Température (°C)' : 'Temperature (°C)';

  String get vitalsTemperatureHint =>
      _isFrench ? 'ex: 37.5' : 'e.g. 37.5';

  String get vitalsWeightLabel =>
      _isFrench ? 'Poids (kg)' : 'Weight (kg)';

  String get vitalsWeightHint =>
      _isFrench ? 'ex: 70.0' : 'e.g. 70.0';

  String get vitalsHeightLabel =>
      _isFrench ? 'Taille (cm)' : 'Height (cm)';

  String get vitalsHeightHint =>
      _isFrench ? 'ex: 175' : 'e.g. 175';

  String get vitalsPulseLabel =>
      _isFrench ? 'Pouls (bpm)' : 'Pulse (bpm)';

  String get vitalsPulseHint =>
      _isFrench ? 'ex: 75' : 'e.g. 75';

  String get vitalsSystolicLabel =>
      _isFrench ? 'Tension Systolique (mmHg)' : 'Systolic BP (mmHg)';

  String get vitalsSystolicHint =>
      _isFrench ? 'ex: 120' : 'e.g. 120';

  String get vitalsDiastolicLabel =>
      _isFrench ? 'Tension Diastolique (mmHg)' : 'Diastolic BP (mmHg)';

  String get vitalsDiastolicHint =>
      _isFrench ? 'ex: 80' : 'e.g. 80';

  String get vitalsSpo2Label =>
      _isFrench ? 'Saturation SpO2 (%)' : 'SpO2 Saturation (%)';

  String get vitalsSpo2Hint =>
      _isFrench ? 'ex: 98' : 'e.g. 98';

  String get vitalsGlycemiaLabel =>
      _isFrench ? 'Glycémie (g/L)' : 'Blood Glucose (g/L)';

  String get vitalsGlycemiaHint =>
      _isFrench ? 'ex: 0.95' : 'e.g. 0.95';

  String get vitalsRespiratoryRateLabel =>
      _isFrench ? 'Fréq. Respiratoire (c/min)' : 'Resp. Rate (breaths/min)';

  String get vitalsRespiratoryRateHint =>
      _isFrench ? 'ex: 16' : 'e.g. 16';

  String get vitalsPainScaleLabel =>
      _isFrench ? 'Douleur (EVA 0-10)' : 'Pain scale (0-10)';

  String get vitalsPainScaleHint =>
      _isFrench ? 'ex: 2' : 'e.g. 2';

  String get consultationNotesTitle =>
      _isFrench ? 'Notes de consultation' : 'Consultation notes';

  String get consultationSubjectiveLabel =>
      _isFrench ? 'S — Subjectif (Anamnèse)' : 'S — Subjective (Anamnesis)';

  String get consultationSubjectiveHint => _isFrench
      ? 'Plaintes exprimées par le patient, histoire de la maladie…'
      : 'Patient complaints, history of illness…';

  String get consultationObjectiveLabel => _isFrench
      ? 'O — Objectif (Examen physique)'
      : 'O — Objective (Physical exam)';

  String get consultationObjectiveHint => _isFrench
      ? 'Observations cliniques, palpation, auscultation…'
      : 'Clinical observations, physical exam findings…';

  String get consultationAssessmentLabel =>
      _isFrench ? 'A — Évaluation (Diagnostic)' : 'A — Assessment (Diagnosis)';

  String get consultationAssessmentHint => _isFrench
      ? 'Hypothèses diagnostiques ou diagnostic retenu…'
      : 'Diagnostic impressions or confirmed diagnosis…';

  String get consultationPlanLabel =>
      _isFrench ? 'P — Plan (Conduite à tenir)' : 'P — Plan (Care plan)';

  String get consultationPlanHint => _isFrench
      ? 'Prescriptions, examens demandés, orientation…'
      : 'Prescriptions, ordered tests, disposition…';

  String get consultationSaveButton =>
      _isFrench ? 'Enregistrer la note' : 'Save clinical note';

  String get consultationSavedSuccess => _isFrench
      ? 'Note clinique enregistrée avec succès'
      : 'Clinical note saved successfully';

  String get assistantTitle =>
      _isFrench ? 'Assistant Vocal Clinique' : 'Clinical Voice Assistant';

  String get assistantDictationLabel =>
      _isFrench ? 'Dictée médicale brute' : 'Raw medical dictation';

  String get assistantDictationHint => _isFrench
      ? 'ex: Température 38.5, tension 120/80, pouls 75, patient fiévreux…'
      : 'e.g. Temp 38.5, BP 120/80, pulse 75, febrile patient…';

  String get assistantParseButton =>
      _isFrench ? 'Analyser la dictée' : 'Analyze dictation';

  String get assistantApplyButton =>
      _isFrench ? 'Appliquer les données extraites' : 'Apply extracted data';

  String get assistantExtractedSummary =>
      _isFrench ? 'Données médicales extraites' : 'Extracted medical data';

  String get assistantListeningStatus =>
      _isFrench ? 'ÉCOUTE VOCALE EN COURS…' : 'REALTIME VOICE LISTENING…';

  String get assistantListeningInstruction => _isFrench
      ? 'Parlez naturellement (ex: "Température 38.5, tension 120/80, pouls 75")'
      : 'Speak naturally (e.g. "Temp 38.5, BP 120/80, pulse 75")';

  String get assistantStopDictation =>
      _isFrench ? 'Arrêter l’écoute' : 'Stop listening';

  String get assistantLiveTranscript =>
      _isFrench ? 'Transcription vocale en direct' : 'Live voice transcript';

  String get assistantApplyLiveVitals => _isFrench
      ? 'Valider & pré-remplir les constantes'
      : 'Validate & pre-fill vitals';

  String get assistantSecuredRecording =>
      _isFrench ? 'Enregistrement sécurisé' : 'Secured recording';

  String get assistantPaused =>
      _isFrench ? 'Assistant en pause' : 'Assistant paused';

  String get assistantListeningStatusText => _isFrench
      ? 'Écoute en cours… Parlez naturellement'
      : 'Listening in progress… Speak naturally';

  String get assistantTapToListen => _isFrench
      ? 'Appuyez sur le micro pour démarrer l’écoute'
      : 'Tap the microphone to start listening';

  String get assistantTipNaturalDictation => _isFrench
      ? 'Conseil : parlez naturellement, les phrases sont conservées au fil de l’eau.'
      : 'Tip: speak naturally, phrases are preserved as you talk.';

  String get assistantStartDictation =>
      _isFrench ? 'Démarrer' : 'Start';

  String get assistantStopDictationButton =>
      _isFrench ? 'Arrêter' : 'Stop';

  String get assistantPresetDictation1 => _isFrench
      ? 'Température 38.5°C, tension 120/80 mmHg, pouls 75 bpm, SpO2 98%'
      : 'Temperature 38.5°C, BP 120/80 mmHg, pulse 75 bpm, SpO2 98%';

  String get assistantPresetDictation2 => _isFrench
      ? 'Poids 75kg, taille 175cm, glycémie 1.10 g/L, douleur EVA 3/10'
      : 'Weight 75kg, height 175cm, glycemia 1.10 g/L, pain scale 3/10';

  String get assistantPresetDictation3 => _isFrench
      ? 'Pris du paracétamol 1000 mg 2 fois par jour. Revenir dans deux semaines.'
      : 'Took paracetamol 1000 mg twice daily. Return in two weeks.';

  String get historyTitle =>
      _isFrench ? 'Dossier & Historique Médical' : 'Medical File & History';

  String get historyTabAntecedents =>
      _isFrench ? 'Antécédents & Allergies' : 'Antecedents & Allergies';

  String get historyTabVisits =>
      _isFrench ? 'Chronologie des Visites' : 'Visit Timeline';

  String get historyAntecedentsHeader =>
      _isFrench ? 'Antécédents connus' : 'Known antecedents';

  String get historyAllergiesHeader =>
      _isFrench ? 'Allergies déclarées' : 'Declared allergies';

  String get historyNoAntecedents =>
      _isFrench ? 'Aucun antécédent répertorié' : 'No antecedents listed';

  String get historyNoAllergies =>
      _isFrench ? 'Aucune allergie connue' : 'No known allergies';

  String get historyVisitsHeader =>
      _isFrench ? 'Historique des consultations' : 'Consultation history';

  String get historyNoVisits =>
      _isFrench ? 'Aucune visite antérieure' : 'No past visits';

  // ── SOAP section help texts (matching Angular clinical-note-editor) ──

  String get voiceAssistantTitle =>
      _isFrench ? 'Assistant vocal clinique' : 'Clinical voice assistant';

  String get consultationSubjectiveHelp => _isFrench
      ? 'Recueillez les plaintes, symptômes et histoire de la maladie rapportés par le patient.'
      : 'Collect complaints, symptoms and illness history reported by the patient.';

  String get consultationObjectiveHelp => _isFrench
      ? 'Consignez les résultats de l\'examen physique et les observations cliniques objectives.'
      : 'Record physical exam findings and objective clinical observations.';

  String get consultationAssessmentHelp => _isFrench
      ? 'Formulez le diagnostic différentiel et le diagnostic retenu.'
      : 'Formulate the differential diagnosis and the retained diagnosis.';

  String get consultationPlanHelp => _isFrench
      ? 'Détaillez la conduite à tenir : traitement, examens complémentaires, suivi.'
      : 'Detail the action plan: treatment, complementary exams, follow-up.';

  // ── Annuaire des patients i18n ──

  String get directorySearchHint => _isFrench
      ? 'Rechercher par nom, DPU, téléphone…'
      : 'Search by name, DPU, phone…';

  String get directoryTabActiveQueue =>
      _isFrench ? 'File d\'attente active' : 'Active queue';

  String get directoryTabAllPatients =>
      _isFrench ? 'Annuaire (Tous les patients)' : 'Directory (All patients)';

  String get directoryEmptyText =>
      _isFrench ? 'Aucun patient trouvé dans l\'annuaire' : 'No patient found in directory';

  String get directoryOpenHistory =>
      _isFrench ? 'Dossier médical' : 'Medical record';

  // ── Action Chips i18n ──

  String get dashboardQueueActionVitals =>
      _isFrench ? 'Constantes' : 'Vitals';

  String get dashboardQueueActionConsultation =>
      _isFrench ? 'SOAP' : 'SOAP';

  String get dashboardQueueActionHistory =>
      _isFrench ? 'Historique' : 'History';
}

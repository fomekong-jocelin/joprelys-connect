import 'package:intl/intl.dart' as intl;

import '../../../l10n/app_localizations.dart';

extension DashboardLocalizations on AppLocalizations {
  bool get _isFrench => localeName.startsWith('fr');

  String get dashboardQueueTitle =>
      _isFrench ? 'File d’attente active' : 'Active waiting queue';

  String get dashboardQueueSubtitleLoading => _isFrench
      ? 'Actualisation de l’activité clinique…'
      : 'Refreshing clinical activity…';

  String dashboardQueueSubtitle(int count) {
    if (_isFrench) {
      return intl.Intl.pluralLogic(
        count,
        locale: localeName,
        zero: 'Aucun patient en attente',
        one: '1 patient à prendre en charge',
        other: '$count patients à prendre en charge',
      );
    }
    return intl.Intl.pluralLogic(
      count,
      locale: localeName,
      zero: 'No patients are waiting',
      one: '1 patient to care for',
      other: '$count patients to care for',
    );
  }

  String get dashboardQueueRefresh => _isFrench
      ? 'Actualiser la file d’attente'
      : 'Refresh the waiting queue';

  String get dashboardQueueTotal =>
      _isFrench ? 'Patients actifs' : 'Active patients';

  String get dashboardQueueWithVitals =>
      _isFrench ? 'Constantes saisies' : 'Vitals recorded';

  String get dashboardQueueWithoutVitals =>
      _isFrench ? 'À évaluer' : 'To assess';

  String get dashboardQueueEmptyTitle =>
      _isFrench ? 'La file est à jour' : 'The queue is clear';

  String get dashboardQueueEmptyBody => _isFrench
      ? 'Aucun patient n’attend actuellement une prise en charge.'
      : 'No patient is currently waiting for care.';

  String get dashboardQueueLoadError => _isFrench
      ? 'La file d’attente ne peut pas être chargée pour le moment.'
      : 'The waiting queue cannot be loaded right now.';

  String get dashboardQueueRetry => _isFrench ? 'Réessayer' : 'Retry';

  String get dashboardQueueVitalsReady =>
      _isFrench ? 'Constantes OK' : 'Vitals ready';

  String get dashboardQueueVitalsPending =>
      _isFrench ? 'À évaluer' : 'To assess';

  String dashboardQueueVisitReference(String visitNumber, String patientDpu) {
    return _isFrench
        ? 'Visite $visitNumber · DPU $patientDpu'
        : 'Visit $visitNumber · DPU $patientDpu';
  }

  String dashboardQueueArrivedAt(String time) {
    return _isFrench ? 'Arrivée à $time' : 'Arrived at $time';
  }
}

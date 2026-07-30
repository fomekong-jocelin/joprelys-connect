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
    return _isFrench
        ? '$visitNumber · DPU $patientDpu'
        : '$visitNumber · DPU $patientDpu';
  }

  String dashboardQueueArrivedAt(String time) {
    return _isFrench ? 'Arrivée $time' : 'Arrived $time';
  }
}

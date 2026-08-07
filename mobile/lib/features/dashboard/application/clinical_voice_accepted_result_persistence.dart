import '../data/clinical_voice_structured_api.dart';
import '../data/consultation_api.dart';
import '../domain/active_visit.dart';
import '../domain/consultation_note.dart';

/// Orchestration applicative du clic explicite « Enregistrer » après acceptation
/// d'un résultat vocal. Les règles métier et autorisations restent portées par
/// les APIs backend dédiées.
final class ClinicalVoiceAcceptedResultPersistence {
  ClinicalVoiceAcceptedResultPersistence({
    required ConsultationGateway consultationGateway,
    required ClinicalVoiceStructuredGateway structuredGateway,
  }) : _consultationGateway = consultationGateway,
       _structuredGateway = structuredGateway;

  final ConsultationGateway _consultationGateway;
  final ClinicalVoiceStructuredGateway _structuredGateway;

  String? _savedPrescriptionFingerprint;
  String? _savedLabOrdersFingerprint;

  Future<SavedConsultationNote> save({
    required ActiveVisit visit,
    required ConsultationNote note,
    String? prescriptionJson,
    String? labOrdersJson,
  }) async {
    final saved = await _consultationGateway.saveConsultationNote(visit.id, note);

    final prescription = _normalized(prescriptionJson);
    if (prescription != null &&
        prescription != _savedPrescriptionFingerprint) {
      await _structuredGateway.savePrescriptionDraft(
        consultationId: saved.consultationId,
        prescriptionJson: prescription,
      );
      _savedPrescriptionFingerprint = prescription;
    }

    final labOrders = _normalized(labOrdersJson);
    if (labOrders != null && labOrders != _savedLabOrdersFingerprint) {
      await _structuredGateway.saveLabOrders(
        patientId: visit.patientId,
        visitId: visit.id,
        labOrdersJson: labOrders,
      );
      _savedLabOrdersFingerprint = labOrders;
    }

    // Only consume the durable voice source after every accepted clinical
    // resource has been persisted. A partial structured failure remains retryable.
    await _consultationGateway.consumeVoiceWorkingSet(visit.id);
    return saved;
  }

  String? _normalized(String? value) {
    final normalized = value?.trim() ?? '';
    return normalized.isEmpty ? null : normalized;
  }
}

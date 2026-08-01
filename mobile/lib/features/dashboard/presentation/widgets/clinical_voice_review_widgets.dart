import 'dart:convert';

import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../data/clinical_voice_ai_api.dart';
import '../../domain/consultation_note.dart';
import '../../domain/patient_vitals.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_transcript_widgets.dart';

typedef ProposalDecisionCallback =
    Future<void> Function(
      ClinicalAiRevision revision,
      ClinicalAiFieldProposal proposal,
      ClinicalAiDecision decision,
    );

typedef RevisionDecisionCallback =
    Future<void> Function(
      ClinicalAiRevision revision,
      ClinicalAiDecision decision,
    );

class ClinicalProposalReviewList extends StatelessWidget {
  const ClinicalProposalReviewList({
    required this.revisions,
    required this.isFrench,
    required this.processing,
    required this.onProposalDecision,
    required this.onRevisionDecision,
    super.key,
  });

  final List<ClinicalAiRevision> revisions;
  final bool isFrench;
  final bool processing;
  final ProposalDecisionCallback onProposalDecision;
  final RevisionDecisionCallback onRevisionDecision;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final pending = revisions.where((item) => item.isPending).toList();
    if (pending.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          isFrench
              ? 'Modifications proposées à valider'
              : 'Proposed changes to review',
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 8),
        for (final revision in pending) ...[
          _RevisionHeader(
            revision: revision,
            isFrench: isFrench,
            processing: processing,
            onDecision: onRevisionDecision,
          ),
          const SizedBox(height: 8),
          for (final proposal in revision.proposals)
            _ProposalCard(
              revision: revision,
              proposal: proposal,
              isFrench: isFrench,
              processing: processing,
              onDecision: onProposalDecision,
            ),
        ],
      ],
    );
  }
}

class _RevisionHeader extends StatelessWidget {
  const _RevisionHeader({
    required this.revision,
    required this.isFrench,
    required this.processing,
    required this.onDecision,
  });

  final ClinicalAiRevision revision;
  final bool isFrench;
  final bool processing;
  final RevisionDecisionCallback onDecision;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: Text(
            isFrench
                ? 'Proposition #${revision.sequence}'
                : 'Proposal #${revision.sequence}',
            style: Theme.of(
              context,
            ).textTheme.labelLarge?.copyWith(fontWeight: FontWeight.w800),
          ),
        ),
        TextButton(
          onPressed: processing
              ? null
              : () => onDecision(revision, ClinicalAiDecision.reject),
          child: Text(isFrench ? 'Tout rejeter' : 'Reject all'),
        ),
        FilledButton.tonal(
          onPressed: processing
              ? null
              : () => onDecision(revision, ClinicalAiDecision.accept),
          child: Text(isFrench ? 'Tout accepter' : 'Accept all'),
        ),
      ],
    );
  }
}

class _ProposalCard extends StatelessWidget {
  const _ProposalCard({
    required this.revision,
    required this.proposal,
    required this.isFrench,
    required this.processing,
    required this.onDecision,
  });

  final ClinicalAiRevision revision;
  final ClinicalAiFieldProposal proposal;
  final bool isFrench;
  final bool processing;
  final ProposalDecisionCallback onDecision;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  _fieldLabel(l10n, proposal.field),
                  style: theme.textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              Text(
                proposal.uncertainty,
                style: theme.textTheme.labelSmall?.copyWith(
                  color: colors.onSurfaceVariant,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          if (proposal.reason.trim().isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              proposal.reason,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
          ],
          const SizedBox(height: 8),
          _ValueBlock(
            label: isFrench ? 'Valeur actuelle' : 'Current value',
            value: _formatValue(proposal.field, proposal.previousValue),
          ),
          const SizedBox(height: 8),
          _ValueBlock(
            label: isFrench ? 'Valeur proposée' : 'Proposed value',
            value: proposal.operation == 'CLEAR'
                ? (isFrench ? 'Effacer' : 'Clear')
                : _formatValue(proposal.field, proposal.proposedValue),
            emphasized: true,
          ),
          if (proposal.isPending) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                TextButton(
                  onPressed: processing
                      ? null
                      : () => onDecision(
                          revision,
                          proposal,
                          ClinicalAiDecision.reject,
                        ),
                  child: Text(isFrench ? 'Rejeter' : 'Reject'),
                ),
                const SizedBox(width: 8),
                FilledButton(
                  onPressed: processing
                      ? null
                      : () => onDecision(
                          revision,
                          proposal,
                          ClinicalAiDecision.accept,
                        ),
                  child: Text(isFrench ? 'Accepter' : 'Accept'),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }

  String _fieldLabel(AppLocalizations l10n, String field) {
    return switch (field) {
      'symptoms' => l10n.consultationSubjectiveLabel,
      'clinicalExam' => l10n.consultationObjectiveLabel,
      'diagnosis' => l10n.consultationDiagnosisLabel,
      'conclusion' => l10n.consultationConclusionLabel,
      'advice' => l10n.consultationAdviceLabel,
      'followUp' => l10n.consultationFollowUpLabel,
      'vitals' => isFrench ? 'Constantes vitales' : 'Vital signs',
      'prescription' => 'Prescription',
      'labOrders' => isFrench ? 'Examens demandés' : 'Lab orders',
      _ => field,
    };
  }

  String _formatValue(String field, String? value) {
    if (value == null || value.trim().isEmpty) {
      return isFrench ? 'Vide' : 'Empty';
    }
    if (!const {'vitals', 'prescription', 'labOrders'}.contains(field)) {
      return value;
    }
    try {
      final decoded = jsonDecode(value);
      if (decoded is Map) {
        return decoded.entries
            .map((item) => '${item.key}: ${item.value}')
            .join(' · ');
      }
      if (decoded is List) {
        return decoded.map((item) => item.toString()).join('\n');
      }
    } catch (_) {
      return value;
    }
    return value;
  }
}

class _ValueBlock extends StatelessWidget {
  const _ValueBlock({
    required this.label,
    required this.value,
    this.emphasized = false,
  });

  final String label;
  final String value;
  final bool emphasized;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: theme.textTheme.labelSmall?.copyWith(
            color: emphasized ? colors.primary : colors.onSurfaceVariant,
            fontWeight: emphasized ? FontWeight.w800 : FontWeight.normal,
          ),
        ),
        Text(
          value,
          style: emphasized
              ? theme.textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                )
              : theme.textTheme.bodySmall,
        ),
      ],
    );
  }
}

class ClinicalAcceptedPreview extends StatelessWidget {
  const ClinicalAcceptedPreview({
    required this.note,
    required this.vitals,
    super.key,
  });

  final ConsultationNote note;
  final PatientVitals vitals;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final pills = <Widget>[
      if (vitals.temperature != null)
        VitalExtractPill(label: 'T°: ${vitals.temperature}°C'),
      if (vitals.systolic != null && vitals.diastolic != null)
        VitalExtractPill(
          label: 'TA: ${vitals.systolic}/${vitals.diastolic} mmHg',
        ),
      if (vitals.pulse != null)
        VitalExtractPill(label: 'Pouls: ${vitals.pulse} bpm'),
      if (vitals.spo2 != null) VitalExtractPill(label: 'SpO2: ${vitals.spo2}%'),
    ];
    final rows = <(String, String?)>[
      (l10n.consultationSubjectiveLabel, note.symptoms),
      (l10n.consultationObjectiveLabel, note.clinicalExam),
      (l10n.consultationDiagnosisLabel, note.diagnosis),
      (l10n.consultationConclusionLabel, note.conclusion),
      (l10n.consultationAdviceLabel, note.advice),
      (l10n.consultationFollowUpLabel, note.followUp),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (pills.isNotEmpty) ...[
          Wrap(spacing: 8, runSpacing: 8, children: pills),
          const SizedBox(height: 12),
        ],
        for (final row in rows)
          if (row.$2?.trim().isNotEmpty == true) ...[
            Text(
              row.$1,
              style: theme.textTheme.labelMedium?.copyWith(
                fontWeight: FontWeight.w900,
              ),
            ),
            const SizedBox(height: 3),
            Text(row.$2!, style: theme.textTheme.bodyMedium),
            const SizedBox(height: 10),
          ],
      ],
    );
  }
}

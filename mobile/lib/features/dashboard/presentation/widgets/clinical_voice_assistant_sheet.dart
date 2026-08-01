import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'clinical_voice_listening_surface.dart';
import 'clinical_voice_transcript_widgets.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../application/clinical_dictation_parser.dart';
import '../../application/clinical_speech_service.dart';
import '../../data/clinical_voice_ai_api.dart';
import '../../domain/active_visit.dart';
import '../../domain/consultation_note.dart';
import '../../domain/patient_vitals.dart';
import '../dashboard_localizations.dart';

class ClinicalVoiceAssistantSheet extends ConsumerStatefulWidget {
  const ClinicalVoiceAssistantSheet({
    required this.visit,
    required this.onExtracted,
    required this.initialDraft,
    required this.locale,
    super.key,
  });

  final ActiveVisit visit;
  final ValueChanged<DictationParseResult> onExtracted;
  final Map<String, String> initialDraft;
  final String locale;

  static Future<void> show(
    BuildContext context, {
    required ActiveVisit visit,
    required ValueChanged<DictationParseResult> onExtracted,
    Map<String, String> initialDraft = const <String, String>{},
  }) {
    final locale = Localizations.localeOf(context).languageCode == 'en'
        ? 'en'
        : 'fr';
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: FractionallySizedBox(
          heightFactor: 0.94,
          child: ClinicalVoiceAssistantSheet(
            visit: visit,
            onExtracted: onExtracted,
            initialDraft: initialDraft,
            locale: locale,
          ),
        ),
      ),
    );
  }

  @override
  ConsumerState<ClinicalVoiceAssistantSheet> createState() =>
      _ClinicalVoiceAssistantSheetState();
}

class _ClinicalVoiceAssistantSheetState
    extends ConsumerState<ClinicalVoiceAssistantSheet>
    with TickerProviderStateMixin {
  late final ClinicalSpeechService _speechService;
  final _dictationController = TextEditingController();

  late AnimationController _haloController;
  late AnimationController _waveController;

  bool get _isFrench => widget.locale == 'fr';

  @override
  void initState() {
    super.initState();
    _haloController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 2800),
    )..repeat();
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..repeat();

    _speechService = ClinicalSpeechService(
      gateway: ref.read(clinicalVoiceAiApiProvider),
      visitId: widget.visit.id,
      initialDraft: widget.initialDraft,
      locale: widget.locale,
    );
    _speechService.addListener(_onSpeechStateChanged);
    Future<void>.microtask(_speechService.startRealtimeListening);
  }

  @override
  void dispose() {
    _speechService.removeListener(_onSpeechStateChanged);
    _speechService.dispose();
    _dictationController.dispose();
    _haloController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  void _onSpeechStateChanged() {
    final state = _speechService.value;
    if (_dictationController.text != state.transcript) {
      _dictationController.value = TextEditingValue(
        text: state.transcript,
        selection: TextSelection.collapsed(offset: state.transcript.length),
      );
    }
  }

  void _onTextChanged(String text) {
    _speechService.updateTranscript(text);
  }

  Future<void> _toggleListening() async {
    final status = _speechService.value.status;
    if (status == SpeechStatus.processing) return;
    if (status == SpeechStatus.listening) {
      await _speechService.stopListening();
    } else {
      await _speechService.startRealtimeListening();
    }
  }

  Future<void> _applyResult() async {
    final state = _speechService.value;
    final hasAccepted = state.revisions.any(
      (revision) => revision.hasAcceptedProposals,
    );
    if (state.hasPendingProposals ||
        state.needsClarification ||
        !hasAccepted) {
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          _isFrench
              ? 'Appliquer les éléments acceptés ?'
              : 'Apply accepted items?',
        ),
        content: Text(
          _isFrench
              ? 'Seuls les éléments que vous avez explicitement acceptés seront proposés au formulaire. Les champs déjà saisis seront conservés.'
              : 'Only items you explicitly accepted will be proposed to the form. Existing entries will be preserved.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: Text(_isFrench ? 'Annuler' : 'Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(_isFrench ? 'Appliquer' : 'Apply'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    widget.onExtracted(
      DictationParseResult(vitals: state.vitals, note: state.note),
    );
    Navigator.of(context).pop();
  }

  String _formattedReference(String visitNum, String rawDpu) {
    var cleaned = rawDpu.trim();
    cleaned = cleaned.replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!cleaned.toUpperCase().startsWith('DPU-')) {
      cleaned = 'DPU-$cleaned';
    }
    final nonBreakingDpu = cleaned.replaceAll('-', '\u2011');
    return '$visitNum · $nonBreakingDpu';
  }

  String _fieldLabel(AppLocalizations l10n, String field) {
    return switch (field) {
      'symptoms' => l10n.consultationSubjectiveLabel,
      'clinicalExam' => l10n.consultationObjectiveLabel,
      'diagnosis' => l10n.consultationDiagnosisLabel,
      'conclusion' => l10n.consultationConclusionLabel,
      'advice' => l10n.consultationAdviceLabel,
      'followUp' => l10n.consultationFollowUpLabel,
      'vitals' => _isFrench ? 'Constantes vitales' : 'Vital signs',
      'prescription' => _isFrench ? 'Prescription' : 'Prescription',
      'labOrders' => _isFrench ? 'Examens demandés' : 'Lab orders',
      _ => field,
    };
  }

  String _formatProposalValue(String field, String? value) {
    if (value == null || value.trim().isEmpty) {
      return _isFrench ? 'Vide' : 'Empty';
    }
    if (!const <String>{'vitals', 'prescription', 'labOrders'}.contains(field)) {
      return value;
    }
    try {
      final decoded = jsonDecode(value);
      if (decoded is Map) {
        return decoded.entries.map((item) => '${item.key}: ${item.value}').join(' · ');
      }
      if (decoded is List) {
        return decoded.map((item) {
          if (item is Map) {
            return item.values
                .where((part) => part != null && part.toString().trim().isNotEmpty)
                .join(' · ');
          }
          return item.toString();
        }).join('\n');
      }
    } catch (_) {
      return value;
    }
    return value;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    return ValueListenableBuilder<RealtimeSpeechState>(
      valueListenable: _speechService,
      builder: (context, state, child) {
        final isListening = state.status == SpeechStatus.listening;
        final isProcessing = state.status == SpeechStatus.processing;
        final canEditTranscript =
            state.status == SpeechStatus.transcriptReview;
        final hasAccepted = state.revisions.any(
          (revision) => revision.hasAcceptedProposals,
        );

        return Container(
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: const BorderRadius.vertical(
              top: Radius.circular(AppDesignTokens.radiusLg),
            ),
          ),
          child: Column(
            children: [
              const SizedBox(height: 12),
              Center(
                child: Container(
                  width: 42,
                  height: 4,
                  decoration: BoxDecoration(
                    color: colors.outlineVariant,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
                child: Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            children: [
                              Icon(
                                Icons.mic_rounded,
                                color: colors.primary,
                                size: 22,
                              ),
                              const SizedBox(width: 6),
                              Text(
                                l10n.assistantTitle,
                                style: theme.textTheme.titleLarge?.copyWith(
                                  fontWeight: FontWeight.w900,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 2),
                          Text(
                            '${widget.visit.patientName} (${_formattedReference(widget.visit.visitNumber, widget.visit.patientDpu)})',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              height: 1.3,
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      onPressed: isProcessing
                          ? null
                          : () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close_rounded),
                    ),
                  ],
                ),
              ),
              const Divider(height: 1),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      ClinicalVoiceListeningSurface(
                        active: isListening,
                        soundLevel: state.soundLevel,
                        haloController: _haloController,
                        waveController: _waveController,
                        badgeText: isProcessing
                            ? (_isFrench ? 'Traitement sécurisé' : 'Secure processing')
                            : isListening
                            ? l10n.assistantSecuredRecording
                            : l10n.assistantPaused,
                        statusText: isProcessing
                            ? (_isFrench
                                  ? 'Transcription et analyse en cours…'
                                  : 'Transcription and analysis in progress…')
                            : isListening
                            ? l10n.assistantListeningStatusText
                            : (_isFrench
                                  ? 'Enregistrez une dictée, puis vérifiez-la avant analyse.'
                                  : 'Record a dictation, then review it before analysis.'),
                        tipText: _isFrench
                            ? 'Aucune donnée ne sera appliquée sans validation explicite.'
                            : 'No data will be applied without explicit approval.',
                        stopLabel: l10n.assistantStopDictationButton,
                        startLabel: l10n.assistantStartDictation,
                        onToggleListening: _toggleListening,
                      ),
                      if (isProcessing) ...[
                        const SizedBox(height: 16),
                        const Center(child: CircularProgressIndicator()),
                      ],
                      if (state.transcript.trim().isNotEmpty) ...[
                        const SizedBox(height: 16),
                        ClinicalTranscriptCard(
                          transcript: state.transcript,
                          controller: _dictationController,
                          onChanged: _onTextChanged,
                          label: _isFrench
                              ? 'Transcription à vérifier'
                              : 'Transcript to review',
                          hint: l10n.assistantDictationHint,
                          editLabel: _isFrench ? 'Corriger' : 'Edit',
                          doneLabel: _isFrench
                              ? 'Terminer la correction'
                              : 'Finish editing',
                          editable: canEditTranscript,
                        ),
                      ],
                      if (state.status == SpeechStatus.transcriptReview) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: colors.tertiaryContainer.withValues(alpha: 0.55),
                            borderRadius: BorderRadius.circular(
                              AppDesignTokens.radiusSm,
                            ),
                          ),
                          child: Text(
                            _isFrench
                                ? 'Relisez et corrigez le texte. L’IA travaillera uniquement sur cette version validée.'
                                : 'Review and correct the text. The AI will only use this approved version.',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onTertiaryContainer,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        const SizedBox(height: 12),
                        AppButton(
                          label: _isFrench
                              ? 'Analyser et proposer le compte rendu'
                              : 'Analyze and propose the clinical note',
                          icon: Icons.auto_awesome_rounded,
                          expand: true,
                          onPressed: state.transcript.trim().isEmpty
                              ? null
                              : _speechService.analyzeTranscript,
                        ),
                      ],
                      if (state.errorMessage != null) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: colors.errorContainer,
                            borderRadius: BorderRadius.circular(
                              AppDesignTokens.radiusSm,
                            ),
                          ),
                          child: Text(
                            state.errorMessage!,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onErrorContainer,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ],
                      if (state.assistantMessage?.trim().isNotEmpty == true) ...[
                        const SizedBox(height: 12),
                        Text(
                          state.assistantMessage!,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: colors.onSurfaceVariant,
                          ),
                        ),
                      ],
                      if (state.revisions.isNotEmpty) ...[
                        const SizedBox(height: 18),
                        Text(
                          _isFrench
                              ? 'Modifications proposées à valider'
                              : 'Proposed changes to review',
                          style: theme.textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        const SizedBox(height: 8),
                        for (final revision in state.revisions.where(
                          (item) => item.isPending,
                        )) ...[
                          Row(
                            children: [
                              Expanded(
                                child: Text(
                                  _isFrench
                                      ? 'Proposition #${revision.sequence}'
                                      : 'Proposal #${revision.sequence}',
                                  style: theme.textTheme.labelLarge?.copyWith(
                                    fontWeight: FontWeight.w800,
                                  ),
                                ),
                              ),
                              TextButton(
                                onPressed: isProcessing
                                    ? null
                                    : () => _speechService.decideRevision(
                                        revision,
                                        ClinicalAiDecision.reject,
                                      ),
                                child: Text(_isFrench ? 'Tout rejeter' : 'Reject all'),
                              ),
                              FilledButton.tonal(
                                onPressed: isProcessing
                                    ? null
                                    : () => _speechService.decideRevision(
                                        revision,
                                        ClinicalAiDecision.accept,
                                      ),
                                child: Text(_isFrench ? 'Tout accepter' : 'Accept all'),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          for (final proposal in revision.proposals) ...[
                            Container(
                              margin: const EdgeInsets.only(bottom: 10),
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: colors.surfaceContainerLow,
                                borderRadius: BorderRadius.circular(
                                  AppDesignTokens.radiusSm,
                                ),
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
                                  Text(
                                    _isFrench ? 'Valeur actuelle' : 'Current value',
                                    style: theme.textTheme.labelSmall?.copyWith(
                                      color: colors.onSurfaceVariant,
                                    ),
                                  ),
                                  Text(
                                    _formatProposalValue(
                                      proposal.field,
                                      proposal.previousValue,
                                    ),
                                    style: theme.textTheme.bodySmall,
                                  ),
                                  const SizedBox(height: 8),
                                  Text(
                                    _isFrench ? 'Valeur proposée' : 'Proposed value',
                                    style: theme.textTheme.labelSmall?.copyWith(
                                      color: colors.primary,
                                      fontWeight: FontWeight.w800,
                                    ),
                                  ),
                                  Text(
                                    proposal.operation == 'CLEAR'
                                        ? (_isFrench ? 'Effacer' : 'Clear')
                                        : _formatProposalValue(
                                            proposal.field,
                                            proposal.proposedValue,
                                          ),
                                    style: theme.textTheme.bodyMedium?.copyWith(
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                  if (proposal.isPending) ...[
                                    const SizedBox(height: 8),
                                    Row(
                                      children: [
                                        TextButton(
                                          onPressed: isProcessing
                                              ? null
                                              : () => _speechService.decideProposal(
                                                  revision,
                                                  proposal,
                                                  ClinicalAiDecision.reject,
                                                ),
                                          child: Text(
                                            _isFrench ? 'Rejeter' : 'Reject',
                                          ),
                                        ),
                                        const SizedBox(width: 8),
                                        FilledButton(
                                          onPressed: isProcessing
                                              ? null
                                              : () => _speechService.decideProposal(
                                                  revision,
                                                  proposal,
                                                  ClinicalAiDecision.accept,
                                                ),
                                          child: Text(
                                            _isFrench ? 'Accepter' : 'Accept',
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ],
                              ),
                            ),
                          ],
                        ],
                      ],
                      if (!state.vitals.isEmpty) ...[
                        const SizedBox(height: 12),
                        _VitalsPreview(vitals: state.vitals),
                      ],
                      if (!state.note.isEmpty && !state.hasPendingProposals) ...[
                        const SizedBox(height: 12),
                        _ClinicalNotePreview(note: state.note),
                      ],
                      if (state.needsClarification) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: colors.errorContainer,
                            borderRadius: BorderRadius.circular(
                              AppDesignTokens.radiusSm,
                            ),
                          ),
                          child: Text(
                            _isFrench
                                ? 'Une clarification clinique est nécessaire. Rien ne peut être appliqué pour le moment.'
                                : 'Clinical clarification is required. Nothing can be applied yet.',
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onErrorContainer,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                      if (state.status == SpeechStatus.done &&
                          hasAccepted &&
                          !state.needsClarification) ...[
                        const SizedBox(height: 18),
                        AppButton(
                          label: _isFrench
                              ? 'Appliquer les éléments acceptés'
                              : 'Apply accepted items',
                          icon: Icons.fact_check_rounded,
                          expand: true,
                          onPressed: _applyResult,
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _VitalsPreview extends StatelessWidget {
  const _VitalsPreview({required this.vitals});

  final PatientVitals vitals;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final pills = <Widget>[
      if (vitals.temperature != null)
        VitalExtractPill(label: 'T°: ${vitals.temperature}°C'),
      if (vitals.systolic != null && vitals.diastolic != null)
        VitalExtractPill(
          label: 'TA: ${vitals.systolic}/${vitals.diastolic} mmHg',
        ),
      if (vitals.pulse != null)
        VitalExtractPill(label: 'Pouls: ${vitals.pulse} bpm'),
      if (vitals.spo2 != null)
        VitalExtractPill(label: 'SpO2: ${vitals.spo2}%'),
      if (vitals.weight != null)
        VitalExtractPill(label: 'Poids: ${vitals.weight} kg'),
      if (vitals.height != null)
        VitalExtractPill(label: 'Taille: ${vitals.height} cm'),
      if (vitals.glycemia != null)
        VitalExtractPill(label: 'Glycémie: ${vitals.glycemia} g/L'),
      if (vitals.painScale != null)
        VitalExtractPill(label: 'Douleur: EVA ${vitals.painScale}/10'),
    ];
    if (pills.isEmpty) return const SizedBox.shrink();
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.06),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Wrap(spacing: 8, runSpacing: 8, children: pills),
    );
  }
}

class _ClinicalNotePreview extends StatelessWidget {
  const _ClinicalNotePreview({required this.note});

  final ConsultationNote note;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
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

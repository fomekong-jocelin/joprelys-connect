import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../application/clinical_dictation_parser.dart';
import '../../application/clinical_speech_service.dart';
import '../../data/clinical_voice_ai_api.dart';
import '../../domain/active_visit.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_listening_surface.dart';
import 'clinical_voice_review_widgets.dart';
import 'clinical_voice_transcript_widgets.dart';

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
      isDismissible: false,
      enableDrag: false,
      backgroundColor: Colors.transparent,
      builder: (sheetContext) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(sheetContext).viewInsets.bottom,
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
  late final AnimationController _haloController;
  late final AnimationController _waveController;
  final _transcriptController = TextEditingController();

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
    )..addListener(_synchronizeTranscript);
    Future.microtask(_speechService.restoreOrStart);
  }

  @override
  void dispose() {
    _speechService.removeListener(_synchronizeTranscript);
    _speechService.dispose();
    _transcriptController.dispose();
    _haloController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  void _synchronizeTranscript() {
    final transcript = _speechService.value.transcript;
    if (_transcriptController.text == transcript) return;
    _transcriptController.value = TextEditingValue(
      text: transcript,
      selection: TextSelection.collapsed(offset: transcript.length),
    );
  }

  Future<void> _toggleListening() async {
    final status = _speechService.value.status;
    if (status == SpeechStatus.processing ||
        status == SpeechStatus.proposalReview) {
      return;
    }
    if (status == SpeechStatus.listening) {
      await _speechService.stopListening();
    } else {
      await _speechService.startRealtimeListening();
    }
  }

  Future<void> _close() async {
    final safeToClose = await _speechService.prepareForClose();
    if (safeToClose && mounted) Navigator.of(context).pop();
  }

  Future<void> _applyAcceptedResult() async {
    final state = _speechService.value;
    if (!state.hasApplicableResult ||
        state.hasPendingProposals ||
        state.needsClarification) {
      return;
    }

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(
          _isFrench
              ? 'Appliquer les éléments vérifiés ?'
              : 'Apply reviewed items?',
        ),
        content: Text(
          _isFrench
              ? 'Seuls les éléments relus et confirmés seront appliqués. Les autres champs resteront inchangés.'
              : 'Only reviewed and confirmed items will be applied. Other fields will remain unchanged.',
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

  String _reference() {
    var dpu = widget.visit.patientDpu.trim().replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!dpu.toUpperCase().startsWith('DPU-')) dpu = 'DPU-$dpu';
    return '${widget.visit.visitNumber} · ${dpu.replaceAll('-', '\u2011')}';
  }

  String _badge(RealtimeSpeechState state) {
    return switch (state.status) {
      SpeechStatus.listening => AppLocalizations.of(
        context,
      ).assistantSecuredRecording,
      SpeechStatus.processing =>
        _isFrench ? 'Traitement sécurisé' : 'Secure processing',
      SpeechStatus.transcriptReview =>
        _isFrench ? 'Transcription à vérifier' : 'Transcript to review',
      SpeechStatus.proposalReview =>
        _isFrench ? 'Décision requise' : 'Decision required',
      _ => AppLocalizations.of(context).assistantPaused,
    };
  }

  String _status(RealtimeSpeechState state) {
    return switch (state.status) {
      SpeechStatus.listening => AppLocalizations.of(
        context,
      ).assistantListeningStatusText,
      SpeechStatus.processing =>
        _isFrench
            ? 'Transcription et analyse en cours…'
            : 'Transcription and analysis in progress…',
      SpeechStatus.transcriptReview =>
        _isFrench
            ? 'Relisez et corrigez avant de demander le compte rendu.'
            : 'Review and correct before requesting the clinical note.',
      SpeechStatus.proposalReview =>
        _isFrench
            ? 'Acceptez ou rejetez chaque modification proposée.'
            : 'Accept or reject each proposed change.',
      _ =>
        _isFrench
            ? 'Enregistrez une nouvelle dictée clinique.'
            : 'Record a new clinical dictation.',
    };
  }

  String _errorText(String raw) {
    if (raw.contains('AI_AUDIO_SILENCE')) {
      return _isFrench
          ? 'Aucune parole n’a été détectée. Recommencez l’enregistrement en parlant près du microphone.'
          : 'No speech was detected. Record again while speaking near the microphone.';
    }
    if (raw.contains('AI_TRANSCRIPT_REVIEW_REQUIRED')) {
      return _isFrench
          ? 'Une transcription est déjà en attente. Relisez-la avant de reprendre le micro.'
          : 'A transcript is already waiting. Review it before recording again.';
    }
    return raw;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    return ValueListenableBuilder<RealtimeSpeechState>(
      valueListenable: _speechService,
      builder: (context, state, child) {
        final listening = state.status == SpeechStatus.listening;
        final processing = state.status == SpeechStatus.processing;
        final hasApplicableResult = state.hasApplicableResult;

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
              Container(
                width: 42,
                height: 4,
                decoration: BoxDecoration(
                  color: colors.outlineVariant,
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              _Header(
                patientName: widget.visit.patientName,
                reference: _reference(),
                processing: processing,
                onClose: _close,
              ),
              const Divider(height: 1),
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      ClinicalVoiceListeningSurface(
                        active: listening,
                        soundLevel: state.soundLevel,
                        haloController: _haloController,
                        waveController: _waveController,
                        badgeText: _badge(state),
                        statusText: _status(state),
                        tipText: _isFrench
                            ? 'Aucune donnée ne sera appliquée sans validation explicite.'
                            : 'No data will be applied without explicit approval.',
                        stopLabel: l10n.assistantStopDictationButton,
                        startLabel: l10n.assistantStartDictation,
                        onToggleListening: _toggleListening,
                      ),
                      if (processing) ...[
                        const SizedBox(height: 16),
                        const Center(child: CircularProgressIndicator()),
                      ],
                      if (state.transcript.trim().isNotEmpty) ...[
                        const SizedBox(height: 16),
                        ClinicalTranscriptCard(
                          transcript: state.transcript,
                          controller: _transcriptController,
                          onChanged: _speechService.updateTranscript,
                          label: _isFrench
                              ? 'Transcription à vérifier'
                              : 'Transcript to review',
                          hint: l10n.assistantDictationHint,
                          editLabel: _isFrench ? 'Corriger' : 'Edit',
                          doneLabel: _isFrench
                              ? 'Terminer la correction'
                              : 'Finish editing',
                          editable:
                              state.status == SpeechStatus.transcriptReview,
                        ),
                      ],
                      if (state.status == SpeechStatus.transcriptReview) ...[
                        const SizedBox(height: 12),
                        _SafetyNotice(
                          message: _isFrench
                              ? 'L’IA travaillera uniquement sur la transcription que vous venez de relire.'
                              : 'The AI will only use the transcript you just reviewed.',
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
                        _ErrorNotice(
                          message: _errorText(state.errorMessage!),
                        ),
                      ],
                      if (state.assistantMessage?.trim().isNotEmpty ==
                          true) ...[
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
                        ClinicalProposalReviewList(
                          revisions: state.revisions,
                          isFrench: _isFrench,
                          processing: processing,
                          onProposalDecision: _speechService.decideProposal,
                          onRevisionDecision: _speechService.decideRevision,
                        ),
                      ],
                      if (state.needsClarification) ...[
                        const SizedBox(height: 12),
                        _ErrorNotice(
                          message: _isFrench
                              ? 'La synthèse est incomplète. Corrigez la transcription avant de relancer l’analyse.'
                              : 'The summary is incomplete. Correct the transcript before running the analysis again.',
                        ),
                      ],
                      if (!state.hasPendingProposals && hasApplicableResult) ...[
                        const SizedBox(height: 16),
                        ClinicalAcceptedPreview(
                          note: state.note,
                          vitals: state.vitals,
                        ),
                      ],
                      if (state.status == SpeechStatus.done &&
                          hasApplicableResult &&
                          !state.needsClarification) ...[
                        const SizedBox(height: 18),
                        AppButton(
                          label: _isFrench
                              ? 'Appliquer les éléments vérifiés'
                              : 'Apply reviewed items',
                          icon: Icons.fact_check_rounded,
                          expand: true,
                          onPressed: _applyAcceptedResult,
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

class _Header extends StatelessWidget {
  const _Header({
    required this.patientName,
    required this.reference,
    required this.processing,
    required this.onClose,
  });

  final String patientName;
  final String reference;
  final bool processing;
  final VoidCallback onClose;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Row(
        children: [
          Icon(Icons.mic_rounded, color: colors.primary, size: 22),
          const SizedBox(width: 8),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  AppLocalizations.of(context).assistantTitle,
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
                ),
                Text(
                  '$patientName ($reference)',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            onPressed: processing ? null : onClose,
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
    );
  }
}

class _SafetyNotice extends StatelessWidget {
  const _SafetyNotice({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.tertiaryContainer.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        message,
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
          color: colors.onTertiaryContainer,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class _ErrorNotice extends StatelessWidget {
  const _ErrorNotice({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        message,
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
          color: colors.onErrorContainer,
          fontWeight: FontWeight.w700,
        ),
      ),
    );
  }
}

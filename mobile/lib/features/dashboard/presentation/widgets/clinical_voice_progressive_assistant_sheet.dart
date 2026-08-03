import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/lifecycle/app_activity_registry.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../auth/application/auth_controller.dart';
import '../../application/clinical_dictation_parser.dart';
import '../../application/clinical_speech_service.dart';
import '../../application/clinical_voice_progressive_coordinator.dart';
import '../../application/clinical_voice_state.dart';
import '../../data/clinical_voice_ai_api.dart';
import '../../data/clinical_voice_capture_api.dart';
import '../../domain/active_visit.dart';
import '../clinical_voice_localizations.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_listening_surface.dart';
import 'clinical_voice_progressive_preview.dart';
import 'clinical_voice_review_widgets.dart';
import 'clinical_voice_segment_timeline.dart';

class ClinicalVoiceProgressiveAssistantSheet extends ConsumerStatefulWidget {
  const ClinicalVoiceProgressiveAssistantSheet({
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
          child: ClinicalVoiceProgressiveAssistantSheet(
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
  ConsumerState<ClinicalVoiceProgressiveAssistantSheet> createState() =>
      _ClinicalVoiceProgressiveAssistantSheetState();
}

class _ClinicalVoiceProgressiveAssistantSheetState
    extends ConsumerState<ClinicalVoiceProgressiveAssistantSheet>
    with TickerProviderStateMixin, WidgetsBindingObserver {
  late final ClinicalSpeechService _speechService;
  late final ClinicalVoiceProgressiveCoordinator _coordinator;
  late final AnimationController _haloController;
  late final AnimationController _waveController;
  late final AppForegroundActivityController _activityController;

  bool get _isFrench => widget.locale == 'fr';

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _activityController = ref.read(appForegroundActivityProvider.notifier);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      _activityController.activate(
        AppForegroundActivity.clinicalVoiceAssistant,
      );
    });

    _haloController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1800),
    )..repeat();
    _waveController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 850),
    )..repeat();

    final aiGateway = ref.read(clinicalVoiceAiApiProvider);
    _speechService = ClinicalSpeechService(
      gateway: aiGateway,
      visitId: widget.visit.id,
      initialDraft: widget.initialDraft,
      locale: widget.locale,
    );
    _coordinator = ClinicalVoiceProgressiveCoordinator(
      speechService: _speechService,
      aiGateway: aiGateway,
      captureGateway: ref.read(clinicalVoiceCaptureApiProvider),
      visitId: widget.visit.id,
      initialDraft: widget.initialDraft,
      locale: widget.locale,
    );
    Future.microtask(() async {
      await _speechService.initialize();
      await _coordinator.start();
    });
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.inactive ||
        state == AppLifecycleState.paused ||
        state == AppLifecycleState.detached) {
      unawaited(_speechService.suspendForLifecycle());
      return;
    }
    if (state == AppLifecycleState.resumed &&
        ref.read(authControllerProvider).value?.status ==
            AuthStatus.authenticated) {
      unawaited(_speechService.resumeAfterLifecycle());
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _activityController.deactivate(
        AppForegroundActivity.clinicalVoiceAssistant,
      );
    });
    _coordinator.dispose();
    _speechService.dispose();
    _haloController.dispose();
    _waveController.dispose();
    super.dispose();
  }

  Future<void> _toggleListening() async {
    final state = _speechService.value;
    if (state.status == SpeechStatus.processing ||
        state.stage == ClinicalVoiceStage.review) {
      return;
    }
    if (state.status == SpeechStatus.listening) {
      await _speechService.saveDictationForReview();
      await _coordinator.synchronizeNow();
    } else {
      await _speechService.startRealtimeListening();
    }
  }

  Future<void> _close() async {
    final safeToClose = await _speechService.prepareForClose();
    if (!safeToClose) return;
    await _coordinator.synchronizeNow();
    if (mounted) Navigator.of(context).pop();
  }

  Future<void> _clearAll() async {
    final l10n = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(l10n.voiceClearConfirmTitle),
        content: Text(l10n.voiceClearConfirmBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: Text(l10n.voiceCancel),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(l10n.voiceConfirmDelete),
          ),
        ],
      ),
    );
    if (confirmed == true) await _coordinator.clearAll();
  }

  Future<void> _retrySynchronization() async {
    final localSaved = await _speechService.retryDraftSave();
    if (localSaved) await _coordinator.synchronizeNow();
  }

  Future<void> _applyAcceptedResult() async {
    final state = _speechService.value;
    if (!state.hasApplicableResult ||
        state.hasPendingProposals ||
        state.needsClarification) {
      return;
    }

    final l10n = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: Text(l10n.voiceApplyTitle),
        content: Text(l10n.voiceApplyBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(false),
            child: Text(l10n.voiceCancel),
          ),
          FilledButton(
            onPressed: () => Navigator.of(dialogContext).pop(true),
            child: Text(l10n.voiceApplyConfirm),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    widget.onExtracted(
      DictationParseResult(vitals: state.vitals, note: state.note),
    );
    await _coordinator.completeCapture();
    if (mounted) Navigator.of(context).pop();
  }

  void _returnToCapture() {
    final state = _speechService.value;
    _speechService.value = state.copyWith(
      status: state.hasTranscript
          ? SpeechStatus.transcriptReview
          : SpeechStatus.idle,
      stage: ClinicalVoiceStage.capture,
      clearError: true,
    );
  }

  String _reference() {
    var dpu = widget.visit.patientDpu.trim().replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!dpu.toUpperCase().startsWith('DPU-')) dpu = 'DPU-$dpu';
    return '${widget.visit.visitNumber} · ${dpu.replaceAll('-', '\u2011')}';
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(authControllerProvider, (previous, next) {
      if (previous?.value?.status == AuthStatus.locked &&
          next.value?.status == AuthStatus.authenticated) {
        unawaited(_speechService.resumeAfterLifecycle());
      }
    });

    return ValueListenableBuilder<RealtimeSpeechState>(
      valueListenable: _speechService,
      builder: (context, state, child) {
        final colors = Theme.of(context).colorScheme;
        final processing = state.status == SpeechStatus.processing;
        return Container(
          decoration: BoxDecoration(
            color: colors.surface,
            borderRadius: const BorderRadius.vertical(
              top: Radius.circular(AppDesignTokens.radiusLg),
            ),
          ),
          child: Column(
            children: [
              const SizedBox(height: 10),
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
                processing: processing || state.isSynchronizingTranscript,
                onClose: _close,
              ),
              const Divider(height: 1),
              Expanded(
                child: state.stage == ClinicalVoiceStage.capture
                    ? _CaptureBody(
                        state: state,
                        haloController: _haloController,
                        waveController: _waveController,
                        onToggleListening: _toggleListening,
                        onClearAll: _clearAll,
                        onFinalize: _coordinator.finishAndReview,
                        onRetrySynchronization: _retrySynchronization,
                        onSegmentChanged: _speechService.updateSegment,
                        onSegmentDeleted: _speechService.deleteSegment,
                      )
                    : _ReviewBody(
                        state: state,
                        isFrench: _isFrench,
                        onProposalDecision: _speechService.decideProposal,
                        onRevisionDecision: _speechService.decideRevision,
                        onApply: _applyAcceptedResult,
                        onReturnToCapture: _returnToCapture,
                      ),
              ),
            ],
          ),
        );
      },
    );
  }
}

class _CaptureBody extends StatelessWidget {
  const _CaptureBody({
    required this.state,
    required this.haloController,
    required this.waveController,
    required this.onToggleListening,
    required this.onClearAll,
    required this.onFinalize,
    required this.onRetrySynchronization,
    required this.onSegmentChanged,
    required this.onSegmentDeleted,
  });

  final RealtimeSpeechState state;
  final AnimationController haloController;
  final AnimationController waveController;
  final VoidCallback onToggleListening;
  final VoidCallback onClearAll;
  final VoidCallback onFinalize;
  final VoidCallback onRetrySynchronization;
  final Future<bool> Function(String segmentId, String text) onSegmentChanged;
  final Future<bool> Function(String segmentId) onSegmentDeleted;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final listening = state.status == SpeechStatus.listening;
    final processing = state.status == SpeechStatus.processing;
    final editable =
        !listening && !processing && !state.isSynchronizingTranscript;

    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 14, 16, 18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                ClinicalVoiceListeningSurface(
                  active: listening,
                  soundLevel: state.soundLevel,
                  haloController: haloController,
                  waveController: waveController,
                  badgeText: listening
                      ? l10n.voiceCaptureBadge
                      : l10n.voiceIdleBadge,
                  statusText: listening
                      ? l10n.voiceCaptureStatus
                      : state.hasTranscript
                      ? l10n.voiceCapturePausedStatus
                      : l10n.voiceStartNewDictation,
                  tipText: l10n.voiceCaptureTip,
                  stopLabel: l10n.voiceSaveDictation,
                  startLabel: state.hasTranscript
                      ? l10n.voiceResumeDictation
                      : l10n.assistantStartDictation,
                  onToggleListening: onToggleListening,
                ),
                const SizedBox(height: 18),
                ClinicalTranscriptTimeline(
                  segments: state.segments,
                  partialTranscript: state.partialTranscript,
                  partialOffset: state.partialOffset,
                  editable: editable,
                  onSegmentChanged: onSegmentChanged,
                  onSegmentDeleted: onSegmentDeleted,
                ),
                if (state.hasApplicableResult) ...[
                  const SizedBox(height: 14),
                  ClinicalVoiceProgressivePreview(state: state),
                ],
                if (processing) ...[
                  const SizedBox(height: 14),
                  LinearProgressIndicator(
                    borderRadius: BorderRadius.circular(
                      AppDesignTokens.radiusXs,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    l10n.voiceAnalyzingBody,
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                ],
                if (state.errorMessage != null) ...[
                  const SizedBox(height: 12),
                  _ErrorNotice(message: state.errorMessage!),
                ],
                const SizedBox(height: 12),
                _SyncNotice(state: state),
              ],
            ),
          ),
        ),
        if (state.hasTranscript && !listening)
          SafeArea(
            top: false,
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 10),
              decoration: BoxDecoration(
                color: colors.surface,
                border: Border(top: BorderSide(color: colors.outlineVariant)),
              ),
              child: Column(
                children: [
                  AppButton(
                    label: l10n.voiceAnalyzeAction,
                    icon: Icons.auto_awesome_rounded,
                    expand: true,
                    loading: processing,
                    onPressed: processing || state.isSynchronizingTranscript
                        ? null
                        : onFinalize,
                  ),
                  const SizedBox(height: 4),
                  if (state.hasTranscriptSyncFailure)
                    OutlinedButton.icon(
                      onPressed: onRetrySynchronization,
                      icon: const Icon(Icons.sync_rounded, size: 18),
                      label: Text(l10n.voiceRetrySave),
                    ),
                  TextButton.icon(
                    onPressed: processing ? null : onClearAll,
                    style: TextButton.styleFrom(foregroundColor: colors.error),
                    icon: const Icon(Icons.delete_sweep_outlined, size: 19),
                    label: Text(l10n.voiceClearAll),
                  ),
                ],
              ),
            ),
          ),
      ],
    );
  }
}

class _ReviewBody extends StatelessWidget {
  const _ReviewBody({
    required this.state,
    required this.isFrench,
    required this.onProposalDecision,
    required this.onRevisionDecision,
    required this.onApply,
    required this.onReturnToCapture,
  });

  final RealtimeSpeechState state;
  final bool isFrench;
  final ProposalDecisionCallback onProposalDecision;
  final RevisionDecisionCallback onRevisionDecision;
  final VoidCallback onApply;
  final VoidCallback onReturnToCapture;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final processing = state.status == SpeechStatus.processing;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: colors.primaryContainer.withValues(alpha: 0.32),
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
              border: Border.all(color: colors.primary.withValues(alpha: 0.35)),
            ),
            child: Text(
              l10n.voiceReviewTitle,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
          if (processing) ...[
            const SizedBox(height: 16),
            const Center(child: CircularProgressIndicator()),
          ],
          if (state.assistantMessage?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 14),
            Text(state.assistantMessage!),
          ],
          if (state.revisions.isNotEmpty) ...[
            const SizedBox(height: 18),
            ClinicalProposalReviewList(
              revisions: state.revisions,
              isFrench: isFrench,
              processing: processing,
              onProposalDecision: onProposalDecision,
              onRevisionDecision: onRevisionDecision,
            ),
          ],
          if (!state.hasPendingProposals && state.hasApplicableResult) ...[
            const SizedBox(height: 18),
            ClinicalAcceptedPreview(note: state.note, vitals: state.vitals),
          ],
          if (state.needsClarification || !state.hasApplicableResult) ...[
            const SizedBox(height: 12),
            _ErrorNotice(message: l10n.voiceClarificationRequired),
            const SizedBox(height: 8),
            OutlinedButton.icon(
              onPressed: processing ? null : onReturnToCapture,
              icon: const Icon(Icons.edit_note_rounded),
              label: Text(l10n.voiceResumeDictation),
            ),
          ],
          if (!state.hasPendingProposals &&
              state.hasApplicableResult &&
              !state.needsClarification) ...[
            const SizedBox(height: 18),
            AppButton(
              label: l10n.voiceApplyAction,
              icon: Icons.fact_check_rounded,
              expand: true,
              onPressed: processing ? null : onApply,
            ),
          ],
          if (state.errorMessage != null) ...[
            const SizedBox(height: 12),
            _ErrorNotice(message: state.errorMessage!),
          ],
        ],
      ),
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
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
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

class _SyncNotice extends StatelessWidget {
  const _SyncNotice({required this.state});

  final RealtimeSpeechState state;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final (icon, text, color) = switch (state.transcriptSyncStatus) {
      TranscriptSyncStatus.syncing => (
        Icons.sync_rounded,
        l10n.voiceTranscriptSaving,
        colors.primary,
      ),
      TranscriptSyncStatus.failed => (
        Icons.cloud_off_rounded,
        l10n.voiceTranscriptSaveFailed,
        colors.error,
      ),
      TranscriptSyncStatus.synced => (
        Icons.cloud_done_outlined,
        l10n.voiceTranscriptSaved,
        colors.onSurfaceVariant,
      ),
      TranscriptSyncStatus.idle => (
        Icons.verified_user_outlined,
        l10n.voicePrivacyNotice,
        colors.onSurfaceVariant,
      ),
    };
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Icon(icon, color: color, size: 17),
        const SizedBox(width: 7),
        Expanded(
          child: Text(
            text,
            style: Theme.of(
              context,
            ).textTheme.bodySmall?.copyWith(color: color, height: 1.35),
          ),
        ),
      ],
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

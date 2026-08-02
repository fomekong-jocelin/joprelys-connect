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
import '../../data/clinical_voice_ai_api.dart';
import '../../domain/active_visit.dart';
import '../clinical_voice_localizations.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_listening_surface.dart';
import 'clinical_voice_review_widgets.dart';
import 'clinical_voice_segment_timeline.dart';

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
    with TickerProviderStateMixin, WidgetsBindingObserver {
  late final ClinicalSpeechService _speechService;
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
    _speechService = ClinicalSpeechService(
      gateway: ref.read(clinicalVoiceAiApiProvider),
      visitId: widget.visit.id,
      initialDraft: widget.initialDraft,
      locale: widget.locale,
    );
    Future.microtask(_speechService.initialize);
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
    } else {
      await _speechService.startRealtimeListening();
    }
  }

  Future<void> _close() async {
    final safeToClose = await _speechService.prepareForClose();
    if (safeToClose && mounted) Navigator.of(context).pop();
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
    if (confirmed == true) await _speechService.clearTranscript();
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
    await _speechService.completeCapture();
    if (mounted) Navigator.of(context).pop();
  }

  String _reference() {
    var dpu = widget.visit.patientDpu.trim().replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!dpu.toUpperCase().startsWith('DPU-')) dpu = 'DPU-$dpu';
    return '${widget.visit.visitNumber} · ${dpu.replaceAll('-', '\u2011')}';
  }

  String _badge(RealtimeSpeechState state, AppLocalizations l10n) {
    if (state.stage == ClinicalVoiceStage.review) {
      return state.hasPendingProposals
          ? l10n.voiceDecisionRequired
          : l10n.voiceReviewComplete;
    }
    return switch (state.status) {
      SpeechStatus.listening => l10n.voiceCaptureBadge,
      SpeechStatus.processing => l10n.voiceSecureProcessing,
      SpeechStatus.transcriptReview => l10n.voiceTranscriptReady,
      _ => l10n.voiceIdleBadge,
    };
  }

  String _status(RealtimeSpeechState state, AppLocalizations l10n) {
    if (state.stage == ClinicalVoiceStage.review) {
      return l10n.voiceReviewSubtitle;
    }
    return switch (state.status) {
      SpeechStatus.listening => l10n.voiceCaptureStatus,
      SpeechStatus.processing => l10n.voiceAnalyzingTitle,
      SpeechStatus.transcriptReview => l10n.voiceCapturePausedStatus,
      _ => l10n.voiceStartNewDictation,
    };
  }

  @override
  Widget build(BuildContext context) {
    ref.listen(authControllerProvider, (previous, next) {
      if (previous?.value?.status == AuthStatus.locked &&
          next.value?.status == AuthStatus.authenticated) {
        unawaited(_speechService.resumeAfterLifecycle());
      }
    });

    final colors = Theme.of(context).colorScheme;

    return ValueListenableBuilder<RealtimeSpeechState>(
      valueListenable: _speechService,
      builder: (context, state, child) {
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
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
                child: _VoicePhaseIndicator(state: state),
              ),
              Expanded(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 220),
                  child: state.stage == ClinicalVoiceStage.capture
                      ? _CapturePhase(
                          key: const ValueKey('capture-phase'),
                          state: state,
                          haloController: _haloController,
                          waveController: _waveController,
                          badge: _badge(state, AppLocalizations.of(context)),
                          status: _status(state, AppLocalizations.of(context)),
                          onToggleListening: _toggleListening,
                          onSave: _speechService.saveDictationForReview,
                          onClearAll: _clearAll,
                          onAnalyze: _speechService.analyzeTranscript,
                          onRetrySave: _speechService.retryDraftSave,
                          onSegmentChanged: _speechService.updateSegment,
                          onSegmentDeleted: _speechService.deleteSegment,
                        )
                      : _ReviewPhase(
                          key: const ValueKey('review-phase'),
                          state: state,
                          isFrench: _isFrench,
                          onProposalDecision: _speechService.decideProposal,
                          onRevisionDecision: _speechService.decideRevision,
                          onApply: _applyAcceptedResult,
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

class _CapturePhase extends StatelessWidget {
  const _CapturePhase({
    required this.state,
    required this.haloController,
    required this.waveController,
    required this.badge,
    required this.status,
    required this.onToggleListening,
    required this.onSave,
    required this.onClearAll,
    required this.onAnalyze,
    required this.onRetrySave,
    required this.onSegmentChanged,
    required this.onSegmentDeleted,
    super.key,
  });

  final RealtimeSpeechState state;
  final AnimationController haloController;
  final AnimationController waveController;
  final String badge;
  final String status;
  final VoidCallback onToggleListening;
  final Future<bool> Function() onSave;
  final VoidCallback onClearAll;
  final VoidCallback onAnalyze;
  final VoidCallback onRetrySave;
  final Future<bool> Function(String segmentId, String text) onSegmentChanged;
  final Future<bool> Function(String segmentId) onSegmentDeleted;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final listening = state.status == SpeechStatus.listening;
    final processing = state.status == SpeechStatus.processing;
    final showActions = !processing && state.hasTranscript;

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
                  badgeText: badge,
                  statusText: status,
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
                  editable:
                      !listening &&
                      !processing &&
                      !state.isSynchronizingTranscript,
                  onSegmentChanged: onSegmentChanged,
                  onSegmentDeleted: onSegmentDeleted,
                ),
                if (processing) ...[
                  const SizedBox(height: 14),
                  _AnalysisProgressCard(
                    title: l10n.voiceAnalyzingTitle,
                    body: l10n.voiceAnalyzingBody,
                  ),
                ],
                if (state.errorMessage != null &&
                    !state.hasTranscriptSyncFailure) ...[
                  const SizedBox(height: 12),
                  _ErrorNotice(message: state.errorMessage!),
                ],
                if (state.isSynchronizingTranscript ||
                    state.hasTranscriptSyncFailure) ...[
                  const SizedBox(height: 12),
                  _TranscriptSyncNotice(state: state),
                ],
              ],
            ),
          ),
        ),
        if (showActions)
          _CaptureActionBar(
            state: state,
            onSave: onSave,
            onClearAll: onClearAll,
            onAnalyze: onAnalyze,
            onRetrySave: onRetrySave,
          ),
      ],
    );
  }
}

class _CaptureActionBar extends StatelessWidget {
  const _CaptureActionBar({
    required this.state,
    required this.onSave,
    required this.onClearAll,
    required this.onAnalyze,
    required this.onRetrySave,
  });

  final RealtimeSpeechState state;
  final Future<bool> Function() onSave;
  final VoidCallback onClearAll;
  final VoidCallback onAnalyze;
  final VoidCallback onRetrySave;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final readyForAnalysis = state.isTranscriptReadyForAnalysis;
    final saving = state.isSynchronizingTranscript;
    final mutationDisabled = saving;

    return SafeArea(
      top: false,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 10),
        decoration: BoxDecoration(
          color: colors.surface,
          border: Border(top: BorderSide(color: colors.outlineVariant)),
          boxShadow: [
            BoxShadow(
              color: colors.shadow.withValues(alpha: 0.08),
              blurRadius: 12,
              offset: const Offset(0, -3),
            ),
          ],
        ),
        child: Column(
          children: [
            SizedBox(
              width: double.infinity,
              height: 50,
              child: FilledButton.icon(
                onPressed: saving
                    ? null
                    : readyForAnalysis
                    ? onAnalyze
                    : () => onSave(),
                icon: saving
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2.4),
                      )
                    : Icon(
                        readyForAnalysis
                            ? Icons.auto_awesome_rounded
                            : Icons.save_rounded,
                      ),
                label: Text(
                  readyForAnalysis
                      ? l10n.voiceAnalyzeAction
                      : l10n.voiceSaveDictation,
                  maxLines: 2,
                  textAlign: TextAlign.center,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontWeight: FontWeight.w900),
                ),
              ),
            ),
            if (state.hasTranscriptSyncFailure) ...[
              const SizedBox(height: 6),
              OutlinedButton.icon(
                onPressed: onRetrySave,
                icon: const Icon(Icons.sync_rounded, size: 18),
                label: Text(l10n.voiceRetrySave),
              ),
            ],
            const SizedBox(height: 4),
            TextButton.icon(
              onPressed: mutationDisabled ? null : onClearAll,
              style: TextButton.styleFrom(foregroundColor: colors.error),
              icon: const Icon(Icons.delete_sweep_outlined, size: 19),
              label: Text(l10n.voiceClearAll),
            ),
          ],
        ),
      ),
    );
  }
}

class _TranscriptSyncNotice extends StatelessWidget {
  const _TranscriptSyncNotice({required this.state});

  final RealtimeSpeechState state;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final (icon, message, color) = switch (state.transcriptSyncStatus) {
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
        Icon(icon, size: 17, color: color),
        const SizedBox(width: 7),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                message,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: color,
                  height: 1.35,
                  fontWeight: state.hasTranscriptSyncFailure
                      ? FontWeight.w700
                      : FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ReviewPhase extends StatelessWidget {
  const _ReviewPhase({
    required this.state,
    required this.isFrench,
    required this.onProposalDecision,
    required this.onRevisionDecision,
    required this.onApply,
    super.key,
  });

  final RealtimeSpeechState state;
  final bool isFrench;
  final ProposalDecisionCallback onProposalDecision;
  final RevisionDecisionCallback onRevisionDecision;
  final VoidCallback onApply;

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
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.fact_check_rounded, color: colors.primary),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.voiceReviewTitle,
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        l10n.voiceReviewSubtitle,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onSurfaceVariant,
                          height: 1.4,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          if (processing) ...[
            const SizedBox(height: 16),
            const Center(child: CircularProgressIndicator()),
          ],
          if (state.assistantMessage?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 14),
            Text(
              state.assistantMessage!,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
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
          if (state.needsClarification) ...[
            const SizedBox(height: 12),
            _ErrorNotice(message: l10n.voiceClarificationRequired),
          ],
          if (!state.hasPendingProposals && state.hasApplicableResult) ...[
            const SizedBox(height: 18),
            ClinicalAcceptedPreview(note: state.note, vitals: state.vitals),
          ],
          if (state.status == SpeechStatus.done &&
              state.hasApplicableResult &&
              !state.needsClarification) ...[
            const SizedBox(height: 18),
            AppButton(
              label: l10n.voiceApplyAction,
              icon: Icons.fact_check_rounded,
              expand: true,
              onPressed: onApply,
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

class _VoicePhaseIndicator extends StatelessWidget {
  const _VoicePhaseIndicator({required this.state});

  final RealtimeSpeechState state;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final showReviewStep =
        state.stage == ClinicalVoiceStage.review ||
        state.isTranscriptReadyForAnalysis;
    return Row(
      children: [
        Expanded(
          child: _PhasePill(
            label: l10n.voiceCaptureStep,
            active: !showReviewStep,
            completed: showReviewStep,
          ),
        ),
        if (showReviewStep) ...[
          const SizedBox(width: 8),
          Expanded(
            child: _PhasePill(
              label: l10n.voiceReviewStep,
              active: true,
              completed: state.stage == ClinicalVoiceStage.review,
            ),
          ),
        ],
      ],
    );
  }
}

class _PhasePill extends StatelessWidget {
  const _PhasePill({
    required this.label,
    required this.active,
    required this.completed,
  });

  final String label;
  final bool active;
  final bool completed;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: active ? colors.primaryContainer : colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(
          color: active ? colors.primary : colors.outlineVariant,
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            completed ? Icons.check_circle_rounded : Icons.circle_outlined,
            size: 16,
            color: active || completed
                ? colors.primary
                : colors.onSurfaceVariant,
          ),
          const SizedBox(width: 6),
          Flexible(
            child: Text(
              label,
              overflow: TextOverflow.ellipsis,
              style: theme.textTheme.labelMedium?.copyWith(
                color: active ? colors.onPrimaryContainer : null,
                fontWeight: active ? FontWeight.w900 : FontWeight.w700,
              ),
            ),
          ),
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

class _AnalysisProgressCard extends StatelessWidget {
  const _AnalysisProgressCard({required this.title, required this.body});

  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: colors.secondaryContainer.withValues(alpha: 0.42),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
      ),
      child: Row(
        children: [
          const SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(strokeWidth: 3),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: theme.textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
                ),
                const SizedBox(height: 2),
                Text(body, style: theme.textTheme.bodySmall),
              ],
            ),
          ),
        ],
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

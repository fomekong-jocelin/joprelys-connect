import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/lifecycle/app_activity_registry.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../auth/application/auth_controller.dart';
import '../../application/clinical_dictation_parser.dart';
import '../../application/cloud_speech_streaming_service.dart';
import '../../application/clinical_voice_progressive_coordinator.dart';
import '../../data/clinical_voice_ai_api.dart';
import '../../data/clinical_voice_capture_api.dart';
import '../../domain/active_visit.dart';
import '../clinical_voice_localizations.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_progressive_assistant_sections.dart';

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
      isDismissible: true,
      enableDrag: true,
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
  late final CloudSpeechStreamingService _speechService;
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
    final authState = ref.read(authControllerProvider).value;
    final jwtToken = authState?.session?.accessToken ?? '';

    _speechService = CloudSpeechStreamingService(
      gateway: aiGateway,
      visitId: widget.visit.id,
      initialDraft: widget.initialDraft,
      locale: widget.locale,
      apiBaseUri: AppConfig.runtime.apiBaseUri,
      jwtToken: jwtToken,
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

  void _close() {
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

    final result = DictationParseResult(vitals: state.vitals, note: state.note);
    final completed = await _coordinator.completeCapture();
    if (!completed || !mounted) return;

    widget.onExtracted(result);
    Navigator.of(context).pop();
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
                onClose: _close,
              ),
              const Divider(height: 1),
              Expanded(
                child: state.stage == ClinicalVoiceStage.capture
                    ? ClinicalVoiceProgressiveCaptureBody(
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
                    : ClinicalVoiceProgressiveReviewBody(
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

class _Header extends StatelessWidget {
  const _Header({
    required this.patientName,
    required this.reference,
    required this.onClose,
  });

  final String patientName;
  final String reference;
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
          IconButton(onPressed: onClose, icon: const Icon(Icons.close_rounded)),
        ],
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../application/clinical_voice_state.dart';
import '../clinical_voice_localizations.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_listening_surface.dart';
import 'clinical_voice_review_widgets.dart';
import 'clinical_voice_segment_timeline.dart';

class ClinicalVoiceProgressiveCaptureBody extends StatelessWidget {
  const ClinicalVoiceProgressiveCaptureBody({
    required this.state,
    required this.haloController,
    required this.waveController,
    required this.onToggleListening,
    required this.onClearAll,
    required this.onFinalize,
    required this.onRetrySynchronization,
    required this.onSegmentChanged,
    required this.onSegmentDeleted,
    super.key,
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
                if (listening && state.transcript.trim().isNotEmpty) ...[
                  const SizedBox(height: 14),
                  _LiveTranscript(text: state.transcript),
                ],
                if (!listening && state.hasTranscript) ...[
                  const SizedBox(height: 16),
                  ClinicalTranscriptTimeline(
                    segments: state.segments,
                    partialTranscript: state.partialTranscript,
                    partialOffset: state.partialOffset,
                    editable: editable,
                    onSegmentChanged: onSegmentChanged,
                    onSegmentDeleted: onSegmentDeleted,
                  ),
                ],
                if (!listening && state.errorMessage != null) ...[
                  const SizedBox(height: 12),
                  _ErrorNotice(message: state.errorMessage!),
                ],
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

class _LiveTranscript extends StatelessWidget {
  const _LiveTranscript({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Text(
        text,
        style: theme.textTheme.bodyLarge?.copyWith(
          height: 1.45,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}

class ClinicalVoiceProgressiveReviewBody extends StatelessWidget {
  const ClinicalVoiceProgressiveReviewBody({
    required this.state,
    required this.isFrench,
    required this.onProposalDecision,
    required this.onRevisionDecision,
    required this.onApply,
    required this.onReturnToCapture,
    super.key,
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
            ClinicalAcceptedPreview(
              note: state.note,
              vitals: state.vitals,
              isFrench: isFrench,
            ),
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
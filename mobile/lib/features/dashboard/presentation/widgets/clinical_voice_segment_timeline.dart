import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../application/clinical_voice_state.dart';
import '../clinical_voice_localizations.dart';

String formatClinicalTranscriptOffset(Duration offset) {
  final totalSeconds = offset.inSeconds.clamp(0, 359999);
  final hours = totalSeconds ~/ 3600;
  final minutes = (totalSeconds % 3600) ~/ 60;
  final seconds = totalSeconds % 60;
  final mm = minutes.toString().padLeft(2, '0');
  final ss = seconds.toString().padLeft(2, '0');
  if (hours == 0) return '$mm:$ss';
  return '${hours.toString().padLeft(2, '0')}:$mm:$ss';
}

class ClinicalTranscriptTimeline extends StatelessWidget {
  const ClinicalTranscriptTimeline({
    required this.segments,
    required this.partialTranscript,
    required this.partialOffset,
    required this.editable,
    required this.onSegmentChanged,
    required this.onSegmentDeleted,
    super.key,
  });

  final List<ClinicalTranscriptSegment> segments;
  final String partialTranscript;
  final Duration partialOffset;
  final bool editable;
  final Future<bool> Function(String segmentId, String text) onSegmentChanged;
  final Future<bool> Function(String segmentId) onSegmentDeleted;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final hasContent =
        segments.isNotEmpty || partialTranscript.trim().isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          l10n.voiceSegmentsTitle,
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 3),
        Text(
          l10n.voiceSegmentsSubtitle,
          style: theme.textTheme.bodySmall?.copyWith(
            color: colors.onSurfaceVariant,
            height: 1.35,
          ),
        ),
        const SizedBox(height: 12),
        if (!hasContent)
          _EmptyTranscriptState(
            title: l10n.voiceSegmentsEmptyTitle,
            body: l10n.voiceSegmentsEmptyBody,
          )
        else ...[
          for (var index = 0; index < segments.length; index++)
            _TimelineRow(
              markerActive: false,
              isLast:
                  index == segments.length - 1 &&
                  partialTranscript.trim().isEmpty,
              child: _TranscriptSegmentCard(
                key: ValueKey(segments[index].id),
                segment: segments[index],
                index: index + 1,
                editable: editable,
                onChanged: onSegmentChanged,
                onDeleted: onSegmentDeleted,
              ),
            ),
          if (partialTranscript.trim().isNotEmpty)
            _TimelineRow(
              markerActive: true,
              isLast: true,
              child: _LiveSegmentCard(
                text: partialTranscript,
                offset: partialOffset,
              ),
            ),
        ],
      ],
    );
  }
}

class _TimelineRow extends StatelessWidget {
  const _TimelineRow({
    required this.markerActive,
    required this.isLast,
    required this.child,
  });

  final bool markerActive;
  final bool isLast;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return IntrinsicHeight(
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            width: 18,
            child: Column(
              children: [
                const SizedBox(height: 16),
                Container(
                  width: 9,
                  height: 9,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: markerActive ? colors.primary : colors.surface,
                    border: Border.all(
                      color: markerActive
                          ? colors.primary
                          : colors.outlineVariant,
                      width: 2,
                    ),
                  ),
                ),
                if (!isLast)
                  Expanded(
                    child: Container(
                      width: 1.5,
                      margin: const EdgeInsets.symmetric(vertical: 4),
                      color: colors.outlineVariant,
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: child,
            ),
          ),
        ],
      ),
    );
  }
}

class _TranscriptSegmentCard extends StatefulWidget {
  const _TranscriptSegmentCard({
    required this.segment,
    required this.index,
    required this.editable,
    required this.onChanged,
    required this.onDeleted,
    super.key,
  });

  final ClinicalTranscriptSegment segment;
  final int index;
  final bool editable;
  final Future<bool> Function(String segmentId, String text) onChanged;
  final Future<bool> Function(String segmentId) onDeleted;

  @override
  State<_TranscriptSegmentCard> createState() => _TranscriptSegmentCardState();
}

class _TranscriptSegmentCardState extends State<_TranscriptSegmentCard> {
  late final TextEditingController _controller;
  bool _editing = false;
  bool _busy = false;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.segment.text);
  }

  @override
  void didUpdateWidget(covariant _TranscriptSegmentCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_editing && oldWidget.segment.text != widget.segment.text) {
      _controller.text = widget.segment.text;
    }
    if (!widget.editable) _editing = false;
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_busy) return;
    setState(() => _busy = true);
    final saved = await widget.onChanged(widget.segment.id, _controller.text);
    if (!mounted) return;
    setState(() {
      _busy = false;
      if (saved) _editing = false;
    });
  }

  Future<void> _delete() async {
    if (_busy) return;
    setState(() => _busy = true);
    final deleted = await widget.onDeleted(widget.segment.id);
    if (!mounted || deleted) return;
    setState(() => _busy = false);
  }

  void _cancelEditing() {
    _controller.text = widget.segment.text;
    setState(() => _editing = false);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      padding: const EdgeInsets.fromLTRB(12, 10, 12, 12),
      decoration: BoxDecoration(
        color: _editing
            ? colors.primaryContainer.withValues(alpha: 0.2)
            : colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(
          color: _editing
              ? colors.primary.withValues(alpha: 0.65)
              : colors.outlineVariant,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                decoration: BoxDecoration(
                  color: colors.surfaceContainerHighest,
                  borderRadius: BorderRadius.circular(AppDesignTokens.radiusXs),
                ),
                child: Text(
                  formatClinicalTranscriptOffset(widget.segment.offset),
                  style: theme.textTheme.labelSmall?.copyWith(
                    fontFeatures: const [FontFeature.tabularFigures()],
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  l10n.voiceSegmentLabel(widget.index),
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: colors.onSurfaceVariant,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              if (_busy)
                const SizedBox(
                  width: 28,
                  height: 28,
                  child: Padding(
                    padding: EdgeInsets.all(5),
                    child: CircularProgressIndicator(strokeWidth: 2.5),
                  ),
                )
              else if (widget.editable && !_editing) ...[
                IconButton.filledTonal(
                  visualDensity: VisualDensity.compact,
                  tooltip: l10n.voiceEditSegment,
                  onPressed: () => setState(() => _editing = true),
                  icon: const Icon(Icons.edit_rounded, size: 17),
                ),
                const SizedBox(width: 4),
                IconButton.filledTonal(
                  visualDensity: VisualDensity.compact,
                  tooltip: l10n.voiceDeleteSegment,
                  onPressed: _delete,
                  style: IconButton.styleFrom(
                    foregroundColor: colors.error,
                    backgroundColor: colors.errorContainer.withValues(alpha: 0.55),
                  ),
                  icon: const Icon(Icons.delete_outline_rounded, size: 17),
                ),
              ],
            ],
          ),
          const SizedBox(height: 9),
          if (_editing)
            TextField(
              controller: _controller,
              autofocus: true,
              minLines: 2,
              maxLines: 8,
              textCapitalization: TextCapitalization.sentences,
              decoration: InputDecoration(
                filled: true,
                fillColor: colors.surface,
                contentPadding: const EdgeInsets.all(12),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
                ),
              ),
            )
          else
            SelectableText(
              widget.segment.text,
              style: theme.textTheme.bodyMedium?.copyWith(height: 1.48),
            ),
          if (_editing) ...[
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: _busy ? null : _cancelEditing,
                  child: Text(l10n.voiceCancel),
                ),
                const SizedBox(width: 6),
                FilledButton.tonalIcon(
                  onPressed: _busy ? null : _save,
                  icon: _busy
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.check_rounded, size: 17),
                  label: Text(l10n.voiceSaveSegment),
                ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _LiveSegmentCard extends StatelessWidget {
  const _LiveSegmentCard({required this.text, required this.offset});

  final String text;
  final Duration offset;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.primaryContainer.withValues(alpha: 0.28),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.primary.withValues(alpha: 0.5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text(
                formatClinicalTranscriptOffset(offset),
                style: theme.textTheme.labelSmall?.copyWith(
                  color: colors.primary,
                  fontWeight: FontWeight.w900,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  l10n.voiceLiveSegment,
                  style: theme.textTheme.labelMedium?.copyWith(
                    color: colors.primary,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              const SizedBox(
                width: 34,
                child: LinearProgressIndicator(minHeight: 3),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            text,
            style: theme.textTheme.bodyMedium?.copyWith(
              height: 1.45,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyTranscriptState extends StatelessWidget {
  const _EmptyTranscriptState({required this.title, required this.body});

  final String title;
  final String body;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 20),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Column(
        children: [
          Icon(Icons.notes_rounded, color: colors.primary, size: 28),
          const SizedBox(height: 8),
          Text(
            title,
            textAlign: TextAlign.center,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            body,
            textAlign: TextAlign.center,
            style: theme.textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }
}

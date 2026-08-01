import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../shared/widgets/app_text_field.dart';

class VitalExtractPill extends StatelessWidget {
  const VitalExtractPill({required this.label, super.key});

  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.primary.withValues(alpha: 0.4)),
      ),
      child: Text(
        label,
        style: theme.textTheme.labelSmall?.copyWith(
          color: colors.primary,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class ClinicalTranscriptCard extends StatefulWidget {
  const ClinicalTranscriptCard({
    required this.transcript,
    required this.controller,
    required this.onChanged,
    required this.label,
    required this.hint,
    required this.editLabel,
    required this.doneLabel,
    this.editable = true,
    super.key,
  });

  final String transcript;
  final TextEditingController controller;
  final ValueChanged<String> onChanged;
  final String label;
  final String hint;
  final String editLabel;
  final String doneLabel;
  final bool editable;

  @override
  State<ClinicalTranscriptCard> createState() => _ClinicalTranscriptCardState();
}

class _ClinicalTranscriptCardState extends State<ClinicalTranscriptCard> {
  bool _isEditing = false;

  @override
  void didUpdateWidget(covariant ClinicalTranscriptCard oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!widget.editable && _isEditing) {
      _isEditing = false;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final primaryColor = colors.primary;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: theme.textTheme.titleSmall?.copyWith(
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 8),
        if (_isEditing && widget.editable) ...[
          AppTextField(
            label: '',
            hint: widget.hint,
            controller: widget.controller,
            onChanged: widget.onChanged,
            maxLines: 5,
          ),
          const SizedBox(height: 6),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton.icon(
              onPressed: () => setState(() => _isEditing = false),
              icon: const Icon(Icons.check_rounded, size: 16),
              label: Text(widget.doneLabel),
            ),
          ),
        ] else ...[
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: colors.primary.withValues(alpha: 0.04),
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
              border: Border.all(color: colors.primary.withValues(alpha: 0.2)),
            ),
            child: widget.transcript.trim().isEmpty
                ? Text(
                    widget.hint,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: colors.onSurfaceVariant,
                      fontStyle: FontStyle.italic,
                    ),
                  )
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        widget.transcript,
                        style: theme.textTheme.bodyMedium?.copyWith(
                          fontWeight: FontWeight.w700,
                          height: 1.4,
                        ),
                      ),
                      if (widget.editable) ...[
                        const SizedBox(height: 10),
                        GestureDetector(
                          onTap: () => setState(() => _isEditing = true),
                          child: Text(
                            widget.editLabel,
                            style: theme.textTheme.labelMedium?.copyWith(
                              color: primaryColor,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
          ),
        ],
      ],
    );
  }
}

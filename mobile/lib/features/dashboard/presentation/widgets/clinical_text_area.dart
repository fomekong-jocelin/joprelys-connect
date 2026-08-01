import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';

class LabeledClinicalTextArea extends StatelessWidget {
  const LabeledClinicalTextArea({
    required this.label,
    required this.hint,
    required this.controller,
    required this.maxLength,
    this.isRequired = false,
    this.requiredMessage,
    super.key,
  });

  final String label;
  final String hint;
  final TextEditingController controller;
  final int maxLength;
  final bool isRequired;
  final String? requiredMessage;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          isRequired ? '$label *' : label,
          style: theme.textTheme.labelMedium?.copyWith(
            fontWeight: FontWeight.w700,
          ),
        ),
        const SizedBox(height: 6),
        ClinicalTextArea(
          controller: controller,
          hint: hint,
          maxLength: maxLength,
          requiredMessage: requiredMessage,
          minLines: 2,
        ),
      ],
    );
  }
}

class ClinicalTextArea extends StatelessWidget {
  const ClinicalTextArea({
    required this.controller,
    required this.hint,
    required this.maxLength,
    this.requiredMessage,
    this.minLines = 3,
    super.key,
  });

  final TextEditingController controller;
  final String hint;
  final int maxLength;
  final String? requiredMessage;
  final int minLines;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return TextFormField(
      controller: controller,
      maxLines: null,
      minLines: minLines,
      maxLength: maxLength,
      autovalidateMode: AutovalidateMode.onUserInteraction,
      validator: requiredMessage == null
          ? null
          : (value) =>
                value == null || value.trim().isEmpty ? requiredMessage : null,
      textInputAction: TextInputAction.newline,
      style: theme.textTheme.bodyMedium?.copyWith(height: 1.5),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: theme.textTheme.bodySmall?.copyWith(
          color: colors.onSurfaceVariant.withValues(alpha: 0.5),
          fontStyle: FontStyle.italic,
        ),
        filled: true,
        fillColor: colors.surfaceContainerHighest.withValues(alpha: 0.3),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 14,
          vertical: 12,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          borderSide: BorderSide(
            color: colors.outlineVariant.withValues(alpha: 0.4),
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          borderSide: BorderSide(color: colors.primary, width: 1.5),
        ),
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../../core/theme/app_design_tokens.dart';

enum AppTextFieldLabelPosition { inline, above }

class AppTextField extends StatelessWidget {
  const AppTextField({
    this.controller,
    this.label,
    this.hint,
    this.errorText,
    this.helperText,
    this.keyboardType,
    this.textInputAction,
    this.onChanged,
    this.onSubmitted,
    this.autofillHints,
    this.enabled = true,
    this.obscureText = false,
    this.autocorrect = true,
    this.enableSuggestions = true,
    this.maxLines = 1,
    this.minLines,
    this.prefixIcon,
    this.suffixIcon,
    this.labelPosition = AppTextFieldLabelPosition.inline,
    this.minimumHeight,
    super.key,
  });

  final TextEditingController? controller;
  final String? label;
  final String? hint;
  final String? errorText;
  final String? helperText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final Iterable<String>? autofillHints;
  final bool enabled;
  final bool obscureText;
  final bool autocorrect;
  final bool enableSuggestions;
  final int? maxLines;
  final int? minLines;
  final Widget? prefixIcon;
  final Widget? suffixIcon;
  final AppTextFieldLabelPosition labelPosition;
  final double? minimumHeight;

  @override
  Widget build(BuildContext context) {
    final textField = TextField(
      controller: controller,
      enabled: enabled,
      obscureText: obscureText,
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      onChanged: onChanged,
      onSubmitted: onSubmitted,
      autofillHints: autofillHints,
      autocorrect: autocorrect,
      enableSuggestions: enableSuggestions,
      maxLines: obscureText ? 1 : maxLines,
      minLines: obscureText ? 1 : minLines,
      decoration: InputDecoration(
        labelText: labelPosition == AppTextFieldLabelPosition.inline
            ? label
            : null,
        hintText: hint,
        errorText: errorText,
        helperText: helperText,
        prefixIcon: prefixIcon,
        suffixIcon: suffixIcon,
        constraints: minimumHeight == null
            ? null
            : BoxConstraints(minHeight: minimumHeight!),
      ),
    );

    if (label == null || labelPosition == AppTextFieldLabelPosition.inline) {
      return textField;
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(label!, style: Theme.of(context).textTheme.labelLarge),
        const SizedBox(height: AppDesignTokens.spaceSm),
        Semantics(label: label, textField: true, child: textField),
      ],
    );
  }
}

import 'package:flutter/material.dart';

import '../../core/theme/app_design_tokens.dart';

enum AppButtonVariant { primary, secondary, destructive }

class AppButton extends StatelessWidget {
  const AppButton({
    required this.label,
    required this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.icon,
    this.loading = false,
    this.expand = false,
    super.key,
  });

  final String label;
  final VoidCallback? onPressed;
  final AppButtonVariant variant;
  final IconData? icon;
  final bool loading;
  final bool expand;

  @override
  Widget build(BuildContext context) {
    final action = loading ? null : onPressed;
    final child = _ButtonContent(
      label: label,
      icon: icon,
      loading: loading,
      foregroundColor: variant == AppButtonVariant.secondary
          ? Theme.of(context).colorScheme.primary
          : AppDesignTokens.onStrongColor,
    );

    final button = switch (variant) {
      AppButtonVariant.primary => ElevatedButton(
        onPressed: action,
        child: child,
      ),
      AppButtonVariant.secondary => OutlinedButton(
        onPressed: action,
        child: child,
      ),
      AppButtonVariant.destructive => ElevatedButton(
        onPressed: action,
        style: ElevatedButton.styleFrom(
          minimumSize: const Size(0, AppDesignTokens.minTouchTarget),
          backgroundColor: AppDesignTokens.error,
          foregroundColor: AppDesignTokens.onStrongColor,
          disabledBackgroundColor: Theme.of(context).disabledColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          ),
          elevation: 0,
        ),
        child: child,
      ),
    };

    return SizedBox(width: expand ? double.infinity : null, child: button);
  }
}

class _ButtonContent extends StatelessWidget {
  const _ButtonContent({
    required this.label,
    required this.loading,
    required this.foregroundColor,
    this.icon,
  });

  final String label;
  final bool loading;
  final Color foregroundColor;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (loading) ...[
          SizedBox.square(
            dimension: 18,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              color: foregroundColor,
            ),
          ),
          const SizedBox(width: AppDesignTokens.spaceSm),
        ] else if (icon != null) ...[
          Icon(icon, size: 18),
          const SizedBox(width: AppDesignTokens.spaceSm),
        ],
        Flexible(
          child: Text(label, maxLines: 1, overflow: TextOverflow.ellipsis),
        ),
      ],
    );
  }
}

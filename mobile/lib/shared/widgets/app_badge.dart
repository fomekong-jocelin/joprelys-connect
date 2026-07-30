import 'package:flutter/material.dart';

import '../../core/theme/app_design_tokens.dart';

class AppBadge extends StatelessWidget {
  const AppBadge({
    required this.label,
    this.tone = AppSemanticTone.neutral,
    this.icon,
    super.key,
  });

  final String label;
  final AppSemanticTone tone;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final foreground = _toneColor(context);

    return DecoratedBox(
      decoration: BoxDecoration(
        color: foreground.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(
          color: foreground.withValues(alpha: 0.32),
          width: AppDesignTokens.borderWidth,
        ),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(
          horizontal: AppDesignTokens.spaceSm,
          vertical: AppDesignTokens.spaceXs,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (icon != null) ...[
              Icon(icon, size: 14, color: foreground),
              const SizedBox(width: AppDesignTokens.spaceXs),
            ],
            Text(
              label,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(
                context,
              ).textTheme.labelMedium?.copyWith(color: foreground),
            ),
          ],
        ),
      ),
    );
  }

  Color _toneColor(BuildContext context) {
    return switch (tone) {
      AppSemanticTone.neutral => Theme.of(context).colorScheme.secondary,
      AppSemanticTone.success => AppDesignTokens.success,
      AppSemanticTone.warning => AppDesignTokens.warning,
      AppSemanticTone.error => AppDesignTokens.error,
      AppSemanticTone.info => AppDesignTokens.info,
    };
  }
}

import 'package:flutter/material.dart';

import '../../core/config/app_config.dart';
import '../../core/theme/app_design_tokens.dart';

class AppBrandLockup extends StatelessWidget {
  const AppBrandLockup({this.logoWidth = 132, this.compact = false, super.key});

  final double logoWidth;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Semantics(
      label: AppConfig.appName,
      image: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(
            padding: EdgeInsets.symmetric(
              horizontal: compact ? 6 : AppDesignTokens.spaceSm,
              vertical: compact ? 4 : 6,
            ),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(
                color: colors.outlineVariant.withValues(alpha: 0.7),
              ),
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
            ),
            child: Image.asset(
              AppConfig.logoAsset,
              width: logoWidth,
              fit: BoxFit.contain,
              alignment: Alignment.centerLeft,
              filterQuality: FilterQuality.high,
              gaplessPlayback: true,
            ),
          ),
          SizedBox(
            width: compact ? AppDesignTokens.spaceSm : AppDesignTokens.spaceMd,
          ),
          Text(
            AppConfig.productName,
            style:
                (compact
                        ? theme.textTheme.titleLarge
                        : theme.textTheme.headlineMedium)
                    ?.copyWith(
                      color: colors.onSurface,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.5,
                    ),
          ),
        ],
      ),
    );
  }
}

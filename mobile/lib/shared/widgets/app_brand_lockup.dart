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
    final isDark = theme.brightness == Brightness.dark;
    final logoAspectRatio = isDark ? 1800 / 1012 : 946 / 506;

    return Semantics(
      label: AppConfig.appName,
      image: true,
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
            child: SizedBox(
              width: logoWidth,
              child: AspectRatio(
                aspectRatio: logoAspectRatio,
                child: Image.asset(
                  isDark ? AppConfig.logoOnDarkAsset : AppConfig.logoAsset,
                  fit: BoxFit.cover,
                  filterQuality: FilterQuality.high,
                ),
              ),
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
                      color: theme.colorScheme.onSurface,
                      fontWeight: FontWeight.w800,
                      letterSpacing: -0.5,
                    ),
          ),
        ],
      ),
    );
  }
}

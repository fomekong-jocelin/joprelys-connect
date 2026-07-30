import 'package:flutter/material.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/theme/app_design_tokens.dart';
import 'auth_preferences_bar.dart';

class AuthShell extends StatelessWidget {
  const AuthShell({
    required this.title,
    required this.child,
    this.subtitle,
    super.key,
  });

  final String title;
  final String? subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              colors.primary.withValues(alpha: 0.08),
              colors.surface,
              colors.surface,
            ],
            stops: const [0, 0.32, 1],
          ),
        ),
        child: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    minHeight: constraints.maxHeight - 40,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Align(
                        alignment: Alignment.centerRight,
                        child: AuthPreferencesBar(),
                      ),
                      const SizedBox(height: AppDesignTokens.spaceMd),
                      const _JoprelysBrandHeader(),
                      const SizedBox(height: AppDesignTokens.spaceMd),
                      Align(
                        alignment: Alignment.topCenter,
                        child: ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 440),
                          child: Container(
                            padding: const EdgeInsets.all(20),
                            decoration: BoxDecoration(
                              color: colors.surface,
                              border: Border.all(color: colors.outlineVariant),
                              borderRadius: BorderRadius.circular(
                                AppDesignTokens.radiusLg,
                              ),
                              boxShadow:
                                  Theme.of(context).brightness == Brightness.dark
                                  ? AppDesignTokens.darkPanelShadow
                                  : AppDesignTokens.lightPanelShadow,
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const _JoprelysBrandIcon(),
                                    const SizedBox(
                                      width: AppDesignTokens.spaceMd,
                                    ),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            title,
                                            style: Theme.of(context)
                                                .textTheme
                                                .titleLarge
                                                ?.copyWith(
                                                  fontWeight: FontWeight.w700,
                                                  height: 1.2,
                                                ),
                                          ),
                                          if (subtitle != null) ...[
                                            const SizedBox(
                                              height: AppDesignTokens.spaceXs,
                                            ),
                                            Text(
                                              subtitle!,
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodySmall
                                                  ?.copyWith(
                                                    color: colors
                                                        .onSurfaceVariant,
                                                    height: 1.4,
                                                  ),
                                            ),
                                          ],
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(
                                  height: AppDesignTokens.spaceLg,
                                ),
                                child,
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

class _JoprelysBrandHeader extends StatelessWidget {
  const _JoprelysBrandHeader();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;
    final logoPath = isDark
        ? AppConfig.logoOnDarkAsset
        : AppConfig.logoAsset;

    return Align(
      alignment: Alignment.center,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440),
        child: Semantics(
          label: AppConfig.appName,
          image: true,
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            mainAxisSize: MainAxisSize.min,
            children: [
              SizedBox(
                width: 154,
                height: 44,
                child: Image.asset(
                  logoPath,
                  alignment: Alignment.centerRight,
                  fit: BoxFit.contain,
                  filterQuality: FilterQuality.high,
                  errorBuilder: (context, error, stackTrace) => Text(
                    AppConfig.appShortName,
                    textAlign: TextAlign.right,
                    style: theme.textTheme.titleLarge?.copyWith(
                      color: colors.primary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Text(
                AppConfig.productName,
                style: theme.textTheme.titleLarge?.copyWith(
                  color: isDark ? colors.onSurface : AppDesignTokens.brandNight,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.3,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _JoprelysBrandIcon extends StatelessWidget {
  const _JoprelysBrandIcon();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Semantics(
      label: AppConfig.appShortName,
      image: true,
      child: SizedBox(
        width: 46,
        height: 46,
        child: Image.asset(
          AppConfig.logoIconAsset,
          fit: BoxFit.contain,
          filterQuality: FilterQuality.high,
          errorBuilder: (context, error, stackTrace) => Container(
            decoration: BoxDecoration(
              color: colors.primaryContainer,
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
            ),
            child: Icon(
              Icons.health_and_safety_outlined,
              size: 24,
              color: colors.onPrimaryContainer,
            ),
          ),
        ),
      ),
    );
  }
}

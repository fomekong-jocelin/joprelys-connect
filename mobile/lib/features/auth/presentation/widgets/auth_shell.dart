import 'package:flutter/material.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/theme/app_design_tokens.dart';
import 'auth_preferences_bar.dart';

class AuthShell extends StatelessWidget {
  const AuthShell({
    required this.icon,
    required this.title,
    required this.child,
    this.subtitle,
    super.key,
  });

  final IconData icon;
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
            stops: const [0, 0.36, 1],
          ),
        ),
        child: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(20, 14, 20, 28),
                child: ConstrainedBox(
                  constraints: BoxConstraints(
                    minHeight: constraints.maxHeight - 42,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Align(
                        alignment: Alignment.centerRight,
                        child: AuthPreferencesBar(),
                      ),
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      const _JoprelysBrandHeader(),
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      Align(
                        alignment: Alignment.topCenter,
                        child: ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 440),
                          child: Container(
                            padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
                            decoration: BoxDecoration(
                              color: colors.surface,
                              border: Border.all(color: colors.outlineVariant),
                              borderRadius: BorderRadius.circular(
                                AppDesignTokens.radiusLg,
                              ),
                              boxShadow: Theme.of(context).brightness ==
                                      Brightness.dark
                                  ? AppDesignTokens.darkPanelShadow
                                  : AppDesignTokens.lightPanelShadow,
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Container(
                                      width: 42,
                                      height: 42,
                                      decoration: BoxDecoration(
                                        color: colors.primaryContainer,
                                        borderRadius: BorderRadius.circular(
                                          AppDesignTokens.radiusMd,
                                        ),
                                      ),
                                      child: Icon(
                                        icon,
                                        size: 22,
                                        color: colors.onPrimaryContainer,
                                      ),
                                    ),
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
                                                .headlineSmall
                                                ?.copyWith(
                                                  fontWeight: FontWeight.w700,
                                                  height: 1.15,
                                                ),
                                          ),
                                          if (subtitle != null) ...[
                                            const SizedBox(
                                              height: AppDesignTokens.spaceSm,
                                            ),
                                            Text(
                                              subtitle!,
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodyMedium
                                                  ?.copyWith(
                                                    color: colors
                                                        .onSurfaceVariant,
                                                    height: 1.45,
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
    final colors = Theme.of(context).colorScheme;

    return Align(
      alignment: Alignment.center,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440),
        child: Row(
          children: [
            Container(
              width: 52,
              height: 52,
              decoration: BoxDecoration(
                color: colors.primary,
                borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
              ),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  Icon(
                    Icons.favorite,
                    size: 34,
                    color: colors.onPrimary,
                  ),
                  Icon(
                    Icons.add,
                    size: 20,
                    color: colors.primary,
                    weight: 800,
                  ),
                ],
              ),
            ),
            const SizedBox(width: AppDesignTokens.spaceMd),
            Expanded(
              child: Text(
                AppConfig.appName,
                style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.4,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

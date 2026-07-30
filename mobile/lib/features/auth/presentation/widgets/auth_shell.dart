import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../shared/widgets/app_brand_lockup.dart';
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
                      const AuthPreferencesBar(),
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      const Align(
                        alignment: Alignment.center,
                        child: AppBrandLockup(),
                      ),
                      const SizedBox(height: AppDesignTokens.spaceLg),
                      Text(
                        title,
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.headlineMedium
                            ?.copyWith(
                              fontWeight: FontWeight.w800,
                              height: 1.2,
                            ),
                      ),
                      if (subtitle != null) ...[
                        const SizedBox(height: AppDesignTokens.spaceSm),
                        Align(
                          alignment: Alignment.center,
                          child: ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 380),
                            child: Text(
                              subtitle!,
                              textAlign: TextAlign.center,
                              style: Theme.of(context).textTheme.bodyMedium
                                  ?.copyWith(
                                    color: colors.onSurfaceVariant,
                                    height: 1.45,
                                  ),
                            ),
                          ),
                        ),
                      ],
                      const SizedBox(height: AppDesignTokens.spaceLg),
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
                                  Theme.of(context).brightness ==
                                      Brightness.dark
                                  ? AppDesignTokens.darkPanelShadow
                                  : AppDesignTokens.lightPanelShadow,
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [child],
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

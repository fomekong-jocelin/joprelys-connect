import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../application/auth_controller.dart';
import '../widgets/auth_error_message.dart';
import '../widgets/auth_shell.dart';

class RecoveryPage extends ConsumerWidget {
  const RecoveryPage({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final auth = ref.watch(authControllerProvider);
    final error = authErrorMessage(l10n, auth.value?.errorCode);

    return AuthShell(
      icon: Icons.cloud_off_outlined,
      title: l10n.authRecoveryTitle,
      subtitle: l10n.authRecoverySubtitle,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (error != null) ...[
            Text(
              error,
              textAlign: TextAlign.center,
              style: TextStyle(color: Theme.of(context).colorScheme.error),
            ),
            const SizedBox(height: AppDesignTokens.spaceMd),
          ],
          AppButton(
            label: l10n.authRetry,
            icon: Icons.refresh,
            expand: true,
            loading: auth.isLoading,
            onPressed: auth.isLoading
                ? null
                : ref.read(authControllerProvider.notifier).retryRestore,
          ),
          const SizedBox(height: AppDesignTokens.spaceSm),
          AppButton(
            label: l10n.authForgetSession,
            variant: AppButtonVariant.destructive,
            expand: true,
            onPressed: auth.isLoading
                ? null
                : ref.read(authControllerProvider.notifier).logout,
          ),
        ],
      ),
    );
  }
}

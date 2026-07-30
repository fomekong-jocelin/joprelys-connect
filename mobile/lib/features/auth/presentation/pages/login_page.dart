import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_text_field.dart';
import '../../application/auth_controller.dart';
import '../widgets/auth_error_message.dart';
import '../widgets/auth_shell.dart';

class LoginPage extends ConsumerStatefulWidget {
  const LoginPage({super.key});

  @override
  ConsumerState<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends ConsumerState<LoginPage> {
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  String? _localError;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final auth = ref.watch(authControllerProvider);
    final state = auth.value;
    final error = _localError ?? authErrorMessage(l10n, state?.errorCode);

    return AuthShell(
      icon: Icons.health_and_safety_outlined,
      title: l10n.authLoginTitle,
      subtitle: l10n.authLoginSubtitle,
      child: AutofillGroup(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AppTextField(
              controller: _emailController,
              label: l10n.authEmailLabel,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              enabled: !auth.isLoading,
              prefixIcon: const Icon(Icons.alternate_email),
            ),
            const SizedBox(height: AppDesignTokens.spaceMd),
            AppTextField(
              controller: _passwordController,
              label: l10n.authPasswordLabel,
              textInputAction: TextInputAction.done,
              enabled: !auth.isLoading,
              obscureText: _obscurePassword,
              prefixIcon: const Icon(Icons.lock_outline),
              suffixIcon: IconButton(
                tooltip: _obscurePassword
                    ? l10n.authShowPassword
                    : l10n.authHidePassword,
                onPressed: auth.isLoading
                    ? null
                    : () => setState(
                        () => _obscurePassword = !_obscurePassword,
                      ),
                icon: Icon(
                  _obscurePassword
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                ),
              ),
            ),
            if (error != null) ...[
              const SizedBox(height: AppDesignTokens.spaceMd),
              Text(
                error,
                textAlign: TextAlign.center,
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
            ],
            const SizedBox(height: AppDesignTokens.spaceLg),
            AppButton(
              label: l10n.authSignIn,
              icon: Icons.login,
              expand: true,
              loading: auth.isLoading,
              onPressed: auth.isLoading ? null : _submit,
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _submit() async {
    final l10n = AppLocalizations.of(context);
    final email = _emailController.text.trim();
    final password = _passwordController.text;
    final emailValid = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email);

    if (!emailValid) {
      setState(() => _localError = l10n.authInvalidEmail);
      return;
    }
    if (password.isEmpty) {
      setState(() => _localError = l10n.authPasswordRequired);
      return;
    }

    setState(() => _localError = null);
    await ref
        .read(authControllerProvider.notifier)
        .login(email: email, password: password);
    _passwordController.clear();
  }
}

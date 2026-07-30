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
  String? _emailError;
  String? _passwordError;

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
    final serverError = authErrorMessage(l10n, state?.errorCode);

    return AuthShell(
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
              autofillHints: const [AutofillHints.username, AutofillHints.email],
              autocorrect: false,
              enableSuggestions: false,
              enabled: !auth.isLoading,
              errorText: _emailError,
              onChanged: (_) {
                if (_emailError != null) {
                  setState(() => _emailError = null);
                }
              },
              prefixIcon: const Icon(Icons.mail_outline),
            ),
            const SizedBox(height: AppDesignTokens.spaceMd),
            AppTextField(
              controller: _passwordController,
              label: l10n.authPasswordLabel,
              textInputAction: TextInputAction.done,
              autofillHints: const [AutofillHints.password],
              autocorrect: false,
              enableSuggestions: false,
              enabled: !auth.isLoading,
              obscureText: _obscurePassword,
              errorText: _passwordError,
              onChanged: (_) {
                if (_passwordError != null) {
                  setState(() => _passwordError = null);
                }
              },
              onSubmitted: (_) {
                if (!auth.isLoading) {
                  _submit();
                }
              },
              prefixIcon: const Icon(Icons.lock_outline),
              suffixIcon: IconButton(
                tooltip: _obscurePassword
                    ? l10n.authShowPassword
                    : l10n.authHidePassword,
                onPressed: auth.isLoading
                    ? null
                    : () =>
                          setState(() => _obscurePassword = !_obscurePassword),
                icon: Icon(
                  _obscurePassword
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                ),
              ),
            ),
            if (serverError != null) ...[
              const SizedBox(height: AppDesignTokens.spaceMd),
              _AuthErrorBanner(message: serverError),
            ],
            const SizedBox(height: AppDesignTokens.spaceLg),
            AppButton(
              label: l10n.authSignIn,
              icon: Icons.arrow_forward,
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

    final emailError = emailValid ? null : l10n.authInvalidEmail;
    final passwordError = password.isEmpty ? l10n.authPasswordRequired : null;

    if (emailError != null || passwordError != null) {
      setState(() {
        _emailError = emailError;
        _passwordError = passwordError;
      });
      return;
    }

    setState(() {
      _emailError = null;
      _passwordError = null;
    });
    await ref
        .read(authControllerProvider.notifier)
        .login(email: email, password: password);
    _passwordController.clear();
  }
}

class _AuthErrorBanner extends StatelessWidget {
  const _AuthErrorBanner({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        border: Border.all(color: colors.error.withValues(alpha: 0.35)),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.error_outline, size: 20, color: colors.onErrorContainer),
          const SizedBox(width: AppDesignTokens.spaceSm),
          Expanded(
            child: Text(
              message,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: colors.onErrorContainer,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

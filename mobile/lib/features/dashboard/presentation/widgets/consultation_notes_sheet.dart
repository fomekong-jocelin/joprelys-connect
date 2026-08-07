import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../application/clinical_voice_accepted_result_persistence.dart';
import '../../data/clinical_voice_structured_api.dart';
import '../../data/consultation_api.dart';
import '../../domain/active_visit.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_progressive_assistant_sheet.dart';
import 'consultation_note_form_controllers.dart';
import 'consultation_notes_form.dart';
import 'consultation_notes_header.dart';

class ConsultationNotesSheet extends ConsumerStatefulWidget {
  const ConsultationNotesSheet({required this.visit, this.onSaved, super.key});

  final ActiveVisit visit;
  final VoidCallback? onSaved;

  static Future<void> show(
    BuildContext context, {
    required ActiveVisit visit,
    VoidCallback? onSaved,
  }) {
    return showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: FractionallySizedBox(
          heightFactor: 0.92,
          child: ConsultationNotesSheet(visit: visit, onSaved: onSaved),
        ),
      ),
    );
  }

  @override
  ConsumerState<ConsultationNotesSheet> createState() =>
      _ConsultationNotesSheetState();
}

class _ConsultationNotesSheetState
    extends ConsumerState<ConsultationNotesSheet> {
  final _formKey = GlobalKey<FormState>();
  final _controllers = ConsultationNoteFormControllers();

  ClinicalVoiceAcceptedResultPersistence? _acceptedResultPersistence;
  bool _isSaving = false;
  bool _isLoading = true;
  bool _didFailToLoad = false;
  String? _error;

  ClinicalVoiceAcceptedResultPersistence get _persistence {
    return _acceptedResultPersistence ??=
        ClinicalVoiceAcceptedResultPersistence(
          consultationGateway: ref.read(consultationApiProvider),
          structuredGateway: ref.read(clinicalVoiceStructuredApiProvider),
        );
  }

  @override
  void initState() {
    super.initState();
    _loadConsultation();
  }

  @override
  void dispose() {
    _controllers.dispose();
    super.dispose();
  }

  Future<void> _loadConsultation() async {
    try {
      final gateway = ref.read(consultationApiProvider);
      final note = await gateway.getConsultationNote(widget.visit.id);
      if (mounted && note != null) _controllers.populate(note);
    } catch (error) {
      if (mounted) {
        setState(() {
          _didFailToLoad = true;
          _error = _errorMessage(error);
        });
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _submit() async {
    if (_didFailToLoad) return;
    if (_formKey.currentState?.validate() != true) return;

    setState(() {
      _isSaving = true;
      _error = null;
    });

    try {
      await _persistence.save(
        visit: widget.visit,
        note: _controllers.toConsultationNote(),
        prescriptionJson: _controllers.acceptedPrescriptions,
        labOrdersJson: _controllers.acceptedLabOrders,
      );
      if (!mounted) return;
      final l10n = AppLocalizations.of(context);
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.consultationSavedSuccess)));
      widget.onSaved?.call();
      Navigator.of(context).pop();
    } catch (error) {
      if (mounted) setState(() => _error = _errorMessage(error));
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _launchAssistant() {
    ClinicalVoiceProgressiveAssistantSheet.show(
      context,
      visit: widget.visit,
      initialDraft: _controllers.toAiDraft(),
      onExtracted: (result) {
        _controllers.applyAcceptedDraft(result.note);
        if (!mounted) return;
        setState(() {});
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              AppLocalizations.of(context).assistantExtractedSummary,
            ),
            duration: const Duration(seconds: 4),
          ),
        );
      },
    );
  }

  Future<void> _retryLoad() async {
    setState(() {
      _isLoading = true;
      _didFailToLoad = false;
      _error = null;
    });
    await _loadConsultation();
  }

  String _errorMessage(Object error) {
    if (error is ApiException &&
        error.message.isNotEmpty &&
        !error.message.startsWith('ApiException')) {
      return error.message;
    }
    return AppLocalizations.of(context).consultationLoadError;
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context);

    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(AppDesignTokens.radiusLg),
        ),
      ),
      child: Column(
        children: [
          ConsultationNotesHeader(
            visit: widget.visit,
            onLaunchAssistant: _launchAssistant,
            onClose: () => Navigator.of(context).pop(),
          ),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        ConsultationReasonBanner(reason: widget.visit.reason),
                        const SizedBox(height: 12),
                        ConsultationNotesForm(
                          formKey: _formKey,
                          controllers: _controllers,
                        ),
                        if (_error != null) ...[
                          const SizedBox(height: 12),
                          _ErrorNotice(
                            message: _error!,
                            onRetry: _didFailToLoad ? _retryLoad : null,
                          ),
                        ],
                        const SizedBox(height: 20),
                        AppButton(
                          label: l10n.consultationSaveButton,
                          icon: Icons.save_rounded,
                          expand: true,
                          loading: _isSaving,
                          onPressed: _isSaving || _didFailToLoad
                              ? null
                              : _submit,
                        ),
                      ],
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}

class _ErrorNotice extends StatelessWidget {
  const _ErrorNotice({required this.message, this.onRetry});

  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.error.withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            message,
            style: theme.textTheme.bodySmall?.copyWith(
              color: colors.onErrorContainer,
              fontWeight: FontWeight.w600,
            ),
          ),
          if (onRetry != null) ...[
            const SizedBox(height: 4),
            TextButton(
              style: TextButton.styleFrom(
                foregroundColor: colors.onErrorContainer,
                padding: EdgeInsets.zero,
                minimumSize: const Size(0, 32),
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                alignment: Alignment.centerLeft,
              ),
              onPressed: onRetry,
              child: Text(
                AppLocalizations.of(context).dashboardQueueRetry,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

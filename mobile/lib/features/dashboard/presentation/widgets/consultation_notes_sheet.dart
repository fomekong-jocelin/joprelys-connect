import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../../../shared/widgets/app_text_field.dart';
import '../../data/consultation_api.dart';
import '../../domain/active_visit.dart';
import '../../domain/consultation_note.dart';
import '../dashboard_localizations.dart';

class ConsultationNotesSheet extends ConsumerStatefulWidget {
  const ConsultationNotesSheet({
    required this.visit,
    this.onSaved,
    super.key,
  });

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
          heightFactor: 0.85,
          child: ConsultationNotesSheet(
            visit: visit,
            onSaved: onSaved,
          ),
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
  final _subjectiveController = TextEditingController();
  final _objectiveController = TextEditingController();
  final _assessmentController = TextEditingController();
  final _planController = TextEditingController();

  bool _loading = false;
  bool _fetchingInitial = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _loadInitialNotes();
  }

  @override
  void dispose() {
    _subjectiveController.dispose();
    _objectiveController.dispose();
    _assessmentController.dispose();
    _planController.dispose();
    super.dispose();
  }

  Future<void> _loadInitialNotes() async {
    try {
      final gateway = ref.read(consultationApiProvider);
      final note = await gateway.getConsultationNote(widget.visit.id);
      if (note != null && mounted) {
        _subjectiveController.text = note.subjective ?? '';
        _objectiveController.text = note.objective ?? '';
        _assessmentController.text = note.assessment ?? '';
        _planController.text = note.plan ?? '';
      }
    } catch (_) {
      // Ignorer si la note initiale est absente
    } finally {
      if (mounted) {
        setState(() {
          _fetchingInitial = false;
        });
      }
    }
  }

  Future<void> _submit() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final gateway = ref.read(consultationApiProvider);
      final note = ConsultationNote(
        subjective: _subjectiveController.text.trim(),
        objective: _objectiveController.text.trim(),
        assessment: _assessmentController.text.trim(),
        plan: _planController.text.trim(),
      );

      await gateway.saveConsultationNote(widget.visit.id, note);

      if (mounted) {
        final l10n = AppLocalizations.of(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.consultationSavedSuccess)),
        );
        widget.onSaved?.call();
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = e.toString();
        });
      }
    } finally {
      if (mounted) {
        setState(() {
          _loading = false;
        });
      }
    }
  }

  String _formattedReference(String visitNum, String rawDpu) {
    var cleaned = rawDpu.trim();
    cleaned = cleaned.replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!cleaned.toUpperCase().startsWith('DPU-')) {
      cleaned = 'DPU-$cleaned';
    }
    final nonBreakingDpu = cleaned.replaceAll('-', '\u2011');
    return '$visitNum · $nonBreakingDpu';
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
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
          const SizedBox(height: 12),
          Center(
            child: Container(
              width: 42,
              height: 4,
              decoration: BoxDecoration(
                color: colors.outlineVariant,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.consultationNotesTitle,
                        style: theme.textTheme.titleLarge?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${widget.visit.patientName} (${_formattedReference(widget.visit.visitNumber, widget.visit.patientDpu)})',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onSurfaceVariant,
                          height: 1.3,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
          ),
          const Divider(height: 1),
          Expanded(
            child: _fetchingInitial
                ? const Center(child: CircularProgressIndicator())
                : SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        AppTextField(
                          label: l10n.consultationSubjectiveLabel,
                          hint: l10n.consultationSubjectiveHint,
                          controller: _subjectiveController,
                          maxLines: 3,
                        ),
                        const SizedBox(height: 14),
                        AppTextField(
                          label: l10n.consultationObjectiveLabel,
                          hint: l10n.consultationObjectiveHint,
                          controller: _objectiveController,
                          maxLines: 3,
                        ),
                        const SizedBox(height: 14),
                        AppTextField(
                          label: l10n.consultationAssessmentLabel,
                          hint: l10n.consultationAssessmentHint,
                          controller: _assessmentController,
                          maxLines: 2,
                        ),
                        const SizedBox(height: 14),
                        AppTextField(
                          label: l10n.consultationPlanLabel,
                          hint: l10n.consultationPlanHint,
                          controller: _planController,
                          maxLines: 3,
                        ),
                        if (_error != null) ...[
                          const SizedBox(height: 12),
                          Text(
                            _error!,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.error,
                            ),
                          ),
                        ],
                        const SizedBox(height: 24),
                        AppButton(
                          label: l10n.consultationSaveButton,
                          icon: Icons.note_add_rounded,
                          expand: true,
                          loading: _loading,
                          onPressed: _loading ? null : _submit,
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

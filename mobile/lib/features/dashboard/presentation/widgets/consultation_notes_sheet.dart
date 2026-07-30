import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../data/consultation_api.dart';
import '../../domain/active_visit.dart';
import '../../domain/consultation_note.dart';
import '../dashboard_localizations.dart';
import 'clinical_voice_assistant_sheet.dart';

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
          heightFactor: 0.92,
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

  void _launchAssistant() {
    ClinicalVoiceAssistantSheet.show(
      context,
      visit: widget.visit,
      onExtracted: (result) {
        // Appliquer le texte dicté dans les champs concernés
        if (result.note.subjective?.isNotEmpty == true) {
          _subjectiveController.text = result.note.subjective!;
        }
        if (result.note.objective?.isNotEmpty == true) {
          _objectiveController.text = result.note.objective!;
        }
        if (result.note.assessment?.isNotEmpty == true) {
          _assessmentController.text = result.note.assessment!;
        }
        if (result.note.plan?.isNotEmpty == true) {
          _planController.text = result.note.plan!;
        }
      },
    );
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
          // ── Drag handle ──
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

          // ── Header with patient info + voice assistant ──
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 12, 0),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.edit_note_rounded,
                              size: 22, color: colors.primary),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              l10n.consultationNotesTitle,
                              style: theme.textTheme.titleLarge?.copyWith(
                                fontWeight: FontWeight.w900,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
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
                // Voice assistant FAB
                _MiniActionButton(
                  icon: Icons.mic_rounded,
                  color: colors.primary,
                  tooltip: l10n.voiceAssistantTitle,
                  onTap: _launchAssistant,
                ),
                const SizedBox(width: 4),
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close_rounded),
                ),
              ],
            ),
          ),

          // ── Reason badge (like Angular) ──
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
            child: Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: colors.primary.withValues(alpha: 0.08),
                borderRadius:
                    BorderRadius.circular(AppDesignTokens.radiusSm),
                border: Border.all(
                  color: colors.primary.withValues(alpha: 0.25),
                ),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline_rounded,
                      size: 14, color: colors.primary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      widget.visit.reason,
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: colors.primary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),

          Divider(height: 1, color: colors.outlineVariant.withValues(alpha: 0.3)),

          // ── Body ──
          Expanded(
            child: _fetchingInitial
                ? const Center(child: CircularProgressIndicator())
                : SingleChildScrollView(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        // S — Subjectif
                        _NumberedSection(
                          number: '1',
                          title: l10n.consultationSubjectiveLabel,
                          subtitle: l10n.consultationSubjectiveHelp,
                          child: _ClinicalTextArea(
                            controller: _subjectiveController,
                            hint: l10n.consultationSubjectiveHint,
                            minLines: 3,
                          ),
                        ),

                        _SectionDivider(),

                        // O — Objectif
                        _NumberedSection(
                          number: '2',
                          title: l10n.consultationObjectiveLabel,
                          subtitle: l10n.consultationObjectiveHelp,
                          child: _ClinicalTextArea(
                            controller: _objectiveController,
                            hint: l10n.consultationObjectiveHint,
                            minLines: 3,
                          ),
                        ),

                        _SectionDivider(),

                        // A — Évaluation
                        _NumberedSection(
                          number: '3',
                          title: l10n.consultationAssessmentLabel,
                          subtitle: l10n.consultationAssessmentHelp,
                          child: _ClinicalTextArea(
                            controller: _assessmentController,
                            hint: l10n.consultationAssessmentHint,
                            minLines: 2,
                          ),
                        ),

                        _SectionDivider(),

                        // P — Plan
                        _NumberedSection(
                          number: '4',
                          title: l10n.consultationPlanLabel,
                          subtitle: l10n.consultationPlanHelp,
                          child: _ClinicalTextArea(
                            controller: _planController,
                            hint: l10n.consultationPlanHint,
                            minLines: 3,
                          ),
                        ),

                        if (_error != null) ...[
                          const SizedBox(height: 12),
                          Container(
                            padding: const EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: colors.error.withValues(alpha: 0.08),
                              borderRadius: BorderRadius.circular(
                                  AppDesignTokens.radiusSm),
                              border: Border.all(
                                  color: colors.error.withValues(alpha: 0.3)),
                            ),
                            child: Text(
                              _error!,
                              style: theme.textTheme.bodySmall?.copyWith(
                                color: colors.error,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ],

                        const SizedBox(height: 20),
                        AppButton(
                          label: l10n.consultationSaveButton,
                          icon: Icons.save_rounded,
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

// ── Numbered section header (matches Angular pattern) ──
class _NumberedSection extends StatelessWidget {
  const _NumberedSection({
    required this.number,
    required this.title,
    required this.subtitle,
    required this.child,
  });

  final String number;
  final String title;
  final String subtitle;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 26,
              height: 26,
              decoration: BoxDecoration(
                color: colors.primary.withValues(alpha: 0.12),
                borderRadius:
                    BorderRadius.circular(AppDesignTokens.radiusSm),
              ),
              alignment: Alignment.center,
              child: Text(
                number,
                style: theme.textTheme.labelSmall?.copyWith(
                  fontWeight: FontWeight.w900,
                  color: colors.primary,
                  fontSize: 12,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: theme.textTheme.titleSmall?.copyWith(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    subtitle,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: colors.onSurfaceVariant,
                      fontSize: 11,
                      height: 1.3,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        child,
      ],
    );
  }
}

// ── Clinical textarea (styled like Angular ui-textarea) ──
class _ClinicalTextArea extends StatelessWidget {
  const _ClinicalTextArea({
    required this.controller,
    required this.hint,
    this.minLines = 3,
  });

  final TextEditingController controller;
  final String hint;
  final int minLines;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return TextField(
      controller: controller,
      maxLines: null,
      minLines: minLines,
      textInputAction: TextInputAction.newline,
      style: theme.textTheme.bodyMedium?.copyWith(
        height: 1.5,
      ),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: theme.textTheme.bodySmall?.copyWith(
          color: colors.onSurfaceVariant.withValues(alpha: 0.5),
          fontStyle: FontStyle.italic,
        ),
        filled: true,
        fillColor: colors.surfaceContainerHighest.withValues(alpha: 0.3),
        contentPadding:
            const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          borderSide: BorderSide(
            color: colors.outlineVariant.withValues(alpha: 0.4),
          ),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          borderSide: BorderSide(
            color: colors.primary,
            width: 1.5,
          ),
        ),
      ),
    );
  }
}

// ── Subtle section divider ──
class _SectionDivider extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Divider(
        height: 1,
        color: Theme.of(context)
            .colorScheme
            .outlineVariant
            .withValues(alpha: 0.25),
      ),
    );
  }
}

// ── Mini action button (for voice assistant) ──
class _MiniActionButton extends StatelessWidget {
  const _MiniActionButton({
    required this.icon,
    required this.color,
    required this.tooltip,
    required this.onTap,
  });

  final IconData icon;
  final Color color;
  final String tooltip;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Tooltip(
      message: tooltip,
      child: Material(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          child: Container(
            width: 38,
            height: 38,
            alignment: Alignment.center,
            child: Icon(icon, size: 20, color: color),
          ),
        ),
      ),
    );
  }
}

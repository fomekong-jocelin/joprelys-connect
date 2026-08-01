import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../dashboard_localizations.dart';
import 'clinical_text_area.dart';
import 'consultation_note_form_controllers.dart';

class ConsultationNotesForm extends StatelessWidget {
  const ConsultationNotesForm({
    required this.formKey,
    required this.controllers,
    super.key,
  });

  final GlobalKey<FormState> formKey;
  final ConsultationNoteFormControllers controllers;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);

    return Form(
      key: formKey,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _NumberedSection(
            number: '1',
            title: l10n.consultationSubjectiveLabel,
            subtitle: l10n.consultationSubjectiveHelp,
            child: ClinicalTextArea(
              controller: controllers.symptoms,
              hint: l10n.consultationSubjectiveHint,
              maxLength: 5000,
              requiredMessage: l10n.consultationSymptomsRequired,
            ),
          ),
          const _SectionDivider(),
          _NumberedSection(
            number: '2',
            title: l10n.consultationObjectiveLabel,
            subtitle: l10n.consultationObjectiveHelp,
            child: ClinicalTextArea(
              controller: controllers.clinicalExam,
              hint: l10n.consultationObjectiveHint,
              maxLength: 5000,
            ),
          ),
          const _SectionDivider(),
          _NumberedSection(
            number: '3',
            title: l10n.consultationAssessmentLabel,
            subtitle: l10n.consultationAssessmentHelp,
            child: LabeledClinicalTextArea(
              label: l10n.consultationDiagnosisLabel,
              hint: l10n.consultationDiagnosisHint,
              controller: controllers.diagnosis,
              maxLength: 5000,
              isRequired: true,
              requiredMessage: l10n.consultationDiagnosisRequired,
            ),
          ),
          const _SectionDivider(),
          _NumberedSection(
            number: '4',
            title: l10n.consultationPlanLabel,
            subtitle: l10n.consultationPlanHelp,
            child: Column(
              children: [
                LabeledClinicalTextArea(
                  label: l10n.consultationConclusionLabel,
                  hint: l10n.consultationConclusionHint,
                  controller: controllers.conclusion,
                  maxLength: 5000,
                ),
                const SizedBox(height: 14),
                LabeledClinicalTextArea(
                  label: l10n.consultationAdviceLabel,
                  hint: l10n.consultationAdviceHint,
                  controller: controllers.advice,
                  maxLength: 3000,
                ),
                const SizedBox(height: 14),
                LabeledClinicalTextArea(
                  label: l10n.consultationFollowUpLabel,
                  hint: l10n.consultationFollowUpHint,
                  controller: controllers.followUp,
                  maxLength: 1000,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

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
                borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
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

class _SectionDivider extends StatelessWidget {
  const _SectionDivider();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Divider(
        height: 1,
        color: Theme.of(
          context,
        ).colorScheme.outlineVariant.withValues(alpha: 0.25),
      ),
    );
  }
}

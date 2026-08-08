import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../domain/patient_record.dart';
import 'patient_record_consultations_section.dart';
import 'patient_record_laboratory_section.dart';
import 'patient_record_section.dart';
import 'patient_record_sections.dart';

class PatientRecordContent extends StatelessWidget {
  const PatientRecordContent({
    required this.record,
    required this.sections,
    required this.selected,
    required this.onSectionChanged,
    super.key,
  });

  final PatientRecordBundle record;
  final List<PatientRecordSection> sections;
  final PatientRecordSection selected;
  final ValueChanged<PatientRecordSection> onSectionChanged;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Column(
      children: [
        PatientRecordHero(record: record),
        Material(
          color: colors.surface,
          child: SizedBox(
            height: 58,
            child: ListView.separated(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              scrollDirection: Axis.horizontal,
              itemCount: sections.length,
              separatorBuilder: (_, _) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final section = sections[index];
                return _SectionChip(
                  section: section,
                  selected: section == selected,
                  onTap: () => onSectionChanged(section),
                );
              },
            ),
          ),
        ),
        Divider(height: 1, color: colors.outlineVariant.withValues(alpha: 0.5)),
        Expanded(
          child: AnimatedSwitcher(
            duration: const Duration(milliseconds: 220),
            child: switch (selected) {
              PatientRecordSection.consultations =>
                PatientRecordConsultationsDetailSection(
                  key: const ValueKey(PatientRecordSection.consultations),
                  record: record,
                ),
              PatientRecordSection.laboratory =>
                PatientRecordLaboratoryDetailSection(
                  key: const ValueKey(PatientRecordSection.laboratory),
                  record: record,
                ),
              _ => PatientRecordSectionView(
                  key: ValueKey(selected),
                  section: selected,
                  record: record,
                ),
            },
          ),
        ),
      ],
    );
  }
}

class _SectionChip extends StatelessWidget {
  const _SectionChip({
    required this.section,
    required this.selected,
    required this.onTap,
  });

  final PatientRecordSection section;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final (label, icon) = switch (section) {
      PatientRecordSection.overview => (
        l10n.recordOverview,
        Icons.grid_view_rounded,
      ),
      PatientRecordSection.medical => (
        l10n.recordMedical,
        Icons.health_and_safety_rounded,
      ),
      PatientRecordSection.consultations => (
        l10n.recordConsultations,
        Icons.history_rounded,
      ),
      PatientRecordSection.laboratory => (
        l10n.recordLaboratory,
        Icons.science_rounded,
      ),
      PatientRecordSection.hospitalizations => (
        l10n.recordHospitalizations,
        Icons.local_hospital_rounded,
      ),
      PatientRecordSection.audit => (l10n.recordAudit, Icons.shield_outlined),
    };

    return Semantics(
      selected: selected,
      button: true,
      label: label,
      child: Material(
        color: selected ? colors.primaryContainer : colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  icon,
                  size: 18,
                  color: selected
                      ? colors.onPrimaryContainer
                      : colors.onSurfaceVariant,
                ),
                const SizedBox(width: 7),
                Text(
                  label,
                  maxLines: 1,
                  style: Theme.of(context).textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                    color: selected
                        ? colors.onPrimaryContainer
                        : colors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

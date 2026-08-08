import 'package:flutter/material.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../domain/patient_history.dart';
import '../../domain/patient_record.dart';
import 'patient_record_page.dart';

class PatientRecordHero extends StatelessWidget {
  const PatientRecordHero({required this.record, super.key});

  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final identity = record.identity;
    final initials = identity.fullName
        .split(RegExp(r'\s+'))
        .where((part) => part.isNotEmpty)
        .take(2)
        .map((part) => part[0].toUpperCase())
        .join();

    return Material(
      color: colors.surface,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 12, 16, 14),
        child: Row(
          children: [
            Container(
              width: 52,
              height: 52,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: colors.primaryContainer,
                borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
              ),
              child: Text(
                initials.isEmpty ? 'P' : initials,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  color: colors.onPrimaryContainer,
                  fontWeight: FontWeight.w900,
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    identity.fullName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                      fontWeight: FontWeight.w900,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Wrap(
                    spacing: 8,
                    runSpacing: 4,
                    children: [
                      _MetaBadge(
                        icon: Icons.badge_outlined,
                        label: _normalizedDpu(identity.globalPatientNumber),
                      ),
                      if (identity.localPatientNumber.isNotEmpty)
                        _MetaBadge(
                          icon: Icons.apartment_rounded,
                          label: identity.localPatientNumber,
                        ),
                    ],
                  ),
                ],
              ),
            ),
            _StatusBadge(
              status: identity.status,
              emergency: identity.emergencyAccessActive,
            ),
          ],
        ),
      ),
    );
  }

  String _normalizedDpu(String raw) {
    final value = raw.trim();
    if (value.isEmpty) return 'DPU';
    return value.toUpperCase().startsWith('DPU') ? value : 'DPU-$value';
  }
}

class PatientRecordSectionView extends StatelessWidget {
  const PatientRecordSectionView({
    required this.section,
    required this.record,
    super.key,
  });

  final PatientRecordSection section;
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    return switch (section) {
      PatientRecordSection.overview => _OverviewSection(record: record),
      PatientRecordSection.medical => _MedicalSection(record: record),
      PatientRecordSection.consultations => _ConsultationsSection(
        record: record,
      ),
      PatientRecordSection.prescriptions => const SizedBox.shrink(),
      PatientRecordSection.laboratory => _LaboratorySection(record: record),
      PatientRecordSection.hospitalizations => _HospitalizationsSection(
        record: record,
      ),
      PatientRecordSection.audit => _AuditSection(record: record),
    };
  }
}

class _OverviewSection extends StatelessWidget {
  const _OverviewSection({required this.record});
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final identity = record.identity;
    final locale = Localizations.localeOf(context);
    final severeAllergies = record.history.allergies
        .where((allergy) => allergy.severity == AllergySeverity.severe)
        .toList(growable: false);

    return _SectionScroll(
      children: [
        _SectionTitle(
          icon: Icons.person_rounded,
          title: l10n.recordPatientIdentity,
        ),
        _PremiumCard(
          child: Column(
            children: [
              _DetailRow(
                icon: Icons.cake_outlined,
                label: l10n.recordDateOfBirth,
                value: identity.birthDate == null
                    ? l10n.recordUnknown
                    : AppLocaleFormatters.formatDate(
                        identity.birthDate!,
                        locale,
                      ),
              ),
              _DetailRow(
                icon: Icons.wc_rounded,
                label: l10n.recordGender,
                value: identity.gender ?? l10n.recordUnknown,
              ),
              _DetailRow(
                icon: Icons.phone_outlined,
                label: l10n.recordPhone,
                value: identity.phone ?? l10n.recordUnknown,
              ),
              _DetailRow(
                icon: Icons.bloodtype_outlined,
                label: l10n.recordBloodGroup,
                value: identity.bloodGroup ?? l10n.recordUnknown,
                showDivider: false,
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        _SectionTitle(
          icon: Icons.health_and_safety_rounded,
          title: l10n.recordSafetySummary,
          critical: severeAllergies.isNotEmpty,
        ),
        _SafetyCard(allergies: severeAllergies),
        const SizedBox(height: 18),
        Row(
          children: [
            Expanded(
              child: _MetricCard(
                icon: Icons.history_edu_rounded,
                value: '${record.history.pastVisits.length}',
                label: l10n.recordConsultations,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _MetricCard(
                icon: Icons.science_rounded,
                value: '${record.labResults.length}',
                label: l10n.recordLabResults,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _MetricCard(
                icon: Icons.local_hospital_rounded,
                value: '${record.hospitalizations.length}',
                label: l10n.recordHospitalizations,
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _MedicalSection extends StatelessWidget {
  const _MedicalSection({required this.record});
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    return _SectionScroll(
      children: [
        _SectionTitle(
          icon: Icons.medical_information_outlined,
          title: l10n.recordKnownAntecedents,
        ),
        if (record.history.antecedents.isEmpty)
          _EmptyCard(message: l10n.recordNoAntecedents)
        else
          ...record.history.antecedents.map(
            (item) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _PremiumCard(
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    _LeadingIcon(icon: Icons.description_rounded),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            item.type,
                            style: Theme.of(context).textTheme.labelMedium
                                ?.copyWith(
                                  color: Theme.of(context).colorScheme.primary,
                                  fontWeight: FontWeight.w900,
                                ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            item.description,
                            style: Theme.of(context).textTheme.bodyMedium
                                ?.copyWith(fontWeight: FontWeight.w700),
                          ),
                        ],
                      ),
                    ),
                    if (item.diagnosedYear != null)
                      Text(
                        '${item.diagnosedYear}',
                        style: Theme.of(context).textTheme.labelMedium,
                      ),
                  ],
                ),
              ),
            ),
          ),
        const SizedBox(height: 14),
        _SectionTitle(
          icon: Icons.warning_amber_rounded,
          title: l10n.recordDeclaredAllergies,
          critical: record.history.allergies.isNotEmpty,
        ),
        if (record.history.allergies.isEmpty)
          _EmptyCard(message: l10n.recordNoAllergies)
        else
          ...record.history.allergies.map(
            (allergy) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _AllergyCard(allergy: allergy),
            ),
          ),
        if (record.vaccinations.isNotEmpty) ...[
          const SizedBox(height: 14),
          _SectionTitle(
            icon: Icons.vaccines_rounded,
            title: AppLocalizations.of(context).localeName.startsWith('fr')
                ? 'Vaccinations'
                : 'Vaccinations',
          ),
          ...record.vaccinations.map(
            (vaccination) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _PremiumCard(
                child: ListTile(
                  contentPadding: EdgeInsets.zero,
                  leading: _LeadingIcon(icon: Icons.vaccines_outlined),
                  title: Text(
                    vaccination.label,
                    style: const TextStyle(fontWeight: FontWeight.w800),
                  ),
                  subtitle: vaccination.date == null
                      ? null
                      : Text(
                          AppLocaleFormatters.formatDate(
                            vaccination.date!,
                            locale,
                          ),
                        ),
                  trailing: vaccination.status == null
                      ? null
                      : _SmallBadge(label: vaccination.status!),
                ),
              ),
            ),
          ),
        ],
      ],
    );
  }
}

class _ConsultationsSection extends StatelessWidget {
  const _ConsultationsSection({required this.record});
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    if (record.history.pastVisits.isEmpty) {
      return _SectionScroll(
        children: [_EmptyCard(message: l10n.recordNoConsultations)],
      );
    }

    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: record.history.pastVisits.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final visit = record.history.pastVisits[index];
        return _PremiumCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _SmallBadge(
                    label: visit.visitNumber.isEmpty
                        ? l10n.recordConsultations
                        : visit.visitNumber,
                  ),
                  const Spacer(),
                  Text(
                    AppLocaleFormatters.formatDate(visit.date, locale),
                    style: Theme.of(context).textTheme.labelMedium?.copyWith(
                      color: Theme.of(context).colorScheme.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Text(
                visit.chiefComplaint.isEmpty
                    ? l10n.recordUnknown
                    : visit.chiefComplaint,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w900,
                  height: 1.3,
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(
                    Icons.medical_services_outlined,
                    size: 16,
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      visit.practitionerName.isEmpty
                          ? l10n.recordUnknown
                          : visit.practitionerName,
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (visit.temperature != null)
                    _VitalChip(
                      icon: Icons.thermostat_rounded,
                      label:
                          '${l10n.recordTemperature} ${visit.temperature!.toStringAsFixed(1)} °C',
                    ),
                  if (visit.systolic != null && visit.diastolic != null)
                    _VitalChip(
                      icon: Icons.monitor_heart_outlined,
                      label:
                          '${l10n.recordBloodPressure} ${visit.systolic}/${visit.diastolic}',
                    ),
                  if (visit.pulse != null)
                    _VitalChip(
                      icon: Icons.favorite_outline_rounded,
                      label: '${l10n.recordPulse} ${visit.pulse} bpm',
                    ),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}

class _LaboratorySection extends StatelessWidget {
  const _LaboratorySection({required this.record});
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    return _SectionScroll(
      children: [
        _SectionTitle(
          icon: Icons.biotech_rounded,
          title: l10n.recordLabResults,
        ),
        if (record.labResults.isEmpty)
          _EmptyCard(message: l10n.recordNoLabOrders)
        else
          ...record.labResults.map(
            (result) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _PremiumCard(
                child: Row(
                  children: [
                    _LeadingIcon(icon: Icons.analytics_outlined),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            result.analyte,
                            style: const TextStyle(fontWeight: FontWeight.w900),
                          ),
                          if (result.referenceRange != null)
                            Text(
                              result.referenceRange!,
                              style: Theme.of(context).textTheme.bodySmall,
                            ),
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text(
                          '${result.value}${result.unit == null ? '' : ' ${result.unit}'}',
                          style: Theme.of(context).textTheme.titleMedium
                              ?.copyWith(fontWeight: FontWeight.w900),
                        ),
                        if (result.interpretation != null)
                          _SmallBadge(label: result.interpretation!),
                      ],
                    ),
                  ],
                ),
              ),
            ),
          ),
        const SizedBox(height: 14),
        _SectionTitle(
          icon: Icons.assignment_outlined,
          title: l10n.recordLabOrders,
        ),
        if (record.labOrders.isEmpty)
          _EmptyCard(message: l10n.recordNoLabOrders)
        else
          ...record.labOrders.map(
            (order) => Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: _PremiumCard(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        _SmallBadge(
                          label: order.number.isEmpty
                              ? order.status
                              : order.number,
                        ),
                        const Spacer(),
                        if (order.createdAt != null)
                          Text(
                            AppLocaleFormatters.formatDate(
                              order.createdAt!,
                              locale,
                            ),
                            style: Theme.of(context).textTheme.labelMedium,
                          ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(
                      order.exams.isEmpty
                          ? l10n.recordUnknown
                          : order.exams.join(' · '),
                      style: const TextStyle(fontWeight: FontWeight.w800),
                    ),
                    if (order.practitioner != null) ...[
                      const SizedBox(height: 6),
                      Text(
                        '${l10n.recordPractitioner} · ${order.practitioner}',
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
      ],
    );
  }
}

class _HospitalizationsSection extends StatelessWidget {
  const _HospitalizationsSection({required this.record});
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    if (record.hospitalizations.isEmpty) {
      return _SectionScroll(
        children: [_EmptyCard(message: l10n.recordNoHospitalizations)],
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: record.hospitalizations.length,
      separatorBuilder: (_, _) => const SizedBox(height: 12),
      itemBuilder: (context, index) {
        final stay = record.hospitalizations[index];
        return _PremiumCard(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  _LeadingIcon(icon: Icons.local_hospital_outlined),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      stay.service ?? l10n.recordHospitalizations,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                  ),
                  _SmallBadge(label: stay.status),
                ],
              ),
              if (stay.reason != null) ...[
                const SizedBox(height: 12),
                Text(stay.reason!),
              ],
              if (stay.admittedAt != null) ...[
                const SizedBox(height: 12),
                Text(
                  AppLocaleFormatters.formatDate(stay.admittedAt!, locale),
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                    color: Theme.of(context).colorScheme.onSurfaceVariant,
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}

class _AuditSection extends StatelessWidget {
  const _AuditSection({required this.record});
  final PatientRecordBundle record;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    if (record.auditLogs.isEmpty) {
      return _SectionScroll(
        children: [_EmptyCard(message: l10n.recordNoAudit)],
      );
    }
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: record.auditLogs.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (context, index) {
        final log = record.auditLogs[index];
        final success = log.status.toUpperCase() == 'SUCCESS';
        return _PremiumCard(
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _LeadingIcon(
                icon: success
                    ? Icons.verified_outlined
                    : Icons.warning_amber_rounded,
                error: !success,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      log.action,
                      style: const TextStyle(fontWeight: FontWeight.w900),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${l10n.recordAuditActor} · ${log.actor ?? l10n.recordUnknown}',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                    if (log.reason != null) ...[
                      const SizedBox(height: 6),
                      Text(log.reason!),
                    ],
                  ],
                ),
              ),
              if (log.createdAt != null)
                Text(
                  AppLocaleFormatters.formatDate(log.createdAt!, locale),
                  style: Theme.of(context).textTheme.labelSmall,
                ),
            ],
          ),
        );
      },
    );
  }
}

class _SectionScroll extends StatelessWidget {
  const _SectionScroll({required this.children});
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    return ListView(padding: const EdgeInsets.all(16), children: children);
  }
}

class _PremiumCard extends StatelessWidget {
  const _PremiumCard({required this.child});
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(color: colors.outlineVariant.withValues(alpha: 0.7)),
        boxShadow: [
          BoxShadow(
            color: colors.shadow.withValues(alpha: 0.05),
            blurRadius: 18,
            offset: const Offset(0, 7),
          ),
        ],
      ),
      child: child,
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle({
    required this.icon,
    required this.title,
    this.critical = false,
  });
  final IconData icon;
  final String title;
  final bool critical;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final color = critical ? colors.error : colors.primary;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Icon(icon, color: color, size: 20),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              title,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                color: color,
                fontWeight: FontWeight.w900,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  const _DetailRow({
    required this.icon,
    required this.label,
    required this.value,
    this.showDivider = true,
  });
  final IconData icon;
  final String label;
  final String value;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 10),
          child: Row(
            children: [
              Icon(icon, size: 19, color: colors.onSurfaceVariant),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  label,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ),
              Flexible(
                child: Text(
                  value,
                  textAlign: TextAlign.end,
                  style: const TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
            ],
          ),
        ),
        if (showDivider) Divider(height: 1, color: colors.outlineVariant),
      ],
    );
  }
}

class _SafetyCard extends StatelessWidget {
  const _SafetyCard({required this.allergies});
  final List<PatientAllergy> allergies;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final hasRisk = allergies.isNotEmpty;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: hasRisk ? colors.errorContainer : colors.secondaryContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            hasRisk
                ? Icons.warning_amber_rounded
                : Icons.verified_user_outlined,
            color: hasRisk
                ? colors.onErrorContainer
                : colors.onSecondaryContainer,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              hasRisk
                  ? allergies.map((item) => item.allergen).join(' · ')
                  : l10n.recordNoCriticalAllergies,
              style: TextStyle(
                color: hasRisk
                    ? colors.onErrorContainer
                    : colors.onSecondaryContainer,
                fontWeight: FontWeight.w800,
                height: 1.35,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _AllergyCard extends StatelessWidget {
  const _AllergyCard({required this.allergy});
  final PatientAllergy allergy;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final label = switch (allergy.severity) {
      AllergySeverity.low => l10n.recordSeverityLow,
      AllergySeverity.moderate => l10n.recordSeverityModerate,
      AllergySeverity.severe => l10n.recordSeveritySevere,
    };
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colors.errorContainer.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(color: colors.error.withValues(alpha: 0.28)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.warning_amber_rounded, color: colors.error),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  allergy.allergen,
                  style: const TextStyle(fontWeight: FontWeight.w900),
                ),
                if (allergy.reaction != null) ...[
                  const SizedBox(height: 4),
                  Text(allergy.reaction!),
                ],
              ],
            ),
          ),
          _SmallBadge(label: label, error: true),
        ],
      ),
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.icon,
    required this.value,
    required this.label,
  });
  final IconData icon;
  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 14),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Column(
        children: [
          Icon(icon, color: colors.primary, size: 20),
          const SizedBox(height: 7),
          Text(
            value,
            style: Theme.of(
              context,
            ).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            textAlign: TextAlign.center,
            style: Theme.of(
              context,
            ).textTheme.labelSmall?.copyWith(color: colors.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Text(
        message,
        textAlign: TextAlign.center,
        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
          color: colors.onSurfaceVariant,
          fontStyle: FontStyle.italic,
        ),
      ),
    );
  }
}

class _LeadingIcon extends StatelessWidget {
  const _LeadingIcon({required this.icon, this.error = false});
  final IconData icon;
  final bool error;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final background = error ? colors.errorContainer : colors.primaryContainer;
    final foreground = error
        ? colors.onErrorContainer
        : colors.onPrimaryContainer;
    return Container(
      width: 40,
      height: 40,
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
      ),
      child: Icon(icon, color: foreground, size: 21),
    );
  }
}

class _SmallBadge extends StatelessWidget {
  const _SmallBadge({required this.label, this.error = false});
  final String label;
  final bool error;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 9, vertical: 4),
      decoration: BoxDecoration(
        color: error ? colors.errorContainer : colors.primaryContainer,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        maxLines: 1,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: error ? colors.onErrorContainer : colors.onPrimaryContainer,
          fontWeight: FontWeight.w900,
        ),
      ),
    );
  }
}

class _VitalChip extends StatelessWidget {
  const _VitalChip({required this.icon, required this.label});
  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      decoration: BoxDecoration(
        color: colors.secondaryContainer,
        borderRadius: BorderRadius.circular(999),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: colors.onSecondaryContainer),
          const SizedBox(width: 5),
          Text(
            label,
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: colors.onSecondaryContainer,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _MetaBadge extends StatelessWidget {
  const _MetaBadge({required this.icon, required this.label});
  final IconData icon;
  final String label;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: colors.onSurfaceVariant),
        const SizedBox(width: 4),
        Text(
          label,
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
            color: colors.onSurfaceVariant,
            fontWeight: FontWeight.w700,
          ),
        ),
      ],
    );
  }
}

class _StatusBadge extends StatelessWidget {
  const _StatusBadge({required this.status, required this.emergency});
  final String status;
  final bool emergency;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Tooltip(
      message: status,
      child: Container(
        width: 12,
        height: 12,
        decoration: BoxDecoration(
          color: emergency ? colors.error : colors.primary,
          shape: BoxShape.circle,
          boxShadow: [
            BoxShadow(
              color: (emergency ? colors.error : colors.primary).withValues(
                alpha: 0.35,
              ),
              blurRadius: 8,
            ),
          ],
        ),
      ),
    );
  }
}

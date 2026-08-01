import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../data/patient_history_api.dart';
import '../../domain/active_visit.dart';
import '../../domain/patient_history.dart';
import '../dashboard_localizations.dart';

class PatientHistorySheet extends ConsumerStatefulWidget {
  const PatientHistorySheet({
    required this.visit,
    super.key,
  });

  final ActiveVisit visit;

  static Future<void> show(
    BuildContext context, {
    required ActiveVisit visit,
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
          child: PatientHistorySheet(visit: visit),
        ),
      ),
    );
  }

  @override
  ConsumerState<PatientHistorySheet> createState() =>
      _PatientHistorySheetState();
}

class _PatientHistorySheetState extends ConsumerState<PatientHistorySheet>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  PatientMedicalHistory? _history;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _loadHistory();
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadHistory() async {
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final gateway = ref.read(patientHistoryApiProvider);
      final res = await gateway.getMedicalHistory(widget.visit.patientId);
      if (mounted) {
        setState(() {
          _history = res;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context);
        setState(() {
          _error = e is ApiException && e.message.isNotEmpty && !e.message.startsWith('ApiException')
              ? e.message
              : l10n.dashboardQueueLoadError;
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
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(
                            Icons.folder_shared_rounded,
                            color: colors.primary,
                            size: 22,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            l10n.historyTitle,
                            style: theme.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${widget.visit.patientName} (${_formattedReference(widget.visit.visitNumber, widget.visit.patientDpu)})',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onSurfaceVariant,
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
          TabBar(
            controller: _tabController,
            labelColor: colors.primary,
            unselectedLabelColor: colors.onSurfaceVariant,
            indicatorColor: colors.primary,
            tabs: [
              Tab(
                icon: const Icon(Icons.badge_rounded, size: 18),
                text: l10n.historyTabAntecedents,
              ),
              Tab(
                icon: const Icon(Icons.history_edu_rounded, size: 18),
                text: l10n.historyTabVisits,
              ),
            ],
          ),
          const Divider(height: 1),
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : _error != null
                    ? Center(
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.cloud_off_rounded,
                                  size: 44, color: colors.error),
                              const SizedBox(height: 12),
                              Text(
                                _error!,
                                textAlign: TextAlign.center,
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  color: colors.onSurfaceVariant,
                                ),
                              ),
                              const SizedBox(height: 16),
                              OutlinedButton.icon(
                                onPressed: _loadHistory,
                                icon: const Icon(Icons.refresh_rounded),
                                label: Text(l10n.dashboardQueueRetry),
                              ),
                            ],
                          ),
                        ),
                      )
                    : TabBarView(
                        controller: _tabController,
                        children: [
                          _AntecedentsAndAllergiesTab(
                            history: _history!,
                            l10n: l10n,
                          ),
                          _VisitsTimelineTab(
                            history: _history!,
                            l10n: l10n,
                          ),
                        ],
                      ),
          ),
        ],
      ),
    );
  }
}

class _AntecedentsAndAllergiesTab extends StatelessWidget {
  const _AntecedentsAndAllergiesTab({
    required this.history,
    required this.l10n,
  });

  final PatientMedicalHistory history;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Section Antécédents
          Row(
            children: [
              Icon(Icons.notes_rounded, color: colors.primary, size: 20),
              const SizedBox(width: 6),
              Text(
                l10n.historyAntecedentsHeader,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: colors.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          if (history.antecedents.isEmpty)
            Text(
              l10n.historyNoAntecedents,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
                fontStyle: FontStyle.italic,
              ),
            )
          else
            Column(
              children: [
                for (final ant in history.antecedents)
                  Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: colors.surfaceContainerHighest.withValues(alpha: 0.5),
                      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
                      border: Border.all(
                        color: colors.outline.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: colors.primary.withValues(alpha: 0.12),
                            borderRadius:
                                BorderRadius.circular(AppDesignTokens.radiusSm),
                          ),
                          child: Text(
                            ant.type,
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: colors.primary,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Text(
                            ant.description,
                            style: theme.textTheme.bodyMedium?.copyWith(
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        if (ant.diagnosedYear != null)
                          Text(
                            '${ant.diagnosedYear}',
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: colors.onSurfaceVariant,
                            ),
                          ),
                      ],
                    ),
                  ),
              ],
            ),
          const SizedBox(height: 20),
          // Section Allergies
          Row(
            children: [
              Icon(Icons.warning_amber_rounded,
                  color: AppDesignTokens.error, size: 20),
              const SizedBox(width: 6),
              Text(
                l10n.historyAllergiesHeader,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: AppDesignTokens.error,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          if (history.allergies.isEmpty)
            Text(
              l10n.historyNoAllergies,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
                fontStyle: FontStyle.italic,
              ),
            )
          else
            Column(
              children: [
                for (final alg in history.allergies)
                  Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppDesignTokens.error.withValues(alpha: 0.05),
                      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
                      border: Border.all(
                        color: AppDesignTokens.error.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              alg.allergen,
                              style: theme.textTheme.titleSmall?.copyWith(
                                fontWeight: FontWeight.w800,
                                color: AppDesignTokens.error,
                              ),
                            ),
                            _AllergySeverityBadge(severity: alg.severity),
                          ],
                        ),
                        if (alg.reaction != null) ...[
                          const SizedBox(height: 4),
                          Text(
                            alg.reaction!,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
              ],
            ),
        ],
      ),
    );
  }
}

class _VisitsTimelineTab extends StatelessWidget {
  const _VisitsTimelineTab({
    required this.history,
    required this.l10n,
  });

  final PatientMedicalHistory history;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final locale = Localizations.localeOf(context);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.history_rounded, color: colors.primary, size: 20),
              const SizedBox(width: 6),
              Text(
                l10n.historyVisitsHeader,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  color: colors.primary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          if (history.pastVisits.isEmpty)
            Text(
              l10n.historyNoVisits,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
                fontStyle: FontStyle.italic,
              ),
            )
          else
            Column(
              children: [
                for (final visit in history.pastVisits)
                  Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(
                      color: colors.surfaceContainerHighest.withValues(alpha: 0.4),
                      borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
                      border: Border.all(
                        color: colors.outline.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              visit.visitNumber,
                              style: theme.textTheme.labelMedium?.copyWith(
                                fontWeight: FontWeight.w800,
                                color: colors.primary,
                              ),
                            ),
                            Text(
                              AppLocaleFormatters.formatDate(
                                  visit.date, locale),
                              style: theme.textTheme.labelSmall?.copyWith(
                                color: colors.onSurfaceVariant,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(
                          visit.chiefComplaint,
                          style: theme.textTheme.bodyMedium?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          visit.practitionerName,
                          style: theme.textTheme.bodySmall?.copyWith(
                            color: colors.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 6,
                          children: [
                            if (visit.temperature != null)
                              _MiniVitalChip(
                                label: 'T°: ${visit.temperature}°C',
                              ),
                            if (visit.systolic != null && visit.diastolic != null)
                              _MiniVitalChip(
                                label: 'TA: ${visit.systolic}/${visit.diastolic}',
                              ),
                            if (visit.pulse != null)
                              _MiniVitalChip(
                                label: 'Pouls: ${visit.pulse} bpm',
                              ),
                          ],
                        ),
                      ],
                    ),
                  ),
              ],
            ),
        ],
      ),
    );
  }
}

class _AllergySeverityBadge extends StatelessWidget {
  const _AllergySeverityBadge({required this.severity});

  final AllergySeverity severity;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final (label, color) = switch (severity) {
      AllergySeverity.severe => ('SÉVÈRE', AppDesignTokens.error),
      AllergySeverity.moderate => ('MODÉRÉE', Colors.orange),
      AllergySeverity.low => ('FAIBLE', theme.colorScheme.secondary),
    };

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: color.withValues(alpha: 0.5)),
      ),
      child: Text(
        label,
        style: theme.textTheme.labelSmall?.copyWith(
          color: color,
          fontWeight: FontWeight.w900,
          fontSize: 10,
        ),
      ),
    );
  }
}

class _MiniVitalChip extends StatelessWidget {
  const _MiniVitalChip({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        label,
        style: theme.textTheme.labelSmall?.copyWith(
          color: colors.primary,
          fontWeight: FontWeight.w700,
          fontSize: 10,
        ),
      ),
    );
  }
}

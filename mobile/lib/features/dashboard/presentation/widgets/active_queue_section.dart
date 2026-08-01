import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../shared/widgets/app_button.dart';
import '../../data/patient_directory_api.dart';
import '../../domain/active_visit.dart';
import '../../domain/patient_directory_item.dart';
import '../dashboard_localizations.dart';
import 'consultation_notes_sheet.dart';
import 'patient_history_sheet.dart';
import 'patient_vitals_sheet.dart';

class ActiveQueueSection extends ConsumerStatefulWidget {
  const ActiveQueueSection({
    required this.queue,
    required this.onRefresh,
    super.key,
  });

  final AsyncValue<List<ActiveVisit>> queue;
  final Future<void> Function() onRefresh;

  @override
  ConsumerState<ActiveQueueSection> createState() => _ActiveQueueSectionState();
}

class _ActiveQueueSectionState extends ConsumerState<ActiveQueueSection> {
  final _searchController = TextEditingController();
  int _selectedTabIndex = 0; // 0: File active, 1: Annuaire global

  List<PatientDirectoryItem>? _directoryResults;
  bool _directoryLoading = false;
  String? _directoryError;

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _performDirectorySearch([String? query]) async {
    setState(() {
      _directoryLoading = true;
      _directoryError = null;
    });

    try {
      final gateway = ref.read(patientDirectoryApiProvider);
      final results = await gateway.searchPatients(query: query ?? _searchController.text);
      if (mounted) {
        setState(() {
          _directoryResults = results;
          _directoryLoading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context);
        setState(() {
          _directoryError = e is ApiException && e.message.isNotEmpty && !e.message.startsWith('ApiException')
              ? e.message
              : l10n.dashboardQueueLoadError;
          _directoryLoading = false;
        });
      }
    }
  }

  void _onSearchChanged(String value) {
    if (value.trim().isNotEmpty && _selectedTabIndex == 0) {
      // Auto-switch to Directory tab when user types a search query
      setState(() {
        _selectedTabIndex = 1;
      });
    }
    _performDirectorySearch(value);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final visits = widget.queue.value ?? [];

    // Filter active queue if search query present
    final searchQuery = _searchController.text.trim().toLowerCase();
    final filteredVisits = searchQuery.isEmpty
        ? visits
        : visits.where((v) {
            return v.patientName.toLowerCase().contains(searchQuery) ||
                v.patientDpu.toLowerCase().contains(searchQuery) ||
                v.visitNumber.toLowerCase().contains(searchQuery) ||
                v.reason.toLowerCase().contains(searchQuery);
          }).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // ── Search bar ──
        TextField(
          controller: _searchController,
          onChanged: _onSearchChanged,
          decoration: InputDecoration(
            hintText: l10n.directorySearchHint,
            hintStyle: theme.textTheme.bodyMedium?.copyWith(
              color: colors.onSurfaceVariant.withValues(alpha: 0.6),
            ),
            prefixIcon: Icon(Icons.search_rounded, color: colors.primary),
            suffixIcon: _searchController.text.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear_rounded),
                    onPressed: () {
                      _searchController.clear();
                      _onSearchChanged('');
                    },
                  )
                : null,
            filled: true,
            fillColor: colors.surfaceContainerHighest.withValues(alpha: 0.35),
            contentPadding:
                const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
              borderSide: BorderSide(
                color: colors.outlineVariant.withValues(alpha: 0.4),
              ),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
              borderSide: BorderSide(
                color: colors.outlineVariant.withValues(alpha: 0.4),
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
              borderSide: BorderSide(color: colors.primary, width: 1.5),
            ),
          ),
        ),

        const SizedBox(height: 12),

        // ── Workspace Tab Switcher ──
        Container(
          padding: const EdgeInsets.all(4),
          decoration: BoxDecoration(
            color: colors.surfaceContainerHighest.withValues(alpha: 0.3),
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
            border: Border.all(
              color: colors.outlineVariant.withValues(alpha: 0.3),
            ),
          ),
          child: Row(
            children: [
              Expanded(
                child: _TabSegmentButton(
                  label: '${l10n.directoryTabActiveQueue} (${filteredVisits.length})',
                  icon: Icons.people_alt_rounded,
                  selected: _selectedTabIndex == 0,
                  onTap: () {
                    setState(() {
                      _selectedTabIndex = 0;
                    });
                  },
                ),
              ),
              const SizedBox(width: 4),
              Expanded(
                child: _TabSegmentButton(
                  label: l10n.directoryTabAllPatients,
                  icon: Icons.badge_rounded,
                  selected: _selectedTabIndex == 1,
                  onTap: () {
                    setState(() {
                      _selectedTabIndex = 1;
                    });
                    if (_directoryResults == null) {
                      _performDirectorySearch();
                    }
                  },
                ),
              ),
            ],
          ),
        ),

        const SizedBox(height: 14),

        // ── Tab 0: Active Visits Queue ──
        if (_selectedTabIndex == 0) ...[
          _SectionHeader(
            title: l10n.dashboardQueueTitle,
            subtitle: widget.queue.value == null
                ? l10n.dashboardQueueSubtitleLoading
                : l10n.dashboardQueueSubtitle(filteredVisits.length),
            loading: widget.queue.isLoading,
            refreshTooltip: l10n.dashboardQueueRefresh,
            onRefresh: widget.onRefresh,
          ),
          const SizedBox(height: 12),
          widget.queue.when(
            data: (_) => _QueueContent(
              visits: filteredVisits,
              onRefresh: widget.onRefresh,
            ),
            loading: () => const _QueueLoading(),
            error: (error, stackTrace) =>
                _QueueError(onRetry: widget.onRefresh),
          ),
        ],

        // ── Tab 1: Global Patient Directory (Tous les patients) ──
        if (_selectedTabIndex == 1) ...[
          _DirectoryContent(
            loading: _directoryLoading,
            error: _directoryError,
            patients: _directoryResults ?? [],
            activeVisits: visits,
            onRetry: _performDirectorySearch,
            onRefresh: widget.onRefresh,
            l10n: l10n,
          ),
        ],
      ],
    );
  }
}

// ── Tab Segment Button ──
class _TabSegmentButton extends StatelessWidget {
  const _TabSegmentButton({
    required this.label,
    required this.icon,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final IconData icon;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Material(
      color: selected ? colors.surface : Colors.transparent,
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm - 2),
      elevation: selected ? 1 : 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm - 2),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                icon,
                size: 16,
                color: selected ? colors.primary : colors.onSurfaceVariant,
              ),
              const SizedBox(width: 6),
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelMedium?.copyWith(
                    fontWeight: selected ? FontWeight.w800 : FontWeight.w600,
                    color: selected ? colors.primary : colors.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Directory Content (Global Patient Directory List) ──
class _DirectoryContent extends StatelessWidget {
  const _DirectoryContent({
    required this.loading,
    required this.error,
    required this.patients,
    required this.activeVisits,
    required this.onRetry,
    required this.onRefresh,
    required this.l10n,
  });

  final bool loading;
  final String? error;
  final List<PatientDirectoryItem> patients;
  final List<ActiveVisit> activeVisits;
  final VoidCallback onRetry;
  final Future<void> Function() onRefresh;
  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    if (loading) {
      return const _QueueLoading();
    }

    if (error != null) {
      return _QueueError(onRetry: () async => onRetry());
    }

    if (patients.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
        decoration: BoxDecoration(
          color: colors.surfaceContainerHighest.withValues(alpha: 0.2),
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          border: Border.all(
            color: colors.outlineVariant.withValues(alpha: 0.3),
          ),
        ),
        child: Column(
          children: [
            Icon(Icons.search_off_rounded,
                size: 40, color: colors.onSurfaceVariant),
            const SizedBox(height: 8),
            Text(
              l10n.directoryEmptyText,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.onSurfaceVariant,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        for (var index = 0; index < patients.length; index++) ...[
          _PatientDirectoryCard(
            patient: patients[index],
            activeVisit: _findActiveVisit(patients[index].id),
            onRefresh: onRefresh,
          ),
          if (index != patients.length - 1)
            const SizedBox(height: AppDesignTokens.spaceSm),
        ],
      ],
    );
  }

  ActiveVisit? _findActiveVisit(String patientId) {
    for (final visit in activeVisits) {
      if (visit.patientId == patientId) return visit;
    }
    return null;
  }
}

// ── Patient Directory Card ──
class _PatientDirectoryCard extends StatelessWidget {
  const _PatientDirectoryCard({
    required this.patient,
    this.activeVisit,
    required this.onRefresh,
  });

  final PatientDirectoryItem patient;
  final ActiveVisit? activeVisit;
  final Future<void> Function() onRefresh;

  String _formattedReference(String rawDpu) {
    var cleaned = rawDpu.trim();
    cleaned = cleaned.replaceAll(
      RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
      'DPU-',
    );
    if (!cleaned.toUpperCase().startsWith('DPU-')) {
      cleaned = 'DPU-$cleaned';
    }
    return cleaned.replaceAll('-', '\u2011');
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    final isCurrentlyInQueue = activeVisit != null;

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceMd),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(
          color: isCurrentlyInQueue
              ? colors.primary.withValues(alpha: 0.35)
              : colors.outlineVariant.withValues(alpha: 0.3),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              // Avatar
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: colors.primary.withValues(alpha: 0.1),
                  borderRadius:
                      BorderRadius.circular(AppDesignTokens.radiusSm),
                ),
                alignment: Alignment.center,
                child: Text(
                  patient.fullName.isNotEmpty
                      ? patient.fullName[0].toUpperCase()
                      : 'P',
                  style: theme.textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w900,
                    color: colors.primary,
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      patient.fullName,
                      style: theme.textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _formattedReference(patient.dpu),
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),

              // Queue badge if active
              if (isCurrentlyInQueue)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: colors.primary.withValues(alpha: 0.1),
                    borderRadius:
                        BorderRadius.circular(AppDesignTokens.radiusSm),
                    border: Border.all(
                      color: colors.primary.withValues(alpha: 0.3),
                    ),
                  ),
                  child: Text(
                    'En file d\'attente',
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: colors.primary,
                      fontWeight: FontWeight.w800,
                      fontSize: 10,
                    ),
                  ),
                ),
            ],
          ),

          const SizedBox(height: 10),

          // Metadata row (Phone, Blood group, Gender)
          Wrap(
            spacing: 12,
            runSpacing: 4,
            children: [
              if (patient.phone != null && patient.phone!.isNotEmpty)
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.phone_rounded,
                        size: 13, color: colors.onSurfaceVariant),
                    const SizedBox(width: 4),
                    Text(
                      patient.phone!,
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontSize: 11,
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              if (patient.bloodGroup != null && patient.bloodGroup!.isNotEmpty)
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.bloodtype_rounded,
                        size: 13, color: colors.error),
                    const SizedBox(width: 4),
                    Text(
                      'Groupe ${patient.bloodGroup}',
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontSize: 11,
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
            ],
          ),

          const SizedBox(height: 12),

          // Actions
          Row(
            children: [
              // View medical history (Dossier médical)
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {
                    // Open PatientHistorySheet for this patient
                    final mockVisitForHistory = ActiveVisit(
                      id: activeVisit?.id ?? patient.id,
                      visitNumber: activeVisit?.visitNumber ?? 'VIS-HISTO',
                      patientId: patient.id,
                      patientName: patient.fullName,
                      patientDpu: patient.dpu,
                      reason: activeVisit?.reason ?? 'Consultation dossier',
                      orientation: 'CONSULTATION',
                      status: 'WAITING',
                      createdAt: DateTime.now(),
                    );
                    PatientHistorySheet.show(
                      context,
                      visit: mockVisitForHistory,
                    );
                  },
                  icon: const Icon(Icons.folder_shared_rounded, size: 16),
                  label: Text(l10n.directoryOpenHistory),
                  style: OutlinedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    textStyle: theme.textTheme.labelMedium?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),

              if (isCurrentlyInQueue) ...[
                const SizedBox(width: 8),
                IconButton(
                  onPressed: () => PatientVitalsSheet.show(
                    context,
                    visit: activeVisit!,
                    onSaved: onRefresh,
                  ),
                  icon: const Icon(Icons.monitor_heart_rounded),
                  tooltip: l10n.dashboardQueueActionVitals,
                  color: colors.primary,
                ),
                IconButton(
                  onPressed: () => ConsultationNotesSheet.show(
                    context,
                    visit: activeVisit!,
                    onSaved: onRefresh,
                  ),
                  icon: const Icon(Icons.edit_note_rounded),
                  tooltip: l10n.dashboardQueueActionConsultation,
                  color: colors.primary,
                ),
              ],
            ],
          ),
        ],
      ),
    );
  }
}

// ── Existing Section Header, Queue Content, Card, Loading, Error ──
class _SectionHeader extends StatelessWidget {
  const _SectionHeader({
    required this.title,
    required this.subtitle,
    required this.loading,
    required this.refreshTooltip,
    required this.onRefresh,
  });

  final String title;
  final String subtitle;
  final bool loading;
  final String refreshTooltip;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.bodySmall?.copyWith(
                  color: colors.onSurfaceVariant,
                  height: 1.25,
                ),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        Tooltip(
          message: refreshTooltip,
          child: Material(
            color: colors.primary.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
            child: InkWell(
              onTap: loading ? null : () => onRefresh(),
              borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
              child: SizedBox.square(
                dimension: AppDesignTokens.minTouchTarget,
                child: Center(
                  child: loading
                      ? SizedBox.square(
                          dimension: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: colors.primary,
                          ),
                        )
                      : Icon(
                          Icons.refresh_rounded,
                          size: 21,
                          color: colors.primary,
                        ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _QueueContent extends StatelessWidget {
  const _QueueContent({required this.visits, required this.onRefresh});

  final List<ActiveVisit> visits;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    if (visits.isEmpty) {
      return const _QueueEmpty();
    }

    final withVitals = visits.where((visit) => visit.hasVitals).length;
    final withoutVitals = visits.length - withVitals;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _QueueSummary(
          total: visits.length,
          withVitals: withVitals,
          withoutVitals: withoutVitals,
        ),
        const SizedBox(height: AppDesignTokens.spaceMd),
        for (var index = 0; index < visits.length; index++) ...[
          _ActiveVisitCard(
            visit: visits[index],
            onRefresh: () => onRefresh(),
            onTap: () => PatientVitalsSheet.show(
              context,
              visit: visits[index],
              onSaved: onRefresh,
            ),
          ),
          if (index != visits.length - 1)
            const SizedBox(height: AppDesignTokens.spaceSm),
        ],
      ],
    );
  }
}

class _QueueSummary extends StatelessWidget {
  const _QueueSummary({
    required this.total,
    required this.withVitals,
    required this.withoutVitals,
  });

  final int total;
  final int withVitals;
  final int withoutVitals;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return LayoutBuilder(
      builder: (context, constraints) {
        const gap = 8.0;
        final columns = constraints.maxWidth < 340 ? 2 : 3;
        final itemWidth =
            (constraints.maxWidth - gap * (columns - 1)) / columns;

        return Wrap(
          spacing: gap,
          runSpacing: gap,
          children: [
            SizedBox(
              width: itemWidth,
              child: _MetricCard(
                value: total,
                label: l10n.dashboardQueueTotalCompact,
                accent: colors.primary,
                icon: Icons.people_alt_outlined,
              ),
            ),
            SizedBox(
              width: itemWidth,
              child: _MetricCard(
                value: withVitals,
                label: l10n.dashboardQueueWithVitalsCompact,
                accent: AppDesignTokens.success,
                icon: Icons.favorite_border_rounded,
              ),
            ),
            SizedBox(
              width: itemWidth,
              child: _MetricCard(
                value: withoutVitals,
                label: l10n.dashboardQueueWithoutVitals,
                accent: colors.tertiary,
                icon: Icons.pending_actions_rounded,
              ),
            ),
          ],
        );
      },
    );
  }
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.value,
    required this.label,
    required this.accent,
    required this.icon,
  });

  final int value;
  final String label;
  final Color accent;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: AppDesignTokens.spaceSm,
        vertical: AppDesignTokens.spaceSm,
      ),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest.withValues(alpha: 0.3),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(color: accent.withValues(alpha: 0.3)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 14, color: accent),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: AppDesignTokens.spaceXs),
          Text(
            '$value',
            style: theme.textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w900,
              color: colors.onSurface,
            ),
          ),
        ],
      ),
    );
  }
}

class _ActiveVisitCard extends StatelessWidget {
  const _ActiveVisitCard({
    required this.visit,
    required this.onTap,
    required this.onRefresh,
  });

  final ActiveVisit visit;
  final VoidCallback onTap;
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);

    final arrivalFormatted = DateFormat.Hm(l10n.localeName).format(visit.queueSince.toLocal());
    final isVitalsDone = visit.hasVitals;
    final badgeColor = isVitalsDone ? AppDesignTokens.success : colors.tertiary;

    return Container(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(
          color: isVitalsDone
              ? AppDesignTokens.success.withValues(alpha: 0.25)
              : colors.outlineVariant.withValues(alpha: 0.4),
        ),
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
          child: Padding(
            padding: const EdgeInsets.all(AppDesignTokens.spaceMd),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: colors.primary.withValues(alpha: 0.1),
                        borderRadius:
                            BorderRadius.circular(AppDesignTokens.radiusSm),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        visit.patientName.isNotEmpty
                            ? visit.patientName[0].toUpperCase()
                            : 'P',
                        style: theme.textTheme.titleMedium?.copyWith(
                          fontWeight: FontWeight.w900,
                          color: colors.primary,
                        ),
                      ),
                    ),
                    const SizedBox(width: AppDesignTokens.spaceSm),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            visit.patientName,
                            style: theme.textTheme.titleMedium?.copyWith(
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            l10n.dashboardQueueVisitReference(
                              visit.visitNumber,
                              visit.patientDpu,
                            ),
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: AppDesignTokens.spaceSm,
                        vertical: AppDesignTokens.spaceXs,
                      ),
                      decoration: BoxDecoration(
                        color: badgeColor.withValues(alpha: 0.12),
                        borderRadius:
                            BorderRadius.circular(AppDesignTokens.radiusSm),
                        border: Border.all(
                          color: badgeColor.withValues(alpha: 0.3),
                        ),
                      ),
                      child: Text(
                        isVitalsDone
                            ? l10n.dashboardQueueVitalsReadyCompact
                            : l10n.dashboardQueueVitalsPending,
                        style: theme.textTheme.labelSmall?.copyWith(
                          color: badgeColor,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppDesignTokens.spaceSm),
                Text(
                  visit.reason,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
                ),
                const SizedBox(height: AppDesignTokens.spaceXs),
                Row(
                  children: [
                    Icon(
                      Icons.schedule_rounded,
                      size: 13,
                      color: colors.onSurfaceVariant,
                    ),
                    const SizedBox(width: 4),
                    Text(
                      l10n.dashboardQueueArrivedAt(arrivalFormatted),
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                        fontSize: 11,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: AppDesignTokens.spaceMd),
                Divider(
                  height: 1,
                  color: colors.outlineVariant.withValues(alpha: 0.3),
                ),
                const SizedBox(height: AppDesignTokens.spaceSm),
                LayoutBuilder(
                  builder: (context, actionConstraints) {
                    final isCompactWidth = actionConstraints.maxWidth < 360;

                    final actions = [
                      Expanded(
                        child: _ActionChipButton(
                          icon: Icons.monitor_heart_rounded,
                          label: l10n.dashboardQueueActionVitals,
                          color: colors.primary,
                          compact: isCompactWidth,
                          onTap: () => PatientVitalsSheet.show(
                            context,
                            visit: visit,
                            onSaved: onRefresh,
                          ),
                        ),
                      ),
                      const SizedBox(width: AppDesignTokens.spaceXs),
                      Expanded(
                        child: _ActionChipButton(
                          icon: Icons.edit_note_rounded,
                          label: l10n.dashboardQueueActionConsultation,
                          color: colors.secondary,
                          compact: isCompactWidth,
                          onTap: () => ConsultationNotesSheet.show(
                            context,
                            visit: visit,
                            onSaved: onRefresh,
                          ),
                        ),
                      ),
                      const SizedBox(width: AppDesignTokens.spaceXs),
                      Expanded(
                        child: _ActionChipButton(
                          icon: Icons.folder_shared_rounded,
                          label: l10n.dashboardQueueActionHistory,
                          color: colors.tertiary,
                          compact: isCompactWidth,
                          onTap: () => PatientHistorySheet.show(
                            context,
                            visit: visit,
                          ),
                        ),
                      ),
                    ];

                    return Row(children: actions);
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ActionChipButton extends StatelessWidget {
  const _ActionChipButton({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
    this.compact = false,
  });

  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Material(
      color: color.withValues(alpha: 0.1),
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        child: Container(
          height: 36,
          padding: EdgeInsets.symmetric(
            horizontal: compact ? 4 : AppDesignTokens.spaceXs,
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, size: 15, color: color),
              SizedBox(width: compact ? 2 : 4),
              Flexible(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: color,
                    fontWeight: FontWeight.w800,
                    fontSize: compact ? 10 : 11,
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _QueueLoading extends StatelessWidget {
  const _QueueLoading();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceXl),
      alignment: Alignment.center,
      child: CircularProgressIndicator(color: colors.primary),
    );
  }
}

class _QueueEmpty extends StatelessWidget {
  const _QueueEmpty();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        border: Border.all(color: colors.outlineVariant.withValues(alpha: 0.3)),
      ),
      child: Column(
        children: [
          Icon(Icons.done_all_rounded, size: 36, color: colors.primary),
          const SizedBox(height: AppDesignTokens.spaceSm),
          Text(
            l10n.dashboardQueueEmptyTitle,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            l10n.dashboardQueueEmptyBody,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

class _QueueError extends StatelessWidget {
  const _QueueError({required this.onRetry});

  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(AppDesignTokens.spaceLg),
      decoration: BoxDecoration(
        color: colors.errorContainer,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.cloud_off_rounded, color: colors.onErrorContainer),
          const SizedBox(height: AppDesignTokens.spaceSm),
          Text(
            l10n.dashboardQueueLoadError,
            textAlign: TextAlign.center,
            style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: colors.onErrorContainer,
            ),
          ),
          const SizedBox(height: AppDesignTokens.spaceMd),
          AppButton(
            label: l10n.dashboardQueueRetry,
            icon: Icons.refresh_rounded,
            variant: AppButtonVariant.secondary,
            onPressed: () => onRetry(),
          ),
        ],
      ),
    );
  }
}

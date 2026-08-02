import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../auth/domain/effective_access.dart';
import '../../../foundation/presentation/mobile_workspace_localizations.dart';
import '../../application/active_queue_controller.dart';
import '../../data/patient_directory_api.dart';
import '../../domain/patient_directory_item.dart';
import '../../domain/patient_record.dart';
import '../widgets/patient_record_page.dart';

class PatientsPage extends ConsumerStatefulWidget {
  const PatientsPage({required this.access, super.key});

  final EffectiveAccess access;

  @override
  ConsumerState<PatientsPage> createState() => _PatientsPageState();
}

class _PatientsPageState extends ConsumerState<PatientsPage>
    with AutomaticKeepAliveClientMixin {
  final _searchController = TextEditingController();
  Timer? _debounce;
  List<PatientDirectoryItem> _patients = const [];
  bool _loading = false;
  String? _error;

  @override
  bool get wantKeepAlive => true;

  @override
  void initState() {
    super.initState();
    if (widget.access.hasPermission('PATIENT_READ')) {
      _search();
    }
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  void _onSearchChanged(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), _search);
  }

  Future<void> _search() async {
    if (!widget.access.hasPermission('PATIENT_READ')) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final patients = await ref.read(patientDirectoryApiProvider).searchPatients(
            query: _searchController.text,
          );
      if (!mounted) return;
      setState(() {
        _patients = patients;
        _loading = false;
      });
    } on ApiException catch (error) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = error.message;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = AppLocalizations.of(context).recordLoadError;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    super.build(context);
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;

    if (!widget.access.hasPermission('PATIENT_READ')) {
      return _PermissionDenied(message: l10n.patientsDirectoryForbidden);
    }

    final activeVisits = ref.watch(activeQueueControllerProvider).value ?? const [];

    return RefreshIndicator(
      onRefresh: _search,
      child: CustomScrollView(
        key: const PageStorageKey('patients-directory-scroll'),
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(16, 18, 16, 12),
            sliver: SliverToBoxAdapter(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    l10n.patientsTitle,
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                  ),
                  const SizedBox(height: 5),
                  Text(
                    l10n.patientsSubtitle,
                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: colors.onSurfaceVariant,
                          height: 1.4,
                        ),
                  ),
                  const SizedBox(height: 18),
                  TextField(
                    controller: _searchController,
                    onChanged: _onSearchChanged,
                    textInputAction: TextInputAction.search,
                    onSubmitted: (_) => _search(),
                    decoration: InputDecoration(
                      hintText: l10n.patientsSearchHint,
                      prefixIcon: const Icon(Icons.search_rounded),
                      suffixIcon: _searchController.text.isEmpty
                          ? null
                          : IconButton(
                              onPressed: () {
                                _searchController.clear();
                                setState(() {});
                                _search();
                              },
                              icon: const Icon(Icons.close_rounded),
                            ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          if (_loading && _patients.isEmpty)
            const SliverFillRemaining(
              hasScrollBody: false,
              child: Center(child: CircularProgressIndicator()),
            )
          else if (_error != null)
            SliverFillRemaining(
              hasScrollBody: false,
              child: _DirectoryError(message: _error!, onRetry: _search),
            )
          else if (_patients.isEmpty)
            SliverFillRemaining(
              hasScrollBody: false,
              child: _DirectoryEmpty(onRefresh: _search),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 28),
              sliver: SliverList.separated(
                itemCount: _patients.length,
                separatorBuilder: (_, _) => const SizedBox(height: 11),
                itemBuilder: (context, index) {
                  final patient = _patients[index];
                  final activeVisit = activeVisits
                      .where((visit) => visit.patientId == patient.id)
                      .firstOrNull;
                  return _PatientDirectoryCard(
                    patient: patient,
                    hasActiveVisit: activeVisit != null,
                    onTap: () => PatientRecordPage.show(
                      context,
                      target: PatientRecordTarget(
                        patientId: patient.id,
                        patientName: patient.fullName,
                        patientDpu: patient.dpu,
                        activeVisit: activeVisit,
                      ),
                      access: widget.access,
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }
}

class _PatientDirectoryCard extends StatelessWidget {
  const _PatientDirectoryCard({
    required this.patient,
    required this.hasActiveVisit,
    required this.onTap,
  });

  final PatientDirectoryItem patient;
  final bool hasActiveVisit;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final initial = patient.fullName.trim().isEmpty
        ? 'P'
        : patient.fullName.trim()[0].toUpperCase();

    return Material(
      color: colors.surface,
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
        child: Container(
          padding: const EdgeInsets.all(15),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(AppDesignTokens.radiusLg),
            border: Border.all(
              color: hasActiveVisit
                  ? colors.primary.withValues(alpha: 0.45)
                  : colors.outlineVariant,
            ),
          ),
          child: Row(
            children: [
              Container(
                width: 46,
                height: 46,
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: colors.primaryContainer,
                  borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
                ),
                child: Text(
                  initial,
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
                      patient.fullName,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            fontWeight: FontWeight.w900,
                          ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      patient.dpu,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: colors.onSurfaceVariant,
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                    if (patient.phone?.trim().isNotEmpty == true) ...[
                      const SizedBox(height: 3),
                      Text(
                        patient.phone!,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                              color: colors.onSurfaceVariant,
                            ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  if (hasActiveVisit)
                    Container(
                      width: 9,
                      height: 9,
                      decoration: BoxDecoration(
                        color: colors.primary,
                        shape: BoxShape.circle,
                      ),
                    ),
                  const SizedBox(height: 8),
                  Icon(Icons.chevron_right_rounded, color: colors.onSurfaceVariant),
                  Text(
                    l10n.patientsOpenRecord,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: colors.primary,
                          fontWeight: FontWeight.w800,
                        ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _PermissionDenied extends StatelessWidget {
  const _PermissionDenied({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.lock_outline_rounded, size: 52, color: colors.error),
            const SizedBox(height: 16),
            Text(message, textAlign: TextAlign.center),
          ],
        ),
      ),
    );
  }
}

class _DirectoryError extends StatelessWidget {
  const _DirectoryError({required this.message, required this.onRetry});
  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.cloud_off_rounded, size: 48),
            const SizedBox(height: 14),
            Text(message, textAlign: TextAlign.center),
            const SizedBox(height: 16),
            FilledButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh_rounded),
              label: Text(l10n.recordRefresh),
            ),
          ],
        ),
      ),
    );
  }
}

class _DirectoryEmpty extends StatelessWidget {
  const _DirectoryEmpty({required this.onRefresh});
  final Future<void> Function() onRefresh;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 70,
              height: 70,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: colors.primaryContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                Icons.person_search_rounded,
                size: 34,
                color: colors.onPrimaryContainer,
              ),
            ),
            const SizedBox(height: 18),
            Text(
              l10n.patientsEmptyTitle,
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
            ),
            const SizedBox(height: 6),
            Text(
              l10n.patientsEmptyBody,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: onRefresh,
              icon: const Icon(Icons.refresh_rounded),
              label: Text(l10n.recordRefresh),
            ),
          ],
        ),
      ),
    );
  }
}

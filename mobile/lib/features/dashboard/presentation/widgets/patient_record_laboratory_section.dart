import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../../../../core/i18n/app_locale_formatters.dart';
import '../../../../core/network/api_exception.dart';
import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../auth/domain/effective_access.dart';
import '../../data/lab_order_api.dart';
import '../../domain/patient_record.dart';
import '../lab_localizations.dart';
import 'lab_order_create_sheet.dart';

class PatientRecordLaboratoryDetailSection extends ConsumerStatefulWidget {
  const PatientRecordLaboratoryDetailSection({
    required this.record,
    required this.access,
    required this.onChanged,
    super.key,
  });

  final PatientRecordBundle record;
  final EffectiveAccess access;
  final VoidCallback onChanged;

  @override
  ConsumerState<PatientRecordLaboratoryDetailSection> createState() =>
      _PatientRecordLaboratoryDetailSectionState();
}

class _PatientRecordLaboratoryDetailSectionState
    extends ConsumerState<PatientRecordLaboratoryDetailSection> {
  final Set<String> _expandedOrderIds = <String>{};
  final Set<String> _busyOrderIds = <String>{};
  final Set<String> _busyResultIds = <String>{};

  Map<String, List<PatientLabResultSummary>> get _resultsByOrder {
    final grouped = <String, List<PatientLabResultSummary>>{};
    for (final result in widget.record.labResults) {
      final key = result.examRequestNumber.trim();
      if (key.isEmpty) continue;
      grouped.putIfAbsent(key, () => <PatientLabResultSummary>[]).add(result);
    }
    return grouped;
  }

  List<PatientLabResultSummary> get _orphanResults {
    final orderNumbers = widget.record.labOrders
        .map((order) => order.number.trim())
        .where((number) => number.isNotEmpty)
        .toSet();
    return widget.record.labResults
        .where(
          (result) =>
              result.examRequestNumber.trim().isEmpty ||
              !orderNumbers.contains(result.examRequestNumber.trim()),
        )
        .toList(growable: false);
  }

  Future<void> _createOrder() {
    return LabOrderCreateSheet.show(
      context,
      patientId: widget.record.identity.id,
      patientName: widget.record.identity.fullName,
      onCreated: widget.onChanged,
    );
  }

  Future<void> _updateStatus(
    PatientLabOrderSummary order,
    String status,
  ) async {
    if (_busyOrderIds.contains(order.id)) return;
    final l10n = AppLocalizations.of(context);
    setState(() => _busyOrderIds.add(order.id));
    try {
      await ref
          .read(labOrderApiProvider)
          .updateStatus(orderId: order.id, status: status);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.labStatusUpdated)));
      widget.onChanged();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_errorMessage(error, l10n.labStatusUpdateError)),
        ),
      );
    } finally {
      if (mounted) setState(() => _busyOrderIds.remove(order.id));
    }
  }

  Future<void> _cancelOrder(PatientLabOrderSummary order) async {
    final l10n = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.labCancelTitle),
        content: Text(l10n.labCancelBody),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(l10n.labBack),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(l10n.labConfirm),
          ),
        ],
      ),
    );
    if (confirmed == true && mounted) {
      await _updateStatus(order, 'CANCELLED');
    }
  }

  Future<void> _downloadPdf(PatientLabResultSummary result) async {
    if (_busyResultIds.contains(result.id)) return;
    final l10n = AppLocalizations.of(context);
    setState(() => _busyResultIds.add(result.id));
    try {
      final bytes = await ref
          .read(labOrderApiProvider)
          .downloadResultPdf(result.id);
      final directory = await getApplicationDocumentsDirectory();
      final rawName = result.resultNumber.trim().isEmpty
          ? 'lab-result-${result.id}'
          : result.resultNumber.trim();
      final safeName = rawName.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '-');
      final file = File('${directory.path}/$safeName.pdf');
      await file.writeAsBytes(bytes, flush: true);
      if (!mounted) return;
      ScaffoldMessenger.of(
        context,
      ).showSnackBar(SnackBar(content: Text(l10n.labPdfSaved)));
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_errorMessage(error, l10n.labPdfError))),
      );
    } finally {
      if (mounted) setState(() => _busyResultIds.remove(result.id));
    }
  }

  String _errorMessage(Object error, String fallback) {
    if (error is ApiException &&
        error.message.isNotEmpty &&
        !error.message.startsWith('ApiException')) {
      return error.message;
    }
    return fallback;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final activeOrders = widget.record.labOrders
        .where((order) => order.isActive)
        .toList(growable: false);
    final historyOrders = widget.record.labOrders
        .where((order) => order.isTerminal)
        .toList(growable: false);
    final resultsByOrder = _resultsByOrder;
    final orphanResults = _orphanResults;
    final canCreate = widget.access.hasPermission('LAB_ORDER_CREATE');
    final canWrite = widget.access.hasPermission('LAB_ORDER_WRITE');

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
      children: [
        _LabHeader(
          activeCount: activeOrders.length,
          resultCount: widget.record.labResults.length,
          canCreate: canCreate,
          onCreate: _createOrder,
        ),
        if (widget.record.labOrders.isEmpty && orphanResults.isEmpty) ...[
          const SizedBox(height: 24),
          _EmptyState(message: l10n.labEmptyBody),
        ] else ...[
          if (activeOrders.isNotEmpty) ...[
            const SizedBox(height: 18),
            _ListHeading(label: l10n.labActive, count: activeOrders.length),
            const SizedBox(height: 8),
            for (final order in activeOrders) ...[
              _LabTrackingCard(
                order: order,
                results: resultsByOrder[order.number] ?? const [],
                expanded: _expandedOrderIds.contains(order.id),
                canWrite: canWrite,
                busy: _busyOrderIds.contains(order.id),
                busyResultIds: _busyResultIds,
                onToggle: () {
                  setState(() {
                    if (!_expandedOrderIds.add(order.id)) {
                      _expandedOrderIds.remove(order.id);
                    }
                  });
                },
                onAdvance: order.nextOperationalStatus == null
                    ? null
                    : () => _updateStatus(order, order.nextOperationalStatus!),
                onCancel: _canCancel(order, canWrite)
                    ? () => _cancelOrder(order)
                    : null,
                onDownloadPdf: _downloadPdf,
              ),
              const SizedBox(height: 10),
            ],
          ],
          if (historyOrders.isNotEmpty || orphanResults.isNotEmpty) ...[
            const SizedBox(height: 16),
            _ListHeading(
              label: l10n.labHistory,
              count: historyOrders.length + (orphanResults.isEmpty ? 0 : 1),
            ),
            const SizedBox(height: 8),
            for (final order in historyOrders) ...[
              _LabTrackingCard(
                order: order,
                results: resultsByOrder[order.number] ?? const [],
                expanded: _expandedOrderIds.contains(order.id),
                canWrite: canWrite,
                busy: _busyOrderIds.contains(order.id),
                busyResultIds: _busyResultIds,
                onToggle: () {
                  setState(() {
                    if (!_expandedOrderIds.add(order.id)) {
                      _expandedOrderIds.remove(order.id);
                    }
                  });
                },
                onAdvance: null,
                onCancel: null,
                onDownloadPdf: _downloadPdf,
              ),
              const SizedBox(height: 10),
            ],
            if (orphanResults.isNotEmpty)
              _OrphanResultsCard(
                results: orphanResults,
                busyResultIds: _busyResultIds,
                onDownloadPdf: _downloadPdf,
              ),
          ],
        ],
      ],
    );
  }

  bool _canCancel(PatientLabOrderSummary order, bool canWrite) {
    if (!canWrite || order.isTerminal) return false;
    return order.normalizedStatus != 'RESULT_AVAILABLE';
  }
}

class _LabHeader extends StatelessWidget {
  const _LabHeader({
    required this.activeCount,
    required this.resultCount,
    required this.canCreate,
    required this.onCreate,
  });

  final int activeCount;
  final int resultCount;
  final bool canCreate;
  final VoidCallback onCreate;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                l10n.labSectionTitle,
                style: theme.textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 3),
              Text(
                '${l10n.labExamsCount(activeCount)} · ${l10n.labResultsCount(resultCount)}',
                style: theme.textTheme.bodySmall?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        if (canCreate)
          TextButton.icon(
            onPressed: onCreate,
            icon: const Icon(Icons.add_rounded, size: 18),
            label: Text(l10n.labCreateOrder),
          ),
      ],
    );
  }
}

class _ListHeading extends StatelessWidget {
  const _ListHeading({required this.label, required this.count});

  final String label;
  final int count;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Row(
      children: [
        Text(
          label,
          style: theme.textTheme.labelLarge?.copyWith(
            color: colors.onSurfaceVariant,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(width: 7),
        Text(
          '$count',
          style: theme.textTheme.labelMedium?.copyWith(
            color: colors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class _LabTrackingCard extends StatelessWidget {
  const _LabTrackingCard({
    required this.order,
    required this.results,
    required this.expanded,
    required this.canWrite,
    required this.busy,
    required this.busyResultIds,
    required this.onToggle,
    required this.onAdvance,
    required this.onCancel,
    required this.onDownloadPdf,
  });

  final PatientLabOrderSummary order;
  final List<PatientLabResultSummary> results;
  final bool expanded;
  final bool canWrite;
  final bool busy;
  final Set<String> busyResultIds;
  final VoidCallback onToggle;
  final VoidCallback? onAdvance;
  final VoidCallback? onCancel;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    final visibleExams = order.exams.take(2).toList(growable: false);

    return Material(
      color: colors.surface,
      borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
      child: Container(
        decoration: BoxDecoration(
          border: Border.all(color: colors.outlineVariant),
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            InkWell(
              onTap: onToggle,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(12, 11, 9, 10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            order.number.isEmpty
                                ? l10n.labOrderNumber
                                : order.number,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: theme.textTheme.titleSmall?.copyWith(
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        _StatusTag(order: order),
                        const SizedBox(width: 4),
                        Icon(
                          expanded
                              ? Icons.expand_less_rounded
                              : Icons.expand_more_rounded,
                          color: colors.onSurfaceVariant,
                        ),
                      ],
                    ),
                    const SizedBox(height: 5),
                    Text(
                      [
                        if (order.createdAt != null)
                          AppLocaleFormatters.formatDate(
                            order.createdAt!,
                            locale,
                          ),
                        if (order.practitioner?.trim().isNotEmpty == true)
                          order.practitioner!.trim(),
                      ].join(' · '),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 10),
                    _ProgressTimeline(order: order),
                    const SizedBox(height: 10),
                    for (final exam in visibleExams)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 3),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Icon(
                              Icons.science_outlined,
                              size: 15,
                              color: colors.onSurfaceVariant,
                            ),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                exam,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: theme.textTheme.bodyMedium?.copyWith(
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    Row(
                      children: [
                        if (order.exams.length > 2)
                          Text(
                            '+${order.exams.length - 2}',
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        const Spacer(),
                        if (results.isNotEmpty)
                          Text(
                            l10n.labResultsCount(results.length),
                            style: theme.textTheme.labelMedium?.copyWith(
                              color: colors.primary,
                              fontWeight: FontWeight.w900,
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            if (expanded) ...[
              Divider(height: 1, color: colors.outlineVariant),
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 12, 12, 13),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _OrderDetails(order: order),
                    if (results.isNotEmpty) ...[
                      const SizedBox(height: 14),
                      Text(
                        l10n.labResults,
                        style: theme.textTheme.labelLarge?.copyWith(
                          color: colors.primary,
                          fontWeight: FontWeight.w900,
                        ),
                      ),
                      const SizedBox(height: 7),
                      for (final group in _groupResults(results)) ...[
                        _ResultGroup(
                          results: group,
                          busyResultIds: busyResultIds,
                          onDownloadPdf: onDownloadPdf,
                        ),
                        const SizedBox(height: 8),
                      ],
                    ] else if (order.progressIndex >= 3) ...[
                      const SizedBox(height: 12),
                      Text(
                        l10n.labNoStructuredResults,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: colors.onSurfaceVariant,
                        ),
                      ),
                    ],
                    if (canWrite &&
                        (onAdvance != null || onCancel != null)) ...[
                      const SizedBox(height: 12),
                      Divider(height: 1, color: colors.outlineVariant),
                      const SizedBox(height: 8),
                      Wrap(
                        alignment: WrapAlignment.end,
                        spacing: 6,
                        runSpacing: 4,
                        children: [
                          if (onCancel != null)
                            TextButton.icon(
                              onPressed: busy ? null : onCancel,
                              icon: Icon(
                                Icons.cancel_outlined,
                                size: 17,
                                color: colors.error,
                              ),
                              label: Text(
                                l10n.labCancelOrder,
                                style: TextStyle(color: colors.error),
                              ),
                            ),
                          if (onAdvance != null)
                            FilledButton.tonalIcon(
                              onPressed: busy ? null : onAdvance,
                              icon: busy
                                  ? const SizedBox.square(
                                      dimension: 16,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2,
                                      ),
                                    )
                                  : const Icon(
                                      Icons.arrow_forward_rounded,
                                      size: 17,
                                    ),
                              label: Text(
                                order.nextOperationalStatus ==
                                        'SAMPLE_COLLECTED'
                                    ? l10n.labMarkSampleCollected
                                    : l10n.labStartProcessing,
                              ),
                            ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _OrderDetails extends StatelessWidget {
  const _OrderDetails({required this.order});

  final PatientLabOrderSummary order;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Wrap(
          spacing: 7,
          runSpacing: 7,
          children: [
            if (order.examType?.trim().isNotEmpty == true)
              _MetaTag(label: l10n.labExamTypeLabel(order.examType!)),
            if (order.priority?.trim().isNotEmpty == true)
              _MetaTag(
                label: order.priority!.trim().toUpperCase() == 'URGENTE'
                    ? l10n.labPriorityUrgent
                    : l10n.labPriorityNormal,
                warning: order.priority!.trim().toUpperCase() == 'URGENTE',
              ),
          ],
        ),
        if (order.reason?.trim().isNotEmpty == true) ...[
          const SizedBox(height: 10),
          _MetadataRow(label: l10n.labReason, value: order.reason!.trim()),
        ],
        if (order.exams.length > 2) ...[
          const SizedBox(height: 10),
          Text(
            l10n.labRequestedExams,
            style: theme.textTheme.labelMedium?.copyWith(
              color: colors.onSurfaceVariant,
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 5),
          for (final exam in order.exams)
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Text('• $exam', style: theme.textTheme.bodySmall),
            ),
        ],
      ],
    );
  }
}

class _ProgressTimeline extends StatelessWidget {
  const _ProgressTimeline({required this.order});

  final PatientLabOrderSummary order;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    if (order.isCancelled) {
      return Row(
        children: [
          Icon(Icons.cancel_outlined, size: 17, color: colors.error),
          const SizedBox(width: 6),
          Text(
            l10n.labStatusLabel(order.status),
            style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: colors.error,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      );
    }

    final steps = <(String, IconData)>[
      (l10n.labStepRequested, Icons.assignment_turned_in_outlined),
      (l10n.labStepSample, Icons.water_drop_outlined),
      (l10n.labStepProcessing, Icons.biotech_outlined),
      (l10n.labStepResult, Icons.analytics_outlined),
      (l10n.labStepValidated, Icons.verified_outlined),
    ];
    final current = order.progressIndex.clamp(0, steps.length - 1);

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (var index = 0; index < steps.length; index++) ...[
          Expanded(
            child: _TimelineStep(
              label: steps[index].$1,
              icon: steps[index].$2,
              complete: index <= current,
              current: index == current,
            ),
          ),
          if (index != steps.length - 1)
            Padding(
              padding: const EdgeInsets.only(top: 9),
              child: Container(
                width: 8,
                height: 1,
                color: index < current ? colors.primary : colors.outlineVariant,
              ),
            ),
        ],
      ],
    );
  }
}

class _TimelineStep extends StatelessWidget {
  const _TimelineStep({
    required this.label,
    required this.icon,
    required this.complete,
    required this.current,
  });

  final String label;
  final IconData icon;
  final bool complete;
  final bool current;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final tone = complete ? colors.primary : colors.onSurfaceVariant;
    return Column(
      children: [
        Icon(
          current ? Icons.radio_button_checked_rounded : icon,
          size: 18,
          color: tone,
        ),
        const SizedBox(height: 3),
        Text(
          label,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: theme.textTheme.labelSmall?.copyWith(
            color: tone,
            fontSize: 9.5,
            fontWeight: current ? FontWeight.w900 : FontWeight.w600,
            height: 1.05,
          ),
        ),
      ],
    );
  }
}

class _ResultGroup extends StatelessWidget {
  const _ResultGroup({
    required this.results,
    required this.busyResultIds,
    required this.onDownloadPdf,
  });

  final List<PatientLabResultSummary> results;
  final Set<String> busyResultIds;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context);
    final first = results.first;
    final pdfResult = results.where((result) => result.hasPdf).firstOrNull;
    final conclusion = _firstNonEmpty(
      results.map((result) => result.conclusion),
    );
    final validator = _firstNonEmpty(
      results.map((result) => result.validatorName),
    );
    final sampleAt = _firstDate(
      results.map((result) => result.sampleCollectedAt),
    );
    final resultAt = _firstDate(results.map((result) => result.resultAt));
    final validatedAt = _firstDate(results.map((result) => result.validatedAt));

    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.outlineVariant.withValues(alpha: 0.7)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  first.resultNumber.trim().isEmpty
                      ? l10n.labResult
                      : first.resultNumber,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.labelLarge?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              if (first.status.trim().isNotEmpty)
                _MiniStatusTag(label: l10n.labStatusLabel(first.status)),
            ],
          ),
          const SizedBox(height: 8),
          for (var index = 0; index < results.length; index++) ...[
            _AnalyteRow(result: results[index]),
            if (index != results.length - 1)
              Divider(height: 14, color: colors.outlineVariant),
          ],
          if (conclusion != null) ...[
            const SizedBox(height: 9),
            _MetadataRow(label: l10n.labConclusion, value: conclusion),
          ],
          if (validator != null) ...[
            const SizedBox(height: 5),
            _MetadataRow(label: l10n.labValidator, value: validator),
          ],
          if (sampleAt != null || resultAt != null || validatedAt != null) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 10,
              runSpacing: 5,
              children: [
                if (sampleAt != null)
                  _DateMeta(
                    label: l10n.labSampleCollectedAt,
                    value: AppLocaleFormatters.formatDate(sampleAt, locale),
                  ),
                if (resultAt != null)
                  _DateMeta(
                    label: l10n.labResultAt,
                    value: AppLocaleFormatters.formatDate(resultAt, locale),
                  ),
                if (validatedAt != null)
                  _DateMeta(
                    label: l10n.labValidatedAt,
                    value: AppLocaleFormatters.formatDate(validatedAt, locale),
                  ),
              ],
            ),
          ],
          if (pdfResult != null) ...[
            const SizedBox(height: 7),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: busyResultIds.contains(pdfResult.id)
                    ? null
                    : () => onDownloadPdf(pdfResult),
                icon: busyResultIds.contains(pdfResult.id)
                    ? const SizedBox.square(
                        dimension: 15,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.picture_as_pdf_outlined, size: 17),
                label: Text(l10n.labDownloadPdf),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _AnalyteRow extends StatelessWidget {
  const _AnalyteRow({required this.result});

  final PatientLabResultSummary result;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final interpretation = result.interpretation?.trim();
    final tone = result.isCritical ? colors.error : colors.onSurfaceVariant;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Text(
                result.analyte.trim().isEmpty ? l10n.labResult : result.analyte,
                style: theme.textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Text(
              '${result.value}${result.unit?.trim().isNotEmpty == true ? ' ${result.unit!.trim()}' : ''}',
              style: theme.textTheme.bodyMedium?.copyWith(
                fontWeight: FontWeight.w900,
                color: result.isCritical ? colors.error : null,
              ),
            ),
          ],
        ),
        if (result.referenceRange?.trim().isNotEmpty == true ||
            interpretation?.isNotEmpty == true) ...[
          const SizedBox(height: 3),
          Row(
            children: [
              if (result.referenceRange?.trim().isNotEmpty == true)
                Expanded(
                  child: Text(
                    '${l10n.labReferenceRange}: ${result.referenceRange}',
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      color: colors.onSurfaceVariant,
                    ),
                  ),
                )
              else
                const Spacer(),
              if (interpretation?.isNotEmpty == true) ...[
                const SizedBox(width: 8),
                Text(
                  interpretation!,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: tone,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ],
            ],
          ),
        ],
        if (result.comment?.trim().isNotEmpty == true) ...[
          const SizedBox(height: 4),
          Text(
            result.comment!,
            style: theme.textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
              fontStyle: FontStyle.italic,
            ),
          ),
        ],
      ],
    );
  }
}

class _OrphanResultsCard extends StatelessWidget {
  const _OrphanResultsCard({
    required this.results,
    required this.busyResultIds,
    required this.onDownloadPdf,
  });

  final List<PatientLabResultSummary> results;
  final Set<String> busyResultIds;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusMd),
        border: Border.all(color: colors.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            l10n.labOrphanResult,
            style: theme.textTheme.titleSmall?.copyWith(
              fontWeight: FontWeight.w900,
            ),
          ),
          const SizedBox(height: 8),
          for (final group in _groupResults(results)) ...[
            _ResultGroup(
              results: group,
              busyResultIds: busyResultIds,
              onDownloadPdf: onDownloadPdf,
            ),
            const SizedBox(height: 8),
          ],
        ],
      ),
    );
  }
}

class _StatusTag extends StatelessWidget {
  const _StatusTag({required this.order});

  final PatientLabOrderSummary order;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final l10n = AppLocalizations.of(context);
    final status = order.normalizedStatus;
    final (tone, icon) = switch (status) {
      'VALIDATED' => (AppDesignTokens.success, Icons.verified_outlined),
      'RESULT_AVAILABLE' => (colors.primary, Icons.analytics_outlined),
      'IN_PROGRESS' => (AppDesignTokens.warning, Icons.biotech_outlined),
      'SAMPLE_COLLECTED' => (colors.secondary, Icons.water_drop_outlined),
      'CANCELLED' => (colors.error, Icons.cancel_outlined),
      _ => (colors.onSurfaceVariant, Icons.schedule_outlined),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 4),
      decoration: BoxDecoration(
        color: tone.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: tone.withValues(alpha: 0.3)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: tone),
          const SizedBox(width: 4),
          Text(
            l10n.labStatusLabel(order.status),
            style: theme.textTheme.labelSmall?.copyWith(
              color: tone,
              fontWeight: FontWeight.w900,
            ),
          ),
        ],
      ),
    );
  }
}

class _MiniStatusTag extends StatelessWidget {
  const _MiniStatusTag({required this.label});

  final String label;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest,
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: colors.onSurfaceVariant,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _MetaTag extends StatelessWidget {
  const _MetaTag({required this.label, this.warning = false});

  final String label;
  final bool warning;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final tone = warning ? AppDesignTokens.warning : colors.onSurfaceVariant;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 4),
      decoration: BoxDecoration(
        color: tone.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: tone.withValues(alpha: 0.22)),
      ),
      child: Text(
        label,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: tone,
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}

class _MetadataRow extends StatelessWidget {
  const _MetadataRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          '$label · ',
          style: theme.textTheme.bodySmall?.copyWith(
            color: colors.onSurfaceVariant,
            fontWeight: FontWeight.w800,
          ),
        ),
        Expanded(child: Text(value, style: theme.textTheme.bodySmall)),
      ],
    );
  }
}

class _DateMeta extends StatelessWidget {
  const _DateMeta({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Text(
      '$label · $value',
      style: Theme.of(
        context,
      ).textTheme.labelSmall?.copyWith(color: colors.onSurfaceVariant),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 26, horizontal: 16),
      child: Column(
        children: [
          Icon(
            Icons.science_outlined,
            color: colors.onSurfaceVariant,
            size: 28,
          ),
          const SizedBox(height: 8),
          Text(
            message,
            textAlign: TextAlign.center,
            style: Theme.of(
              context,
            ).textTheme.bodyMedium?.copyWith(color: colors.onSurfaceVariant),
          ),
        ],
      ),
    );
  }
}

List<List<PatientLabResultSummary>> _groupResults(
  List<PatientLabResultSummary> results,
) {
  final groups = <String, List<PatientLabResultSummary>>{};
  for (final result in results) {
    final key = result.resultNumber.trim().isEmpty
        ? 'result-${result.id}'
        : result.resultNumber.trim();
    groups.putIfAbsent(key, () => <PatientLabResultSummary>[]).add(result);
  }
  final values = groups.values.toList(growable: false);
  values.sort((left, right) {
    final leftDate = _firstDate(
      left.map((item) => item.validatedAt ?? item.resultAt ?? item.createdAt),
    );
    final rightDate = _firstDate(
      right.map((item) => item.validatedAt ?? item.resultAt ?? item.createdAt),
    );
    if (leftDate == null && rightDate == null) return 0;
    if (leftDate == null) return 1;
    if (rightDate == null) return -1;
    return rightDate.compareTo(leftDate);
  });
  return values;
}

String? _firstNonEmpty(Iterable<String?> values) {
  for (final value in values) {
    if (value?.trim().isNotEmpty == true) return value!.trim();
  }
  return null;
}

DateTime? _firstDate(Iterable<DateTime?> values) {
  for (final value in values) {
    if (value != null) return value;
  }
  return null;
}

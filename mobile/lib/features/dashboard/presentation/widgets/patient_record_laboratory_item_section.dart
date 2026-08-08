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

class PatientRecordLaboratoryItemSection extends ConsumerStatefulWidget {
  const PatientRecordLaboratoryItemSection({
    required this.record,
    required this.access,
    required this.onChanged,
    super.key,
  });

  final PatientRecordBundle record;
  final EffectiveAccess access;
  final VoidCallback onChanged;

  @override
  ConsumerState<PatientRecordLaboratoryItemSection> createState() =>
      _PatientRecordLaboratoryItemSectionState();
}

class _PatientRecordLaboratoryItemSectionState
    extends ConsumerState<PatientRecordLaboratoryItemSection> {
  final Set<String> _expandedOrders = <String>{};
  final Set<String> _busyItems = <String>{};
  final Set<String> _busyResults = <String>{};

  bool get _canCreate => widget.access.hasPermission('LAB_ORDER_CREATE');
  bool get _canWrite => widget.access.hasPermission('LAB_ORDER_WRITE');

  Future<void> _createOrder() {
    return LabOrderCreateSheet.show(
      context,
      patientId: widget.record.identity.id,
      patientName: widget.record.identity.fullName,
      onCreated: widget.onChanged,
    );
  }

  Future<void> _advanceItem(
    PatientLabOrderSummary order,
    PatientLabOrderItemSummary item,
    String status,
  ) async {
    if (item.id.isEmpty) return;
    final key = '${order.id}:${item.id}';
    if (_busyItems.contains(key)) return;
    final l10n = AppLocalizations.of(context);
    setState(() => _busyItems.add(key));
    try {
      await ref.read(labOrderApiProvider).updateItemStatus(
            orderId: order.id,
            itemId: item.id,
            status: status,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.labStatusUpdated)),
      );
      widget.onChanged();
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(_errorMessage(error, l10n.labStatusUpdateError)),
        ),
      );
    } finally {
      if (mounted) setState(() => _busyItems.remove(key));
    }
  }

  Future<void> _cancelItem(
    PatientLabOrderSummary order,
    PatientLabOrderItemSummary item,
  ) async {
    final l10n = AppLocalizations.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.labCancelExamTitle(item.examName)),
        content: Text(l10n.labCancelExamBody),
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
      await _advanceItem(order, item, 'CANCELLED');
    }
  }

  Future<void> _downloadPdf(PatientLabResultSummary result) async {
    if (_busyResults.contains(result.id)) return;
    final l10n = AppLocalizations.of(context);
    setState(() => _busyResults.add(result.id));
    try {
      final bytes = await ref
          .read(labOrderApiProvider)
          .downloadResultPdf(result.id);
      final directory = await getApplicationDocumentsDirectory();
      final rawName = result.resultNumber.trim().isEmpty
          ? 'lab-result-${result.id}'
          : result.resultNumber.trim();
      final safeName = rawName.replaceAll(RegExp(r'[^A-Za-z0-9._-]'), '-');
      await File('${directory.path}/$safeName.pdf').writeAsBytes(
        bytes,
        flush: true,
      );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.labPdfSaved)),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(_errorMessage(error, l10n.labPdfError))),
      );
    } finally {
      if (mounted) setState(() => _busyResults.remove(result.id));
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
    final active = widget.record.labOrders
        .where((order) => order.isActive)
        .toList(growable: false);
    final history = widget.record.labOrders
        .where((order) => order.isTerminal)
        .toList(growable: false);

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 14, 16, 28),
      children: [
        _Header(
          activeOrders: active.length,
          resultCount: widget.record.labResults.length,
          canCreate: _canCreate,
          onCreate: _createOrder,
        ),
        if (widget.record.labOrders.isEmpty) ...[
          const SizedBox(height: 24),
          _EmptyState(message: l10n.labEmptyBody),
        ] else ...[
          if (active.isNotEmpty) ...[
            const SizedBox(height: 18),
            _Heading(label: l10n.labActive, count: active.length),
            const SizedBox(height: 8),
            for (final order in active) ...[
              _OrderCard(
                order: order,
                results: _resultsForOrder(order),
                expanded: _expandedOrders.contains(order.id),
                canWrite: _canWrite,
                busyItems: _busyItems,
                busyResults: _busyResults,
                onToggle: () => _toggle(order.id),
                onAdvanceItem: (item, status) =>
                    _advanceItem(order, item, status),
                onCancelItem: (item) => _cancelItem(order, item),
                onDownloadPdf: _downloadPdf,
              ),
              const SizedBox(height: 10),
            ],
          ],
          if (history.isNotEmpty) ...[
            const SizedBox(height: 16),
            _Heading(label: l10n.labHistory, count: history.length),
            const SizedBox(height: 8),
            for (final order in history) ...[
              _OrderCard(
                order: order,
                results: _resultsForOrder(order),
                expanded: _expandedOrders.contains(order.id),
                canWrite: false,
                busyItems: _busyItems,
                busyResults: _busyResults,
                onToggle: () => _toggle(order.id),
                onAdvanceItem: (_, _) {},
                onCancelItem: (_) {},
                onDownloadPdf: _downloadPdf,
              ),
              const SizedBox(height: 10),
            ],
          ],
        ],
      ],
    );
  }

  void _toggle(String orderId) {
    setState(() {
      if (!_expandedOrders.add(orderId)) {
        _expandedOrders.remove(orderId);
      }
    });
  }

  List<PatientLabResultSummary> _resultsForOrder(
    PatientLabOrderSummary order,
  ) {
    return widget.record.labResults
        .where(
          (result) =>
              result.examRequestNumber.trim().isNotEmpty &&
              result.examRequestNumber.trim() == order.number.trim(),
        )
        .toList(growable: false);
  }
}

class _Header extends StatelessWidget {
  const _Header({
    required this.activeOrders,
    required this.resultCount,
    required this.canCreate,
    required this.onCreate,
  });

  final int activeOrders;
  final int resultCount;
  final bool canCreate;
  final VoidCallback onCreate;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                l10n.labSectionTitle,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.w900,
                    ),
              ),
              const SizedBox(height: 2),
              Text(
                '${l10n.labExamsCount(activeOrders)} · ${l10n.labResultsCount(resultCount)}',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
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

class _Heading extends StatelessWidget {
  const _Heading({required this.label, required this.count});

  final String label;
  final int count;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Row(
      children: [
        Text(
          label,
          style: Theme.of(context).textTheme.labelLarge?.copyWith(
                color: colors.onSurfaceVariant,
                fontWeight: FontWeight.w900,
              ),
        ),
        const SizedBox(width: 7),
        Text('$count', style: Theme.of(context).textTheme.labelMedium),
      ],
    );
  }
}

class _OrderCard extends StatelessWidget {
  const _OrderCard({
    required this.order,
    required this.results,
    required this.expanded,
    required this.canWrite,
    required this.busyItems,
    required this.busyResults,
    required this.onToggle,
    required this.onAdvanceItem,
    required this.onCancelItem,
    required this.onDownloadPdf,
  });

  final PatientLabOrderSummary order;
  final List<PatientLabResultSummary> results;
  final bool expanded;
  final bool canWrite;
  final Set<String> busyItems;
  final Set<String> busyResults;
  final VoidCallback onToggle;
  final void Function(PatientLabOrderItemSummary item, String status)
      onAdvanceItem;
  final ValueChanged<PatientLabOrderItemSummary> onCancelItem;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final locale = Localizations.localeOf(context);
    final completed = order.items.where((item) => item.isTerminal).length;
    final preview = order.items.take(2).toList(growable: false);

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
                            style: Theme.of(context)
                                .textTheme
                                .titleSmall
                                ?.copyWith(fontWeight: FontWeight.w900),
                          ),
                        ),
                        const SizedBox(width: 8),
                        _StatusPill(status: order.status),
                        Icon(
                          expanded
                              ? Icons.expand_less_rounded
                              : Icons.expand_more_rounded,
                          color: colors.onSurfaceVariant,
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      [
                        if (order.createdAt != null)
                          AppLocaleFormatters.formatDate(order.createdAt!, locale),
                        if (order.practitioner?.trim().isNotEmpty == true)
                          order.practitioner!.trim(),
                        if (order.items.isNotEmpty)
                          l10n.labItemProgress(completed, order.items.length),
                      ].join(' · '),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            color: colors.onSurfaceVariant,
                          ),
                    ),
                    const SizedBox(height: 9),
                    for (final item in preview)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 5),
                        child: _ExamPreview(item: item),
                      ),
                    if (order.items.length > 2)
                      Text(
                        '+${order.items.length - 2}',
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              fontWeight: FontWeight.w800,
                            ),
                      ),
                  ],
                ),
              ),
            ),
            if (expanded) ...[
              Divider(height: 1, color: colors.outlineVariant),
              Padding(
                padding: const EdgeInsets.fromLTRB(12, 8, 12, 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (order.reason?.trim().isNotEmpty == true)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 9),
                        child: Text(
                          order.reason!.trim(),
                          style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                color: colors.onSurfaceVariant,
                              ),
                        ),
                      ),
                    for (var index = 0; index < order.items.length; index++) ...[
                      _ExamUnitRow(
                        order: order,
                        item: order.items[index],
                        results: _resultsForItem(order.items[index]),
                        canWrite: canWrite && order.hasStructuredItems,
                        busy: busyItems.contains(
                          '${order.id}:${order.items[index].id}',
                        ),
                        busyResults: busyResults,
                        onAdvance: onAdvanceItem,
                        onCancel: onCancelItem,
                        onDownloadPdf: onDownloadPdf,
                      ),
                      if (index != order.items.length - 1)
                        Divider(height: 1, color: colors.outlineVariant),
                    ],
                    if (_requestLevelResults.isNotEmpty) ...[
                      const SizedBox(height: 10),
                      Text(
                        l10n.labRequestLevelResults,
                        style: Theme.of(context).textTheme.labelSmall?.copyWith(
                              color: colors.onSurfaceVariant,
                              fontWeight: FontWeight.w800,
                            ),
                      ),
                      const SizedBox(height: 6),
                      _ResultGroups(
                        results: _requestLevelResults,
                        busyResults: busyResults,
                        onDownloadPdf: onDownloadPdf,
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

  List<PatientLabResultSummary> _resultsForItem(
    PatientLabOrderItemSummary item,
  ) {
    return results.where((result) {
      if (item.id.isNotEmpty && result.labOrderItemId?.trim() == item.id) {
        return true;
      }
      return result.labOrderItemId?.trim().isNotEmpty != true &&
          result.examName?.trim().isNotEmpty == true &&
          result.examName!.trim().toLowerCase() == item.examName.toLowerCase();
    }).toList(growable: false);
  }

  List<PatientLabResultSummary> get _requestLevelResults {
    return results
        .where(
          (result) =>
              result.labOrderItemId?.trim().isNotEmpty != true &&
              result.examName?.trim().isNotEmpty != true,
        )
        .toList(growable: false);
  }
}

class _ExamPreview extends StatelessWidget {
  const _ExamPreview({required this.item});

  final PatientLabOrderItemSummary item;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Row(
      children: [
        Icon(Icons.science_outlined, size: 15, color: colors.onSurfaceVariant),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            item.examName,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                  fontWeight: FontWeight.w700,
                ),
          ),
        ),
        const SizedBox(width: 8),
        _StatusPill(status: item.status, compact: true),
      ],
    );
  }
}

class _ExamUnitRow extends StatelessWidget {
  const _ExamUnitRow({
    required this.order,
    required this.item,
    required this.results,
    required this.canWrite,
    required this.busy,
    required this.busyResults,
    required this.onAdvance,
    required this.onCancel,
    required this.onDownloadPdf,
  });

  final PatientLabOrderSummary order;
  final PatientLabOrderItemSummary item;
  final List<PatientLabResultSummary> results;
  final bool canWrite;
  final bool busy;
  final Set<String> busyResults;
  final void Function(PatientLabOrderItemSummary item, String status) onAdvance;
  final ValueChanged<PatientLabOrderItemSummary> onCancel;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final next = item.nextOperationalStatus;
    final canCancel = canWrite &&
        !item.isTerminal &&
        item.normalizedStatus != 'RESULT_AVAILABLE';

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 9),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(
                  item.examName,
                  style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                ),
              ),
              const SizedBox(width: 8),
              _StatusPill(status: item.status),
            ],
          ),
          if (results.isNotEmpty) ...[
            const SizedBox(height: 8),
            _ResultGroups(
              results: results,
              busyResults: busyResults,
              onDownloadPdf: onDownloadPdf,
            ),
          ] else if (item.normalizedStatus == 'RESULT_AVAILABLE') ...[
            const SizedBox(height: 7),
            Text(
              l10n.labNoStructuredResults,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
            ),
          ],
          if (canWrite && (next != null || canCancel)) ...[
            const SizedBox(height: 7),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                if (canCancel)
                  TextButton(
                    onPressed: busy ? null : () => onCancel(item),
                    child: Text(l10n.labCancelExam),
                  ),
                if (next != null)
                  TextButton.icon(
                    onPressed: busy ? null : () => onAdvance(item, next),
                    icon: busy
                        ? const SizedBox.square(
                            dimension: 14,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.arrow_forward_rounded, size: 16),
                    label: Text(
                      next == 'SAMPLE_COLLECTED'
                          ? l10n.labMarkSampleCollected
                          : l10n.labStartProcessing,
                    ),
                  ),
              ],
            ),
          ],
        ],
      ),
    );
  }
}

class _ResultGroups extends StatelessWidget {
  const _ResultGroups({
    required this.results,
    required this.busyResults,
    required this.onDownloadPdf,
  });

  final List<PatientLabResultSummary> results;
  final Set<String> busyResults;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final groups = <String, List<PatientLabResultSummary>>{};
    for (final result in results) {
      final key = result.resultNumber.trim().isEmpty
          ? result.id
          : result.resultNumber.trim();
      groups.putIfAbsent(key, () => []).add(result);
    }
    return Column(
      children: [
        for (final entry in groups.entries)
          _ResultGroup(
            number: entry.key,
            results: entry.value,
            busyResults: busyResults,
            onDownloadPdf: onDownloadPdf,
          ),
      ],
    );
  }
}

class _ResultGroup extends StatelessWidget {
  const _ResultGroup({
    required this.number,
    required this.results,
    required this.busyResults,
    required this.onDownloadPdf,
  });

  final String number;
  final List<PatientLabResultSummary> results;
  final Set<String> busyResults;
  final ValueChanged<PatientLabResultSummary> onDownloadPdf;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final first = results.first;
    final downloadable = results.where((result) => result.hasPdf).firstOrNull;
    return Container(
      margin: const EdgeInsets.only(bottom: 7),
      padding: const EdgeInsets.all(9),
      decoration: BoxDecoration(
        color: colors.surfaceContainerLowest,
        border: Border.all(color: colors.outlineVariant),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  number,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        fontWeight: FontWeight.w900,
                      ),
                ),
              ),
              _StatusPill(status: first.status, compact: true),
            ],
          ),
          const SizedBox(height: 6),
          for (final result in results)
            Padding(
              padding: const EdgeInsets.only(bottom: 4),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      result.analyte,
                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Text(
                    [result.value, if (result.unit?.trim().isNotEmpty == true) result.unit!.trim()].join(' '),
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          fontWeight: FontWeight.w900,
                        ),
                  ),
                ],
              ),
            ),
          if (first.conclusion?.trim().isNotEmpty == true) ...[
            const SizedBox(height: 3),
            Text(
              first.conclusion!.trim(),
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
            ),
          ],
          if (downloadable != null) ...[
            const SizedBox(height: 3),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: busyResults.contains(downloadable.id)
                    ? null
                    : () => onDownloadPdf(downloadable),
                icon: const Icon(Icons.picture_as_pdf_outlined, size: 16),
                label: Text(l10n.labDownloadPdf),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _StatusPill extends StatelessWidget {
  const _StatusPill({required this.status, this.compact = false});

  final String status;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final colors = Theme.of(context).colorScheme;
    final normalized = status.trim().toUpperCase();
    final foreground = switch (normalized) {
      'VALIDATED' => colors.primary,
      'CANCELLED' => colors.error,
      'RESULT_AVAILABLE' => colors.tertiary,
      _ => colors.onSurfaceVariant,
    };
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: compact ? 6 : 7,
        vertical: compact ? 2 : 3,
      ),
      decoration: BoxDecoration(
        color: foreground.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
      ),
      child: Text(
        l10n.labStatusLabel(status),
        maxLines: 1,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
              color: foreground,
              fontWeight: FontWeight.w900,
            ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Text(
      message,
      textAlign: TextAlign.center,
      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: colors.onSurfaceVariant,
          ),
    );
  }
}

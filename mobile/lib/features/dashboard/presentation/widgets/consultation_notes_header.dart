import 'package:flutter/material.dart';

import '../../../../core/theme/app_design_tokens.dart';
import '../../../../l10n/app_localizations.dart';
import '../../domain/active_visit.dart';
import '../dashboard_localizations.dart';
import '../prescription_localizations.dart';

class ConsultationNotesHeader extends StatelessWidget {
  const ConsultationNotesHeader({
    required this.visit,
    required this.onLaunchAssistant,
    required this.onClose,
    this.onOpenPrescription,
    super.key,
  });

  final ActiveVisit visit;
  final VoidCallback onLaunchAssistant;
  final VoidCallback onClose;
  final VoidCallback? onOpenPrescription;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Column(
      key: const ValueKey('consultation-fixed-header'),
      children: [
        const _SheetDragHandle(),
        _ConsultationHeaderBar(
          visit: visit,
          onLaunchAssistant: onLaunchAssistant,
          onOpenPrescription: onOpenPrescription,
          onClose: onClose,
        ),
        Divider(height: 1, color: colors.outlineVariant.withValues(alpha: 0.3)),
      ],
    );
  }
}

class ConsultationReasonBanner extends StatelessWidget {
  const ConsultationReasonBanner({required this.reason, super.key});

  final String reason;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      key: const ValueKey('consultation-scrollable-reason'),
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: colors.primary.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        border: Border.all(color: colors.primary.withValues(alpha: 0.25)),
      ),
      child: Row(
        children: [
          Icon(Icons.info_outline_rounded, size: 14, color: colors.primary),
          const SizedBox(width: AppDesignTokens.spaceSm),
          Expanded(
            child: Text(
              reason,
              style: theme.textTheme.bodySmall?.copyWith(
                fontWeight: FontWeight.w600,
                color: colors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SheetDragHandle extends StatelessWidget {
  const _SheetDragHandle();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 10),
      child: Center(
        child: Container(
          width: 42,
          height: 4,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.outlineVariant,
            borderRadius: BorderRadius.circular(2),
          ),
        ),
      ),
    );
  }
}

class _ConsultationHeaderBar extends StatelessWidget {
  const _ConsultationHeaderBar({
    required this.visit,
    required this.onLaunchAssistant,
    required this.onClose,
    this.onOpenPrescription,
  });

  final ActiveVisit visit;
  final VoidCallback onLaunchAssistant;
  final VoidCallback onClose;
  final VoidCallback? onOpenPrescription;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context);

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 8, 10),
      child: Row(
        children: [
          Icon(Icons.edit_note_rounded, size: 22, color: colors.primary),
          const SizedBox(width: AppDesignTokens.spaceSm),
          Expanded(child: _ConsultationIdentity(visit: visit)),
          _MiniActionButton(
            icon: Icons.medication_outlined,
            color: colors.primary,
            tooltip: onOpenPrescription == null
                ? l10n.prescriptionConsultationRequired
                : l10n.prescriptionTitle,
            onTap: onOpenPrescription,
          ),
          const SizedBox(width: AppDesignTokens.spaceXs),
          _MiniActionButton(
            icon: Icons.mic_rounded,
            color: colors.primary,
            tooltip: l10n.voiceAssistantTitle,
            onTap: onLaunchAssistant,
          ),
          const SizedBox(width: AppDesignTokens.spaceXs),
          IconButton(
            tooltip: l10n.dashboardClose,
            onPressed: onClose,
            icon: const Icon(Icons.close_rounded),
          ),
        ],
      ),
    );
  }
}

class _ConsultationIdentity extends StatelessWidget {
  const _ConsultationIdentity({required this.visit});

  final ActiveVisit visit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          AppLocalizations.of(context).consultationNotesTitle,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          '${visit.patientName} · ${_formattedReference(visit)}',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: theme.textTheme.bodySmall?.copyWith(
            color: theme.colorScheme.onSurfaceVariant,
            height: 1.2,
          ),
        ),
      ],
    );
  }
}

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
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final enabled = onTap != null;
    final foreground = enabled
        ? color
        : Theme.of(context).colorScheme.onSurfaceVariant;
    return Tooltip(
      message: tooltip,
      child: Material(
        color: foreground.withValues(alpha: enabled ? 0.12 : 0.06),
        borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(AppDesignTokens.radiusSm),
          child: SizedBox.square(
            dimension: 38,
            child: Center(
              child: Icon(icon, size: 20, color: foreground),
            ),
          ),
        ),
      ),
    );
  }
}

String _formattedReference(ActiveVisit visit) {
  var dpu = visit.patientDpu.trim().replaceAll(
    RegExp(r'^(DPU[\s\-]*)+', caseSensitive: false),
    'DPU-',
  );
  if (!dpu.toUpperCase().startsWith('DPU-')) dpu = 'DPU-$dpu';
  return '${visit.visitNumber} · ${dpu.replaceAll('-', '\u2011')}';
}

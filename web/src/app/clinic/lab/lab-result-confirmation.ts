import { I18nService } from '../../core/i18n/i18n.service';
import { interpolate } from '../../core/i18n/interpolate';
import { ConfirmDialogOptions } from '../../shared/ui/dialog.service';

export interface LabResultConfirmationLine {
  readonly analyteName: string;
  readonly value: string;
  readonly unit: string;
  readonly interpretation: string;
}

export interface LabResultConfirmationSummary {
  readonly examRequestNumber: string;
  readonly patientName: string;
  readonly examName: string;
  readonly statusLabel: string;
  readonly validatorName: string;
  readonly lines: readonly LabResultConfirmationLine[];
}

/** Récapitulatif des résultats (analyte, valeur, interprétation) avant validation et transmission au dossier. */
export function labResultConfirmation(i18n: I18nService, summary: LabResultConfirmationSummary): ConfirmDialogOptions {
  const lines = summary.lines
    .map((line) => interpolate(i18n.t('lab.validateConfirm.line'), {
      analyte: line.analyteName.trim(),
      value: line.value.trim(),
      unit: line.unit.trim(),
      interpretation: i18n.t(`lab.interpretation.${line.interpretation}`),
    }).trim())
    .join('\n');

  return {
    title: i18n.t('lab.validateConfirm.title'),
    message: interpolate(i18n.t('lab.validateConfirm.message'), {
      number: summary.examRequestNumber,
      patient: summary.patientName,
      exam: summary.examName || '—',
      status: summary.statusLabel,
      validator: summary.validatorName.trim(),
      lines,
    }),
    confirmLabel: i18n.t('lab.validateConfirm.action'),
    cancelLabel: i18n.t('common.cancel'),
    tone: 'warning',
    icon: 'clipboard-document-list',
  };
}

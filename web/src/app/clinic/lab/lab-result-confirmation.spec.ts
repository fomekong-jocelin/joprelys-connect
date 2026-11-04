import { I18nService } from '../../core/i18n/i18n.service';
import { labResultConfirmation } from './lab-result-confirmation';

describe('labResultConfirmation', () => {
  const dictionary: Record<string, string> = {
    'lab.validateConfirm.message': '{number} · {patient} · {exam} · {status} · {validator}\n{lines}',
    'lab.validateConfirm.line': '• {analyte} : {value} {unit} ({interpretation})',
    'lab.interpretation.HIGH': 'Élevé',
  };
  const i18n = { t: (key: string) => dictionary[key] ?? key } as unknown as I18nService;

  it('récapitule le patient, le valideur et chaque résultat avec son interprétation', () => {
    const options = labResultConfirmation(i18n, {
      examRequestNumber: 'EXAM-1',
      patientName: 'Patient Démo',
      examName: '',
      statusLabel: 'Validée',
      validatorName: ' Dr Bio ',
      lines: [{ analyteName: 'Glycémie', value: '1.45', unit: 'g/L', interpretation: 'HIGH' }],
    });

    expect(options.message).toBe('EXAM-1 · Patient Démo · — · Validée · Dr Bio\n• Glycémie : 1.45 g/L (Élevé)');
    expect(options.tone).toBe('warning');
    expect(options.confirmLabel).toBe('lab.validateConfirm.action');
  });
});

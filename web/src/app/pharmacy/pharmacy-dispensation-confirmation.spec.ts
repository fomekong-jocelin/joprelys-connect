import { I18nService } from '../core/i18n/i18n.service';
import { dispensationConfirmation } from './pharmacy-dispensation-confirmation';

describe('dispensationConfirmation', () => {
  const dictionary: Record<string, string> = {
    'pharmacy.dispenseConfirm.message': '{number} — {patient} ({pharmacy})\n{lines}',
    'pharmacy.dispenseConfirm.line': '• {drug} : {quantity}',
    'pharmacy.dispenseConfirm.substitution': '(substitué par {substitute})',
  };
  const i18n = { t: (key: string) => dictionary[key] ?? key } as unknown as I18nService;

  it('récapitule chaque ligne délivrée et les substitutions', () => {
    const options = dispensationConfirmation(
      i18n,
      { prescriptionNumber: 'ORD-1', patientName: 'Patient Démo' },
      'Pharmacie du Centre',
      [
        { drugName: 'Amoxicilline 500 mg', quantity: 2 },
        { drugName: 'Paracétamol 1 g', quantity: 1, substitutedWith: 'Doliprane 1 g' },
      ],
    );

    expect(options.message).toBe(
      'ORD-1 — Patient Démo (Pharmacie du Centre)\n• Amoxicilline 500 mg : 2\n• Paracétamol 1 g : 1 (substitué par Doliprane 1 g)',
    );
    expect(options.confirmLabel).toBe('pharmacy.dispenseConfirm.action');
  });
});

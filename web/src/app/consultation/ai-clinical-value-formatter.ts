import { AiField } from './ai-consultation-api.service';

export type ClinicalValueLocale = 'fr' | 'en';

export function formatClinicalValue(
  field: AiField,
  value: string,
  locale: ClinicalValueLocale,
  vitalFormatter?: (key: string, value: unknown) => string,
): string {
  const raw = value?.trim();
  if (!raw || !['prescription', 'labOrders', 'vitals'].includes(field)) return raw ?? '';

  try {
    const parsed: unknown = JSON.parse(raw);
    if (field === 'prescription') return formatPrescription(parsed, locale) ?? raw;
    if (field === 'labOrders') return formatLabOrders(parsed) ?? raw;
    if (field === 'vitals') return formatVitals(parsed, vitalFormatter) ?? raw;
  } catch {
    return raw;
  }
  return raw;
}

function formatPrescription(value: unknown, locale: ClinicalValueLocale): string | null {
  const items = Array.isArray(value) ? value : [value];
  const blocks: string[] = [];

  for (const item of items) {
    if (!isRecord(item)) {
      if (typeof item === 'string' && item.trim()) blocks.push(item.trim());
      continue;
    }

    const name = text(item['drugName']) ?? (locale === 'fr' ? 'Médicament' : 'Medication');
    const details = [
      labelled(locale === 'fr' ? 'Dosage' : 'Dosage', item['dosage']),
      labelled(locale === 'fr' ? 'Forme' : 'Form', item['form']),
      labelled(locale === 'fr' ? 'Posologie' : 'Directions', item['posology']),
      labelled(locale === 'fr' ? 'Fréquence' : 'Frequency', item['frequency']),
      labelled(locale === 'fr' ? 'Voie' : 'Route', item['route']),
      labelled(locale === 'fr' ? 'Durée' : 'Duration', item['duration']),
      labelled(locale === 'fr' ? 'Quantité' : 'Quantity', item['quantity']),
      labelled('Instructions', item['instructions']),
    ].filter((detail): detail is string => !!detail);

    const substitution = item['substitutionAllowed'];
    if (typeof substitution === 'boolean') {
      details.push(
        `${locale === 'fr' ? 'Substitution' : 'Substitution'} : ${
          substitution
            ? (locale === 'fr' ? 'autorisée' : 'allowed')
            : (locale === 'fr' ? 'non autorisée' : 'not allowed')
        }`,
      );
    }

    blocks.push([name, ...details.map(detail => `• ${detail}`)].join('\n'));
  }

  return blocks.length > 0 ? blocks.join('\n\n') : null;
}

function formatLabOrders(value: unknown): string | null {
  if (!Array.isArray(value)) return null;
  const items = value
    .map(text)
    .filter((item): item is string => !!item)
    .map(item => `• ${item}`);
  return items.length > 0 ? items.join('\n') : null;
}

function formatVitals(
  value: unknown,
  vitalFormatter?: (key: string, value: unknown) => string,
): string | null {
  if (!isRecord(value)) return null;
  const items = Object.entries(value)
    .filter(([, item]) => item !== null && item !== undefined)
    .map(([key, item]) => vitalFormatter?.(key, item) ?? `${key}: ${String(item)}`);
  return items.length > 0 ? items.join(' · ') : null;
}

function labelled(label: string, value: unknown): string | null {
  const item = text(value);
  return item ? `${label} : ${item}` : null;
}

function text(value: unknown): string | null {
  if (typeof value !== 'string') return null;
  const trimmed = value.trim();
  return trimmed || null;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}

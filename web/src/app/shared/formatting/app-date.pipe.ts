import { formatDate } from '@angular/common';
import { LOCALE_ID, Pipe, PipeTransform, inject } from '@angular/core';
import { APP_DATE_FORMAT, APP_DATE_TIME_FORMAT, EMPTY_DISPLAY_VALUE } from './app-formats';

export type AppDateKind = 'date' | 'dateTime';

const FORMAT_BY_KIND: Readonly<Record<AppDateKind, string>> = {
  date: APP_DATE_FORMAT,
  dateTime: APP_DATE_TIME_FORMAT,
};

/**
 * `{{ valeur | appDate }}` → 04/11/2026 ; `{{ valeur | appDate:'dateTime' }}` → 04/11/2026 14:30.
 * Une valeur absente n'affiche rien ; une date invalide affiche « — » au lieu de faire échouer l'écran.
 */
@Pipe({ name: 'appDate' })
export class AppDatePipe implements PipeTransform {
  private readonly locale = inject(LOCALE_ID);

  transform(value: string | number | Date | null | undefined, kind: AppDateKind = 'date'): string {
    if (value === null || value === undefined || value === '') return '';
    const date = value instanceof Date ? value : new Date(value);
    if (Number.isNaN(date.getTime())) return EMPTY_DISPLAY_VALUE;
    return formatDate(date, FORMAT_BY_KIND[kind], this.locale);
  }
}

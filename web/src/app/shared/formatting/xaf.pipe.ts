import { Pipe, PipeTransform } from '@angular/core';
import { formatXaf } from './app-formats';

/** `{{ montant | xaf }}` → « 12 500 FCFA » (espaces insécables), « — » si la valeur est absente. */
@Pipe({ name: 'xaf' })
export class XafPipe implements PipeTransform {
  transform(value: number | string | null | undefined): string {
    return formatXaf(value);
  }
}

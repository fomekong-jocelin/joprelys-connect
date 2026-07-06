import { Injectable, Provider, signal } from '@angular/core';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { I18nService } from '../app/core/i18n/i18n.service';

// This helper is intended for unit tests running under Node/Vitest only.
// It loads the French dictionary synchronously from the assets folder.
function loadFrenchDictionary(): Record<string, string> {
  const filePath = path.join(process.cwd(), 'src/assets/i18n/fr.json');
  return JSON.parse(fs.readFileSync(filePath, 'utf8')) as Record<string, string>;
}

@Injectable()
export class I18nTestingService extends I18nService {
  private readonly testDictionary = loadFrenchDictionary();
  override readonly locale = signal<'fr' | 'en'>('fr');

  override t(key: string): string {
    return this.testDictionary[key] ?? key;
  }

  override async setLocale(): Promise<void> {
    // no-op in tests
  }

  override toggle(): void {
    // no-op in tests
  }
}

export function provideI18nTesting(): Provider {
  return { provide: I18nService, useClass: I18nTestingService };
}

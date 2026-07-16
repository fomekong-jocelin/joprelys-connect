import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';

const sourceRoot = new URL('../src/', import.meta.url);
const i18nRoot = fileURLToPath(new URL('assets/i18n/', sourceRoot));

function collectLocaleFiles(directory, locale, result = []) {
  for (const name of readdirSync(directory)) {
    const path = join(directory, name);
    if (statSync(path).isDirectory()) collectLocaleFiles(path, locale, result);
    else if (name === `${locale}.json`) result.push(path);
  }
  return result;
}

function loadDictionary(locale) {
  return Object.assign({}, ...collectLocaleFiles(i18nRoot, locale).map((path) => JSON.parse(readFileSync(path, 'utf8'))));
}

const shell = readFileSync(new URL('app/shared/layout/app-shell.component.ts', sourceRoot), 'utf8');
const nav = readFileSync(new URL('app/shared/layout/app-shell-nav.component.ts', sourceRoot), 'utf8');
const keys = new Set([
  ...Array.from(shell.matchAll(/i18n\.t\(\s*['"]([^'"]+)['"]/g), (match) => match[1]),
  ...Array.from(nav.matchAll(/this\.item\([^\n]*?['"](menu\.[^'"]+)['"]/g), (match) => match[1]),
]);

let failed = false;
for (const locale of ['fr', 'en']) {
  const dictionary = loadDictionary(locale);
  const missing = [...keys].filter((key) => !key.endsWith('.') && !(key in dictionary));
  if (missing.length) {
    failed = true;
    console.error(`${locale}: clés du shell manquantes: ${missing.join(', ')}`);
  }
}

if (failed) process.exitCode = 1;
else console.log(`i18n shell: ${keys.size} clés présentes en français et en anglais.`);

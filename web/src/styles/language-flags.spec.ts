import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const stylesheet = readFileSync(
  resolve(process.cwd(), 'src/styles/language-flags.css'),
  'utf8',
);

describe('language flag selector scope', () => {
  it('does not target every select-none container inside the application shell', () => {
    expect(stylesheet).not.toContain('app-shell .select-none > button');
  });

  it('keeps flags scoped to the desktop and mobile language controls', () => {
    expect(stylesheet).toContain('app-shell .app-topbar .select-none > button');
    expect(stylesheet).toContain(
      'app-shell .app-page > .flex.flex-1 > .fixed.inset-0 .select-none > button',
    );
  });
});

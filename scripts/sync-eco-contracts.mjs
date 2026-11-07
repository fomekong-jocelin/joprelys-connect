#!/usr/bin/env node
// Copie les schémas eco-contracts v1 dans les ressources de test du backend (ECO-20261107, T2.4).
// Usage : node scripts/sync-eco-contracts.mjs [chemin du dépôt eco-contracts]   (défaut : ../eco-contracts)
// La copie est vérifiée par EcoContractChecksumTest contre SHA256SUMS : ne jamais la modifier à la main.
import { cpSync, existsSync, mkdirSync, readFileSync, rmSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(process.argv[2] ?? join(repoRoot, '..', 'eco-contracts'));
const target = join(repoRoot, 'backend', 'src', 'test', 'resources', 'eco-contracts', 'v1');

if (!existsSync(join(source, 'v1', 'SHA256SUMS'))) {
  console.error(`eco-contracts introuvable : ${source}`);
  process.exit(1);
}
rmSync(target, { recursive: true, force: true });
mkdirSync(target, { recursive: true });
cpSync(join(source, 'v1', 'schemas'), join(target, 'schemas'), { recursive: true });
cpSync(join(source, 'v1', 'SHA256SUMS'), join(target, 'SHA256SUMS'));
const count = readFileSync(join(target, 'SHA256SUMS'), 'utf8').trim().split('\n').length;
console.log(`${count} schémas copiés vers ${target}`);

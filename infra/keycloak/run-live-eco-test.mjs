// Test d'intégration réel API éco ↔ Joprelys ID (dev uniquement, comptes fictifs).
// Prérequis : Keycloak démarré sur localhost:8180 avec le realm joprelys-id, et dans l'environnement
// ALLOPHARMA_SERVER_CLIENT_SECRET et JOPRELYS_ID_TEST_USER_PASSWORD.
// Obtient un jeton patient (authorization code + PKCE, consentement accepté) et un jeton serveur
// (client credentials), puis lance EcoKeycloakLiveIT. Les jetons ne sont jamais affichés.
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const realm = 'http://localhost:8180/realms/joprelys-id';
const oidc = `${realm}/protocol/openid-connect`;
const redirect = 'cm.allopharma.app:/oauth2redirect';
const form = (o) => new URLSearchParams(o);

async function serverToken() {
  const r = await fetch(`${oidc}/token`, { method: 'POST', body: form({ grant_type: 'client_credentials',
    client_id: 'allopharma-server', client_secret: process.env.ALLOPHARMA_SERVER_CLIENT_SECRET }) });
  if (r.status !== 200) throw new Error(`client_credentials refusé (${r.status})`);
  return (await r.json()).access_token;
}

async function patientToken(username) {
  const cookies = new Map();
  const keep = (r) => { for (const c of r.headers.getSetCookie()) { const [kv] = c.split(';'); const i = kv.indexOf('='); cookies.set(kv.slice(0, i), kv.slice(i + 1)); } };
  const jar = () => [...cookies].map(([k, v]) => `${k}=${v}`).join('; ');
  const abs = (u) => new URL(u, 'http://localhost:8180').toString();
  const action = (html) => abs(html.match(/action="([^"]+)"/)[1].replace(/&amp;/g, '&'));
  const postForm = (url, body) => fetch(url, { method: 'POST', redirect: 'manual',
    headers: { cookie: jar(), 'content-type': 'application/x-www-form-urlencoded' }, body: form(body) });

  const verifier = crypto.randomBytes(32).toString('base64url');
  const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');
  let r = await fetch(`${oidc}/auth?` + form({ client_id: 'allopharma-mobile', response_type: 'code', redirect_uri: redirect,
    scope: 'openid eco:patient-link', state: crypto.randomUUID(), code_challenge: challenge, code_challenge_method: 'S256' }),
  { redirect: 'manual' });
  keep(r);
  r = await postForm(action(await r.text()), { username, password: process.env.JOPRELYS_ID_TEST_USER_PASSWORD });
  keep(r);
  let location = r.headers.get('location') ?? '';
  if (!location.startsWith(redirect)) {
    // Écran de consentement (premier passage) : le patient fictif accepte.
    if (r.status === 302) { r = await fetch(abs(location), { redirect: 'manual', headers: { cookie: jar() } }); keep(r); }
    r = await postForm(action(await r.text()), { accept: 'Oui' });
    location = r.headers.get('location') ?? '';
  }
  const code = new URL(location.replace(`${redirect}`, 'http://app/cb')).searchParams.get('code');
  if (!code) throw new Error('aucun code d\'autorisation reçu');
  const t = await fetch(`${oidc}/token`, { method: 'POST', body: form({ grant_type: 'authorization_code',
    client_id: 'allopharma-mobile', code, redirect_uri: redirect, code_verifier: verifier }) });
  if (t.status !== 200) throw new Error(`échange du code refusé (${t.status})`);
  return (await t.json()).access_token;
}

const backend = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..', 'backend');
const env = { ...process.env,
  JOPRELYS_ID_LIVE_PATIENT_TOKEN: await patientToken('patient.demo1'),
  JOPRELYS_ID_LIVE_SERVER_TOKEN: await serverToken() };
console.log('Jetons obtenus auprès de Joprelys ID ; lancement de EcoKeycloakLiveIT…');
const mvnw = join(backend, process.platform === 'win32' ? 'mvnw.cmd' : 'mvnw');
const run = spawnSync(mvnw, ['-q', 'test', '-Dtest=EcoKeycloakLiveIT', '-Dsurefire.failIfNoSpecifiedTests=false'],
  { cwd: backend, env, stdio: 'inherit', shell: process.platform === 'win32' });
process.exit(run.status ?? 1);

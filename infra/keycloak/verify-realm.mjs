// Vérification du realm joprelys-id contre un Keycloak démarré (docker compose --profile eco up -d keycloak).
// Usage : node infra/keycloak/verify-realm.mjs avec ALLOPHARMA_SERVER_CLIENT_SECRET, JOPRELYS_ID_TEST_USER_PASSWORD
// et KC_BOOTSTRAP_ADMIN_PASSWORD (= KEYCLOAK_ADMIN_PASSWORD) dans l'environnement. N'affiche aucun secret.
// Réinitialise le consentement de patient.demo1 : dev uniquement.
import crypto from 'node:crypto';

const base = 'http://localhost:8180/realms/joprelys-id/protocol/openid-connect';
const redirect = 'cm.allopharma.app:/oauth2redirect';
const results = [];
const check = (name, ok, detail = '') => results.push(`${ok ? 'PASS' : 'FAIL'} ${name}${detail ? ' - ' + detail : ''}`);
const claims = (jwt) => JSON.parse(Buffer.from(jwt.split('.')[1], 'base64url').toString());
const form = (o) => new URLSearchParams(o);

// 1. client credentials
{
  const r = await fetch(`${base}/token`, { method: 'POST', body: form({ grant_type: 'client_credentials', client_id: 'allopharma-server', client_secret: process.env.ALLOPHARMA_SERVER_CLIENT_SECRET }) });
  const j = await r.json();
  const c = j.access_token ? claims(j.access_token) : {};
  const aud = [].concat(c.aud ?? []);
  check('client_credentials allopharma-server', r.status === 200, `status ${r.status}`);
  check('  aud contient joprelys-api', aud.includes('joprelys-api'), JSON.stringify(c.aud));
  check('  scope eco:consent.revoke', (c.scope ?? '').split(' ').includes('eco:consent.revoke'), c.scope);
  check('  sub présent', !!c.sub);
  check('  pas de claim dpu ni d\'identité', !('dpu' in c) && !('email' in c) && !('name' in c));
  check('  durée 300 s', c.exp - c.iat === 300, String(c.exp - c.iat));
  check('  pas de refresh token', !j.refresh_token);
}
// 1b. mauvais secret
{
  const r = await fetch(`${base}/token`, { method: 'POST', body: form({ grant_type: 'client_credentials', client_id: 'allopharma-server', client_secret: 'wrong' }) });
  check('client_credentials mauvais secret refusé', r.status === 401, `status ${r.status}`);
}
// 2. password grant mobile refusé
{
  const r = await fetch(`${base}/token`, { method: 'POST', body: form({ grant_type: 'password', client_id: 'allopharma-mobile', username: 'patient.demo1', password: process.env.JOPRELYS_ID_TEST_USER_PASSWORD }) });
  const j = await r.json();
  check('password grant mobile refusé', r.status >= 400 && j.error === 'unauthorized_client', `${r.status} ${j.error}`);
}
// 2b. client credentials mobile refusé
{
  const r = await fetch(`${base}/token`, { method: 'POST', body: form({ grant_type: 'client_credentials', client_id: 'allopharma-mobile' }) });
  check('client_credentials mobile refusé', r.status >= 400, `status ${r.status}`);
}
// 3. autorisation sans PKCE refusée
{
  const u = `${base}/auth?` + form({ client_id: 'allopharma-mobile', response_type: 'code', redirect_uri: redirect, scope: 'openid eco:patient-link', state: 's1' });
  const r = await fetch(u, { redirect: 'manual' });
  const loc = r.headers.get('location') ?? '';
  check('autorisation sans code_challenge refusée', loc.includes('error=invalid_request') && loc.includes('code_challenge'), decodeURIComponent(loc).slice(0, 120));
}
// 3b. redirect non déclarée refusée
{
  const u = `${base}/auth?` + form({ client_id: 'allopharma-mobile', response_type: 'code', redirect_uri: 'https://evil.example/cb', scope: 'openid', code_challenge: 'x'.repeat(43), code_challenge_method: 'S256' });
  const r = await fetch(u, { redirect: 'manual' });
  check('redirect_uri non déclarée refusée', r.status === 400, `status ${r.status}`);
}
// 4. parcours PKCE complet + consentement
{
  let cookies = new Map();
  const keep = (r) => { for (const sc of r.headers.getSetCookie()) { const [kv] = sc.split(';'); const i = kv.indexOf('='); cookies.set(kv.slice(0, i), kv.slice(i + 1)); } };
  const jar = () => [...cookies].map(([k, v]) => `${k}=${v}`).join('; ');
  const action = (html) => html.match(/action="([^"]+)"/)[1].replace(/&amp;/g, '&');
  const abs = (u) => new URL(u, 'http://localhost:8180').toString();
  // remise à zéro du consentement mémorisé (API d'administration)
  {
    const at = (await (await fetch('http://localhost:8180/realms/master/protocol/openid-connect/token', { method: 'POST', body: form({ grant_type: 'password', client_id: 'admin-cli', username: 'admin', password: process.env.KC_BOOTSTRAP_ADMIN_PASSWORD }) })).json()).access_token;
    const admin = 'http://localhost:8180/admin/realms/joprelys-id';
    const [u] = await (await fetch(`${admin}/users?username=patient.demo1&exact=true`, { headers: { authorization: `Bearer ${at}` } })).json();
    const d = await fetch(`${admin}/users/${u.id}/consents/allopharma-mobile`, { method: 'DELETE', headers: { authorization: `Bearer ${at}` } });
    check('attribut dpu visible en admin', u.attributes?.dpu?.[0] === 'DPU-TEST-000123', JSON.stringify(u.attributes));
    void d;
  }
  const verifier = crypto.randomBytes(32).toString('base64url');
  const challenge = crypto.createHash('sha256').update(verifier).digest('base64url');
  let r = await fetch(`${base}/auth?` + form({ client_id: 'allopharma-mobile', response_type: 'code', redirect_uri: redirect, scope: 'openid eco:patient-link', state: 'st', code_challenge: challenge, code_challenge_method: 'S256', ui_locales: 'fr' }), { redirect: 'manual' });
  keep(r);
  let html = await r.text();
  r = await fetch(abs(action(html)), { method: 'POST', redirect: 'manual', headers: { cookie: jar(), 'content-type': 'application/x-www-form-urlencoded' }, body: form({ username: 'patient.demo1', password: process.env.JOPRELYS_ID_TEST_USER_PASSWORD }) });
  keep(r);
  if (r.status === 302) { r = await fetch(abs(r.headers.get('location')), { redirect: 'manual', headers: { cookie: jar() } }); keep(r); }
  html = await r.text();
  const consentShown = html.includes('Relier votre profil AllôPharma') || html.includes('Relier votre profil All&ocirc;Pharma') || html.includes('Relier votre profil All');
  check('écran de consentement affiché (texte FR du scope)', r.status === 200 && consentShown, `status ${r.status}`);
  r = await fetch(abs(action(html)), { method: 'POST', redirect: 'manual', headers: { cookie: jar(), 'content-type': 'application/x-www-form-urlencoded' }, body: form({ accept: 'Oui' }) });
  const loc = r.headers.get('location') ?? '';
  const code = new URL(loc.replace('cm.allopharma.app:/', 'http://x/')).searchParams.get('code');
  check('consentement accepté → code renvoyé sur le schéma de l\'app', loc.startsWith(redirect) && !!code, loc.slice(0, 40));
  // échange sans verifier refusé
  let t = await fetch(`${base}/token`, { method: 'POST', body: form({ grant_type: 'authorization_code', client_id: 'allopharma-mobile', code, redirect_uri: redirect, code_verifier: crypto.randomBytes(32).toString('base64url') }) });
  check('échange avec mauvais code_verifier refusé', t.status === 400, `status ${t.status}`);
  // nouveau parcours complet (nouvelle session) : connexion, consentement si redemandé, code
  cookies = new Map();
  r = await fetch(`${base}/auth?` + form({ client_id: 'allopharma-mobile', response_type: 'code', redirect_uri: redirect, scope: 'openid eco:patient-link', state: 'st2', code_challenge: challenge, code_challenge_method: 'S256' }), { redirect: 'manual' });
  keep(r);
  html = await r.text();
  r = await fetch(abs(action(html)), { method: 'POST', redirect: 'manual', headers: { cookie: jar(), 'content-type': 'application/x-www-form-urlencoded' }, body: form({ username: 'patient.demo1', password: process.env.JOPRELYS_ID_TEST_USER_PASSWORD }) });
  keep(r);
  let loc2 = r.headers.get('location') ?? '';
  if (!loc2.startsWith(redirect)) {
    if (r.status === 302) { r = await fetch(abs(loc2), { redirect: 'manual', headers: { cookie: jar() } }); keep(r); }
    html = await r.text();
    r = await fetch(abs(action(html)), { method: 'POST', redirect: 'manual', headers: { cookie: jar(), 'content-type': 'application/x-www-form-urlencoded' }, body: form({ accept: 'Oui' }) });
    loc2 = r.headers.get('location') ?? '';
  }
  const code2 = new URL(loc2.replace('cm.allopharma.app:/', 'http://x/')).searchParams.get('code');
  t = await fetch(`${base}/token`, { method: 'POST', body: form({ grant_type: 'authorization_code', client_id: 'allopharma-mobile', code: code2, redirect_uri: redirect, code_verifier: verifier }) });
  const j = await t.json();
  const c = j.access_token ? claims(j.access_token) : {};
  check('échange PKCE valide', t.status === 200, `status ${t.status} ${j.error ?? ''}`);
  check('  claim dpu = DPU-TEST-000123', c.dpu === 'DPU-TEST-000123', c.dpu);
  check('  aud contient joprelys-api', [].concat(c.aud ?? []).includes('joprelys-api'), JSON.stringify(c.aud));
  check('  scope eco:patient-link', (c.scope ?? '').split(' ').includes('eco:patient-link'), c.scope);
  check('  azp = allopharma-mobile', c.azp === 'allopharma-mobile');
  check('  ni nom ni e-mail dans le jeton d\'accès', !('email' in c) && !('name' in c) && !('given_name' in c));
}
// 5. sans le scope eco:patient-link : pas de dpu
{
  // via client credentials déjà vérifié ; ici on vérifie la découverte
  const r = await fetch('http://localhost:8180/realms/joprelys-id/.well-known/openid-configuration');
  const j = await r.json();
  check('découverte OIDC : S256 supporté', (j.code_challenge_methods_supported ?? []).includes('S256'));
}
console.log(results.join('\n'));
const failed = results.filter((l) => l.startsWith('FAIL')).length;
console.log(`\n${results.length - failed}/${results.length} vérifications OK`);
process.exit(failed ? 1 : 0);

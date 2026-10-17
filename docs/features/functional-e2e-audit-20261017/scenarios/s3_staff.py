import json, sys, time, re
sys.path.insert(0, '.')
from harness import _req as R, login, check, LOG, latest_mail, text_of
S = json.load(open('state.json')); ts = S['ts']
A = login(S['admin'], S['pw'])
st, roles = R('GET', '/api/staff/assignment-roles', A); print('assignment roles', roles)
st, specs = R('GET', '/api/hospital-organization/catalogs/specialties', A); print('specialties', [s.get('code') for s in specs][:40] if isinstance(specs, list) else specs)
role_codes = [r['code'] for r in roles]
now = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime(time.time() - 3600))
def ua(unit, code): return [{"organizationalUnitId": unit, "assignmentRoleCode": code, "primary": True, "validFrom": now}]
U = S['units']
pick = lambda *c: next((x for x in c if x in role_codes), role_codes[0])
people = {
  'accueil': ('AGENT_ACCUEIL', 'Agent Accueil', None),
  'ide': ('INFIRMIER', 'Infirmière Audit', ua(U['cu'], pick('NURSE', 'INFIRMIER', 'CARE_STAFF'))),
  'drA': ('MEDECIN', 'Dr A Généraliste', ua(U['mg'], pick('PHYSICIAN', 'MEDECIN', 'DOCTOR'))),
  'drB': ('MEDECIN', 'Dr B Interniste', ua(U['mi'], pick('PHYSICIAN', 'MEDECIN', 'DOCTOR'))),
  'drC': ('MEDECIN', 'Dr C Sans affectation', None),
  'bio': ('BIOLOGISTE', 'Biologiste Audit', None),
  'pharma': ('PHARMACIEN', 'Pharmacien Audit', None),
  'caisse': ('CAISSIER', 'Caissier Audit', None),
  'lits': ('RESPONSABLE_HOSPITALISATION', 'Gestionnaire Lits', None),
  'compta': ('SECRETAIRE_COMPTABLE', 'Facturation Audit', None),
}
accounts = dict(S.get('acc', {}))
for key, (role, name, assign) in people.items():
    if key in accounts: continue
    email = f"{key.lower()}{ts}@example.test"
    body = {"email": email, "displayName": name, "role": role, "roles": [role]}
    if assign: body["unitAssignments"] = assign
    if role == "MEDECIN": body["registrationNumber"] = "ONMC-" + key + ts
    st, r = R('POST', '/api/staff', A, body)
    if st >= 300: print('invite err', key, st, r); continue
    m, _ = latest_mail(email); t = text_of(m)
    pw = next((c for c in re.findall(r"[A-Za-z0-9!@#$%^&*()_+\-=]{10,}", t) if re.search(r"\d", c) and re.search(r"[A-Za-z]", c) and '-' in c), None)
    accounts[key] = {"email": email, "pw": pw, "id": r.get('id'), "role": role}
check('ROLE-01', 'Invitation de 10 collaborateurs (rôles système)', len(accounts) == 10, list(accounts))
ok = 0
for k, a in accounts.items():
    try:
        tok = login(a['email'], a['pw']); st, me = R('GET', '/api/rbac/me', tok); a['perms'] = me.get('permissions'); ok += 1
    except Exception as e: print('login fail', k, e)
check('ROLE-02', 'Chaque collaborateur se connecte', ok == len(accounts), ok)
S['acc'] = accounts; S['assign_now'] = now
json.dump(S, open('state.json', 'w'))
json.dump(LOG, open('log_s3.json', 'w'), ensure_ascii=False, indent=1)

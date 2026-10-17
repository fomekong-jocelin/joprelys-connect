import json, sys, time
sys.path.insert(0, '.')
from harness import _req as R, login, check, LOG, latest_mail, text_of
import re
P = login('audit.platform@example.test', 'Audit!Test2026')
ts = str(int(time.time()))[-6:]
st, org = R('POST', '/api/organizations', P, {"name": f"Clinique Audit {ts}", "email": f"clinique{ts}@example.test", "city": "Douala", "country": "CM", "type": "CLINIQUE"})
check('CFG-01', 'Plateforme crée un établissement', st in (200, 201), (st, org if st >= 300 else org.get('id')))
org_id = org['id']
adm_email = f"admin{ts}@example.test"
st, a = R('POST', f'/api/organizations/{org_id}/admin', P, {"displayName": "Admin Audit", "email": adm_email})
check('CFG-02', 'Plateforme crée l administrateur établissement', st == 201, (st, a))
m, _ = latest_mail(adm_email)
t = text_of(m)
pw = None
for c in re.findall(r"[A-Za-z0-9!@#$%^&*()_+\-=\[\]{};:,.?]{10,}", t):
    if re.search(r"\d", c) and re.search(r"[A-Za-z]", c) and '@' not in c and 'http' not in c:
        pw = c; break
print('subject', m.get('subject'), 'pw-candidate', pw)
A = login(adm_email, pw)
check('CFG-03', 'Admin établissement se connecte (mot de passe temporaire + OTP)', bool(A))
# platform admin tries to configure tenant structure
st, x = R('GET', '/api/hospital-organization/units', P)
check('CFG-04', 'Plateforme et structure d un établissement : comportement', True, (st, str(x)[:150]))
st, cat = R('GET', '/api/hospital-organization/catalogs/services', A)
check('CFG-05', 'Catalogue national des services disponible', st == 200 and len(cat) > 0, (st, len(cat) if isinstance(cat, list) else cat))
codes = [c.get('code') for c in cat]
print('service codes', codes[:60])
json.dump({"org": org_id, "admin": adm_email, "pw": pw, "ts": ts, "codes": codes, "cat": cat}, open('state.json', 'w'))
json.dump(LOG, open('log_s1.json', 'w'), ensure_ascii=False, indent=1)

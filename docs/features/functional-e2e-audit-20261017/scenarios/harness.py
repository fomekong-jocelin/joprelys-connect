import json, re, time, urllib.request, urllib.error, http.cookiejar, sys
BASE = "http://localhost:8095"
MAIL = "http://localhost:1080/api/email"
LOG = []

def _req(method, path, token=None, body=None, raw=False, headers=None):
    url = path if path.startswith("http") else BASE + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    req.add_header("Accept-Language", "fr")
    if token: req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items(): req.add_header(k, v)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            content = r.read()
            status = r.status
    except urllib.error.HTTPError as e:
        content = e.read(); status = e.code
    if raw: return status, content
    try: payload = json.loads(content) if content else None
    except Exception: payload = content.decode(errors="replace")[:500]
    return status, payload

def mails():
    with urllib.request.urlopen(MAIL) as r: return json.loads(r.read())

def latest_mail(to, after=0):
    for _ in range(40):
        ms = [m for m in mails() if any(a.get("address","").lower()==to.lower() for a in m.get("to",[]))]
        ms = [m for m in ms if m.get("_ts",0) >= 0]
        if len(ms) > after: return ms[-1], len(ms)
        time.sleep(0.5)
    raise RuntimeError("no mail for " + to)

def count_mail(to):
    return len([m for m in mails() if any(a.get("address","").lower()==to.lower() for a in m.get("to",[]))])

def text_of(m):
    t = m.get("text") or ""
    if not t: t = re.sub(r"<[^>]+>", " ", m.get("html") or "")
    return t

def login(email, password):
    before = count_mail(email)
    s, p = _req("POST", "/api/auth/login", body={"email": email, "password": password})
    if s != 200: raise RuntimeError(f"login {email} {s} {p}")
    if p.get("accessToken"): return p["accessToken"]
    m, _ = latest_mail(email, before)
    code = re.findall(r"\b(\d{6})\b", text_of(m))[0]
    s, p = _req("POST", "/api/auth/verify-otp", body={"email": email, "otpCode": code})
    if s != 200: raise RuntimeError(f"otp {email} {s} {p}")
    return p["accessToken"]

def temp_password(email):
    m, _ = latest_mail(email)
    t = text_of(m)
    cands = re.findall(r"(?:mot de passe temporaire|temporary password|Mot de passe)\s*[:：]?\s*([^\s<]{8,})", t, re.I)
    return cands[0] if cands else t

def check(sid, label, cond, detail=""):
    LOG.append({"id": sid, "label": label, "ok": bool(cond), "detail": str(detail)[:400]})
    print(("OK  " if cond else "KO  ") + sid + " " + label + ((" | " + str(detail)[:300]) if detail else ""))

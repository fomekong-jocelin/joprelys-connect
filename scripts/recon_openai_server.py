#!/usr/bin/env python3
"""Reconnaissance lecture seule avant configuration des modèles OpenAI sur le serveur."""

import os
import sys

if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

import paramiko

HOST = os.environ.get("DIAG_HOST", "161.97.181.177")
PORT = int(os.environ.get("DIAG_PORT", "22"))
USER = os.environ.get("DIAG_USER", "jocelin")
PASSWD = os.environ.get("DIAG_PASS", "")

SCRIPT = r"""
set +e
echo "===SERVICE==="
systemctl cat joprelys-connect-api.service 2>/dev/null | grep -E 'EnvironmentFile|ExecStart|WorkingDirectory' || echo "(unit introuvable)"
echo "===ENV_FILES==="
find /opt /srv /home /root -maxdepth 4 -name '.env' -o -maxdepth 4 -name '*.env' 2>/dev/null | head -10
echo "===OPENAI_VARS==="
ENV_FILE=$(systemctl cat joprelys-connect-api.service 2>/dev/null | grep EnvironmentFile | sed 's/.*EnvironmentFile=//' | tr -d ' ' | head -1)
echo "env_file=$ENV_FILE"
if [ -n "$ENV_FILE" ] && [ -f "$ENV_FILE" ]; then
  grep -E '^(OPENAI_|AI_|SPEECH_|JOPRELYS_AI)' "$ENV_FILE" | sed -E 's/(API_KEY=).*/\1***MASKED***/'
fi
echo "===PORT==="
ss -tlnp 2>/dev/null | grep -E '8084|8080' || echo "(port non trouvé)"
echo "===MODELS_ACCESS==="
if [ -n "$ENV_FILE" ] && [ -f "$ENV_FILE" ]; then
  KEY=$(grep -E '^OPENAI_API_KEY=' "$ENV_FILE" | cut -d= -f2-)
  for M in gpt-4o-transcribe gpt-4o-mini-transcribe gpt-4.1 gpt-5.6-terra; do
    CODE=$(curl -s -o /dev/null -w '%{http_code}' -m 15 -H "Authorization: Bearer $KEY" "https://api.openai.com/v1/models/$M")
    echo "$M -> HTTP $CODE"
  done
fi
echo "===FIN==="
"""

def main():
    if not PASSWD:
        print("[ERREUR] DIAG_PASS manquant.", file=sys.stderr)
        sys.exit(1)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(hostname=HOST, port=PORT, username=USER, password=PASSWD,
                   look_for_keys=False, allow_agent=False, timeout=20,
                   banner_timeout=20, auth_timeout=20)
    print("[*] Connecté. Collecte...")
    stdin, stdout, stderr = client.exec_command("sudo -S -p '' bash -s", timeout=120)
    stdin.write(PASSWD + "\n")
    stdin.write(SCRIPT)
    stdin.flush()
    stdin.channel.shutdown_write()
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    client.close()
    if PASSWD:
        out = out.replace(PASSWD, "***REDACTED***")
        err = err.replace(PASSWD, "***REDACTED***")
    print(out)
    if err.strip():
        print("[stderr]", err)

if __name__ == "__main__":
    main()

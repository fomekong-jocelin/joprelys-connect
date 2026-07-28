#!/usr/bin/env python3
"""
Script d'audit et de mise à jour des variables d'environnement des modèles OpenAI
sur le serveur de recette (161.97.181.177).

Variables configurées :
- JOPRELYS_AI_ENABLED
- AI_PROVIDER
- SPEECH_PROVIDER
- OPENAI_MODEL
- OPENAI_TRANSCRIBE_MODEL
- OPENAI_AMBIENT_TRANSCRIBE_MODEL
- OPENAI_TTS_MODEL
- OPENAI_TTS_VOICE
- OPENAI_FINAL_REVIEW_ENABLED
- OPENAI_FINAL_REVIEW_MODEL
- OPENAI_REALTIME_MODEL
- OPENAI_REALTIME_FALLBACK_MODEL
- OPENAI_REALTIME_VOICE
- OPENAI_REALTIME_TRANSCRIBE_MODEL
"""

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
PASSWD = os.environ.get("DIAG_PASS", os.environ.get("JOPRELYS_SSH_PASSWORD", ""))
ENV_FILE = "/opt/joprelys-connect/api/.env"
SERVICE = "joprelys-connect-api.service"

NEW_VARS = {
    "JOPRELYS_AI_ENABLED": "false",
    "AI_PROVIDER": "openai",
    "SPEECH_PROVIDER": "openai",
    "OPENAI_MODEL": "gpt-4o-mini",
    "OPENAI_TRANSCRIBE_MODEL": "gpt-4o-mini-transcribe",
    "OPENAI_AMBIENT_TRANSCRIBE_MODEL": "gpt-4o-transcribe-diarize",
    "OPENAI_TTS_MODEL": "tts-1",
    "OPENAI_TTS_VOICE": "marin",
    "OPENAI_FINAL_REVIEW_ENABLED": "true",
    "OPENAI_FINAL_REVIEW_MODEL": "gpt-5.6-terra",
    "OPENAI_REALTIME_MODEL": "gpt-realtime-2.1-mini",
    "OPENAI_REALTIME_FALLBACK_MODEL": "gpt-realtime-2.1-mini",
    "OPENAI_REALTIME_VOICE": "marin",
    "OPENAI_REALTIME_TRANSCRIBE_MODEL": "gpt-4o-mini-transcribe",
}

SCRIPT_TEMPLATE = r"""
set -e
ENV_FILE="__ENV_FILE__"
SERVICE="__SERVICE__"
TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="/root/server-fix-backups/$TS"

echo "=== 1. SAUVEGARDE ==="
mkdir -p "$BACKUP_DIR"
cp -a "$ENV_FILE" "$BACKUP_DIR/.env"
echo "Backup créé dans $BACKUP_DIR/.env"

echo "=== 2. MISE A JOUR DU .ENV ==="
__SED_COMMANDS__

echo "=== 3. ETAT DU .ENV APRES MISE A JOUR ==="
grep -E '^(OPENAI_|AI_|SPEECH_|JOPRELYS_AI)' "$ENV_FILE" | sed -E 's/(API_KEY=).*/\1***MASKED***/'

echo "=== 4. REDEMARRAGE DU SERVICE ==="
systemctl restart "$SERVICE"
echo "Attente de l'activation du service et du port 8084..."
for i in $(seq 1 20); do
  if systemctl is-active --quiet "$SERVICE" && ss -tln | grep -q ':8084 '; then
    echo "Service actif et port 8084 en écoute (${i}x5s)"
    break
  fi
  sleep 5
done

systemctl status "$SERVICE" --no-pager | head -n 15
echo "=== FIN ==="
"""

def generate_sed_commands() -> str:
    lines = []
    for key, val in NEW_VARS.items():
        # Insert or update
        cmd = (
            f"if grep -q '^{key}=' \"$ENV_FILE\"; then\n"
            f"  sed -i -E 's|^{key}=.*|{key}={val}|' \"$ENV_FILE\"\n"
            f"else\n"
            f"  echo '{key}={val}' >> \"$ENV_FILE\"\n"
            f"fi"
        )
        lines.append(cmd)
    return "\n".join(lines)

def main():
    if not PASSWD:
        print("[ERREUR] Mot de passe SSH non renseigné. Définir DIAG_PASS ou JOPRELYS_SSH_PASSWORD.", file=sys.stderr)
        sys.exit(1)

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(
        hostname=HOST,
        port=PORT,
        username=USER,
        password=PASSWD,
        look_for_keys=False,
        allow_agent=False,
        timeout=20,
        banner_timeout=20,
        auth_timeout=20,
    )
    print(f"[*] Connecté à {HOST}. Mise à jour de {ENV_FILE}...")

    script_content = SCRIPT_TEMPLATE.replace("__ENV_FILE__", ENV_FILE).replace("__SERVICE__", SERVICE).replace("__SED_COMMANDS__", generate_sed_commands())

    stdin, stdout, stderr = client.exec_command("sudo -S -p '' bash -s", timeout=180)
    stdin.write(PASSWD + "\n")
    stdin.write(script_content)
    stdin.flush()
    stdin.channel.shutdown_write()

    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    rc = stdout.channel.recv_exit_status()
    client.close()

    if PASSWD:
        out = out.replace(PASSWD, "***REDACTED***")
        err = err.replace(PASSWD, "***REDACTED***")

    print(out)
    if err.strip():
        print("[stderr]", err)

    sys.exit(0 if rc == 0 else 1)

if __name__ == "__main__":
    main()

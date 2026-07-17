#!/usr/bin/env python3
"""
Configuration des modèles OpenAI sur le serveur de production joprelys.com.

Actions :
1. Sauvegarde horodatée du .env dans /root/server-fix-backups/<ts>/
2. OPENAI_TRANSCRIBE_MODEL=gpt-4o-transcribe (remplace whisper-1)
3. OPENAI_MODEL=gpt-4.1 (remplace gpt-4o-mini ; gpt-5.6-terra réservé à après
   redéploiement du jar avec la garde temperature)
4. Ajout de OPENAI_TRANSCRIBE_PROMPT si absent (inerte tant que le jar actuel
   ne l'envoie pas — actif après prochain redéploiement backend)
5. Redémarrage du service et vérification (statut + port 8084 + logs)

Secrets lus depuis l'environnement, masqués dans la sortie.
"""

import os
import sys
import time

if sys.platform == "win32":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

import paramiko

HOST = os.environ.get("DIAG_HOST", "161.97.181.177")
PORT = int(os.environ.get("DIAG_PORT", "22"))
USER = os.environ.get("DIAG_USER", "jocelin")
PASSWD = os.environ.get("DIAG_PASS", "")
ENV_FILE = "/opt/joprelys-connect/api/.env"
SERVICE = "joprelys-connect-api.service"

TRANSCRIBE_PROMPT = (
    "Ordonnance et consultation médicales dictées en français. "
    "Médicaments fréquents : amoxicilline, acide clavulanique, ceftriaxone, "
    "paracétamol, artéméther-luméfantrine, oméprazole, métoclopramide, salbutamol, "
    "ibuprofène, cotrimoxazole, métronidazole, amlodipine, metformine. "
    "Unités et formes : mg, g, ml, UI, comprimé, gélule, ampoule, sachet, sirop, "
    "cuillère-mesure, gouttes, patch. "
    "Posologies : matin, midi et soir, une fois par jour, deux fois par jour, "
    "trois fois par jour, pendant 5 jours, 7 jours, 10 jours, 14 jours."
)

SCRIPT = r"""
set -e
ENV_FILE="__ENV_FILE__"
SERVICE="__SERVICE__"
TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="/root/server-fix-backups/$TS"

echo "===1. SAUVEGARDE==="
mkdir -p "$BACKUP_DIR"
cp -a "$ENV_FILE" "$BACKUP_DIR/.env"
echo "backup=$BACKUP_DIR/.env"

echo "===2. MISE A JOUR==="
sed -i -E 's|^OPENAI_TRANSCRIBE_MODEL=.*|OPENAI_TRANSCRIBE_MODEL=gpt-4o-transcribe|' "$ENV_FILE"
sed -i -E 's|^OPENAI_MODEL=.*|OPENAI_MODEL=gpt-4.1|' "$ENV_FILE"
if ! grep -q '^OPENAI_TRANSCRIBE_PROMPT=' "$ENV_FILE"; then
  printf '%s\n' 'OPENAI_TRANSCRIBE_PROMPT=__PROMPT__' >> "$ENV_FILE"
  echo "prompt=ajouté"
else
  sed -i -E 's|^OPENAI_TRANSCRIBE_PROMPT=.*|OPENAI_TRANSCRIBE_PROMPT=__PROMPT__|' "$ENV_FILE"
  echo "prompt=remplacé"
fi

echo "===3. RESULTAT==="
grep -E '^(OPENAI_|AI_PROVIDER|SPEECH_PROVIDER|JOPRELYS_AI)' "$ENV_FILE" | sed -E 's/(API_KEY=).*/\1***MASKED***/'

echo "===4. REDEMARRAGE==="
systemctl restart "$SERVICE"
echo "restart demandé, attente du démarrage..."
for i in $(seq 1 24); do
  if systemctl is-active --quiet "$SERVICE" && ss -tln | grep -q ':8084 '; then
    echo "service actif et port 8084 en écoute après ${i}x5s"
    break
  fi
  sleep 5
done
systemctl is-active "$SERVICE"

echo "===5. LOGS==="
journalctl -u "$SERVICE" -n 15 --no-pager | grep -v -i 'api.key\|secret\|password' || true
echo "===FIN==="
"""


def build_script() -> str:
    return (SCRIPT
            .replace("__ENV_FILE__", ENV_FILE)
            .replace("__SERVICE__", SERVICE)
            .replace("__PROMPT__", TRANSCRIBE_PROMPT))


def main():
    if not PASSWD:
        print("[ERREUR] DIAG_PASS manquant.", file=sys.stderr)
        sys.exit(1)
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(hostname=HOST, port=PORT, username=USER, password=PASSWD,
                   look_for_keys=False, allow_agent=False, timeout=20,
                   banner_timeout=20, auth_timeout=20)
    print(f"[*] Connecté à {HOST}. Application de la configuration...")
    stdin, stdout, stderr = client.exec_command("sudo -S -p '' bash -s", timeout=300)
    stdin.write(PASSWD + "\n")
    stdin.write(build_script())
    stdin.flush()
    stdin.channel.shutdown_write()
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    rc = stdout.channel.recv_exit_status()
    client.close()
    for secret in (PASSWD,):
        if secret:
            out = out.replace(secret, "***REDACTED***")
            err = err.replace(secret, "***REDACTED***")
    print(out)
    if err.strip():
        print("[stderr]", err)
    print(f"[*] Code retour distant : {rc}")
    sys.exit(0 if rc == 0 else 1)


if __name__ == "__main__":
    main()

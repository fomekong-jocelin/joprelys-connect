#!/usr/bin/env python3
"""
Configuration des modèles OpenAI sur le serveur de production joprelys.com.

Actions :
1. Sauvegarde horodatée du .env dans /root/server-fix-backups/<ts>/
2. OPENAI_TRANSCRIBE_MODEL=gpt-4o-mini-transcribe
3. OPENAI_MODEL=gpt-4o-mini pour les tours normaux / extractions
4. OPENAI_REALTIME_MODEL=gpt-realtime-2.1-mini
5. OPENAI_REALTIME_FALLBACK_MODEL=gpt-realtime-2.1-mini (pas d'escalade coûteuse)
6. OPENAI_REALTIME_TRANSCRIBE_MODEL=gpt-4o-mini-transcribe
7. OPENAI_TTS_MODEL=tts-1
8. OPENAI_FINAL_REVIEW_ENABLED=true
9. OPENAI_FINAL_REVIEW_MODEL=gpt-5.6-terra
10. Ajout / mise à jour de OPENAI_TRANSCRIBE_PROMPT
11. Redémarrage du service et vérification (statut + port 8084 + logs)

Le modèle de diarisation ambiante n'est volontairement pas modifié : il reste
un mécanisme de sécurité / récupération distinct du flux normal optimisé.

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

set_env() {
  KEY="$1"
  VALUE="$2"
  if grep -q "^${KEY}=" "$ENV_FILE"; then
    sed -i -E "s|^${KEY}=.*|${KEY}=${VALUE}|" "$ENV_FILE"
  else
    printf '%s=%s\n' "$KEY" "$VALUE" >> "$ENV_FILE"
  fi
}

echo "===2. MISE A JOUR==="
set_env OPENAI_TRANSCRIBE_MODEL gpt-4o-mini-transcribe
set_env OPENAI_MODEL gpt-4o-mini
set_env OPENAI_REALTIME_MODEL gpt-realtime-2.1-mini
set_env OPENAI_REALTIME_FALLBACK_MODEL gpt-realtime-2.1-mini
set_env OPENAI_REALTIME_TRANSCRIBE_MODEL gpt-4o-mini-transcribe
set_env OPENAI_TTS_MODEL tts-1
set_env OPENAI_FINAL_REVIEW_ENABLED true
set_env OPENAI_FINAL_REVIEW_MODEL gpt-5.6-terra

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

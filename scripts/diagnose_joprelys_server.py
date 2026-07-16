#!/usr/bin/env python3
"""Collect a read-only Joprelys API startup diagnostic over SSH."""
from __future__ import annotations
import base64, datetime as dt, hashlib, os, re, shlex, socket, sys
from pathlib import Path
import paramiko

DEFAULT_HOST, DEFAULT_PORT = "161.97.181.177", 22
DEFAULT_SERVICE, COMMAND_TIMEOUT_SECONDS = "joprelys-connect-api.service", 30
KNOWN_HOSTS_FILE = Path.home() / ".ssh" / "known_hosts"
SECRET_VALUE_PATTERN = re.compile(r"(?im)^([A-Z0-9_]*(?:PASSWORD|SECRET|TOKEN|API_KEY|PRIVATE_KEY|CREDENTIAL)[A-Z0-9_]*\s*=).*$")

def env_required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value: raise ValueError(f"Variable d'environnement obligatoire absente: {name}")
    return value

def ssh_fingerprint(key: paramiko.PKey) -> str:
    return "SHA256:" + base64.b64encode(hashlib.sha256(key.asbytes()).digest()).decode().rstrip("=")

def scan_host_key(host: str, port: int) -> paramiko.PKey:
    transport = paramiko.Transport((host, port))
    try:
        transport.start_client(timeout=10)
        return transport.get_remote_server_key()
    finally: transport.close()

def ensure_host_key(host: str, port: int) -> str:
    key = scan_host_key(host, port); fingerprint = ssh_fingerprint(key)
    if os.getenv("JOPRELYS_SSH_HOST_FINGERPRINT", "").strip() != fingerprint:
        raise ValueError(f"Clé hôte non validée. Empreinte reçue: {fingerprint}. Comparez-la avec MobaXterm puis définissez JOPRELYS_SSH_HOST_FINGERPRINT.")
    KNOWN_HOSTS_FILE.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
    host_keys = paramiko.HostKeys()
    if KNOWN_HOSTS_FILE.exists(): host_keys.load(str(KNOWN_HOSTS_FILE))
    host_keys.add(host if port == 22 else f"[{host}]:{port}", key.get_name(), key)
    host_keys.save(str(KNOWN_HOSTS_FILE)); return fingerprint

def redact(text: str) -> str:
    return SECRET_VALUE_PATTERN.sub(lambda m: m.group(1) + "<masqué>", text)

def run_command(client: paramiko.SSHClient, title: str, command: str) -> str:
    stdin, stdout, stderr = client.exec_command(command, timeout=COMMAND_TIMEOUT_SECONDS); stdin.close()
    code = stdout.channel.recv_exit_status()
    return redact(f"\n===== {title} (exit={code}) =====\n" + stdout.read().decode(errors="replace") + stderr.read().decode(errors="replace"))

def environment_names_command(service: str) -> str:
    service = shlex.quote(service)
    script = f"systemctl show {service} -p Environment --value; systemctl show {service} -p EnvironmentFiles --value"
    return "sudo -n sh -c " + shlex.quote(script) + " | sed -E 's/=([^ ]*)/=<masqué>/g'"

def diagnostic_commands(service: str) -> list[tuple[str, str]]:
    service = shlex.quote(service)
    return [("État systemd", f"sudo -n systemctl status {service} --no-pager -l"),
            ("Unité systemd", f"sudo -n systemctl cat {service}"),
            ("Variables systemd (valeurs masquées)", environment_names_command(service)),
            ("Journal Spring Boot", f"sudo -n journalctl -u {service} -n 300 --no-pager -o short-iso"),
            ("Ports Java", "sudo -n ss -lntp | grep -E 'java|:8080|:8081' || true"),
            ("Santé locale", "curl -sS -i --max-time 10 http://127.0.0.1:8080/actuator/health || true"),
            ("Ressources", "df -h; free -h; uptime")]

def connect(host: str, port: int, username: str, password: str) -> paramiko.SSHClient:
    client = paramiko.SSHClient(); client.load_system_host_keys(); client.set_missing_host_key_policy(paramiko.RejectPolicy())
    client.connect(hostname=host, port=port, username=username, password=password, allow_agent=False,
                   look_for_keys=False, timeout=10, auth_timeout=10, banner_timeout=10)
    return client

def main() -> int:
    host = os.getenv("JOPRELYS_SSH_HOST", DEFAULT_HOST).strip(); port = int(os.getenv("JOPRELYS_SSH_PORT", str(DEFAULT_PORT)))
    username, password = env_required("JOPRELYS_SSH_USER"), env_required("JOPRELYS_SSH_PASSWORD")
    service = os.getenv("JOPRELYS_SYSTEMD_SERVICE", DEFAULT_SERVICE).strip(); fingerprint = ensure_host_key(host, port)
    report = Path(f"joprelys_server_diagnostic_{dt.datetime.now():%Y%m%d-%H%M%S}.txt")
    header = f"Joprelys server diagnostic\nHost: {host}\nPort: {port}\nUser: {username}\nService: {service}\nHost key: {fingerprint}\n"
    with connect(host, port, username, password) as client:
        sections = [run_command(client, title, command) for title, command in diagnostic_commands(service)]
    report.write_text(header + "".join(sections), encoding="utf-8"); print(f"Diagnostic terminé: {report.resolve()}"); return 0

if __name__ == "__main__":
    try: raise SystemExit(main())
    except (ValueError, OSError, socket.error, paramiko.SSHException) as exc:
        print(f"Erreur: {exc}", file=sys.stderr); raise SystemExit(1)

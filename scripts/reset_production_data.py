#!/usr/bin/env python3
"""Reset Joprelys production data after a mandatory PostgreSQL backup."""
from __future__ import annotations

import argparse
import base64
import os
import shlex
import stat
import time
import uuid
from pathlib import Path

import bcrypt
import paramiko

DEFAULT_HOST = "161.97.181.177"
DEFAULT_PORT = 22
DEFAULT_USER = "jocelin"
DEFAULT_SERVICE = "joprelys-connect-api.service"
DEFAULT_DATABASE = "joprelys"
ADMIN_EMAIL = "jocelin.fomekong@joprelys.com"
ADMIN_DISPLAY_NAME = "Jocelin Fomekong"
ADMIN_ROLE = "SUPER_ADMIN"
CONFIRMATION = "PURGE_PRODUCTION"


def required_env(name: str) -> str:
    value = os.getenv(name, "")
    if not value:
        raise ValueError(f"Variable obligatoire absente: {name}")
    return value


def sql_literal(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def build_reset_sql(admin_password: str) -> str:
    password_hash = bcrypt.hashpw(admin_password.encode(), bcrypt.gensalt(rounds=12)).decode()
    admin_id = str(uuid.uuid4())
    return f"""
BEGIN;
DO $reset$
DECLARE
    tables_to_truncate text;
BEGIN
    SELECT string_agg(format('%I.%I', schemaname, tablename), ', ' ORDER BY tablename)
      INTO tables_to_truncate
      FROM pg_tables
     WHERE schemaname = 'public'
       AND tablename <> 'flyway_schema_history';
    IF tables_to_truncate IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || tables_to_truncate || ' RESTART IDENTITY CASCADE';
    END IF;
END
$reset$;
INSERT INTO public.users (
    id, email, display_name, role, password_hash, enabled,
    organization_id, created_at, updated_at
) VALUES (
    {sql_literal(admin_id)}::uuid,
    {sql_literal(ADMIN_EMAIL)},
    {sql_literal(ADMIN_DISPLAY_NAME)},
    {sql_literal(ADMIN_ROLE)},
    {sql_literal(password_hash)},
    TRUE, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
);
COMMIT;
""".strip() + "\n"


class RemoteExecutor:
    def __init__(self, client: paramiko.SSHClient, sudo_password: str) -> None:
        self.client = client
        self.sudo_password = sudo_password

    def sudo(self, command: str, timeout: int = 120) -> str:
        stdin, stdout, stderr = self.client.exec_command(
            f"sudo -S -p '' {command}", timeout=timeout
        )
        stdin.write(self.sudo_password + "\n")
        stdin.flush()
        stdin.channel.shutdown_write()
        output = stdout.read().decode("utf-8", errors="replace")
        error = stderr.read().decode("utf-8", errors="replace")
        code = stdout.channel.recv_exit_status()
        if code != 0:
            raise RuntimeError(f"Commande distante en échec (code {code}): {error.strip()}")
        return output.strip()


def connect(host: str, port: int, user: str, password: str) -> paramiko.SSHClient:
    client = paramiko.SSHClient()
    client.load_system_host_keys()
    known_hosts = Path.home() / ".ssh" / "known_hosts"
    if known_hosts.exists():
        client.load_host_keys(str(known_hosts))
    client.set_missing_host_key_policy(paramiko.RejectPolicy())
    client.connect(
        hostname=host,
        port=port,
        username=user,
        password=password,
        look_for_keys=False,
        allow_agent=False,
        timeout=20,
        banner_timeout=20,
        auth_timeout=20,
    )
    return client


def upload_private_file(client: paramiko.SSHClient, path: str, content: str) -> None:
    sftp = client.open_sftp()
    try:
        with sftp.file(path, "w") as remote_file:
            remote_file.write(content)
        sftp.chmod(path, stat.S_IRUSR | stat.S_IWUSR)
    finally:
        sftp.close()


def wait_for_service(remote: RemoteExecutor, service: str) -> None:
    for _ in range(12):
        if remote.sudo(f"systemctl is-active {shlex.quote(service)} || true") == "active":
            return
        time.sleep(5)
    raise RuntimeError("Le service n'est pas redevenu actif après 60 secondes")


def wait_for_verification(remote: RemoteExecutor, database: str) -> str:
    query = (
        "SELECT (SELECT count(*) FROM users)||'|'||"
        "(SELECT count(*) FROM organizations)||'|'||"
        "(SELECT count(*) FROM patients)||'|'||"
        "(SELECT count(*) FROM roles)||'|'||"
        "(SELECT count(*) FROM permissions)||'|'||"
        "(SELECT count(*) FROM user_roles)||'|'||"
        "(SELECT email FROM users LIMIT 1)||'|'||"
        "(SELECT role FROM users LIMIT 1)"
    )
    expected = ["1", "0", "0", "15", "44", "1", ADMIN_EMAIL, ADMIN_ROLE]
    for _ in range(12):
        verification = remote.sudo(
            "-u postgres psql -X -At -d " + shlex.quote(database)
            + " -c " + shlex.quote(query)
        )
        if verification.split("|") == expected:
            return verification
        time.sleep(5)
    raise RuntimeError(f"Vérification post-purge inattendue: {verification}")


def execute_reset(args: argparse.Namespace) -> None:
    ssh_password = required_env("JOPRELYS_SSH_PASSWORD")
    admin_password = required_env("JOPRELYS_RESET_ADMIN_PASSWORD")
    client = connect(args.host, args.port, args.user, ssh_password)
    remote = RemoteExecutor(client, ssh_password)
    timestamp = time.strftime("%Y%m%d%H%M%S")
    backup = f"/opt/joprelys-connect/backups/joprelys_before_reset_{timestamp}.dump"
    remote_sql = f"/tmp/joprelys-reset-{os.getpid()}.sql"
    service_stopped = False
    try:
        upload_private_file(client, remote_sql, build_reset_sql(admin_password))
        remote.sudo("install -d -o postgres -g postgres -m 700 /opt/joprelys-connect/backups")
        remote.sudo(f"systemctl stop {shlex.quote(args.service)}")
        service_stopped = True
        remote.sudo(
            f"-u postgres pg_dump -Fc -d {shlex.quote(args.database)} -f {shlex.quote(backup)}",
            timeout=300,
        )
        backup_size = int(remote.sudo(f"stat -c %s {shlex.quote(backup)}"))
        if backup_size < 1024:
            raise RuntimeError("Sauvegarde trop petite; purge annulée")
        remote.sudo(f"chown postgres:postgres {shlex.quote(remote_sql)}")
        remote.sudo(
            f"-u postgres psql -X -v ON_ERROR_STOP=1 -d {shlex.quote(args.database)} -f {shlex.quote(remote_sql)}",
            timeout=300,
        )
        remote.sudo(f"rm -f {shlex.quote(remote_sql)}")
        remote.sudo(f"systemctl start {shlex.quote(args.service)}")
        service_stopped = False
        wait_for_service(remote, args.service)
        verification = wait_for_verification(remote, args.database)
        print(f"BACKUP_PATH={backup}")
        print(f"BACKUP_SIZE_BYTES={backup_size}")
        print(f"VERIFICATION={verification}")
        print("SERVICE_STATE=active")
    finally:
        try:
            remote.sudo(f"rm -f {shlex.quote(remote_sql)} || true")
            if service_stopped:
                remote.sudo(f"systemctl start {shlex.quote(args.service)}")
        finally:
            client.close()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true")
    parser.add_argument("--confirm", default="")
    parser.add_argument("--host", default=os.getenv("JOPRELYS_SSH_HOST", DEFAULT_HOST))
    parser.add_argument("--port", type=int, default=int(os.getenv("JOPRELYS_SSH_PORT", DEFAULT_PORT)))
    parser.add_argument("--user", default=os.getenv("JOPRELYS_SSH_USER", DEFAULT_USER))
    parser.add_argument("--service", default=DEFAULT_SERVICE)
    parser.add_argument("--database", default=DEFAULT_DATABASE)
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if not args.execute or args.confirm != CONFIRMATION:
        raise SystemExit(
            "Purge refusée. Utiliser --execute --confirm PURGE_PRODUCTION après validation."
        )
    execute_reset(args)


if __name__ == "__main__":
    main()

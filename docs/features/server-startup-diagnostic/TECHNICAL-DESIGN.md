# Diagnostic de démarrage du serveur — Conception technique

Le script Paramiko reçoit les identifiants par environnement, désactive agent et recherche de clés, et exige que l'empreinte SHA-256 corresponde à `JOPRELYS_SSH_HOST_FINGERPRINT`. Les commandes sont en lecture seule et utilisent `sudo -n`. Obligatoires : `JOPRELYS_SSH_USER`, `JOPRELYS_SSH_PASSWORD`, `JOPRELYS_SSH_HOST_FINGERPRINT`. Options : hôte, port et service. Impact SemVer : aucun bump applicatif.

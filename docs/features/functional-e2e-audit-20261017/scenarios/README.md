# Scénarios API de l'audit du 2026-10-17

Scripts Python sans dépendance externe, à exécuter **uniquement** contre une instance de test vide, avec des données fictives.

## Démarrer une instance isolée

1. Créer une base dédiée : `CREATE DATABASE joprelys_audit_e2e;`.
2. Lancer le backend sur un port libre, par exemple 8095, avec :
   - `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/joprelys_audit_e2e` ;
   - `JOPRELYS_SEED_ADMIN_ENABLED=true`, `JOPRELYS_ADMIN_EMAIL=audit.platform@example.test`, `JOPRELYS_ADMIN_NAME=Audit Plateforme`, `JOPRELYS_ADMIN_PASSWORD=Audit!Test2026` (identifiants de test uniquement) ;
   - `OPENAI_API_KEY`, `GEMINI_API_KEY` et `ANTHROPIC_API_KEY` vides ;
   - `--server.port=8095 --spring.mail.host=localhost --spring.mail.port=1025 --joprelys.rate-limiting.enabled=false`.
3. Démarrer MailDev (`scripts/start-local-mail-server.ps1`) : le harnais lit les OTP et les mots de passe temporaires dans `http://localhost:1080/api/email`.

## Exécution

```bash
python -I s1_setup.py && python -I s2_structure.py && python -I s3_staff.py \
  && python -I s4_outpatient.py && python -I s5_outpatient2.py \
  && python -I s6_inpatient.py && python -I s7_emergency.py && python -I s8_misc.py
```

Chaque script lit et complète `state.json`, et écrit `log_sN.json`. Le libellé de chaque vérification décrit le comportement **attendu** ; une ligne `KO` est un écart, ou un artefact documenté dans [RUNTIME-EVIDENCE.md](../RUNTIME-EVIDENCE.md).

Après correction des constats, ces scripts servent de base de non-régression : les libellés attendus restent valables.

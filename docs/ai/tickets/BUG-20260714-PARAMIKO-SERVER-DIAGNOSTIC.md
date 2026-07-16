# BUG-20260714 — Diagnostic Paramiko serveur de production

## Objectif
Fournir un diagnostic SSH non interactif et en lecture seule depuis le poste Windows qui atteint le serveur sur le port 22, afin d'identifier pourquoi le backend Spring Boot ne démarre pas.

## Informations de connexion
- Hôte effectif : `161.97.181.177` (l'IP `61.97.181.177` fournie initialement ne répondait pas sur le port 22)
- Port : `22`
- Utilisateur SSH : `jocelin`
- Accès privilégié : `sudo -S` avec le mot de passe SSH (équivalent à `sudo -i`)
- Image contextuelle : `C:\Users\Jocelin FOMEKONG\AppData\Local\Temp\pasted-image-1.png` (non accessible depuis l'environnement courant)

## Critères et actions
- [x] Lire `AGENTS.md`, `SKILL.md`, `README-IA.md`, `WORKFLOW-IA.md`, `PROJECT-TRACKING.md`, `CHANGELOG.md`, `review-checklist.md`.
- [x] Identifier le ticket existant.
- [x] Se connecter en SSH non interactif avec Paramiko.
- [x] Collecter les variables d'environnement, le statut du service backend, les logs récents, les processus Java, les ports ouverts, les ressources système.
- [x] Masquer les secrets dans les sorties collectées et dans les rapports.
- [x] Analyser la cause du non-démarrage du backend.
- [x] Vérifier les migrations V35/V37 locales et sur le serveur.
- [x] Exécuter la correction Option B (rebuild + redeploy + migration V68).
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`.

## Diagnostic

### État du service
- Service : `joprelys-connect-api.service`
- Statut : `activating (auto-restart)` avec **1 416 redémarrages**
- Dernier PID : `3857304` (exit code 1)
- Commande : `/usr/bin/java -Xms256m -Xmx768m -jar /opt/joprelys-connect/api/app.jar`
- Port Tomcat configuré : `8084`
- Base de données : PostgreSQL 16.14 sur `127.0.0.1:5432`, base `joprelys`, schema `joprelys,public`

### Cause racine
Le backend ne démarre pas à cause d'une **erreur de validation Flyway** :

```text
Migration checksum mismatch for migration version 35
-> Applied to database : 400119028
-> Resolved locally    : -1026216252

Migration checksum mismatch for migration version 37
-> Applied to database : 15779302
-> Resolved locally    : -269569901

Either revert the changes to the migration, or run repair to update the schema history.
```

### Analyse du mismatch
Les migrations concernées sont :
- `V35__lab_results_cdc_alignment.sql`
- `V37__medical_documents_cdc_alignment.sql`

Historique Git des fichiers locaux :
- Commit `b009959` du 2026-07-06 15:02 : version initiale avec contraintes `CHECK` actives.
- Commit `dbb9f7f` du 2026-07-09 14:13 : contraintes `CHECK` commentées (modification du fichier source).

La production a appliqué les migrations le 2026-07-06 17:15 (checksums `400119028` et `15779302`), donc avec les contraintes actives. Vérification en base :

```text
chk_lab_result_status       | lab_results       | CHECK (((status)::text = ANY ((ARRAY['DRAFT','VALIDATED','CANCELLED'])::text[])))
chk_medical_document_status | medical_documents | CHECK (((status)::text = ANY ((ARRAY['VALID','REVOQUE','ANNULE','REMPLACE'])::text[])))
```

Le jar déployé (`/opt/joprelys-connect/api/app.jar`) contient la version du commit `dbb9f7f` où les contraintes sont commentées, d'où le mismatch de checksum.

### Impact
- Le schéma de production est cohérent avec l'ancienne version des migrations (contraintes présentes).
- Seuls les checksums stockés dans `public.flyway_schema_history` sont désynchronisés par rapport au contenu actuel des fichiers.
- Aucune donnée n'est corrompue.

## Plan de correction

### Option A — Recommandée (rapide, production)
Exécuter `flyway repair` sur la base de production pour mettre à jour les checksums de V35 et V37 sans toucher au schéma.

Commande indicative (à exécuter après sauvegarde/snapshot) :

```bash
# En tant que root ou joprelys, avec l'outil flyway ou via le jar
sudo -u joprelys flyway -url=jdbc:postgresql://127.0.0.1:5432/joprelys \
  -user=joprelys -password=<DB_PASSWORD> \
  -locations=filesystem:/opt/joprelys-connect/api/migration \
  -schemas=joprelys,public repair
```

Ou, si Flyway est intégré à Spring Boot, démarrer temporairement l'application avec `spring.flyway.clean-disabled=true` et `spring.flyway.validate-on-migrate=false` puis `flyway.repair()` programmatique. La méthode la plus simple reste la CLI Flyway.

### Option B — Propre Flyway (nécessite redeploiement)
1. Revenir aux versions originales de V35 et V37 dans le repo (décommenter les contraintes).
2. Créer une nouvelle migration `V39__remove_status_check_constraints.sql` qui supprime les contraintes si elles ne sont plus désirées :
   ```sql
   ALTER TABLE lab_results DROP CONSTRAINT IF EXISTS chk_lab_result_status;
   ALTER TABLE medical_documents DROP CONSTRAINT IF EXISTS chk_medical_document_status;
   ```
3. Rebuilder et redéployer le jar.

### Option C — Manually update checksums (intermédiaire)
Mettre à jour manuellement les checksums dans `public.flyway_schema_history` :

```sql
UPDATE public.flyway_schema_history SET checksum = -1026216252 WHERE version = '35';
UPDATE public.flyway_schema_history SET checksum = -269569901  WHERE version = '37';
```

Cette option est déconseillée car elle court-circuite Flyway.

## Déploiement Option B exécuté

### Modifications apportées
1. **Restauration des migrations V35 et V37** : décommentaire des contraintes `CHECK` pour réaligner les checksums avec la base de production.
2. **Nouvelle migration V68** : suppression propre des contraintes `CHECK` après validation Flyway.
   - Fichier : `backend/src/main/resources/db/migration/V68__remove_status_check_constraints.sql`
3. **Tests Maven** : `./mvnw clean test` → 372 tests, 0 failure, 0 error, 1 skipped.
4. **Build** : `./mvnw clean package -DskipTests` → `backend/target/joprelys-backend-0.0.1-SNAPSHOT.jar`.

### Opérations sur le serveur
- **Backup base** : `/tmp/joprelys_backup_20260714_203357.dump`
- **Backup jar** : `/opt/joprelys-connect/api/app.jar.bak.20260714_203358`
- **Backup `.env`** : `/opt/joprelys-connect/api/.env.bak.<timestamp>`
- **Déploiement jar** : copie du nouveau `app.jar` dans `/opt/joprelys-connect/api/`.
- **Configuration** : ajout de `JOPRELYS_LAB_INTEGRATION_API_KEY=<secret-temporaire-masqué>` dans `/opt/joprelys-connect/api/.env` (variable requise par `application-prod.yml`, valeur temporaire à renouveler).
- **Redémarrage** : `systemctl restart joprelys-connect-api.service`.

## Résultat

```text
● joprelys-connect-api.service - Joprelys Connect API (Spring Boot)
     Loaded: loaded (/etc/systemd/system/joprelys-connect-api.service; enabled; preset: enabled)
     Active: active (running) since Tue 2026-07-14 20:45:53 CEST
   Main PID: 3863617 (java)
     Memory: 499.7M
      CPU: 1min 17.112s

Tomcat started on port 8084 (http)
Started JoprelysBackendApplication in 25.607 seconds
```

- Flyway : `Schema "public" is up to date. No migration necessary.` (version 68).
- Ports : `8084` en écoute sur `127.0.0.1`.
- HTTP local : réponse `401` (sécurisé, donc accessible).

## Actions restantes
1. **Renouveler le mot de passe SSH** exposé dans la conversation.
2. **Remplacer `JOPRELYS_LAB_INTEGRATION_API_KEY`** par la vraie clé d'intégration lab dans `/opt/joprelys-connect/api/.env`.
3. **Supprimer les backups temporaires** sur le serveur (`/tmp/joprelys_backup_*.dump`) après validation.
4. **Surveillance** : vérifier que le service reste stable et que les logs ne montrent pas d'erreur métier.

## Fichiers de référence
- Rapport complet : `logs/dev/server_diagnostic_report.txt`
- Script de diagnostic : `scripts/diagnose_server_paramiko.py`
- Migrations modifiées / créées :
  - `backend/src/main/resources/db/migration/V35__lab_results_cdc_alignment.sql`
  - `backend/src/main/resources/db/migration/V37__medical_documents_cdc_alignment.sql`
  - `backend/src/main/resources/db/migration/V68__remove_status_check_constraints.sql`

## Suivi
- Dernière mise à jour : 2026-07-14
- Responsable : Codex
- Reviewer : Tech Lead / exploitation
- Statut : **Terminé — service en ligne**

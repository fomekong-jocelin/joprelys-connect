# Runbook — Reset RECETTE, Flyway et extension PostgreSQL `btree_gist`

**Date de validation terrain :** 2026-07-24  
**Environnement validé :** RECETTE  
**Commit applicatif validé lors de l'incident :** `07db2d865afcdbf4767861431d8488af67e532b3`  
**Service concerné :** `joprelys-connect-recette-api`  
**Base d'exemple :** `joprelys_recette`

> Ce runbook s'applique aux environnements de développement et de recette dont les données sont explicitement jetables. Ne pas appliquer un reset destructif sur un environnement contenant des données à conserver sans backup, validation et plan de reprise.

## 1. Contexte

Pendant la phase de développement, Joprelys Connect autorise encore des resets complets de DEV/RECETTE afin d'éviter de conserver artificiellement des données ou structures legacy devenues incompatibles avec le modèle cible.

Le reset complet suivant est valide dans ce contexte :

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
```

Cependant, `DROP SCHEMA public CASCADE` supprime également les objets PostgreSQL installés dans ce schéma, notamment l'extension `btree_gist` utilisée par plusieurs migrations de contraintes temporelles.

Les migrations PostgreSQL qui utilisent `btree_gist` incluent notamment les contraintes d'exclusion temporelles des affectations de lits, des espaces et du personnel.

## 2. Symptôme observé

Après un reset complet puis un redéploiement, le backend compile correctement mais ne démarre pas.

Le healthcheck reste à :

```text
HTTP=000
curl: (7) Failed to connect to 127.0.0.1 port 8085
```

Spring peut afficher ensuite des erreurs comme :

```text
Error creating bean with name 'organizationApiKeyRepository'
Cannot resolve reference to bean 'jpaSharedEM_entityManagerFactory'
```

Ces erreurs JPA sont secondaires.

### Cause racine observée

La vraie erreur est dans Flyway :

```text
ERROR: permission denied to create extension "btree_gist"
Hint: Must have CREATE privilege on current database to create this extension.
```

La migration échoue avant la création de l'`EntityManagerFactory`, puis les repositories JPA échouent en cascade.

Chaîne de panne :

```text
reset schema
  ↓
`btree_gist` supprimé
  ↓
Flyway tente CREATE EXTENSION
  ↓
utilisateur applicatif sans privilège CREATE database
  ↓
Flyway échoue
  ↓
EntityManagerFactory absent
  ↓
repositories JPA non créés
  ↓
Tomcat ne termine pas son démarrage
  ↓
HTTP 8085 = 000
```

## 3. Politique retenue

L'utilisateur PostgreSQL applicatif ne doit pas recevoir des privilèges élevés permanents uniquement pour installer une extension.

La séparation retenue est :

```text
Compte PostgreSQL administrateur
└── installe/prépare les extensions techniques

Compte applicatif Joprelys
├── exécute Flyway
├── crée/modifie les objets applicatifs autorisés
└── accède aux données applicatives
```

Après tout reset complet qui supprime le schéma `public`, `btree_gist` doit donc être réinstallé avec un compte PostgreSQL autorisé **avant** le démarrage du backend/Flyway.

## 4. Procédure de reset DEV / RECETTE

### 4.1 Vérifier l'environnement

Avant toute destruction :

- confirmer explicitement que la cible est DEV ou RECETTE ;
- confirmer que les données sont jetables ;
- arrêter le backend ;
- réaliser un backup si une restauration pourrait être utile.

Exemple RECETTE :

```bash
sudo systemctl stop joprelys-connect-recette-api
```

### 4.2 Réinitialiser le schéma

```bash
sudo -u postgres psql -d joprelys_recette
```

Puis :

```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

GRANT ALL ON SCHEMA public TO postgres;
GRANT ALL ON SCHEMA public TO public;
```

Adapter les droits si l'environnement utilise un rôle applicatif PostgreSQL dédié.

Quitter :

```text
\q
```

### 4.3 Réinstaller `btree_gist` — étape obligatoire

Avant de relancer l'application :

```bash
sudo -u postgres psql -d joprelys_recette -c \
"CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;"
```

Vérifier :

```bash
sudo -u postgres psql -d joprelys_recette -c \
"SELECT extname, extversion
 FROM pg_extension
 WHERE extname = 'btree_gist';"
```

Résultat attendu : une ligne `btree_gist`.

### 4.4 Déployer le commit souhaité

Exemple :

```bash
/root/deploy-joprelys-connect-recette.sh main
```

Le script doit déployer le même commit `main` déjà validé par la CI, sans divergence de branche RECETTE permanente.

### 4.5 Vérifier Flyway

Après démarrage réussi :

```bash
sudo -u postgres psql -d joprelys_recette -c "
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 20;
"
```

Toutes les migrations appliquées doivent avoir `success = true`.

### 4.6 Vérifier le service

```bash
sudo systemctl status joprelys-connect-recette-api --no-pager
```

Puis :

```bash
curl -i http://127.0.0.1:8085/actuator/health
```

Résultat attendu : HTTP 200 et statut applicatif `UP` selon la configuration Actuator en vigueur.

## 5. Contrôle de rollback sur base vierge

Un rollback peut restaurer une base dont `flyway_schema_history` n'existe pas encore, notamment lorsqu'un backup a été pris juste après un reset complet et avant le premier démarrage Flyway.

Le script de rollback ne doit donc pas exécuter directement :

```sql
SELECT ... FROM flyway_schema_history;
```

sans vérifier d'abord l'existence de la table.

### Contrôle robuste recommandé

```bash
sudo -u postgres psql -d joprelys_recette -tAc "
SELECT CASE
    WHEN to_regclass('public.flyway_schema_history') IS NULL
        THEN 'FLYWAY_HISTORY_ABSENT'
    ELSE 'FLYWAY_HISTORY_PRESENT'
END;
"
```

Puis seulement, si le résultat est `FLYWAY_HISTORY_PRESENT`, lire son contenu.

Cette règle rend le rollback compatible avec :

- une base déjà migrée ;
- une base vierge ;
- un reset complet immédiatement suivi d'un échec avant initialisation Flyway.

## 6. Diagnostic rapide en cas de `HTTP=000`

Ne pas conclure immédiatement à un problème réseau ou Tomcat.

Ordre de diagnostic recommandé :

```bash
sudo journalctl -u joprelys-connect-recette-api -n 800 --no-pager \
  | grep -Ei -C 12 'Flyway|migration|Caused by:|ERROR|btree_gist'
```

Chercher la **dernière cause racine** de la pile d'exception.

### Cas confirmé le 2026-07-24

```text
organizationApiKeyRepository failure
    ↓ conséquence
jpaSharedEM_entityManagerFactory failure
    ↓ conséquence
entityManagerFactory absent
    ↓ conséquence
flywayInitializer failure
    ↓ cause racine
permission denied to create extension "btree_gist"
```

## 7. Warnings observés mais non bloquants

Lors du déploiement validé, les éléments suivants ont été observés sans empêcher les builds :

### Backend

```text
PdfGeneratorService.java: getRoomNumber() ... deprecated and marked for removal
```

Il s'agit d'une dette de nettoyage liée au passage `Room → Space`. Elle ne doit pas être confondue avec une panne de démarrage.

### Frontend

Warnings observés :

- `DatePipe` importé mais non utilisé dans un composant urgence ;
- bundle initial légèrement supérieur au budget Angular ;
- quelques feuilles CSS dépassent leur budget configuré.

Ces points sont à traiter séparément et ne sont pas la cause du démarrage impossible du backend.

## 8. Anti-patterns à éviter

Ne pas :

- accorder `SUPERUSER` au compte applicatif ;
- accorder durablement des privilèges de création d'extension uniquement pour contourner le problème ;
- modifier manuellement `flyway_schema_history` pour forcer une migration en succès ;
- éditer une migration Flyway historique déjà fusionnée pour contourner l'environnement ;
- supprimer manuellement des tables au cas par cas lorsqu'un reset complet a été décidé ;
- interpréter une erreur JPA en cascade comme la cause racine sans lire les dernières lignes `Caused by`.

## 9. Évolution recommandée du script de déploiement

Le script `deploy-joprelys-connect-recette.sh` est actuellement géré sur le serveur et n'est pas versionné dans ce dépôt.

Les améliorations suivantes sont recommandées :

1. ajouter un preflight `btree_gist` avant démarrage lorsque la base vient d'être réinitialisée ;
2. échouer avec un message explicite si l'extension manque ;
3. rendre le contrôle rollback de `flyway_schema_history` conditionnel à l'existence de la table ;
4. afficher le commit exact déployé ;
5. continuer à restaurer JAR, frontend et base lors d'un healthcheck rouge ;
6. ne jamais masquer la cause Flyway racine derrière le seul `HTTP=000`.

## 10. Politique pendant la phase de développement

Jusqu'au gel fonctionnel décidé après les retours de démonstration :

- DEV/RECETTE peuvent être réinitialisés si les données sont explicitement jetables ;
- le modèle cible est prioritaire sur la conservation de données legacy sans valeur métier ;
- aucune compatibilité legacy artificielle ne doit être ajoutée uniquement pour conserver des données de test ;
- les migrations déjà fusionnées restent immuables : toute correction de schéma ultérieure passe par une nouvelle migration `Vxx`.

Dès que les données deviennent réellement à conserver, cette politique doit être remplacée par une stratégie de migration non destructive avec backup, reprise et rollback validés.

## 11. Checklist opérateur

### Avant reset

- [ ] environnement confirmé DEV/RECETTE ;
- [ ] données confirmées jetables ;
- [ ] backend arrêté ;
- [ ] backup réalisé si nécessaire.

### Après reset

- [ ] schéma `public` recréé ;
- [ ] droits PostgreSQL réappliqués ;
- [ ] `btree_gist` réinstallé avec un compte administrateur ;
- [ ] extension vérifiée dans `pg_extension`.

### Après déploiement

- [ ] commit exact affiché ;
- [ ] Flyway terminé sans échec ;
- [ ] `flyway_schema_history` cohérente ;
- [ ] service systemd `active (running)` ;
- [ ] healthcheck HTTP 200 ;
- [ ] aucun `Caused by` bloquant dans les logs.
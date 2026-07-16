# TASK-20260716 — Réinitialisation contrôlée des données de production

## Objectif

Purger les données métier de production, supprimer patients, utilisateurs et cliniques, conserver l'historique Flyway, régénérer le catalogue RBAC système et laisser un unique compte `SUPER_ADMIN`.

## Critères d'acceptation

- [x] Inventaire préalable des tables et volumes réalisé.
- [x] Script non interactif avec confirmation explicite créé.
- [x] Sauvegarde PostgreSQL obligatoire avant purge.
- [x] Mot de passe absent du code, du SQL versionné et des logs.
- [x] Purge exécutée avec succès.
- [x] Vérification : 1 utilisateur, 0 organisation, 0 patient.
- [x] Vérification : catalogue RBAC système régénéré et service actif.

## Plan d'action

- [x] Analyser les dépendances et le bootstrap RBAC.
- [x] Définir le rollback par `pg_restore`.
- [x] Créer le script Paramiko transactionnel.
- [x] Exécuter en production.
- [x] Consigner la sauvegarde et les preuves après exécution.

## Estimation

- Effort : 0,5 jour senior backend/DBA.
- Reviewer : Lead Backend + DBA + sécurité.
- Risque : critique, perte de données volontaire compensée par sauvegarde complète.
- Impact version : aucun bump applicatif, opération de données.

## Rollback

Arrêter le service, recréer une base vide si nécessaire, restaurer le dump avec `pg_restore --clean --if-exists`, puis redémarrer et contrôler Flyway.

## Résultat de production — 2026-07-16

- Sauvegardes :
  - `/opt/joprelys-connect/backups/joprelys_before_reset_20260716234048.dump` — 272 740 octets ;
  - `/opt/joprelys-connect/backups/joprelys_before_reset_20260716234124.dump` — 272 740 octets.
- Utilisateurs : 1.
- Organisations : 0.
- Patients : 0.
- Tables métier non vides hors configuration : aucune.
- Catalogue : 15 rôles, 44 permissions, 1 affectation utilisateur-rôle.
- Administrateur : `jocelin.fomekong@joprelys.com`, rôle `SUPER_ADMIN`, actif, sans organisation.
- Mot de passe : correspondance BCrypt vérifiée sans exposition de la valeur.
- Backend : service `active`, Flyway version 68 validée.

## Incident contrôlé pendant l'exécution

La première tentative s'est arrêtée avant la transaction car le fichier SQL temporaire n'était pas lisible par PostgreSQL. Aucune suppression n'avait commencé. Le script a été corrigé pour transférer la propriété à `postgres`, puis la seconde exécution a réussi. Le contrôle RBAC initial était trop précoce ; le script attend désormais jusqu'à 60 secondes la fin du bootstrap.

## Statut

DONE — purge et validations terminées.

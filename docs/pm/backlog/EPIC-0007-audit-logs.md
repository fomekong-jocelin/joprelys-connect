# EPIC-0007 — Traçabilité & Audit Logs

## 1. Objectif métier

Assurer la conformité légale (notamment avec la loi de protection des données personnelles) en traçant de manière non modifiable toutes les actions critiques et tous les accès aux dossiers médicaux des patients.

## 2. Périmètre

### Inclus

- Enregistrement automatique en base de données de chaque action sensible : connexion (succès/échec), consultation de fiche patient, création/modification d'un patient, génération ou téléchargement de document, révocation de document, consultation de la page de vérification.
- Capture de : date/heure, utilisateur (ou IP pour le vérificateur public), type d'action, identifiant de la ressource consultée.
- Interface d'affichage des logs d'audit réservée exclusivement aux administrateurs système.
- Protection en écriture : les logs d'audit ne peuvent pas être modifiés ou supprimés via l'application.

### Exclus

- Export vers des outils de SIEM externes (ex. Elasticsearch/Splunk) ou fichiers de syslog distants.
- Export direct au format PDF/CSV depuis l'interface d'administration.

## 3. Utilisateurs concernés

- Administrateur Joprelys
- Administrateur Clinique

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| STORY-0701 | Enregistrement automatique des logs d'audit | P0 | 3 | BACKLOG | SPRINT-0003 |
| STORY-0702 | Consultation de l'historique des accès par l'administrateur | P1 | 2 | BACKLOG | SPRINT-0004 |

## 5. Dépendances

- EPIC-0001 (Authentification) pour l'identité de l'opérateur effectuant l'action.
- Initialisation des tables de données pour disposer des entités à tracer.

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Saturation de la base de données par des écritures de logs trop fréquentes | Moyen | Limiter les logs d'audit aux événements réellement critiques et sensibles, optimiser les index sur la date d'enregistrement. |

## 7. Définition de succès

- [ ] Tout accès à un profil patient ou téléchargement de PDF produit instantanément une ligne de log d'audit.
- [ ] Même un administrateur clinique ne peut pas supprimer sa propre ligne de log d'audit.

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 5 |
| Effort senior | 1.5j |
| Effort intermédiaire | 2j |
| Effort junior | 3j |
| Nombre de sprints estimé | 1 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout du module de traçabilité et d'audit logs obligatoires |
| Breaking change | Non |
| Release cible | v0.5.0 |

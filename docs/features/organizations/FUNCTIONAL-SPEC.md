# FUNCTIONAL-SPEC — Complétion de la gestion des établissements (Module 1)

## 1. Résumé métier

Le Module 1 (Gestion des établissements) de Joprelys Connect a pour objectif de gérer les acteurs connectés à la plateforme de santé. Actuellement, les attributs décrivant ces acteurs sont parcellaires et les fonctionnalités liées aux clés d'API (génération, révocation) et à l'autorisation des modules sont absentes.
Ce projet vise à compléter les attributs requis par le Cahier des Charges (type d'acteur, pays, responsable légal, autorisation API) et d'ajouter une gestion sécurisée et granulaire des clés API de chaque organisation.

## 2. Objectifs

- [ ] Structurer les types d'acteurs de santé selon le CDC.
- [ ] Gérer l'état d'autorisation et d'activation API par établissement.
- [ ] Offrir aux administrateurs globaux (Joprelys) la possibilité de générer et de révoquer des clés API d'organisation.
- [ ] Bloquer automatiquement tout appel API d'un établissement dont le statut est suspendu ou dont l'API est désactivée.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| ADMIN_JOPRELYS | Gérer tous les établissements du système (création, modification, blocage) et leurs clés API. | Accès global (lecture, écriture, activation). |
| ADMIN_CLINIQUE | Gérer son personnel local et ses consultations locales. | Ne peut pas voir ou modifier son propre établissement ni ses clés API. |
| API_CLIENT | Consommer les services de Joprelys Connect par connexion machine-to-machine. | Restreint par la validité de sa clé API et l'autorisation de son établissement. |

## 4. Périmètre

### Inclus

- Évolution du modèle de données d'organisation (type, pays, responsable, api_enabled).
- Création d'une table `organization_api_keys` contenant les métadonnées et le hash SHA-256 de la clé API.
- Écran d'administration mis à jour dans le frontend Angular.
- Filtre de sécurité backend `ApiKeyAuthenticationFilter` interceptant `X-API-KEY`.
- Blocage automatique des requêtes si le statut de l'organisation est `INACTIVE` ou si `api_enabled` est `false`.

### Exclus

- Intégration de passerelles d'incidents (exclus du MVP).
- Gestion fine des modules autorisés par établissement (sera traité ultérieurement par un système RBAC étendu).

## 5. Parcours utilisateur

1. **Création d'un établissement** : L'admin Joprelys se rend sur le panneau d'administration, clique sur "Créer une organisation", renplit les champs (Nom, Email, Téléphone, Adresse, Ville, Pays, Type d'établissement, Nom du responsable) puis enregistre.
2. **Génération d'une clé API** : Dans la fiche détail de l'établissement, l'admin clique sur "Générer une clé API". Le système crée un jeton unique, affiche la clé en clair une seule et unique fois à l'écran, puis stocke son hash SHA-256 en base de données.
3. **Révocation d'une clé API** : L'admin sélectionne une clé active dans la liste et clique sur "Révoquer". La clé est marquée révoquée en base et n'est plus acceptée.
4. **Appel API par un tiers** : Le client API de l'établissement effectue une requête HTTP en ajoutant le header `X-API-KEY: jop_live_xxxx`. Le backend valide l'empreinte et le statut de l'établissement.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| BR-ORG-001 | Un établissement doit obligatoirement être affecté à un type d'acteur. | P0 | CDC Module 1 |
| BR-ORG-002 | Le statut de l'établissement régit l'accès API. Un statut `INACTIVE` ou `api_enabled=false` bloque toutes les requêtes du client associé. | P0 | CDC Module 1 (FR-ORG-004) |
| BR-ORG-003 | La clé API en clair ne doit jamais être stockée en base de données ou journalisée. Seul son hash SHA-256 est persistant. | P0 | Bonnes pratiques OWASP |
| BR-ORG-004 | Une clé API révoquée ne peut plus être réactivée. | P1 | CDC Module 1 (FR-ORG-003) |

## 7. Critères d’acceptation

- [ ] L'IHM frontend Angular affiche et permet de configurer le Type d'acteur, le Pays et le Responsable légal.
- [ ] Les clés API générées commencent par le préfixe `jop_`.
- [ ] Le hachage SHA-256 des clés est validé en base.
- [ ] Un appel avec une clé API révoquée ou invalide retourne un code HTTP `401 Unauthorized`.
- [ ] Un appel avec une clé valide mais pour une organisation désactivée ou avec `api_enabled = false` retourne un code HTTP `403 Forbidden`.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| Clé API manquante dans l'entête | Retourner un code HTTP `401 Unauthorized`. |
| Organisation inactive mais clé API valide | Retourner un code HTTP `403 Forbidden` indiquant que l'établissement est suspendu. |
| Clé API générée perdue | Impossible de la récupérer. L'administrateur doit révoquer l'ancienne et en générer une nouvelle. |

## 9. Textes / i18n

| Clé | Français | English |
|---|---|---|
| org.type | Type d'établissement | Organization Type |
| org.country | Pays | Country |
| org.responsibleName | Responsable légal | Legal Responsible |
| org.apiEnabled | Accès API activé | API Access Enabled |
| org.apiKeys | Clés API | API Keys |
| org.generateKey | Générer une clé | Generate a key |
| org.revokeKey | Révoquer la clé | Revoke the key |

## 10. Impacts UI / branding

| Point | Impact |
|---|---|
| Nom de l’app | Non |
| Logo | Non |
| Thème light/dark | Oui (le tableau des clés API et le modal de clé générée doivent s'adapter aux thèmes) |
| Composants réutilisables | Oui (utilisation de `app-ui-button`, `app-ui-card` et formulaires standardisés) |

## 11. Hypothèses et questions ouvertes

- Les pays gérés par défaut au MVP sont : "Cameroun", "France", "Canada", "Sénégal".
- Les types d'établissement supportés sont : `HOSPITAL`, `CLINIC`, `CABINET`, `LABORATORY`, `IMAGING_CENTER`, `PHARMACY`, `HEALTH_PLATFORM`, `NGO`, `INSTITUTION`.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-05 | Antigravity | Création initiale de la spécification fonctionnelle |

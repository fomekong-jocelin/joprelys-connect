# FUNCTIONAL-SPEC — Traçabilité & Audit Logs

## 1. Résumé métier

Dans le domaine de la santé (HealthTech), la traçabilité des accès aux données de santé des patients est une exigence réglementaire majeure (RGPD, exigences de conformité HDS/HIPAA). Chaque accès à un dossier patient, chaque émission, téléchargement ou révocation de document médical, ainsi que toutes les connexions (réussies ou échouées) doivent être consignés de manière inaltérable (immuabilité des logs d'audit) pour permettre des enquêtes d'audit et rassurer les patients sur le respect du secret médical.

## 2. Objectifs

- [x] Consigner systématiquement et de manière immuable toutes les actions sensibles effectuées sur le système.
- [x] Offrir une API REST sécurisée de consultation des logs d'audit pour les auditeurs et administrateurs cliniques.
- [x] Garantir l'isolation multi-tenant pour les administrateurs cliniques (ne voir que les logs de leur clinique).

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| **Auditeur Système** | Consulter l'historique complet de tous les événements de toutes les cliniques à des fins de contrôle de conformité. | Accès global (lecture seule). |
| **Administrateur Clinique** | Consulter l'historique des accès et des actions au sein de son propre établissement uniquement. | Accès restreint à son `organizationId`. |
| **Médecin** | Voir qui a accédé aux dossiers de ses propres patients s'il suspecte un accès non autorisé. | Accès restreint aux patients sous sa responsabilité. |
| **Patient** | Voir qui a accédé à son dossier médical (exigence RGPD). | Hors périmètre STORY-0701 (placé en backlog). |

## 4. Périmètre

### Inclus

- Enregistrement automatique en base de données (table `audit_logs`) pour les événements suivants :
  - Connexion réussie / Échec de connexion (Bad Credentials)
  - Création de patient / Modification d'identité de patient
  - Création de visite clinique / Clôture de visite
  - Consultation d'un dossier médical (visualisation de l'historique)
  - Génération de document médical / Téléchargement de document PDF
  - Révocation ou annulation d'un document médical
- API REST sécurisée pour extraire et filtrer les logs d'audit.
- Isolation stricte des données (tenant-isolation) lors de la consultation.

### Exclus

- Interface graphique utilisateur de consultation (STORY-0702 - Frontend).
- Notification en temps réel des accès urgents ou suspects (EPIC-0009).
- Accès du patient à son propre journal d'audit (placé en backlog).

## 5. Parcours utilisateur

1. **Génération automatique des logs** :
   - Un utilisateur (ex: Dr. Alpha) se connecte ou consulte le dossier d'un patient.
   - Le système intercepte l'action et insère de manière asynchrone ou synchrone une ligne d'audit dans la table `audit_logs` détaillant l'acteur, le patient concerné, la ressource accédée (visite, ordonnance, etc.), l'adresse IP, le statut de l'action (SUCCESS, DENIED) et l'horodatage.
2. **Consultation réglementaire** :
   - Un auditeur système ou admin de clinique interroge l'endpoint `/api/audit/...`.
   - Le système vérifie ses droits, applique le filtre d'organisation (multi-tenant) et renvoie la liste paginée des logs d'audit.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| **BR-AUDIT-001** | **Immuabilité** : Les logs d'audit sont en écriture seule. Aucune route d'API `DELETE` ou `PUT` / `PATCH` ne doit exister sur la ressource d'audit. | P0 | Cahier des charges |
| **BR-AUDIT-002** | **Accès refusés** : Toute tentative d'accès bloquée par la sécurité (403 Forbidden, 401 Unauthorized) sur une donnée patient ou une ressource clinique sensible doit générer un log d'audit avec le statut `DENIED`. | P0 | Cahier des charges |
| **BR-AUDIT-003** | **Multi-tenancy** : Un administrateur clinique (`ADMIN_CLINIQUE`) ne peut requêter que les logs de sa propre clinique. L'auditeur système (`AUDITEUR`) a accès à toutes les organisations. | P0 | Spécifications d'architecture |
| **BR-AUDIT-004** | **Traçabilité de l'acteur** : Les logs doivent capturer l'adresse IP de la requête et le User-Agent (appareil) du client. | P1 | Cahier des charges |

## 7. Critères d’acceptation

- [ ] L'API backend expose un endpoint `GET /api/audit/patients/{patientId}` retournant les logs filtrés par patient.
- [ ] L'API backend expose un endpoint `GET /api/audit/organizations/{organizationId}` retournant les logs filtrés par organisation.
- [ ] L'accès à ces endpoints requiert les rôles `AUDITEUR`, `ADMIN_CLINIQUE` ou `MEDECIN`.
- [ ] Un `ADMIN_CLINIQUE` ou un `MEDECIN` d'une organisation A ne peut pas consulter les logs d'une organisation B (retourne 403 ou liste vide selon droit d'accès au patient).
- [ ] Les actions de connexion (succès/échec), consultation de patient, édition, et révocation de document déclenchent la création automatique d'un log.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| ID patient inexistant | Retourne une réponse 404 ou une liste vide si le patient n'existe pas. |
| Requête par un utilisateur non authentifié | Retourne un statut 401 Unauthorized. |
| Admin clinique qui tente de lire les logs d'un patient d'une autre clinique | Retourne 403 Forbidden (violation de multi-tenancy). |

## 9. Textes / i18n

*Sans impact direct sur la STORY-0701 car c'est une implémentation purement backend/API.*

## 10. Impacts UI / branding

*Sans impact sur la STORY-0701 (Backend-only).*

## 11. Hypothèses et questions ouvertes

- **Synchrone vs Asynchrone** : Pour cette première phase, l'écriture des logs d'audit se fera de manière synchrone dans la même transaction que l'action ou via un événement Spring d'application transactionnel (`@TransactionalEventListener`) afin de garantir que le log est persisté si l'action réussit.

## 12. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-02 | Antigravity | Création initiale pour EPIC-0007 |

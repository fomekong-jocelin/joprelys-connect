# RELEASE NOTE — v0.9.0

## 1. Résumé

La version `0.9.0` de Joprelys Connect introduit des fonctionnalités d'interopérabilité majeures et sécurise le partage de données de santé :
1. **Télétransmission d'ordonnances à AllôPharma (STORY-1601/1602)** : Télétransmission d'ordonnances actives vers la pharmacie partenaire externe AllôPharma. Intégration côté patient (portail) et médecin (dossier consultation) avec badges de statut et boutons d'action. Client HTTP simulé avec traçabilité et logs d'audit.
2. **Demandes d'Accès Externes (STORY-1301/1302/1303)** : Système d'autorisation temporaire permettant aux praticiens d'autres cliniques de demander l'accès au DPU d'un patient. Le patient peut approuver ou rejeter les demandes depuis son portail. Planificateur automatique d'expiration et traçabilité de sécurité (urgence brise-glace).
3. **Centre de Notifications (STORY-1501/1502)** : Socle backend d'envoi et de stockage de notifications d'événements de santé et de sécurité. Centre de notifications visuel sur le portail patient avec badge dynamique et options de lecture individuelle et globale.
4. **Configuration Git (TICKET-1603)** : Ajout du dépôt distant officiel (remote `origin`).

## 2. Version

| Champ | Valeur |
|---|---|
| Version précédente | 0.8.0 |
| Version publiée | 0.9.0 |
| Type de bump | MINOR |
| Date | 2026-07-04 |
| Responsable release | Antigravity |

## 3. Justification SemVer

Le bump est de type **MINOR** :
- Ajout de nouvelles fonctionnalités rétrocompatibles (télétransmission, demandes d'accès externes, système de notifications).
- Nouvelles tables Flyway `notifications` (V21) et colonnes de télétransmission dans `prescriptions` (V22) additives.
- Aucun changement de contrat destructif sur les API existantes.

## 4. Tickets inclus

| Ticket | Titre | Type | Statut |
|---|---|---|---|
| [STORY-1601](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1601-teletransmission-ordonnance-backend.md) | Télétransmission d'ordonnances à AllôPharma (backend) | User Story | DONE |
| [STORY-1602](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1602-teletransmission-ordonnance-ui.md) | Interface de télétransmission (IHM) | User Story | DONE |
| [STORY-1301](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1301-demande-acces-externe-backend.md) | Enregistrement de demande d'accès externe (backend) | User Story | DONE |
| [STORY-1302](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1302-validation-acces-externe-ui.md) | Validation de demande d'accès externe (portail patient) | User Story | DONE |
| [STORY-1303](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1303-controle-acces-expiration.md) | Contrôle d'accès & Expiration des droits externes | User Story | DONE |
| [STORY-1501](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1501-service-notifications-backend.md) | Socle et service d'envoi de notifications (backend) | User Story | DONE |
| [STORY-1502](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/STORY-1502-centre-notifications-ui.md) | Centre de notifications sur le portail patient (IHM) | User Story | DONE |
| [TICKET-1603](file:///C:/MES-APPLICATIONS/joprelys-connect/docs/ai/tickets/TICKET-1603-add-git-remote.md) | Ajout du dépôt remote git | DevOps | DONE |

## 5. Changements

### Added

- **Télétransmission** : Colonnes `transmission_status` et `transmitted_at` (table `prescriptions` - Flyway V22), client d'intégration externe simulation `AlloPharmaClient`, contrôleurs de télétransmission médecin et patient.
- **Accès Externes** : Entité `ExternalAccessRequestEntity` et table `external_access_requests` (Flyway V20), service d'expiration automatique scheduler `ExternalAccessExpirationScheduler`.
- **Notifications** : Entité `NotificationEntity` et table `notifications` (Flyway V21), composants IHM `PatientNotificationsComponent` avec décompte dynamique.
- **Git Config** : Remote origin configurée pour le dépôt distant.

### Changed

- **Consultation DTO** : Mappage des informations de prescription (ID, numéro, statut, statut de transmission, date de transmission) pour les médecins et les patients dans `ConsultationResponse` et `PatientPortalMeResponse`.

### Fixed

- **Sécurité IDOR** : Ajout d'autowires et nettoyage SQL strict dans les tests de contrôleur patient pour éviter les collisions de données de test et échecs de sécurité.

## 6. Breaking changes

- Aucun.

## 7. Migrations

- Flyway `V20__create_external_access_requests_table.sql` (additive).
- Flyway `V21__create_notifications_table.sql` (additive).
- Flyway `V22__add_teletransmission_to_prescriptions.sql` (additive).

## 8. Tests et QA

| Vérification | Résultat | Preuve |
|---|---|---|
| Backend tests | ✅ Succès | 166 tests unitaires et d'intégration au vert (`./mvnw test`) |
| Angular build | ✅ Succès | Build Angular compilé sans erreur (`npm run build`) |
| Angular tests | ✅ Succès | 62 tests unitaires Angular passés au vert (`npm run test`) |

## 9. Déploiement

```bash
# Packaging backend
./mvnw clean package

# Build frontend
cd web && npm ci && npm run build
```

## 10. Tag Git

```bash
git tag -a v0.9.0 -m "Release v0.9.0"
git push origin v0.9.0
```

## 11. Rollback

En cas d'anomalie critique en production :
1. Revenir sur le tag précédent `v0.8.0`.
2. Déployer à nouveau les packages correspondants.
3. Les tables créées par Flyway V20/V21 et les colonnes ajoutées par V22 sont additives et n'altèrent pas les données pré-existantes.

## 12. Risques restants

- Aucun risque identifié. L'intégration d'AllôPharma est sécurisée par simulation locale, et les demandes d'accès d'urgence sont journalisées sous statut critique `EMERGENCY_DPU_ACCESS` avec alerte visuelle.

---

## Vérification Maven / Tailwind / Angular Material

- [x] Backend Spring Boot vérifié avec Maven uniquement.
- [x] Angular vérifié avec Tailwind CSS v4.
- [x] Aucun Angular Material introduit.
- [x] Spring Boot utilise `application.yml` et profils YAML ; aucun `application.properties`.
- [x] Angular possède `proxy.conf.json`, `proxyConfig` dans `angular.json`, et aucune URL backend hardcodée.

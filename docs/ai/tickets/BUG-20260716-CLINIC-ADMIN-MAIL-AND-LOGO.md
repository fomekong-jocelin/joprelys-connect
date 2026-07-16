# BUG-20260716-CLINIC-ADMIN-MAIL-AND-LOGO — Création admin en erreur et logo perdu

**Mode** : Diagnostic + Engineering + QA Review

**Date** : 2026-07-16

**Statut** : DONE

**Priorité** : P0

**Estimation** : 0,75 j senior

**Profil recommandé** : Senior full-stack Spring Boot / Angular

**Reviewer** : Lead Backend + Lead Frontend + QA

**Impact version** : PATCH

## Diagnostic initial

- [x] Le `POST /api/organizations/{id}/admin` persiste l'utilisateur avant l'envoi SMTP ; une panne SMTP produit un `500` après une création partielle.
- [x] `CreateOrganizationRequest` ne déclare pas `logoPath` ; le champ envoyé par Angular est ignoré lors de la création.
- [x] Le répertoire des téléversements repose sur un chemin relatif par défaut et n'est pas explicitement configurable dans `application.yml`.
- [x] L'interface remplace les erreurs backend normalisées (`error.message`) par un message générique.

## Critères d'acceptation

- [x] Une création d'admin et l'envoi de son mot de passe sont atomiques.
- [x] Un échec d'envoi ne laisse aucun compte inaccessible en base.
- [x] L'API renvoie `503` avec un message exploitable lorsque l'e-mail ne peut pas partir.
- [x] Le frontend restitue le message normalisé sans exposer de détail technique.
- [x] Le logo envoyé lors de la création est persisté et renvoyé par l'API.
- [x] Le stockage des fichiers est configurable par variable d'environnement.
- [x] Les tests backend et frontend couvrent les régressions.
- [x] Le shell HTML n'est jamais mis en cache et les assets hashés sont immuables.
- [x] Un asset Angular absent renvoie `404` au lieu du shell HTML.

## Actions

- [x] Extraire le cas d'usage de création d'admin dans un service transactionnel.
- [x] Ajouter le traitement API de l'indisponibilité e-mail.
- [x] Corriger le contrat de création d'organisation.
- [x] Centraliser la configuration du stockage.
- [x] Améliorer le retour d'erreur Angular.
- [x] Exécuter les tests et builds.
- [x] Mettre à jour le suivi et le changelog.

## Reste à faire

- Effectuer une QA métier complémentaire des créations d'admin, OTP interne/patient et logos avec des données réelles.
- Corriger séparément quatre défauts de nettoyage/interférence révélés par la suite Maven complète.

## Validation production — 2026-07-17

- `MAIL_HOST` corrigé vers `mail.joprelys.com` et service redémarré sans erreur récente.
- Répertoire persistant configuré dans `/opt/joprelys-connect/storage/uploads` avec reprise des fichiers existants.
- Demande réelle de récupération pour le super-administrateur : HTTP `200`.
- `index.html` : `Cache-Control: no-store, no-cache, must-revalidate`.
- Bundle courant : HTTP `200`, type `text/javascript`.
- Ancien chunk absent : HTTP `404` au lieu de `200 text/html`.
- Sauvegarde environnement : `/opt/joprelys-connect/api/.env.bak.mailfix-20260717002748`.
- Sauvegarde Apache : `/var/www/vhosts/joprelys.com/httpdocs/.htaccess.bak.20260717002748`.

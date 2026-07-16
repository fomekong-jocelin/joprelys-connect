# BUG-20260716-CLINIC-ADMIN-MAIL-AND-LOGO — Création admin en erreur et logo perdu

**Mode** : Diagnostic + Engineering + QA Review

**Date** : 2026-07-16

**Statut** : QA — déploiement production restant

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

## Actions

- [x] Extraire le cas d'usage de création d'admin dans un service transactionnel.
- [x] Ajouter le traitement API de l'indisponibilité e-mail.
- [x] Corriger le contrat de création d'organisation.
- [x] Centraliser la configuration du stockage.
- [x] Améliorer le retour d'erreur Angular.
- [x] Exécuter les tests et builds.
- [x] Mettre à jour le suivi et le changelog.

## Reste à faire

- Injecter `MAIL_HOST=mail.joprelys.com` et `JOPRELYS_UPLOAD_DIR=/opt/joprelys-connect/storage/uploads` sur le serveur.
- Déployer le backend et le frontend puis effectuer une QA réelle des quatre parcours e-mail et du logo.
- Corriger séparément quatre défauts de nettoyage/interférence révélés par la suite Maven complète.

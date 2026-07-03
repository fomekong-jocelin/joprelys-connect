# FUNCTIONAL-SPEC — Socle de notifications (backend) - STORY-1501

## 1. Résumé métier

Afin de maintenir le patient informé des événements clés touchant à la sécurité de son Dossier Patient Unique (DPU) et à son parcours de soins, Joprelys Connect intègre un système de notifications. Ce module enregistre et distribue des messages d'alertes, d'informations ou de sécurité (ex: "Demande d'accès externe reçue", "Accès d'urgence activé"). Le patient peut consulter son historique de notifications depuis son espace portail, marquer les notifications comme lues, et le système peut simuler des envois par canaux alternatifs (Email, SMS).

## 2. Objectifs

- Offrir une infrastructure d'enregistrement et d'historisation des notifications en base de données.
- Distribuer automatiquement une notification au patient lorsqu'une clinique externe formule une demande d'accès à son dossier.
- Fournir des API REST sécurisées pour lister ses notifications et les marquer comme lues.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Patient | Recevoir des alertes de sécurité et des rappels de soins, et pouvoir les lister et les marquer comme lues | Accède à ses propres notifications. |
| Système / Praticien | Émettre des notifications au patient lors d'actions clés (ex: demande d'accès externe) | Génère des notifications automatiquement. |

## 4. Périmètre

### Inclus

- Base de données : création de la table `notifications`.
- Service métier `NotificationService` avec envoi simulé (logs de passerelle) et enregistrement en base.
- Automatisation : émission d'une notification lors de la création d'une demande d'accès externe (STORY-1301) et lors d'un accès d'urgence Brise-Glace (STORY-1303).
- API REST sécurisées pour le patient connecté.

### Exclus

- Routage réel SMTP ou passerelle SMS payante (simulés par logs).

## 5. Parcours utilisateur

1. Un établissement de santé externe soumet une demande d'accès temporaire pour le patient A.
2. Le système crée la demande d'accès et déclenche automatiquement l'émission d'une notification :
   - Message : "La Clinique B demande l'accès à votre dossier médical pour : Consultation cardiologique."
3. Le patient A se connecte sur son portail.
4. Il voit la notification non lue dans son espace.
5. Il clique sur "Marquer comme lu", la notification passe au statut lu et le compteur de notifications non lues est décrémenté.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| RM-1501-1 | Une notification doit être adressée à un destinataire (UUID patient) et comporter un titre, un message, un statut (LU/NON_LU) et une date. | P0 | Spécification de traçabilité |
| RM-1501-2 | Toute création de demande d'accès externe doit émettre une notification instantanée au patient. | P0 | Spécification fonctionnelle |
| RM-1501-3 | Tout accès d'urgence (Brise-Glace) doit émettre une notification de sécurité immédiate au patient. | P0 | Spécification de sécurité |
| RM-1501-4 | Un patient ne peut lister ou marquer comme lues que ses propres notifications (prévention IDOR). | P0 | OWASP A01 |

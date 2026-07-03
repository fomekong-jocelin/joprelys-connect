# FUNCTIONAL-SPEC — Contrôle d'accès & Expiration des droits externes - STORY-1303

## 1. Résumé métier

Lorsqu'un praticien (médecin, infirmier) issu d'un établissement de santé externe souhaite accéder au Dossier Patient Unique (DPU) d'un patient d'un autre établissement, le système doit vérifier qu'un consentement valide existe. Ce consentement se matérialise soit par un consentement local (si le patient est de la même clinique), soit par une demande d'accès externe approuvée par le patient et non expirée.

En cas d'urgence absolue, le médecin externe peut outrepasser cette restriction via la procédure "Brise-Glace" (Break-the-Glass), qui lève temporairement le blocage tout en générant une trace d'audit critique de sécurité (`EMERGENCY_DPU_ACCESS`).

Les droits d'accès temporaires accordés au titre des demandes d'accès externes expirent automatiquement à la fin de leur durée de validité et sont révoqués sans intervention humaine.

## 2. Objectifs

- Bloquer l'accès au DPU du patient par un établissement externe sans demande d'accès approuvée et en cours de validité.
- Permettre aux médecins de forcer l'accès en cas d'urgence avec traçabilité renforcée.
- Automatiser la purge et l'expiration des droits d'accès externes.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Praticien externe | Accéder au DPU du patient d'une autre clinique pour ses soins | Doit avoir une demande d'accès active (APPROUVEE) et non expirée. En cas d'urgence, peut utiliser le mode Brise-Glace. |
| Patient | Être protégé contre les accès non autorisés à son DPU | Reçoit une alerte/notification et voit dans ses logs d'audit tout accès externe et tout accès d'urgence. |

## 4. Périmètre

### Inclus

- Filtrage des requêtes de consultation du DPU (`GET /api/patients/{id}`) pour vérifier la validité de l'accès externe si le praticien appartient à un autre établissement.
- Mécanisme automatique (tâche planifiée `@Scheduled`) de passage des demandes d'accès approuvées dont la date d'expiration est dépassée à l'état `EXPIREE`.
- Enregistrement de log d'audit critique rouge `EMERGENCY_DPU_ACCESS` lors d'un accès d'urgence.

### Exclus

- Notifications SMS directes (couvertes par la story notifications ultérieure).

## 5. Parcours utilisateur

### Parcours 1 : Accès autorisé par demande active
1. Le Dr Martin (Clinique B) recherche le patient A (Clinique A).
2. Il tente d'ouvrir le dossier du patient A.
3. Le système vérifie qu'une demande d'accès externe au nom de la Clinique B a été validée par le patient A et n'a pas expiré.
4. L'accès est accordé et le dossier s'affiche.

### Parcours 2 : Accès bloqué sans demande active
1. Le Dr Martin tente d'ouvrir le dossier du patient C (Clinique A).
2. Aucun accès externe n'a été approuvé pour la Clinique B sur ce patient.
3. Le système bloque la requête et renvoie une erreur `CONSENT_REQUIRED` (403 Forbidden).

### Parcours 3 : Accès d'urgence (Brise-Glace)
1. Le patient C arrive inconscient aux urgences de la Clinique B. Le Dr Martin a un besoin critique d'accéder à ses antécédents médicaux.
2. N'ayant pas d'accès approuvé, il clique sur le bouton "Accès d'urgence (Brise-Glace)".
3. Le système lui demande de renseigner un motif médical obligatoire (ex: "Arrêt cardio-respiratoire").
4. Une autorisation temporaire de 15 minutes est créée, le dossier est débloqué.
5. Un log d'audit de type `EMERGENCY_DPU_ACCESS` est immédiatement enregistré et marqué en critique rouge.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| RM-1303-1 | L'accès externe requiert une demande au statut `APPROUVEE` avec `expires_at > maintenant`. | P0 | Spécification de sécurité |
| RM-1303-2 | L'accès d'urgence outrepasse les consentements pour une durée fixe de 15 minutes. | P0 | Clause de sauvegarde médicale |
| RM-1303-3 | Tout accès d'urgence doit générer un audit log `EMERGENCY_DPU_ACCESS` avec le motif saisi. | P0 | Réglementation de traçabilité |
| RM-1303-4 | Les demandes d'accès expirées doivent être basculées automatiquement au statut `EXPIREE`. | P1 | Tâche planifiée de sécurité |

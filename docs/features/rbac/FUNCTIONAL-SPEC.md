# Spécification Fonctionnelle — Contrôle d'Accès Basé sur les Rôles (RBAC)

## 1. Problème métier

Dans un système d'information médical (Joprelys Connect), les données de santé et les actions administratives sont hautement confidentielles et soumises à des réglementations strictes. Tous les acteurs (administrateurs, médecins, pharmaciens, patients) ne doivent pas avoir les mêmes privilèges de lecture, d'écriture et de validation de données.

## 2. Objectif

Cette fonctionnalité garantit la cloisonnement et l'autorisation d'accès aux différentes ressources cliniques et administratives selon le rôle de l'utilisateur authentifié.

## 3. Rôles et Périmètres Applicatifs

| Rôle | Description | Droits fonctionnels clés |
|---|---|---|
| **`ADMIN_JOPRELYS`** | Administrateur système | Accès aux configurations globales, journaux d'audit de sécurité, actions de supervision. |
| **`MEDECIN`** | Professionnel de santé clinique | Consultation et modification des dossiers médicaux patients, création d'ordonnances. |
| **`PHARMACIEN`** | Professionnel de santé officinal | Consultation des ordonnances, validation de délivrance de médicaments. |

## 4. Parcours Utilisateur

### 4.1 Accueil & Tableau de Bord Dynamique
* À l'authentification réussie, l'utilisateur est redirigé vers l'écran d'accueil `/dashboard`.
* L'accueil se personnalise automatiquement en fonction du rôle :
  * **Administrateur** : Affiche les statistiques du système, le bouton d'accès aux logs d'audit.
  * **Médecin** : Affiche la liste des dossiers patients et des consultations rapides.
  * **Pharmacien** : Affiche la recherche d'ordonnances et la délivrance en cours.
* Si l'utilisateur change de rôle dans sa session, le menu s'adapte instantanément.

### 4.2 Tentative d'accès non autorisé
* Si un utilisateur force la saisie d'une URL dans le navigateur (ex: un pharmacien qui tente d'accéder à `/admin`), l'application intercepte la navigation et affiche une page **Accès refusé (403)** avec possibilité de retourner au tableau de bord.
* Si un appel direct à l'API est effectué sans les privilèges nécessaires, le système backend renvoie une erreur HTTP `403 Forbidden` et l'utilisateur reçoit une notification d'erreur.

## 5. Critères d'acceptation fonctionnels

1. **Visibilité conditionnelle** : Les menus d'administration ne sont visibles que pour `ADMIN_JOPRELYS`. Les actions de soin ne sont visibles que par les médecins et pharmaciens.
2. **Cloisonnement strict** : Un utilisateur avec le rôle `PHARMACIEN` ou `MEDECIN` ne peut en aucun cas charger la page d'administration système.
3. **Traduction / i18n** : Les messages d'accès refusé et les titres de rôles doivent supporter les langues française et anglaise (standards de la plateforme).

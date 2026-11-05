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

## 6. Écran « Rôles et autorisations » (`/clinic/rbac`) — UX-20261105 S3

But de l'écran : décider **ce que chaque personne peut voir et faire** dans l'établissement. Une personne reçoit
un ou plusieurs rôles ; un rôle regroupe des autorisations. Trois onglets, dans l'ordre de fréquence d'usage :

1. **Personnes** : recherche par nom ou e-mail (sans tenir compte des accents), liste avec les **noms** des rôles,
   rôles à cocher (les autorisations s'additionnent), bouton « Enregistrer les rôles » actif seulement après une
   modification. Au moins un rôle doit rester coché. Les **autorisations effectives** s'affichent par nom, regroupées
   par domaine traduit, dans une section repliable. Le compte connecté est en **lecture seule** (pas
   d'auto-élévation : la règle est appliquée par le serveur, l'écran ne fait que l'expliquer).
2. **Rôles** : un rôle système est un **modèle en lecture seule** (un seul indicateur « Lecture seule ») qu'on peut
   **dupliquer** ou remplacer par un rôle vide. Un rôle personnalisé se modifie : nom, code interne **proposé à partir
   du nom** (modifiable, même règle que le serveur `[A-Z][A-Z0-9_]{2,63}`), description, « peut être donné aux
   personnes », « rôle actif ». Les autorisations sont présentées par domaine repliable avec un compteur
   « cochées / total », par nom et description, **sans code technique**.
3. **Historique** : date au format `dd/MM/yyyy HH:mm`, changement en clair (« Rôle créé », « Rôles d'une personne
   modifiés »…), détails. Une action inconnue reste lisible telle quelle.

États : chargement (squelette), erreur traduite (+ précision du serveur quand l'écran est en français), liste vide,
recherche sans résultat, succès annoncé, saisie conservée en cas d'échec d'enregistrement. Textes FR/EN dans
`assets/i18n/features/rbac/`.

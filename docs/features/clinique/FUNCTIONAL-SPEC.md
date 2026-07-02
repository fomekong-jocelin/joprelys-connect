# Spécification Fonctionnelle — Gestion de la Clinique Pilote & Multi-tenant (STORY-0201)

## 1. Problème métier

Joprelys Connect est conçu pour être une plateforme multi-cliniques (multi-tenant de base). Chaque clinique pilote (ou cabinet médical) doit disposer de son propre espace étanche. Un utilisateur (médecin, pharmacien, etc.) d'une clinique ne doit en aucun cas pouvoir lire, modifier ou interagir avec les données (patients, ordonnances, visites) d'une autre clinique.

## 2. Objectif

Permettre à l'**administrateur Joprelys** de configurer et d'enregistrer les cliniques pilotes dans le système, d'isoler leurs données et d'associer les professionnels de santé à leur organisation respective.

## 3. Rôles et Autorisations

* **`ADMIN_JOPRELYS`** : 
  * Créer une clinique pilote (nom, adresse, e-mail de contact, téléphone, ville).
  * Activer ou désactiver une clinique.
  * Associer les comptes utilisateurs aux cliniques.
* **Tous les profils cliniques (`MEDECIN`, `PHARMACIEN`, `ASSISTANT`)** :
  * Travailler exclusivement dans l'espace de leur propre clinique (tenant).

## 4. Parcours Utilisateur

### 4.1 Enregistrement et Supervision (Admin)
* L'administrateur Joprelys accède à l'écran "Gestion des Cliniques".
* Il clique sur "Nouvelle Clinique", saisit les informations (ex: *Clinique de l'Espoir, Douala*) et valide.
* En mode création, l'écran affiche uniquement le formulaire de nouvelle clinique afin d'éviter de mélanger saisie et consultation de la liste.
* En mode consultation, l'écran affiche les cliniques enregistrées sans le formulaire.
* Un identifiant unique (`organization_id`) est généré pour cette clinique.
* L'administrateur peut ensuite affecter des professionnels de santé à cette clinique.

### 4.2 Connexion & Blocage Tenant
* À la connexion, le système vérifie le statut de la clinique associée à l'utilisateur :
  * Si la clinique est **active** (`ACTIVE`), la connexion se poursuit normalement.
  * Si la clinique est **désactivée** (`INACTIVE`), l'utilisateur se voit refuser l'accès avec un message d'erreur : *"Votre établissement est désactivé. Veuillez contacter le support."*

### 4.3 Isolation multi-tenant stricte
* Aucun utilisateur clinique ne peut forcer le chargement de données appartenant à une autre clinique (ex: en modifiant un ID dans une URL ou un corps de requête). Le filtrage est appliqué au niveau système.

## 5. Critères d'acceptation fonctionnels

1. **Création clinique** : Tous les champs obligatoires (nom, ville, e-mail, téléphone) doivent être saisis avec validation de format.
2. **Statut d'activité** : Le basculement du statut (actif/inactif) prend effet immédiatement à la prochaine tentative de connexion.
3. **Contrôle d'accès** : L'accès aux écrans d'administration des cliniques est strictement réservé au rôle `ADMIN_JOPRELYS`.
4. **Traductions** : Les écrans et formulaires doivent être entièrement traduits en français et anglais.
5. **Branding/Logo** : Le logo de la clinique est chargé et associé, puis exposé pour affichage futur sur les documents de prescription.
6. **Mobile-first** : Sur mobile, la liste des cliniques doit s'afficher sous forme de cartes lisibles avec action directe, sans tableau compressé ni dépendance à un scroll horizontal comme expérience principale.
7. **Shell applicatif** : Le logo, les informations de session utilisateur, l'action de déconnexion et le footer doivent rester présents lors de la navigation entre le dashboard et l'écran de gestion des cliniques.

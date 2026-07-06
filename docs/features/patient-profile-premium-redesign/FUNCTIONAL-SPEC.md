# Spécification Fonctionnelle — Refonte Premium du Profil Patient (Portail Patient)

## 1. Problème métier à résoudre
L'écran du profil patient (`/patient/profile`) est jugé trop austère, froid et peu esthétique ("allure de programme d'obsèques" selon les retours). La présentation actuelle sous forme de blocs blancs de texte brut, sans icône, sans hiérarchie visuelle ou éléments de design modernes, donne une impression de produit inachevé et ne suscite pas la confiance de l'utilisateur final.

## 2. Utilisateurs concernés
- Les patients accédant à leur propre espace patient pour consulter leurs informations d'identité, de coordonnées, de contact d'urgence et leurs données médicales de base (allergies, antécédents).

## 3. Objectif de la fonctionnalité
Améliorer l'esthétique générale de la page de profil patient pour atteindre un rendu moderne, haut de gamme ("premium"), et chaleureux, tout en respectant scrupuleusement la charte graphique et la règle des arrondis sobres (4px à 6px recommandés, 8px maximum) de `DESIGN.md`. L'interface doit comporter des indicateurs visuels qualitatifs (icônes, badges d'état, dégradés subtils, avatar d'initiale premium) pour faciliter la lecture des données.

## 4. Périmètre inclus / exclu
- **Inclus** :
  - Le template et les styles du composant `app-patient-profile-page` (`patient-profile-page.component.ts`).
  - L'ajout de clés de traduction i18n correspondantes dans `fr.json` et `en.json`.
- **Exclu** :
  - Modification des services de récupération des données patient (`PatientPortalService`).
  - Ajout de nouvelles fonctionnalités (comme l'édition du profil, qui n'est pas prévue dans le périmètre actuel).

## 5. Parcours utilisateur attendu
1. Le patient se connecte à son portail et clique sur son profil.
2. Il arrive sur une page moderne affichant une bannière d'en-tête premium avec un dégradé de marque, un grand avatar de son initiale, son numéro de DPU, son genre et sa date de naissance structurés par des icônes.
3. Il visualise ses coordonnées, ses contacts d'urgence et ses informations médicales présentés dans des cartes de design premium avec des icônes de section colorées, des fonds de champs adaptés et des effets de survol subtils.

## 6. Règles métier connues
- Toutes les données patient (Nom, DPU, Date de naissance, Genre, Groupe sanguin, Téléphone, E-mail, Adresse, Ville, Contact d'urgence, Allergies, Antécédents) doivent rester affichées au même endroit et avec le même niveau de détail.
- L'application doit gérer l'affichage bilingue (français/anglais) de l'ensemble de l'interface.

## 7. Critères d'acceptation
- L'interface utilise une bannière d'en-tête premium avec un fond dégradé subtil adapté aux modes clair/sombre.
- Un grand avatar circulaire stylisé affiche l'initiale du nom du patient, avec le groupe sanguin en badge superposé.
- Chaque section de données (Coordonnées, Contact d'urgence, Allergies, Antécédents) possède une icône SVG distinctive et colorée.
- Les données textuelles sont intégrées dans des blocs structurés (`.ui-card-muted`) pour un rendu "formulaire/fiche" plus moderne.
- Aucune chaîne de texte utilisateur n'est codée en dur (utilisation de l'i18n bilingue).
- Le build et les tests de l'application Angular passent avec succès.

# Spécification Fonctionnelle — Refonte Premium de la Page de Gestion des Doublons Patients

## 1. Contexte et Problématique

Dans l'application Joprelys Connect, l'administrateur clinique (`ADMIN_CLINIQUE`) gère les suspicions de doublons patients afin de garantir la qualité et l'unicité des dossiers médicaux partagés (DPU). 
L'ancienne interface de gestion des doublons (`/clinic/duplicates`) manquait d'intégration avec la charte graphique de l'application (pas d'utilisation du conteneur d'enveloppe globale `app-shell`, absence de titre de page standard `app-page-header`, look grisâtre et non premium). L'objectif est d'appliquer les principes du Design System (`DESIGN.md`) pour rendre cette page moderne, rassurante et alignée visuellement avec le reste de l'application.

## 2. Objectifs Fonctionnels

- **Cohérence Visuelle** : Intégrer l'écran dans l'enveloppe applicative globale avec le menu de navigation et l'en-tête de page standard.
- **Visualisation Facilitée** : Présenter les paires de doublons de manière claire (Patient Source A vs Patient Cible B), avec des badges de scores de similarité contrastés et colorés.
- **Aide à la Décision** : Mettre en valeur les différences d'attributs entre deux fiches (ex: différence de téléphone ou d'adresse) dans l'assistant de fusion grâce à des surbrillances colorées.
- **Sécurisation des Opérations** : Rappeler clairement les impacts du transfert de données lors de la fusion de dossiers.

## 3. Parcours Utilisateur

1. L'administrateur clinique accède à l'onglet "Gestion des doublons" depuis le menu latéral gauche.
2. La page s'affiche dans l'enveloppe `app-shell` avec un titre et un sous-titre officiels.
3. Si aucun doublon n'est détecté, un message d'état vide (`app-empty-state`) élégant et centré s'affiche.
4. Si des doublons sont suspectés, ils s'affichent sous forme de grille de cartes premium. Chaque carte indique le score de similarité et détaille en vis-à-vis le Patient A et le Patient B (Nom, date de naissance, téléphone, numéro unique DPU).
5. L'administrateur peut rejeter la suspicion ("Ignorer") ou lancer l'assistant de fusion ("Fusionner").
6. L'assistant de fusion s'ouvre sous forme de dialogue modal moderne. Il permet de sélectionner la fiche principale à conserver (`Primary`) et montre une table de comparaison side-by-side de tous les champs. Les lignes présentant des différences (ex: orthographe du nom, numéros différents) sont colorées en arrière-plan (jaune/orange désaturé) pour attirer l'attention.
7. Après confirmation, l'appel API est déclenché et la liste est rafraîchie.

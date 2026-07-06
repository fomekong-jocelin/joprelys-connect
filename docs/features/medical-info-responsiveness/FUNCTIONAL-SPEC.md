# Spécifications Fonctionnelles — Responsivité des En-têtes du Dossier Médical

## 1. Problème Métier
Sur les terminaux mobiles ou fenêtres de navigateur étroites, les en-têtes de section du dossier médical patient ("Allergies", "Antécédents", "Vaccinations") affichent le titre et le bouton d'action côte à côte. La largeur limitée provoque la compression du texte du titre, qui se retrouve scindé sur deux lignes ("ALLERGIES \n CONNUES" ou "ANTÉCÉDENTS \n MÉDICAUX"), ce qui nuit gravement à la lisibilité et à l'esthétique premium de l'application Joprelys Connect.

## 2. Objectifs
- Assurer une lisibilité parfaite des titres de section sur tous les écrans, y compris les mobiles (320px de largeur et plus).
- Éviter le retour à la ligne forcé des titres en raison du manque de place.
- Offrir une mise en page fluide et soignée sur tablette et bureau (côte à côte) et empilée proprement sur mobile.

## 3. Parcours Utilisateur et Rendu Visuel
- **Sur Mobile (largeur < 640px) :**
  - Le titre de la section (ex: "🛡️ ALLERGIES CONNUES") est affiché sur sa propre ligne.
  - Le bouton d'action (ex: "+ Ajouter une allergie") se positionne sur la ligne suivante, justifié à gauche (pas d'étirement artificiel en pleine largeur pour conserver la sobriété).
  - Un espacement cohérent de `8px` (`gap-2`) ou `12px` (`gap-3`) est appliqué entre le titre et le bouton.
- **Sur Tablette & Bureau (largeur >= 640px) :**
  - Le titre et le bouton restent côte à côte sur la même ligne (mise en page `flex justify-between items-center`).

## 4. Critères d'Acceptation
- **Pas de texte écrasé** : Les titres ne s'affichent jamais sur 2 lignes à cause de la présence du bouton sur la même ligne.
- **Sobriété des boutons** : Les boutons en mode empilé ont une largeur ajustée à leur contenu (`w-fit`), pas `w-full`.
- **Harmonie globale** : La même logique s'applique de manière cohérente sur les trois sections d'informations médicales et l'en-tête de gestion des stocks de pharmacie.

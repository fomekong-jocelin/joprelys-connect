# TASK-2205 — Clarifier les actions Avoir et Devis dans le détail facture

## Mode

Engineering / UI Angular — amélioration ergonomique ciblée.

## Besoin

Dans le panneau de détail d’une facture, les boutons et icônes liés aux avoirs et aux devis ressemblent trop à des informations de consultation. L’utilisateur doit distinguer immédiatement l’historique affiché des actions qui déclenchent une création ou une mutation financière.

## Critères d’acceptation

- [x] La zone Devis / Proformas est identifiée comme un historique, avec une action primaire libellée « Créer un devis ».
- [x] Le formulaire de devis est visuellement identifié comme un mode de création et ses champs ont des libellés persistants.
- [x] L’action « Créer un avoir » utilise une icône et une couleur dédiées aux actions financières exceptionnelles.
- [x] Le formulaire d’avoir explique l’impact de l’action et distingue clairement « Enregistrer l’avoir » de l’annulation.
- [x] Les boutons et champs utilisent les composants/tokens UI centralisés du thème avec padding, bordures, fonds et focus visibles.
- [x] Les boutons ont un type explicite, un état `aria-expanded` quand nécessaire et un libellé accessible.
- [x] Les traductions françaises et anglaises sont disponibles.
- [x] Aucun endpoint, contrat API ou règle métier backend n’est modifié.
- [x] Les lignes de devis restent contenues dans la largeur du panneau, y compris dans un tiroir latéral étroit.
- [x] L’action de suppression de ligne est visible, libellée et accessible au clavier.

## Plan d’action

- [x] Analyser le composant de détail facture et les tokens UI existants.
- [x] Modifier la hiérarchie visuelle des sections Devis et Avoir.
- [x] Ajouter les libellés i18n FR/EN et les attributs d’accessibilité.
- [x] Mettre à jour la documentation fonctionnelle et technique.
- [x] Exécuter le build Angular et la suite de tests disponibles.
- [ ] Vérifier visuellement light/dark et responsive sur le panneau latéral.
- [x] Corriger le débordement de la grille de saisie des devis dans le panneau latéral.
- [x] Rendre l’action de suppression de ligne visible et compréhensible.

## Estimation et qualité

- Estimation initiale : 0,5 jour senior frontend/UX santé.
- Reviewer recommandé : Lead Developer + Product/DAF.
- Tests attendus : compilation Angular, tests unitaires existants, vérification manuelle clavier/responsive/light-dark.
- Définition de fini : critères cochés, documentation et suivi mis à jour, preuves de vérification reportées.

## Risques / reste à faire

- Le composant contient encore d’anciens libellés métier hors de ce périmètre qui pourront être migrés dans une tâche i18n dédiée.
- Une validation complète des règles d’autorisation d’avoir reste couverte par le backend existant et hors de cette modification visuelle.
- Vérification visuelle navigateur du comportement dans le tiroir latéral, en light et dark, à réaliser dans une session disposant du navigateur intégré.

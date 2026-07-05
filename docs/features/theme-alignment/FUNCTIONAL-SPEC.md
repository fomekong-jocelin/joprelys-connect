# Spécifications Fonctionnelles — Alignement esthétique des contrôles de formulaires

## 1. Contexte & Problématique métier

Dans le cadre de l'amélioration de l'expérience utilisateur (UX) et de l'alignement avec la charte graphique de Joprelys Connect, il a été identifié que les contrôles de formulaire (champs de saisie, zones de texte, listes déroulantes, cases à cocher et boutons radio) présentaient des incohérences visuelles :
- Les angles des champs de saisie étaient trop arrondis (rayon de `12px` au lieu de `4px` préconisé dans le design system centralisé `DESIGN.md`).
- Les cases à cocher et les boutons radio n'utilisaient pas les couleurs thématiques d'accentuation en état sélectionné ou au survol.
- Certaines zones du back-office utilisaient des styles CSS ad-hoc, empêchant la personnalisation dynamique et centralisée (par exemple, lors du passage en mode sombre).

L'objectif est d'harmoniser ces éléments afin de garantir une interface professionnelle, sobre et cohérente pour les praticiens médicaux et les patients.

## 2. Objectifs d'utilisabilité & Accessibilité

- **Clarté visuelle** : Les boutons radio, cases à cocher et dropdowns doivent refléter clairement leur état (sélectionné, non sélectionné, survolé, désactivé).
- **Sobriété** : Conformément à la règle des arrondis sobres, le rayon de courbure des formulaires et des champs est ramené à `4px` (taille `sm` de notre design system) afin d'éviter un effet trop "iPhone" ou "jouet".
- **Focus visible** : Pour des raisons d'accessibilité numérique (WCAG AA), tout contrôle de formulaire en cours de navigation clavier doit afficher une bague de focus visible (`var(--focus-ring)`).
- **Mode sombre natif** : Les styles de fond et de bordure doivent s'adapter automatiquement aux thèmes `light` et `dark` via les variables CSS centralisées (`--bg-input`, `--border-input`, `--text-primary`).

## 3. Critères d'acceptation fonctionnels

1. **Champs de saisie (Inputs) & Zones de texte (Textareas)** :
   - Coins carrés ou très légèrement arrondis (rayon de `4px`).
   - Fond et bordure réagissant de manière fluide au survol et à l'activation.
   - État désactivé explicite (curseur interdit, opacité réduite et fond grisé).

2. **Listes déroulantes (Dropdowns / Selects)** :
   - Même apparence et rayon de courbure que les champs de saisie.
   - Flèche de sélection colorée avec le gris atténué de la charte.

3. **Cases à cocher (Checkboxes) & Boutons radio (Radios)** :
   - Fond et bordure par défaut alignés avec le thème.
   - Couleur de remplissage en bleu cyan (`var(--brand-primary)`) en état coché.
   - Bague de focus présente lors de la navigation au clavier.

4. **Homogénéité back-office** :
   - Tous les anciens formulaires à style ad-hoc (ex: dossier médical, hospitalisation, consentements, demandes d'accès) utilisent désormais la charte graphique harmonisée.

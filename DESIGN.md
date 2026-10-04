---
version: 1.0.0
name: Joprelys Connect Design System
description: Design system central de Joprelys Connect (Google/Material 3 & Tailwind CSS v4 CSS-first).
colors:
  primary: '#0b91b2'       # Cyan Médical (Light) / #22a8c8 (Dark)
  on-primary: '#ffffff'
  secondary: '#40556f'     # Slate Blue (Light) / #c6d2e1 (Dark)
  on-secondary: '#ffffff'
  background: '#f7fafc'    # Light Gray (Light) / #071124 (Dark)
  on-background: '#0a1d3d' # Brand Night
  surface: '#ffffff'       # White (Light) / #111c31 (Dark)
  on-surface: '#0a1d3d'
  outline: '#d8e5e8'       # Light border (Light) / #21314b (Dark)
  error: '#dc2626'
  on-error: '#ffffff'
typography:
  headline-lg:
    fontFamily: Montserrat, sans-serif
    fontSize: 32px
    fontWeight: 800
    lineHeight: 40px
  headline-md:
    fontFamily: Montserrat, sans-serif
    fontSize: 24px
    fontWeight: 700
    lineHeight: 32px
  body-lg:
    fontFamily: Inter, sans-serif
    fontSize: 18px
    fontWeight: 400
    lineHeight: 28px
  body-md:
    fontFamily: Inter, sans-serif
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
  body-sm:
    fontFamily: Inter, sans-serif
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
  label-lg:
    fontFamily: Inter, sans-serif
    fontSize: 14px
    fontWeight: 600
    lineHeight: 20px
  label-md:
    fontFamily: Inter, sans-serif
    fontSize: 12px
    fontWeight: 600
    lineHeight: 16px
  label-sm:
    fontFamily: Inter, sans-serif
    fontSize: 11px
    fontWeight: 600
    lineHeight: 16px
rounded:
  none: 0px
  xs: 2px
  sm: 4px
  md: 6px
  lg: 8px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
components:
  button-primary:
    backgroundColor: 'var(--brand-primary)'
    textColor: 'var(--on-primary)'
    typography: '{typography.label-lg}'
    rounded: '{rounded.sm}'
    padding: '{spacing.sm} {spacing.md}'
  card-default:
    backgroundColor: 'var(--app-surface)'
    textColor: 'var(--text-primary)'
    rounded: '{rounded.lg}'
    padding: '{spacing.lg}'
  input-default:
    backgroundColor: 'var(--bg-input)'
    textColor: 'var(--text-primary)'
    rounded: '{rounded.sm}'
    padding: '{spacing.sm} {spacing.md}'
---

## Overview

Joprelys Connect est un outil de santé destiné aux cliniques pilotes et aux patients (Unified Patient Record / DPU). L'interface doit véhiculer confiance, crédibilité médicale, et simplicité d'usage, tout en évitant de surcharger cognitivement les praticiens.

## Colors

Les couleurs s'appuient sur :
- **Brand Night (#0A1D3D)** : pour le texte principal, renvoie au sérieux institutionnel.
- **Brand Cyan (#0B91B2)** : pour les actions principales (primaire), renvoie au monde médical et de la santé.
- **Brand Green (#16A34A)** : pour les indicateurs de succès et de validité.
- **Thème Light / Dark** : géré de manière globale par la classe `html[data-theme='dark']` et les variables CSS centralisées dans `styles.css`.

## Typography

Le projet utilise deux familles de polices :
1. **Montserrat** : pour les titres importants (Headline) afin de conférer de l'autorité et du caractère.
2. **Inter** : pour les textes courants (Body, Labels, Formulaires) assurant une excellente lisibilité à petite taille.

## Layout

Grille responsive basée sur 12 colonnes standard (desktop) et comportement empilé (mobile) avec une largeur de page maximale de `1120px` centrée (`.app-container`).

## Elevation & Depth

Le projet utilise des bordures discrètes (`1px solid var(--app-border)`) combinées avec une ombre très légère (`var(--shadow-panel)`) pour séparer les plans, au lieu d'ombres portées massives ou colorées.

## Shapes

Conformément à la règle des arrondis sobres :
- Coins carrés ou très légèrement arrondis.
- Boutons et formulaires : `4px` (`rounded-sm`).
- Cartes / Fiche Patient / Modales : `8px` max (`rounded-lg`).
- **Strictement interdit** : les boutons ou inputs en forme de pilule (`rounded-full`) hors avatars circulaires ou petits badges d'état circulaires approuvés.

## Components

### Buttons

Les boutons sont définis dans `shared/ui/button.component.ts`. Ils héritent de la charte de couleurs dynamiques et ont un arrondi de `4px` à `6px`.

Le contenu d'un bouton standard forme une unité indivisible : icône et libellé restent sur une seule ligne. La règle globale est portée par `src/styles/button-layout.css` avec `white-space: nowrap`, `flex-shrink: 0` et une hauteur de ligne explicite. Quand l'espace manque, le conteneur d'actions fait revenir les boutons entiers à la ligne ou les empile ; il ne compresse pas le texte interne du bouton.

### Inputs

Les champs texte utilisent un fond gris clair `var(--bg-input)` avec bordure fine, basculant vers un fond sombre en mode dark, et un arrondi de `4px`.

### Cards

Les conteneurs de cartes utilisent la classe `.ui-card` avec `8px` d'arrondi et l'ombre légère centralisée.

### Entrée en consultation

L'entrée suit la maquette acceptée du 2026-10-04 : contexte patient et alertes conservés, étapes lisibles, trois choix de même niveau (manuel, dictée, conversation) à gauche et description/action du choix à droite. Les panneaux s'empilent sous le breakpoint desktop. Les choix utilisent des radios natives, un focus visible et un état sélectionné porté par les tokens centraux ; sélectionner un mode ne démarre aucun microphone. L'action principale annonce ce qui démarre et la disponibilité du microphone est explicite. Rayons des choix : token `radius-brand-md` (6px) ; panneaux : `.ui-card`, 8px maximum. Textes FR/EN, typographies et thèmes restent centralisés.

L'affichage du formulaire doit dépendre d'un état explicite partagé avec l'assistant, jamais d'une modification artificielle des champs. « Compte rendu validé » est réservé à l'acceptation explicite d'un rapport ; l'ouverture manuelle et le chargement d'une consultation affichent « Formulaire de consultation ».

### Confirmation dialogs

Les actions financières destructives utilisent une modale maison, jamais le `confirm()` du navigateur. La surface conserve un rayon sobre compris entre `4px` et `8px`, l’overlay est centralisé, le focus reste visible, la fermeture par Échap est disponible et l’action destructive est clairement distincte dans les thèmes light et dark.

### Structure hospitalière

La configuration Service → Chambre → Lit utilise une hiérarchie de panneaux à bordure fine, avec un rayon maximal de `6px` et les ombres légères du design system. Les actions de création restent au niveau de leur parent, les opérations destructives passent par la modale de confirmation partagée et les états de lits conservent les couleurs sémantiques communes aux thèmes light/dark.

### Connexion premium mobile-first

La page publique de connexion suit les règles suivantes :

- le composant partagé `app-logo` est l'unique source du logo et du nom Connect ;
- sur desktop, le bloc de marque doit être identifiable au premier regard : logo blanc sans carte, image comprise entre `68px` et `84px` de hauteur et nom Connect clairement aligné ;
- mobile d'abord : contrôles thème/langue, identité, choix Personnel/Patient puis formulaire ;
- à partir du breakpoint desktop, un panneau institutionnel complète le formulaire sans afficher de donnée clinique fictive ;
- le panneau desktop est une vitrine produit : une promesse claire, un résumé fonctionnel et trois bénéfices réels ;
- le titre de la vitrine respecte l'échelle `headline-lg` et ne dépasse pas `32px` ;
- le slogan officiel « Parce qu’elle est précieuse, nous innovons pour la protéger. » reste une signature secondaire et ne doit jamais devenir un hero title surdimensionné ;
- à partir de `760px` de hauteur desktop, la composition tient dans `100dvh` sans scroll ; sous ce seuil, le défilement reste autorisé pour préserver l'accès aux actions ;
- les illustrations isolées, barres de pagination décoratives, orbites et grands vides sans fonction sont interdits sur cet écran ;
- un pseudo-élément décoratif réutilisé pour du contenu doit réinitialiser explicitement `position`, `inset`, dimensions, bordure, rayon et `transform` afin d'éviter toute fuite de style héritée ;
- le sélecteur FR/EN utilise les drapeaux 🇫🇷 et 🇬🇧 avec un état actif accessible ;
- le changement light/dark reste disponible avant authentification via `ThemeService` ;
- le décor emploie seulement les tokens centraux avec `color-mix`, les ombres partagées et des rayons de 4 à 8 px ;
- aucune carte « Flux clinique synchronisé », aucun faux indicateur et aucun nouveau mode d'authentification ne doivent être introduits ;
- les étapes Personnel, OTP professionnel, Patient et OTP patient conservent la même hiérarchie visuelle et les mêmes contrats fonctionnels.
- le contenu de vitrine est du HTML réel alimenté par les catalogues FR/EN ; aucun texte visible ne doit être généré avec `content:` ou sélectionné selon la langue via `:has(...)` ;
- le logo posé sur le panneau institutionnel utilise explicitement l'apparence `on-dark` et l'asset officiel prévu pour ce fond, sans filtre d'inversion hérité du thème global.

### Authentification Flutter native

- la connexion Flutter reprend la hiérarchie mobile du web : thème compact à gauche, segment `FR | EN` à droite, marque, titre/sous-titre puis carte actionnable ;
- la carte d’authentification ne contient ni logo secondaire ni titre répété ;
- les libellés de champs restent visibles au-dessus des zones de saisie et les icônes sont purement indicatives ;
- l’asset officiel conserve son ratio : aucun logo ne doit être comprimé dans une boîte trop horizontale ;
- le bouton principal reste pleine largeur et mesure au moins `44px` ;
- les pages OTP, recovery et unlock utilisent le même shell et la même densité ;
- l’accueil professionnel n’expose aucun identifiant technique de ticket ou de build ;
- l’identité et la sécurité sont séparées en surfaces compactes ; la déconnexion destructive utilise une variante secondaire afin de ne pas dominer l’écran ;
- les réglages langue/thème ne sont jamais dupliqués dans le contenu lorsque l’en-tête les expose déjà ;
- les thèmes light/dark, les libellés FR/EN, les rayons de `4px` à `8px` et les ombres sobres restent obligatoires.

### Espacement clinique et actions d'admission

- Le conteneur de consultation utilise une colonne avec `gap-6` (`gap-8` dès sm) pour séparer les hôtes Angular, le bandeau patient et le workspace ; ne pas compter sur des marges verticales entre hôtes inline.
- La progression utilise une grille empilée sur mobile, trois colonnes dès sm, avec `py-4` / `sm:py-5` et `gap-3` / `sm:gap-6` ; les cartes de choix restent séparées par un gap explicite.
- Dans le drawer d'admission étroit, les actions se présentent en colonne pleine largeur, consultation en premier ; « Fermer » utilise une zone secondaire séparée. Chaque bouton a une cible d'au moins 44px ; header/footer ne rétrécissent pas, seul le corps défile.
- Ces dispositions utilisent l'échelle Tailwind v4 et les tokens existants ; aucune palette ou rayon propre à l'écran n'est ajoutée.

### Interfaces IA et assistant de constantes

- mobile d'abord : sous `640px`, l'assistant de constantes démarre replié et ne monopolise pas la modale ;
- l'explication et l'exemple de dictée utilisent une divulgation progressive ; la tâche principale reste la saisie ou la validation des constantes ;
- une surface IA mobile autonome est bornée par `dvh`, défilable intérieurement et conserve les actions dans la largeur utile ; lorsqu'elle est intégrée à la modale de constantes, le corps de la modale est l'unique zone de défilement et l'en-tête/footer restent visibles ;
- les actions tactiles mesurent au moins `44px` et s'empilent en pleine largeur lorsque l'espace est contraint ;
- replier ou détruire l'assistant coupe le microphone et la session Realtime ;
- une proposition IA ne modifie que le brouillon visible ; l'enregistrement clinique reste une action explicite du professionnel ;
- les composants IA utilisent les clés i18n de feature et les libellés partagés, en français et en anglais ;
- la saisie manuelle est le mode initial ; sélectionner Dictée ou Écoute continue ne démarre pas le microphone. Les trois choix exposent leur usage ; le démarrage vocal utilise un CTA séparé avec état du microphone ;
- la phrase à analyser est une aide optionnelle, dans un textarea pleine largeur avec label et bouton distincts, jamais comprimée entre deux actions vocales ;
- « Reporter dans le formulaire » et « Enregistrer les constantes » sont deux actions distinctes. Le bouton final reste visible et attend la fin de la capture/analyse ;
- la modale conserve un en-tête patient unique, des labels associés aux champs, un focus initial, un confinement du focus et une fermeture Échap ; largeur desktop maximale `max-w-3xl`, plein écran sous `640px`, rayons et ombres centraux.

### Notes cliniques SOAP cross-stack

- Angular et Flutter utilisent la même hiérarchie `S — Subjectif`, `O — Objectif`,
  `A — Évaluation`, `P — Plan`, dans cet ordre ;
- la parité porte sur les champs, libellés, validations et états, pas sur une copie
  pixel à pixel : Angular peut utiliser une grille desktop et Flutter une feuille
  native empilée ;
- l'évaluation expose un seul champ `Diagnostic` ; le niveau de certitude reste
  exprimé dans le texte clinique sans créer de statuts artificiels dans l'UI ;
- le plan distingue synthèse, conseils et suivi ; prescriptions, examens et
  constantes restent des ressources structurées et ne sont pas remplacés par un
  champ texte libre ;
- les champs utilisent les rayons sobres du design system, les thèmes light/dark,
  les libellés FR/EN et une limite visible cohérente avec le contrat backend ;
- l'action d'enregistrement reste explicite et les erreurs `204`, `400`, `403` et
  `404` conservent leur sens métier sur desktop comme sur mobile.
- l'intégration dans une modale Angular reste déclarative, sans `querySelector`, mutation de classes ni montage dynamique pour piloter le layout.

#### Surface d’écoute vocale unifiée

- Dictée et Realtime utilisent la même surface dans Consultation et Constantes.
- La structure obligatoire est : badge d’activité en haut à gauche, action
  d’arrêt en haut à droite, microphone circulaire central, halos concentriques,
  ondes bleues, message d’écoute centré puis conseil contextuel.
- La carte utilise `--app-surface`, `--app-border`, `--brand-primary`,
  `--brand-primary-subtle`, `--text-primary`, `--text-muted` et
  `--shadow-panel` ; aucune palette locale n’est autorisée.
- Le rayon de la carte reste limité à `--radius-brand-lg` (8 px). Les cercles
  sont autorisés uniquement pour le microphone, ses halos et les indicateurs
  d’état.
- Les dimensions et l’ordre des éléments restent identiques pour les quatre
  parcours ; seul le conseil Consultation/Constantes peut varier.
- Le composant est purement présentationnel et émet une intention d’arrêt. Les
  moteurs Dictée et Realtime conservent leurs contrôleurs distincts.
- L’historique de transcription reste une surface séparée, bornée et scrollable
  afin de préserver la stabilité de la carte d’écoute.
- Sous `640px`, le header reste sur une ligne, l’action d’arrêt mesure au moins
  44 px et le visualiseur se réduit sans débordement.
- `prefers-reduced-motion: reduce` neutralise les pulsations décoratives.

#### Conversation, correction et finalisation

- La Dictée est visuellement et vocalement passive. Le Realtime peut restituer
  une question clinique courte par un canal TTS unique ; il ne lit jamais une
  instruction interne.
- La question reste aussi visible. La reprise de parole interrompt la lecture
  sans changer l’état visuel actif du microphone.
- Une transcription à relire utilise le composant d’édition partagé avec
  `ui-input`, bordure fine et rayon `--radius-brand-sm`. Les actions « Corriger
  et analyser » et « Écarter » restent accessibles sur mobile.
- Pour les constantes, la dernière phrase entendue est un `textarea` éditable ;
  la proposition chiffrée demeure séparée et ne s’applique qu’après validation.
- Après « Terminer », la carte conserve sa place et affiche l’état de
  finalisation tant que des phrases, corrections ou décisions restent en
  attente. Aucun flash, effacement de transcript ou changement prématuré de
  mode n’est autorisé.

### Disponibilités médecin (STORY-2602)

La page « Mes disponibilités » (`clinic/availability`) introduit la grille hebdomadaire `shared/ui/weekly-availability-grid` (7 colonnes Lun → Dim, défilement horizontal sur mobile) et l'aperçu des créneaux. Les plages horaires sont des cartes compactes à rayon sobre (≤ `8px`, tokens `--radius-brand-*`) avec ombre légère `var(--shadow-panel)` ; le jour sélectionné est souligné par `var(--brand-primary)` et les plages désactivées sont atténuées (`opacity-50`). Toutes les couleurs passent par les tokens centralisés (`--app-surface`, `--app-border`, `--brand-primary`, `--text-*`) — aucune couleur en dur, thèmes light/dark automatiques. La désactivation d'une plage et la suppression d'une indisponibilité passent par la modale de confirmation partagée.

### Dossier patient progressif mobile-first

- La route `Fiche d’identité` est la surface unique de l’identité : elle affiche directement les informations administratives utiles et ne crée pas un second accordéon « Informations administratives ».
- Les informations secondaires utilisent une divulgation progressive avec des titres courts : `Contexte d’urgence`, `Informations médicales`, `Contact d’urgence`.
- Les titres d’accordéons restent sur une ligne à partir de `360px` ; réduire ou clarifier le libellé est préféré à diminuer excessivement la taille de police.
- Les informations de sécurité critiques (allergies critiques, Break-Glass, urgence active) ne sont jamais masquées pour gagner de la place.
- Sur mobile, l’action clinique principale (`Ouvrir une visite` ou `Démarrer la consultation`) occupe une ligne pleine largeur avant les actions secondaires. `Synthèse PDF` et `Retour` partagent ensuite une ligne lorsque les deux sont disponibles.
- Sur desktop, le même groupe reste compact et aligné ; l’ordre visuel conserve une action primaire clairement identifiable.
- Les libellés de boutons restent indivisibles et les cibles tactiles mesurent au moins `44px`.

## Do's and Don'ts

### Panel administrateur — correction du 2026-10-04

Les champs requis du formulaire d'établissement utilisent `shared/ui/InputComponent` : label lié à un identifiant stable, bordure et message via `--brand-danger`/`--brand-danger-text`, `aria-invalid` et `aria-describedby`. La validation de saisie aide l'utilisateur ; la validation serveur reste maîtresse. Le type d'établissement possède une explication FR/EN indiquant que les autorisations déterminent l'accès aux modules.

Les cartes dashboard/interop utilisent les rayons centraux `--radius-brand-md` et les surfaces/ombres existantes light/dark (maximum 8px). Téléversement, breadcrumbs et erreurs restent dans les dictionnaires FR/EN. Les sections prescription, constantes et analyses de la consultation sont des panneaux partagés de présentation ; le découpage conserve les tokens et la disposition existants.

### Do
- Toujours utiliser les variables CSS centrales de `styles.css`.
- Respecter les contrastes légaux en mode light et dark.
- Maintenir l'internationalisation FR/EN de chaque libellé.
- Laisser le conteneur responsive disposer les boutons sans couper leur libellé.

### Don't
- **Interdiction d'importer `@angular/material`** ou d'utiliser ses directives.
- Interdiction de Tailwind v3 ou d'utiliser un fichier de configuration externe `tailwind.config.js`.
- Ne pas utiliser d'arrondis supérieurs à `8px` sur les cartes et boutons.
- Ne pas réautoriser le retour à la ligne interne d'un bouton pour masquer un défaut de layout.


## Parcours hospitalier — correction du 2026-10-04

Consentements et compte-rendu opératoire sont des panneaux Angular dédiés. Les cartes/formulaires de ce périmètre réutilisent `--radius-brand-sm` (4px à 6px), avec bordures et ombres existantes légères ; aucun arrondi supérieur à 8px. Les surfaces/textes/états réutilisent les tokens centraux light/dark. Les libellés et erreurs sont centralisés dans les catalogues `features/hospital-continuity/{fr,en}.json`.

Chaque lecture clinique distingue chargement, erreur et absence de données. L'erreur offre une reprise. Les formulaires désactivent la soumission pendant la requête ; la validation CRO utilise ConfirmationDialogComponent. L'administration médicamenteuse propose une ligne de prescription validée au lieu d'un nom libre. La recette visuelle multi-thèmes/langues reste ouverte et ne se déduit pas des tests unitaires.

Complément signature PNG (2026-10-04) : uploader partagé conservé sur profil et fiche médecin, PNG/JPEG uniquement et aide FR/EN centralisée invitant à enregistrer les modifications ; PNG transparent recommandé. Aucun style local ni changement de tokens/thèmes.

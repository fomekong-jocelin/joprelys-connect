# STORY-1910 — Spécification fonctionnelle

## Portails patient, pro, labo, pharmacie et vérification publique conformes CDC

---

## 1. Problème métier

La section 13 du cahier des charges définit des parcours et écrans dédiés pour chaque portail. Le frontend actuel couvre les fonctionnalités principales mais présente des écarts :

- plusieurs écrans attendus sont absents ;
- certains composants dépassent les limites de taille imposées ;
- les traductions sont embarquées en ligne dans le code ;
- le titre de l’application est figé dans `index.html` ;
- quelques textes utilisateur restent codés en dur.

## 2. Objectif

Aligner le frontend Angular sur les attentes du CDC section 13 tout en respectant les standards du projet :

- Tailwind CSS v4 CSS-first ;
- design system centralisé (`DESIGN.md`) ;
- thèmes light/dark ;
- i18n FR/EN externalisée ;
- composants partagés `shared/ui` ;
- arrondis sobres ≤ 8 px ;
- composants ≤ 500 lignes.

## 3. Périmètre

### 3.1 Inclus

- **Portail patient (CDC 13.1)** : création des pages `/patient/profile`, `/patient/documents`, `/patient/qr-code`, `/patient/privacy` ; les pages `/patient/summary`, `/patient/prescriptions`, `/patient/results` existent déjà et seront harmonisées.
- **Portail professionnel (CDC 13.2)** : création de `/clinic/documents` et `/patients/:id/history` ; refactor de `consultation.component.ts` pour extraire la prescription et la demande d’examen.
- **Portail labo (CDC 13.3)** : création de `/lab/dashboard`, `/lab/orders/:id`, écran de validation de résultat et écran d’envoi PDF.
- **Portail pharmacie (CDC 13.4)** : page de détail d’ordonnance dédiée, liaison stocks ↔ vérification, vocabulaire "délivrance partielle / totale".
- **Vérification publique (CDC 13.6)** : page de saisie du numéro de document existante, ajout d’un parcours de demande d’accès au dossier.
- **Standards transverses** : externalisation i18n, utilisation systématique de `shared/ui`, arrondis, titre dynamique.

### 3.2 Exclus

- Refonte complète du design system (les tokens existants sont conservés).
- Modification des contrats backend (STORY-1901 à STORY-1909 doivent être livrées).
- Développement mobile Flutter (hors périmètre de ce ticket).

## 4. Utilisateurs concernés

- Patient connecté (`PATIENT`).
- Personnel clinique (`MEDECIN`, `INFIRMIER`, `AGENT_ACCUEIL`, `ADMIN_CLINIQUE`).
- Biologiste (`BIOLOGISTE`).
- Pharmacien (`PHARMACIEN`).
- Public non authentifié (vérification de document).

## 5. Parcours utilisateur

### 5.1 Patient

1. Connexion via le portail patient.
2. Accès aux rubriques : profil, synthèse, documents, ordonnances, résultats, QR code temporaire, confidentialité.
3. Navigation via la sidebar contextuelle du portail patient.

### 5.2 Professionnel de santé

1. Connexion au back-office.
2. Depuis le dossier patient, accès à l’historique dédié.
3. Depuis le menu clinique, accès aux documents générés.
4. Lors d’une consultation, saisie de la prescription et/ou de la demande d’examen dans des sous-composants dédiés.

### 5.3 Biologiste

1. Connexion au portail labo.
2. Vue d’ensemble du dashboard, accès à une demande, saisie/validation des résultats, envoi du PDF.

### 5.4 Pharmacien

1. Vérification d’une ordonnance par numéro + PIN.
2. Affichage du détail complet de l’ordonnance.
3. Indication des stocks disponibles et délivrance partielle ou totale.

### 5.5 Public

1. Saisie du numéro de document sur `/verify`.
2. Visualisation des métadonnées de vérification.
3. Option "Demander l’accès au dossier" pour initier une demande d’accès externe (si le backend l’expose).

## 6. Règles métier

- Tout texte visible doit être traduit en FR et EN.
- Le nom de l’application et les liens publics doivent provenir de `APP_BRAND_CONFIG`.
- Les arrondis des cards, formulaires, inputs et boutons ne doivent pas dépasser 8 px.
- Aucun composant ne doit dépasser 500 lignes ; alerte dès 300 lignes.
- Les appels API doivent utiliser des chemins relatifs (`/api/...`).

## 7. Critères d’acceptation

Voir le ticket `docs/ai/tickets/STORY-1910-portails-frontend-cdc.md` pour la checklist complète.

## 8. Hypothèses et dépendances

- Les endpoints backend des stories 1901 à 1909 sont disponibles.
- Le design system actuel (`DESIGN.md`) reste la source de vérité.
- Angular 22 / Tailwind CSS v4 sont maintenus.

## 9. Cas limites

- Patient sans données : affichage d’un état vide traduit.
- Document non trouvé en vérification publique : message d’erreur traduit.
- Rôle inconnu ou non autorisé : redirection vers `/unauthorized`.

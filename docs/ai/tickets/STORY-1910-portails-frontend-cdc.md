# STORY-1910 — Alignement Frontend : Portails patient, pro, labo, pharmacie et vérification publique

| Champ | Valeur |
|---|---|
| **ID** | STORY-1910 |
| **Epic** | EPIC-0014 |
| **Type** | User Story |
| **Titre** | Alignement Frontend — Portails patient, pro, labo, pharmacie et vérification publique conformes CDC |
| **Statut** | READY |
| **Priorité** | P0 |
| **Stack** | Frontend |
| **Profil recommandé** | Senior Frontend |
| **Estimation Senior** | 2.0j |
| **Estimation Intermédiaire** | 3.0j |
| **Estimation Junior** | 5.0j |
| **Sprint cible** | SPRINT-0011 |
| **Assigné** | À assigner |
| **Reviewer** | Lead Developer |
| **Dernière MAJ** | 2026-07-05 |

---

## 1. Contexte

Le CDC section 13 définit des écrans précis pour chaque portail. Le frontend actuel couvre les fonctionnalités principales mais manque plusieurs écrans dédiés et présente des violations des standards design (taille des composants, arrondis, i18n, textes en dur).

---

## 2. Critères d’acceptation

### Portail patient (CDC 13.1)

- [ ] Page "Mon profil" dédiée (`/patient/profile`).
- [ ] Page "Ma synthèse médicale" (`/patient/summary`).
- [ ] Page "Mes documents" (`/patient/documents`).
- [ ] Page "Mes ordonnances" dédiée (`/patient/prescriptions`).
- [ ] Page "Mes résultats" (`/patient/results`).
- [ ] Page "QR code temporaire" (`/patient/qr-code`).
- [ ] Page "Paramètres de confidentialité" (`/patient/privacy`).

### Portail professionnel (CDC 13.2)

- [ ] Page "Documents générés" (`/clinic/documents`).
- [ ] Page "Historique patient" dédiée (`/patients/{id}/history`).
- [ ] Refactorer `consultation.component.ts` (> 500 lignes) en extraire prescription et demande d’examen.

### Portail labo (CDC 13.3)

- [ ] Dashboard labo (`/lab/dashboard`).
- [ ] Écran de détail d’une demande (`/lab/orders/{id}`).
- [ ] Écran de validation de résultat dédié.
- [ ] Écran d’envoi PDF.

### Portail pharmacie (CDC 13.4)

- [ ] Page de détail d’ordonnance dédiée.
- [ ] Liaison stocks ↔ vérification (indisponibilité partielle).
- [ ] Vocabulaire "délivrance partielle / totale" explicite.

### Vérification publique (CDC 13.6)

- [ ] Page de saisie du numéro de document (`/verify`).
- [ ] Page de demande d’accès au dossier depuis la vérification.

### Standards transverses

- [ ] Tous les textes visibles internationalisés (FR/EN).
- [ ] Utilisation systématique des composants `shared/ui`.
- [ ] Arrondis ≤ 8px sur cards, formulaires, inputs, boutons.
- [ ] Aucun composant > 500 lignes ; alerte si > 300 lignes.
- [ ] `<title>` dynamique avec `APP_BRAND_CONFIG.appName`.

---

## 3. Tâches techniques

1. Créer les nouvelles pages et routes Angular.
2. Refactorer `consultation.component.ts` et `app-shell.component.ts`.
3. Externaliser le dictionnaire i18n vers `assets/i18n/fr.json` / `en.json`.
4. Uniformiser les arrondis et les couleurs via les tokens CSS.
5. Remplacer les inputs/boutons natifs par `<app-ui-input>`, `<app-ui-button>`, `<app-ui-card>`.
6. Mettre à jour `index.html` pour le titre dynamique.
7. Tests unitaires et build production.

---

## 4. Fichiers impactés

- `web/src/app/app.routes.ts`
- `web/src/app/app.config.ts`
- `web/src/app/index.html`
- `web/src/app/shared/layout/app-shell.component.ts`
- `web/src/app/core/i18n/i18n.service.ts`
- `web/src/app/core/config/app-brand.config.ts`
- `web/src/app/consultation/consultation.component.ts`
- `web/src/app/patient/portal/pages/*` (nouveaux)
- `web/src/app/clinic/lab/*`
- `web/src/app/pharmacy/*`
- `web/src/app/styles.css`
- `web/src/assets/i18n/fr.json` (nouveau)
- `web/src/assets/i18n/en.json` (nouveau)

---

## 5. Tests attendus

- [ ] `npm run lint` sans erreur.
- [ ] `npm run test` passant.
- [ ] `npm run build` passant.
- [ ] Tests unitaires pour chaque nouvelle page.

---

## 6. Dépendances

- STORY-1901 à STORY-1909 pour les données backend nécessaires.

---

## 7. Risques

- Taille du refactor frontend.
- Risque de régression visuelle sur les écrans existants.

---

## 8. Impact version / SemVer

- Bump : **MINOR** (0.10.0).

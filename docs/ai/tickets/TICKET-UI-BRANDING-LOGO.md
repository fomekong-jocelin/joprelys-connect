# TICKET-UI-BRANDING-LOGO — Remplacement du logo Joprelys Connect par le nouveau logo

## 1. Objectif
Remplacer l'ancien logo SVG de Joprelys Connect par le nouveau logo officiel fourni dans le kit de marque `joprelys_brand_kit_final_nouveau_logo_complet`. Comme le nouveau logo comporte déjà le texte "Joprelys", le libellé textuel adjacent au logo doit uniquement afficher "Connect".

## 2. Critères d'acceptation
- [x] Les fichiers du nouveau logo (images PNG, icônes) sont copiés dans `web/src/assets/branding/`.
- [x] Le favicon de l'application est mis à jour avec le nouveau favicon officiel.
- [x] Le composant `AppLogoComponent` est mis à jour pour charger l'image du nouveau logo et afficher uniquement "Connect" (la partie textuelle "Joprelys" étant déjà incluse dans l'image du logo).
- [x] Les pages de login (personnel et patient) et la page de mot de passe oublié sont mises à jour pour utiliser `AppLogoComponent` au lieu de l'ancienne icône SVG et du texte codé en dur "JoprelysConnect".
- [x] La charte de couleurs du design system central (couleurs du thème bleu et vert) est alignée avec les couleurs du nouveau kit de marque (`#001D4A` et `#009730`).

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | |
| User story parent | |
| Sprint cible | SPRINT-0011 |
| Priorité business | P1 |
| Complexité | S |
| Story points | 2 |
| Profil recommandé | Intermédiaire / Senior |
| Effort estimé senior | 2h |
| Effort estimé intermédiaire | 4h |
| Effort estimé junior | 8h |
| Responsable | Antigravity |
| Reviewer obligatoire | User |
| Risque fonctionnel | Faible |
| Risque technique | Faible |
| Dépendances | Aucun |
| Bloquants connus | Aucun |

## 4. Contexte analysé
- [x] `AGENTS.md` lu
- [x] `SKILL.md` lu
- [x] `README-IA.md` lu
- [x] `WORKFLOW-IA.md` lu
- [x] `PROJECT-TRACKING.md` lu
- [x] `CHANGELOG.md` lu
- [x] `review-checklist.md` lu
- [x] Code existant analysé
- [x] Fichiers du kit de marque analysés (`joprelys_brand_kit_final_nouveau_logo_complet`)

## 5. Hypothèses
- Le nouveau logo principal `01_logo_principal_couleurs_pleines_transparent.png` est utilisé comme logo principal de l'application.
- L'icône de marque `12_icone_joprelys_transparent.png` est copiée sous le nom `logo_icon.png` pour des usages futurs ou secondaires.

## 6. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Logo trop grand ou mal proportionné | Affichage cassé dans la barre de navigation ou sur mobile | Ajustement des classes de hauteur et largeur CSS |

## 7. Action plan
- [x] Créer le répertoire `web/src/assets/branding` s'il n'existe pas.
- [x] Copier le nouveau logo principal, l'icône et le favicon dans le projet.
- [x] Mettre à jour `AppLogoComponent` pour afficher le nouveau logo et n'ajouter que "Connect".
- [x] Remplacer les SVGs et textes codés en dur dans `login.component.html`, `forgot-password.component.ts`, `patient-login.component.ts`.
- [x] Mettre à jour les styles et les variables CSS dans `styles.css` (et potentiellement `DESIGN.md`) avec les nouvelles couleurs (`#001D4A` et `#009730`).
- [x] Valider l'affichage de l'application (build Angular complet et tests).
- [x] Mettre à jour `CHANGELOG.md` et `PROJECT-TRACKING.md`.

## 8. Implémentation réalisée
- Copie des ressources : `logo_principal.png` (transparent), `logo_icon.png`, `logo_white_blue_bg.png` créés dans `web/src/assets/branding/`. Remplacement de `web/public/favicon.ico`.
- Refactorisation de `AppLogoComponent` pour utiliser la nouvelle ressource image et adapter l'affichage textuel (seul le suffixe "Connect" est conservé à côté de l'image). Ajout d'une propriété `size` pour gérer les déclinaisons `md` et `lg`.
- Unification des pages de login : `login.component.html`, `forgot-password.component.ts` et `patient-login.component.ts` utilisent désormais `AppLogoComponent` au lieu des SVGs et textes inline.
- Alignement du design system : mise à jour des tokens `--color-brand-night`, `--color-brand-green`, `--text-primary` et `--brand-success` dans `web/src/styles.css` pour correspondre aux spécifications du guide de marque.

## 9. Suivi d'exécution

| Date | Développeur | Temps passé | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---:|---:|---|---|
| 2026-07-06 | Antigravity | 0.1j | 100% | Aucun | Aucun | Intégration complète du nouveau logo et tests OK |

## 10. Tests et vérifications

### Commandes exécutées ou à exécuter

```bash
# Angular build status
npm run build # OK

# Angular tests status
npm run test # OK (79 tests au vert, 15/15 fichiers)
```

### Résultats

- [x] Tests unitaires OK
- [x] Tests intégration OK (N/A)
- [x] Tests UI/widget OK
- [x] Tests sécurité OK
- [x] Build OK
- [x] Analyse statique OK

## 11. Documentation

- [ ] README mis à jour si nécessaire
- [ ] API docs mises à jour si nécessaire
- [ ] ADR créé si décision structurante
- [x] Changelog mis à jour
- [x] Suivi projet mis à jour

## 12. Reste à faire
Aucun.

## 13. Statut final
Statut : DONE

## 14. Notes finales
Tous les anciens logos SVG inline ont été remplacés par le composant centralisé `AppLogoComponent`. Le favicon de l'application et les variables CSS de couleur ont été harmonisés.

## 15. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Oui |
| Type de bump | PATCH |
| Justification | Remplacement de ressources de marque (logos et favicon) et harmonisation des couleurs sans changement de comportement fonctionnel. |
| Breaking change | Non |
| Migration DB | Non |
| Changement API | Non |
| Impact Angular | Oui |
| Impact Flutter | Non |
| Changelog requis | Oui |
| Release note requise | Non |

## 16. Impact thème / i18n / branding
- [x] Impact Angular UI analysé
- [x] Thème centralisé vérifié
- [x] Thèmes light/dark vérifiés
- [x] Configuration app/branding vérifiée : nom, logo, slogan
- [x] Aucun texte ou branding hardcodé prévu

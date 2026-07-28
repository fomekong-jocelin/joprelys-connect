# BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG — Correctifs UX Calendrier Disponibilités (Mobile-First, Jour vs Date, Route API 404)

## Metadata
- **ID** : `BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG`
- **Epic** : `EPIC-0025` / `DOCTOR_AVAILABILITY_APPOINTMENTS`
- **Composants** : Angular (`shared/ui/weekly-availability-grid`, `clinic/availability`), Spring Boot (`AvailabilityController`)
- **Statut** : `DONE`
- **Priorité** : P0 (Bloquant utilisateur sur la gestion des créneaux de consultation)
- **Profil recommandé** : Senior Full-Stack (Angular / Spring Boot)

## Diagnostic des anomalies signalées

1. **Erreur 404 lors de l'enregistrement d'une plage (`POST /api/availabilities`)** :
   - En environnement de recette (`https://recette.joprelys.com/api/availabilities`), des variations de routage proxy/Nginx avec ou sans slash final peuvent provoquer un retour 404 sous Spring Boot 3 si le path matcher n'accepte pas explicitement le slash optionnel.
   - **Solution** : Étendre le mapping du controller backend `@RequestMapping({"/api/availabilities", "/api/availabilities/"})` et sur les sous-endpoints.

2. **Désalignement jour / date dans la modale (Sélection d'un mardi → Formulaire pré-remplit "Lundi")** :
   - Cause : `onCalendarRangeSelected` et `onWeekdaySelected` appelaient `resetRuleForm()` qui réinitialisait `formWeekday` à `1` (Lundi). En outre, la balise `<option [value]="day">` dans le `<select>` Angular n'avait pas l'attribut `[selected]="day === formWeekday()"`, causant un désalignement entre l'état interne de la date (`2026-07-31` = Vendredi) et la sélection affichée (`1` = Lun.).
   - **Solution** : Suppression des `resetRuleForm()` parasites dans la chaîne d'événements, synchronisation bidirectionnelle automatique entre la date de début (`formValidFrom`) et le jour de semaine (`formWeekday`), et ajout de `[selected]="day === formWeekday()"` sur le `<select>`.

3. **Grille de calendrier non Mobile-First & Débordement / Scroll Horizontal Forcé** :
   - Cause : `WeeklyAvailabilityGridComponent` ne restreignait pas le `min-w-0` et le bandeau d'onglets mobile provoquait un élargissement du conteneur grid/card parent au-delà de la largeur de l'écran (360px).
   - **Solution** : Refonte responsive Mobile-First :
     - **Sous 768px (Mobile)** : Hôte `@Component` et conteneurs parent configurés avec `w-full min-w-0 max-w-full`. Bandeau d'onglets jours avec défilement horizontal fluide et `shrink-0`. Vue timeline verticale avec gros boutons tactiles (min 38-44px), liste des cartes de plages et raccourcis de création rapide ("Matin 08h-12h", "A-M 14h-18h", "Jour 08h-17h"). Modales adaptées avec `min-w-0` et padding `p-3 sm:p-4`.
     - **À partir de 768px (Desktop)** : Grille hebdomadaire 7 colonnes complète conservée dans un conteneur responsive.

4. **Ergonomie UI/UX Unifiée, i18n FR/EN à 100% & Épurage du texte d'aperçu** :
   - **Bouton & Modale d'ajout unifiée** : Remplacement des 2 boutons d'en-tête redondants par un seul bouton principal `+ Ajouter`. La modale intègre en haut un sélecteur d'enregistrement sous forme de boutons radio (`Disponibilité (Plage de consultation)` vs `Indisponibilité (Absence / Congé)`), basculant dynamiquement les champs requis sans encombrer la vue.
   - **Internationalisation FR/EN 100%** : Suppression de toutes les chaînes en français codées en dur dans le template `WeeklyAvailabilityGridComponent` (onglets, boutons d'action "Modifier", "Désactiver", raccourcis rapides "Matin", "A-M", "Jour", "Personnalisé..."), déportées vers les fichiers i18n `features/availability/{fr,en}.json`.
   - **Épurage de l'aperçu des créneaux** : Suppression du paragraphe explicatif technique (règle de calcul des 7 jours et durée de créneau) dans le panneau déroulant des créneaux libres pour ne conserver que l'affichage utile.

## Actions à réaliser

- [x] Rédiger le ticket de suivi `docs/ai/tickets/BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG.md`
- [x] Sécuriser `AvailabilityController.java` pour accepter les variantes d'URL avec/sans trailing slash
- [x] Refondre `WeeklyAvailabilityGridComponent` pour supporter la vue jour par jour mobile-first sous 768px sans débordement horizontal
- [x] Corriger `AvailabilityPageComponent` et son template pour aligner strictement le jour et la date et appliquer les contraintes `min-w-0 max-w-full`
- [x] Vérifier la suite de tests backend (`AvailabilityControllerTest`) et les tests unitaires Angular (`weekly-availability-grid.component.spec.ts`, `availability-page.component.spec.ts`)
- [x] Mettre à jour la documentation fonctionnelle `docs/features/doctor-availability-appointments/FUNCTIONAL-SPEC.md` et technique `TECHNICAL-DESIGN.md`
- [x] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Suivi des régressions & règles de design
- Conforme à `DESIGN.md` et `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` (arrondis ≤ 8px, ombre légère, tokens `--brand-primary`, `--brand-danger`, `--app-surface`).
- Zéro dépendance externe lourde (Full Angular native signals & Tailwind v4).
- Respect du thème Light / Dark et i18n FR/EN.


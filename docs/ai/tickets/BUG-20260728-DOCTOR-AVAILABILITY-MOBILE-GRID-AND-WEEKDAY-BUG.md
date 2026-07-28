# BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG — Correctifs UX Calendrier Disponibilités (Mobile-First, Jour vs Date, Route API 404)

## Metadata
- **ID** : `BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG`
- **Epic** : `EPIC-0025` / `DOCTOR_AVAILABILITY_APPOINTMENTS`
- **Composants** : Angular (`shared/ui/weekly-availability-grid`, `clinic/availability`), Spring Boot (`AvailabilityController`)
- **Statut** : `IN_PROGRESS`
- **Priorité** : P0 (Bloquant utilisateur sur la gestion des créneaux de consultation)
- **Profil recommandé** : Senior Full-Stack (Angular / Spring Boot)

## Diagnostic des anomalies signalées

1. **Erreur 404 lors de l'enregistrement d'une plage (`POST /api/availabilities`)** :
   - En environnement de recette (`https://recette.joprelys.com/api/availabilities`), des variations de routage proxy/Nginx avec ou sans slash final peuvent provoquer un retour 404 sous Spring Boot 3 si le path matcher n'accepte pas explicitement le slash optionnel.
   - **Solution** : Étendre le mapping du controller backend `@RequestMapping({"/api/availabilities", "/api/availabilities/"})` et sur les sous-endpoints.

2. **Désalignement jour / date dans la modale (Sélection d'un mardi → Formulaire pré-remplit "Lundi")** :
   - Cause : `onCalendarRangeSelected` et `onWeekdaySelected` appelaient `resetRuleForm()` qui réinitialisait `formWeekday` à `1` (Lundi). En outre, la balise `<option [value]="day">` dans le `<select>` Angular n'avait pas l'attribut `[selected]="day === formWeekday()"`, causant un désalignement entre l'état interne de la date (`2026-07-31` = Vendredi) et la sélection affichée (`1` = Lun.).
   - **Solution** : Suppression des `resetRuleForm()` parasites dans la chaîne d'événements, synchronisation bidirectionnelle automatique entre la date de début (`formValidFrom`) et le jour de semaine (`formWeekday`), et ajout de `[selected]="day === formWeekday()"` sur le `<select>`.

3. **Grille de calendrier non Mobile-First** :
   - Cause : `WeeklyAvailabilityGridComponent` imposait une largeur minimale `min-w-[980px]` avec un défilement horizontal forcé sur mobile.
   - **Solution** : Refonte responsive Mobile-First :
     - **Sous 768px (Mobile)** : Sélecteur de jour sous forme d'onglets ergonomiques (Lun à Dim avec dates), affichage vertical timeline jour par jour avec gros boutons tactiles (min 44px) et action rapide "+ Ajouter une plage".
     - **À partir de 768px (Desktop)** : Grille hebdomadaire 7 colonnes complète conservée.

## Actions à réaliser

- [x] Rédiger le ticket de suivi `docs/ai/tickets/BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG.md`
- [ ] Sécuriser `AvailabilityController.java` pour accepter les variantes d'URL avec/sans trailing slash
- [ ] Refondre `WeeklyAvailabilityGridComponent` pour supporter la vue jour par jour mobile-first sous 768px
- [ ] Corriger `AvailabilityPageComponent` et son template pour aligner strictement le jour et la date
- [ ] Vérifier la suite de tests backend (`AvailabilityControllerTest`) et ajouter les tests unitaires Angular (`weekly-availability-grid.component.spec.ts`, `availability-page.component.spec.ts`)
- [ ] Mettre à jour la documentation fonctionnelle `docs/features/doctor-availability-appointments/FUNCTIONAL-SPEC.md` et technique `TECHNICAL-DESIGN.md`
- [ ] Mettre à jour `PROJECT-TRACKING.md` et `CHANGELOG.md`

## Suivi des régressions & règles de design
- Conforme à `DESIGN.md` et `docs/standards/UI-RADIUS-AND-SHADOW-STANDARDS.md` (arrondis ≤ 8px, ombre légère, tokens `--brand-primary`, `--brand-danger`, `--app-surface`).
- Zéro dépendance externe lourde (Full Angular native signals & Tailwind v4).
- Respect du thème Light / Dark et i18n FR/EN.

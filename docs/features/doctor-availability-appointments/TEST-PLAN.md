# Plan de test — Disponibilités médecin & rendez-vous (EPIC-0025)

> Périmètre : feature `doctor-availability-appointments`.
> Ce fichier est complété story par story (voir sections datées).

## STORY-2602 — Gestion des disponibilités médecin (règles hebdo + exceptions)

Livrables couverts :

- **Backend** : endpoints `/api/availabilities` (CRUD règles, désactivation logique) et `/api/availabilities/exceptions` (création, liste, suppression physique) + générateur de créneaux.
- **Frontend** : page Angular `clinic/availability` « Mes disponibilités » (médecin/admin), grille hebdomadaire, formulaires, aperçu client des créneaux, i18n FR/EN, light/dark.

### 1. Tests backend (livrés — 19 cas)

Les 19 cas prévus par la story sont implémentés via 20 méthodes de test réparties en deux suites.

#### 1.1 `backend/src/test/java/com/joprelys/backend/appointment/api/AvailabilityControllerTest.java` (11 cas, MockMvc)

| # | Méthode | Cas de test | Attendu |
|---|---------|-------------|---------|
| B-01 | `shouldReturn401WhenNoToken` | Appel sans JWT | 401 |
| B-02 | `shouldReturn403ForAgentAccueilRole` | Rôle non autorisé (AGENT_ACCUEIL) | 403 |
| B-03 | `shouldReturn403WhenMedecinTargetsAnotherMedecin` | Création / lecture / modification ciblant un autre médecin | 403 `ACCESS_DENIED` |
| B-04 | `shouldCreateReadUpdateAndDeactivateRule` | CRUD nominal d'une règle + DELETE | 200 ; DELETE = `active=false` conservé en base (désactivation logique, RM-07) |
| B-05 | `shouldAllowAdminCliniqueToManageAnyDoctor` | Admin ciblant un médecin via `doctorId` | 200, création + lecture ciblée |
| B-06 | `shouldReturn409WhenRuleOverlapsActiveRule` | Chevauchement d'une règle active | 409 `AVAILABILITY_OVERLAP` ; fenêtres disjointes / plages adjacentes / autre jour acceptés |
| B-07 | `shouldReturn409WhenDeactivatingRuleWithFutureAppointments` | Désactivation avec RDV futur (RM-07) | 409 `AVAILABILITY_CONFLICT`, règle conservée active |
| B-08 | `shouldReturn404ForRuleOfAnotherTenant` | Règle d'un autre tenant (isolation) | 404 `AVAILABILITY_NOT_FOUND` |
| B-09 | `shouldCreateListAndDeleteException` | CRUD nominal d'une exception | 200 ; filtre `from/to` ; suppression physique (liste vide ensuite) |
| B-10 | `shouldReturn400WhenRangesAreInvalid` | `endTime<=startTime`, `validTo<validFrom`, `endAt<=startAt` | 400 `VALIDATION_ERROR` |
| B-11 | `shouldGenerateSlotsThroughService` | Génération via service (règle − exception − RDV réservé) | seuls les créneaux libres restent |

#### 1.2 `backend/src/test/java/com/joprelys/backend/appointment/application/AppointmentSlotGeneratorTest.java` (9 cas, unitaires purs)

| # | Méthode | Cas de test | Attendu |
|---|---------|-------------|---------|
| B-12 | `shouldGenerateSlotsForSimpleRule` | Plage de 4 h en pas de 30 min | 8 créneaux, bornes exactes |
| B-13 | `shouldMaskAllSlotsWhenExceptionCoversWholeRange` | Exception couvrant toute la plage | aucun créneau |
| B-14 | `shouldMaskOnlyOverlappingSlots` | Exception partielle | seuls les créneaux chevauchés sont masqués (contact en fin ≠ chevauchement) |
| B-15 | `shouldMaskSlotsAcrossMidnight` | Indisponibilité à cheval sur minuit | créneaux masqués des deux côtés |
| B-16 | `shouldRespectValidityWindow` | Fenêtre `validFrom` / `validTo` | créneaux uniquement dans la fenêtre |
| B-17 | `shouldExcludeTruncatedSlotWhenDurationDoesNotDivide` | Plage non divisible par le pas | créneau tronqué exclu |
| B-18 | `shouldExcludeReservedSlot` | Créneau déjà réservé (RDV) | exclu |
| B-19 | `shouldReturnEmptyWhenNoRule` + `shouldApplyIsoWeekdays` | Sans règle ; weekdays ISO (1 = lundi … 7 = dimanche) | liste vide ; lundi/dimanche corrects |

Exécution : `cd backend && ./mvnw test -Dtest='AvailabilityControllerTest,AppointmentSlotGeneratorTest'` (profil `test`).

### 2. Tests frontend (livrés)

#### 2.1 `web/src/app/clinic/availability/availability-page.component.spec.ts`

Pattern : TestBed `imports: [AvailabilityPageComponent]`, `provideRouter([])`, `AvailabilityApiService` mocké (`vi.fn().mockReturnValue(of(...))`), `I18nService` stubbé (la clé est renvoyée telle quelle pour des assertions déterministes).

| # | Cas de test | Assertions |
|---|-------------|------------|
| F-01 | Chargement initial | `listRules` / `listExceptions` appelés, signals `rules` / `exceptions` remplis, `loading=false`, `pageError=null` |
| F-02 | Échec de chargement | `pageError` renseigné (500 → `common.error.server`), `loading=false` |
| F-03 | État vide | listes vides, `hasAnySlots()=false` |
| F-04 | Création de plage | payload `UpsertAvailabilityRuleRequest` exact, message succès, formulaire fermé |
| F-05 | Validation plage (fin ≤ début) | appel API non effectué, `ruleFormError` renseigné |
| F-06 | Modification de plage | pré-remplissage du formulaire, `updateRule(id, payload)`, succès |
| F-07 | Mapping code métier | erreur 409 `AVAILABILITY_OVERLAP` → libellé i18n `availability.error.*` |
| F-08 | Désactivation de plage | confirmation → `deactivateRule(id)`, action purgée, succès |
| F-09 | Création d'indisponibilité | `datetime-local` → Instant ISO, motif trimé, succès |
| F-10 | Validation indisponibilité (fin ≤ début) | pas d'appel API, `exceptionFormError` renseigné |
| F-11 | Suppression d'indisponibilité | confirmation → `deleteException(id)`, succès |
| F-12 | Aperçu des créneaux | règle 2 h → 4 créneaux ; exception 1 h → 2 restants ; règle inactive ignorée |

#### 2.2 `web/src/app/shared/ui/weekly-availability-grid/weekly-availability-grid.component.spec.ts`

| # | Cas de test | Assertions |
|---|-------------|------------|
| G-01 | Groupement par jour | 7 colonnes, règles triées par `startTime`, jours vides |
| G-02 | Sélection de jour | `weekdaySelected` émis avec le jour cliqué |
| G-03 | Sélection de plage | `ruleSelected` émis avec la règle cliquée |
| G-04 | Désactivation | `ruleDeactivateRequested` uniquement sur plage active (bouton absent si inactive) |
| G-05 | Jour sélectionné | bordure `--brand-primary` sur la colonne active |

Exécution : `cd web && npm run test`.

### 3. Parcours manuels (recette)

Pré-requis : compte `MEDECIN` (ou `ADMIN_CLINIQUE`) avec permission `AVAILABILITY_MANAGE`, backend démarré.

| # | Parcours | Étapes | Attendu |
|---|----------|--------|---------|
| M-01 | Accès page | Menu latéral « Disponibilités » (icône calendrier) | route `/clinic/availability`, titre « Mes disponibilités », retour `/dashboard` |
| M-02 | Création plage | Cliquer un jour (grille) → heures 08:00–12:00 → « Créer la plage » | carte créée dans la colonne du jour, succès, aperçu alimenté |
| M-03 | Chevauchement | Recréer une plage qui chevauche | erreur i18n `AVAILABILITY_OVERLAP` affichée dans le formulaire |
| M-04 | Modification | Cliquer une plage → changer l'heure de fin → « Mettre à jour » | plage mise à jour, succès |
| M-05 | Désactivation | Icône ✕ sur une plage → confirmer dans la modale | plage grisée « Inactive », absente de l'aperçu ; jamais de `confirm()` natif |
| M-06 | Conflit RM-07 | Désactiver une plage avec RDV futur (données seed) | erreur `AVAILABILITY_CONFLICT` |
| M-07 | Indisponibilité | Ajouter début / fin / motif → créer | ligne listée (dates formatées), succès |
| M-08 | Suppression indisponibilité | « Supprimer » → confirmer | ligne retirée, créneaux réapparaissent dans l'aperçu |
| M-09 | Aperçu créneaux | Vérifier les 7 prochains jours | créneaux de 30 min (défaut local), passés masqués, exceptions déduites, badge compteur |
| M-10 | i18n | Basculer FR ↔ EN | tous les libellés traduits (aucun texte en dur), menu et erreurs compris |
| M-11 | Thème | Basculer light ↔ dark | contrastes OK, aucune couleur cassée (tokens CSS) |
| M-12 | Rôle interdit | Compte AGENT_ACCUEIL | item menu absent ; URL directe → page unauthorized |

### 4. Vérifications CI

- `npm run test` (specs F-01…G-05)
- `npm run build` (compilation AOT des templates)
- `npm run i18n:check` (clé `menu.availability` présente FR/EN, dictionnaire feature fusionné)
- `cd backend && ./mvnw test` (B-01…B-19)

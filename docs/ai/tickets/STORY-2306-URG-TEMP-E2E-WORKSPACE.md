# STORY-2306 — Workspace URG-TEMP et parcours E2E

- GitHub : #47
- Branche : `feat/47-urg-temp-e2e-workspace`
- Statut : IN REVIEW
- Priorité : P0
- Dépendance : #46 / PR #96

## Livré sur la branche

- [x] workspace urgence avec synthèse, identité, soins, médico-légal et documents ;
- [x] numéro URG-TEMP et statut provisoire persistants ;
- [x] action de rapprochement depuis l’urgence ;
- [x] patient et urgence présélectionnés dans le workspace de rapprochement ;
- [x] résultat de décision conservé à l’écran ;
- [x] accès au DPU canonique ;
- [x] continuité vers l’hospitalisation avec l’urgence source ;
- [x] orientation de stabilisation `ADMISSION` / `OR_DIRECT` branchée sur l’hospitalisation ;
- [x] onglet Documents et génération idempotente ;
- [x] tests Angular de navigation, rejeu et documents ;
- [x] textes FR/EN ;
- [x] spécification fonctionnelle et plan E2E.

## Scénarios

- [x] patient inconscient arrivé seul — couvert par l’admission unifiée existante ;
- [x] patient accompagné — couvert par le formulaire et le dossier médico-légal existants ;
- [x] triage puis réanimation avant administratif — couvert par le workspace urgence ;
- [x] hospitalisation et finance différée — apport #46 ;
- [x] création d’un nouveau DPU — décision existante et continuité ajoutée ;
- [x] rapprochement DPU existant — décision existante et continuité ajoutée ;
- [x] plusieurs candidats / report — interface existante conservée ;
- [x] erreur réseau / rejeu idempotent — test Angular et backend existants ;
- [x] cross-tenant — contrôles backend existants ;
- [x] correction — interface, historique et clé d’idempotence conservés.

## Validation requise

- [ ] synchronisation avec le dernier commit vert de #46 ;
- [ ] tests Angular verts ;
- [ ] build Angular production vert ;
- [ ] Maven strict vert ;
- [ ] migrations PostgreSQL 16 vertes ;
- [ ] recette manuelle du parcours principal ;
- [ ] contrôle mobile, light/dark et FR/EN ;
- [ ] revue métier avant fusion.

## Conditions de clôture

L’issue #47 sera clôturée après :

1. fusion de #46 ;
2. CI complète verte sur cette branche ;
3. fusion de la PR de cette story ;
4. recette E2E consignée sans régression bloquante.

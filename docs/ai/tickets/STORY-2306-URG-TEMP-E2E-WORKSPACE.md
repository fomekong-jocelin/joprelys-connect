# STORY-2306 — Workspace URG-TEMP et parcours E2E

- GitHub : #47
- PR : #97
- Commit fusion : `678c070eb9e2c4b14d7c59330b5ed2a21a980d1e`
- Statut : **CODE FUSIONNÉ / QA AUTOMATISÉE VERTE / UAT HUMAINE À FINALISER**
- Priorité : P0
- Dépendance livrée : #46 / PR #96

## Livré dans `main`

- [x] workspace urgence avec synthèse, identité, soins, médico-légal et documents ;
- [x] numéro URG-TEMP et statut provisoire persistants ;
- [x] action de rapprochement depuis l’urgence ;
- [x] patient et urgence présélectionnés dans le workspace de rapprochement ;
- [x] résultat de décision conservé à l’écran ;
- [x] accès au DPU canonique ;
- [x] continuité vers l’hospitalisation avec l’urgence source ;
- [x] orientations `ADMISSION` / `OR_DIRECT` branchées sur l’hospitalisation ;
- [x] onglet Documents et génération idempotente ;
- [x] tests Angular de navigation, rejeu et documents ;
- [x] textes FR/EN ;
- [x] spécification fonctionnelle et plan E2E.

## Scénarios techniquement couverts

- [x] patient inconscient arrivé seul — admission unifiée ;
- [x] patient accompagné — formulaire et dossier médico-légal ;
- [x] triage puis réanimation avant administratif ;
- [x] hospitalisation et finance différée — apport #46 ;
- [x] création d’un nouveau DPU ;
- [x] rapprochement DPU existant ;
- [x] plusieurs candidats / report ;
- [x] erreur réseau / rejeu idempotent ;
- [x] cross-tenant ;
- [x] correction/traçabilité du rapprochement selon l'architecture existante.

## Validation automatisée

La PR #97 a été fusionnée après validation du différentiel combiné avec #96 :

- [x] tests Angular ;
- [x] build Angular production ;
- [x] Maven strict ;
- [x] migrations et tests backend ;
- [x] navigation urgence → rapprochement → hospitalisation ;
- [x] rejeu idempotent ;
- [x] lot documentaire et provenance ;
- [x] workspace à cinq sections.

Les tests actuels incluent notamment :

- `EmergencyTriageAssessmentControllerTest` ;
- `patient-reconciliation-page.component.spec.ts` ;
- `emergency-hospitalization-continuation.component.spec.ts` ;
- `emergency-dashboard.component.spec.ts` ;
- `emergency-documents-panel.component.spec.ts`.

## Validation humaine restante

- [ ] répétition du parcours principal sur l'environnement prévu pour la démonstration ;
- [ ] contrôle 320/375/768/1366/1920 px sur les écrans réellement montrés ;
- [ ] contrôle light/dark et FR/EN ;
- [ ] validation métier accueil, urgence, hospitalisation et DPO ;
- [ ] aucun P0 restant après répétition générale.

## Statut de clôture

L'issue #47 est techniquement clôturée et la PR #97 est fusionnée. L'UAT restante est suivie dans l'epic #36 et la readiness spécifique à la démonstration du 25/07/2026 dans #127.

## Non-régression après HOS-RBAC-001-D

Le parcours ne doit jamais utiliser `HOSPITALIZATION_MANAGE` comme fallback. Depuis V86, les actions hospitalières reposent uniquement sur les permissions spécialisées, notamment `HOSPITALIZATION_ADMIT` pour l'admission.

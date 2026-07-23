# STORY-2305 — Continuité URG-TEMP vers hospitalisation

- GitHub : #46
- PR : #96
- Commit fusion : `644d19e25f57c59776cfb4f08e57d504fb938915`
- Statut : **CODE FUSIONNÉ / QA AUTOMATISÉE VERTE / RECETTE HUMAINE À FINALISER**
- Priorité : P0
- Impact : backend, Angular, PostgreSQL, documents, facturation

## Livré dans `main`

- [x] admission depuis une urgence sans visite préalable ;
- [x] lien explicite urgence ↔ visite ↔ hospitalisation via `emergencyId` ;
- [x] création ou réutilisation contrôlée d'une visite de continuité ;
- [x] résolution canonique et prévention des doublons source/cible ;
- [x] admission sur structure typée et lit configuré/disponible ;
- [x] cinq documents d'urgence vérifiables ;
- [x] billet d'entrée persisté et versionné ;
- [x] statut financier différé `REGULARIZATION_PENDING` lorsqu'applicable ;
- [x] agrégation canonique hospitalisations/documents/factures sans renumérotation silencieuse ;
- [x] interface de continuité avec avertissement d'identité provisoire ;
- [x] textes FR/EN ;
- [x] tests backend et Angular ;
- [x] spécifications fonctionnelle, technique et plan de tests.

## Preuves de validation automatisée

PR #96 fusionnée après CI verte sur son dernier état :

- [x] Maven strict ;
- [x] tests Angular ;
- [x] build Angular production ;
- [x] migrations/test PostgreSQL du lot ;
- [x] tests admission depuis urgence ;
- [x] tests de concurrence et réservation de lit ;
- [x] tests finance différée et provenance documentaire.

Le code courant contient notamment `HospitalizationAdmissionServiceTest.shouldCreateAVisitAndRetainEmergencyLinkWhenNoVisitWasProvided`, qui vérifie la création de la visite de continuité et la conservation de `emergencyId`.

## Validation humaine restante

- [ ] répétition du parcours URG-TEMP → hospitalisation sur l'environnement prévu pour la démonstration ;
- [ ] contrôle visuel de l'interface de continuité ;
- [ ] contrôle du lot documentaire si celui-ci est montré au client ;
- [ ] contrôle de la finance `REGULARIZATION_PENDING` uniquement si elle reste dans le scope de démonstration ;
- [ ] validation métier accueil/urgence/hospitalisation ;
- [ ] absence de P0 après répétition générale.

## Statut de clôture

L'issue #46 est techniquement clôturée et la PR #96 est fusionnée. La recette humaine globale n'est pas recréée dans cette story : elle est suivie au niveau de l'epic #36 et, pour la démonstration du 25/07/2026, dans #127.

## Non-régression actuelle

- aucune réintroduction de `HOSPITALIZATION_MANAGE` : la permission a été supprimée par HOS-RBAC-001-D / Flyway V86 ;
- l'admission repose sur `HOSPITALIZATION_ADMIT` ;
- aucune donnée spatiale ne doit être créée implicitement par le use case d'admission ;
- le lit doit être configuré, ouvert, prêt et libre ;
- un échec documentaire après une admission réussie ne doit pas provoquer une seconde admission.

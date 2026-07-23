# QA-DEMO-20260725 — Readiness démonstration client du 25 juillet 2026

## Métadonnées

- **GitHub** : #127
- **Baseline** : `main@81b7d20c4cf44800e436c86b964a38bc62929305`
- **Branche** : `qa/demo-20260725-readiness`
- **Type** : QA Review + stabilisation pré-démo
- **Priorité** : P0 jusqu'au 25/07/2026
- **Estimation** : 3 SP / 1 à 1,5 j senior QA/full-stack, hors défaut majeur découvert
- **Reviewer** : Tech Lead + référent métier
- **Statut** : IN_PROGRESS

## Objectif

Sécuriser une démonstration client répétable du parcours clinique déjà livré, sans ajouter de dette technique ni ouvrir un chantier architectural risqué juste avant la démonstration.

Le périmètre fonctionnel convenu est :

1. accueil et patient ;
2. ouverture de visite ;
3. constantes ;
4. consultation ;
5. urgence URG-TEMP et triage ;
6. rapprochement vers le DPU canonique ;
7. continuité vers l'hospitalisation ;
8. service/chambre/lit cohérents.

## Contraintes non négociables

- GitHub est l'unique source de vérité.
- Aucun développement direct sur PROD ou RECETTE.
- Aucun mock, bypass RBAC, alias legacy ou donnée hardcodée spécifiquement pour la démo.
- Aucun retour de `HOSPITALIZATION_MANAGE` supprimée par HOS-RBAC-001-D / Flyway V86.
- Aucune modification des migrations V1–V86 déjà fusionnées.
- Aucun gros chantier HOS-STAFF/HOS-DIS/ABAC avant la démo sauf anomalie P0 démontrée.
- Toute correction doit être minimale, documentée, testée et revue.

## État déjà prouvé

- STORY-2305 / #46 : PR #96 fusionnée ; continuité URG-TEMP → hospitalisation/documents/finance livrée.
- STORY-2306 / #47 : PR #97 fusionnée ; workspace urgence, rapprochement et continuité vers hospitalisation livrés.
- HOS-02 / #73 : typage des services livré et issue clôturée ; à revalider dans le code courant pour la démo.
- HOS-RBAC-001-D / #121 : PR #122 fusionnée ; `HOSPITALIZATION_MANAGE` supprimée par V86.
- Documentation de gouvernance HOS-RBAC réalignée via #124.

## Action plan

### A — Gouvernance et documentation

- [ ] Aligner l'EPIC GitHub #36 sur les stories #40/#42/#44/#45/#46/#47 déjà terminées techniquement.
- [ ] Aligner STORY-2305 sur l'état post-fusion de #96.
- [ ] Aligner STORY-2306 sur l'état post-fusion de #97.
- [ ] Réconcilier l'EPIC GitHub #25 : retirer les constats P0 devenus faux sans déclarer les 43 modules terminés.
- [ ] Réconcilier #29 : #31/#33 terminées ; #34 à qualifier contre le code courant.
- [ ] Mettre `PROJECT-TRACKING.md` à jour après #122/#124 et ajouter la contrainte de démo.

### B — Audit code / tests de la démo

- [ ] Inventorier les classes, composants et tests du parcours normal.
- [ ] Inventorier les classes, composants et tests du parcours URG-TEMP.
- [ ] Vérifier le typage des services/chambres/lits.
- [ ] Vérifier la matrice RBAC des acteurs de démonstration après V86.
- [ ] Identifier les scénarios automatisés réellement manquants avant d'ajouter du code.

### C — Validation technique

- [ ] Ajouter uniquement les tests de non-régression manquants qui protègent directement la démo.
- [ ] Maven strict vert si backend/tests backend modifiés.
- [ ] Angular tests + build verts si frontend/tests Angular modifiés.
- [ ] Vérifier la couverture Flyway/PostgreSQL 16 de la baseline V86.

### D — Runbook et répétition

- [ ] Préparer le scénario principal normal.
- [ ] Préparer le scénario urgence/rapprochement/hospitalisation.
- [ ] Documenter les acteurs et données à préparer sans secrets.
- [ ] Définir les critères GO/NO-GO de la répétition générale.
- [ ] Consigner honnêtement les limites non validées humainement.

## Definition of Ready

- baseline `main` connue ;
- parcours de démonstration connu ;
- dépendances #46/#47/#73/#121 fusionnées ou clôturées ;
- aucune décision métier structurante requise pour démarrer l'audit ;
- données de démonstration génériques définissables sans secrets.

## Definition of Done

- documentation et tickets parents alignés avec le code actuel ;
- aucun code existant dupliqué ;
- aucun workaround de démonstration ;
- preuves automatisées des parcours critiques suffisantes ou lacunes explicitement consignées ;
- CI verte sur les changements de la branche ;
- runbook client prêt ;
- risques P0/P1 avant démo consignés ;
- aucune action serveur effectuée.

## Impact version / SemVer

- Documentation/QA seule : aucun bump applicatif.
- Toute correction fonctionnelle découverte sera évaluée séparément selon `docs/release/SEMANTIC-VERSIONING.md`.

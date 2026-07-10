# STORY-2110 - Rapprochement opérationnel de caisse

| Champ | Valeur |
|---|---|
| Type | Engineering / Correctif fonctionnel |
| Priorité | P0 |
| Statut | DONE |
| Estimation | 0.5 j senior |
| Profil | Senior full-stack |
| Reviewer | Lead Developer + DAF |

## Objectif

Rendre la clôture de caisse exploitable au quotidien en distinguant strictement les espèces physiques des chèques et virements, et en améliorant la lecture des opérations par le caissier.

## Critères d'acceptation

- [ ] Le solde théorique de clôture ne comprend que les espèces physiques.
- [ ] Les encaissements chèque et virement restent visibles séparément et ne créent pas d'écart espèces.
- [ ] Un versement banque est tracé comme une sortie d'espèces et exige une référence de dépôt.
- [ ] Les mouvements d'une session ne sont accessibles qu'au tenant autorisé.
- [ ] L'écran affiche le détail espèces, chèques, virements, sorties et versements avant clôture.
- [ ] Les tests backend couvrent la clôture avec des moyens de règlement mixtes.

## Plan d'action

- [x] Analyser le flux session, paiement, mouvement et clôture existant.
- [x] Identifier le défaut de rapprochement des moyens non espèces.
- [x] Implémenter le calcul et les validations côté backend.
- [x] Faire évoluer l'écran caisse et le contrat Angular.
- [x] Ajouter les tests de non-régression.
- [x] Exécuter les vérifications Maven et Angular.
- [x] Mettre à jour le suivi et le changelog.

## Risques et sécurité

- Les règles de solde restent exclusivement côté backend; le frontend ne fournit qu'un aperçu.
- Les anciens mouvements restent compatibles; seuls les nouveaux versements bancaires sont contraints.
- Une validation DAF reste requise pour confirmer les procédures de dépôt et les seuils métier.

## Definition of ready / done

- Ready: documentation fonctionnelle et technique créée, contrat existant analysé.
- Done: calcul backend, UI, tests, documentation et suivi mis à jour avec preuve de vérification.

## Vérifications exécutées

- [x] `mvn test -Dtest=CashRegisterControllerTest` : 3 tests réussis.
- [x] `npm run test -- --watch=false` : 101 tests réussis.
- [x] `npm run build` : build Angular de production réussi.

## Reste à faire

- Validation DAF des règles de dépôt et de la future gestion des coupures/pièces.

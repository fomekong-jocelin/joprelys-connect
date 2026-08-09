# Addendum — Lecture praticien du parcours jusqu’au lit

Date de revue : 2026-08-09.

Ce document complète `AUDIT-REPORT.md` pour le chemin court « patient → décision d’hospitalisation → attribution du lit ». Il ne remplace pas la recette terrain ni l’audit visuel capturé.

## Verdict

Le socle technique sait créer un séjour lié à une urgence, vérifier l’appartenance du lit et empêcher le double-booking. Le parcours praticien reste toutefois incomplet pour une utilisation hospitalière : il saute directement de la décision à l’occupation du lit, sans demande/acceptation/réservation/arrivée/handoff, et sans compatibilité clinique du placement.

## Gaps prioritaires

| Priorité | Écart | Décision attendue |
|---|---|---|
| P0 | Séjour et lit occupé créés au même clic | Séparer décision, réservation et arrivée confirmée. |
| P0 | Pas d’acceptation de l’unité receveuse | Ajouter responsable, statut, échéance et handoff. |
| P0 | Pas de compatibilité clinique du lit | Définir contraintes patient/unité/lit côté backend. |
| P0 | Praticien non validé par affectation/habilitation | Utiliser les affectations datées et la relation de soin. |
| P0 | Contexte patient incomplet au choix du lit | Afficher synthèse clinique utile et alertes critiques. |
| P1 | Aucun plan si aucun lit n’est disponible | File d’attente, overflow, transfert ou escalade. |
| P1 | États de présence/transport/handoff absents | Ajouter les jalons jusqu’à l’installation. |
| P1 | Filtres de disponibilité différents selon le parcours | Centraliser la projection backend des lits admissibles. |
| P1 | Erreur documentaire potentiellement masquée | Conserver l’état de rattrapage jusqu’à acquittement. |

## Limites de preuve

La revue a utilisé le code, les contrats, les tests et les spécifications du dépôt. La capture d’écran du parcours local n’a pas pu être réalisée car le navigateur a refusé l’accès à l’URL locale ; la qualité visuelle, le focus clavier, le rendu mobile et les états réellement rendus restent donc à vérifier en recette.

## Suite recommandée

Faire signer le workflow par un médecin responsable, un cadre infirmier, les admissions et le bed manager, puis découper l’implémentation dans `EPIC-0027` (`HOS-ADM-001`, `HOS-MOV-001`, `HOS-PATH-001`).

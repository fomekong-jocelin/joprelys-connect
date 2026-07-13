# Conception technique — Triage ABCDE et réévaluations

## Architecture

```text
EmergencyTriageAssessmentController
  → EmergencyTriageAssessmentUseCase
    → DefaultEmergencyTriageAssessmentUseCase
      → EmergencyRepository (verrou dossier)
      → EmergencyTriageAssessmentRepository
```

Le contrôleur ne porte aucune logique métier. Le use case gère les transactions, le verrouillage, les séquences, l’état stabilisé et la persistance. Angular appelle uniquement l’API et gère les états de présentation.

## Modèle

Nouvelle table append-only `emergency_triage_assessments`.

Chaque ligne contient : tenant, urgence, type `INITIAL` ou `REASSESSMENT`, séquence, niveau de triage, état hémodynamique, cinq axes ABCDE, constantes, orientation recommandée, notes, heure clinique, auteur et date technique de création.

## Compatibilité

- Les colonnes historiques de `emergencies` restent inchangées.
- Les DTO existants restent compatibles ; un champ optionnel `abcdeAssessment` est ajouté à la création d’urgence.
- Une migration V64 crée la table et reprend chaque urgence existante avec `NOT_ASSESSED` pour les axes non disponibles.
- Aucun numéro patient, urgence, document ou facture n’est modifié.

## Concurrence

Pour une réévaluation :

1. verrou pessimiste sur l’urgence ;
2. contrôle qu’elle n’est pas stabilisée ;
3. lecture de la séquence maximale ;
4. insertion avec `max + 1` ;
5. contrainte unique `(organization_id, emergency_id, sequence_number)`.

## Sécurité

- GET : `EMERGENCY_READ` ou rôles cliniques historiques autorisés.
- POST : `EMERGENCY_WRITE` ou rôles d’écriture historiques.
- `@TenantId` sur l’entité d’évaluation.
- FK vers l’urgence et l’auteur.
- aucune PII dans les logs ou messages d’erreur.
- une urgence d’un autre tenant est exposée comme introuvable.

## Angular

Nouveau composant feature `EmergencyTriagePanelComponent` :

- reçoit `emergencyId` ;
- charge l’historique via `EmergencyApiService` ;
- affiche loading, erreur, historique et formulaire ;
- crée une réévaluation puis recharge la liste ;
- ne calcule aucune règle clinique ;
- réutilise `AlertComponent`, `ButtonComponent`, `.ui-card`, `.ui-input` ;
- FR/EN, light/dark, rayons sobres.

Le dashboard importe le panneau sans augmenter sa logique métier ni dépasser les limites de responsabilité.

## Observabilité

Les erreurs utilisent des codes métier stables :

- `EMERGENCY_NOT_FOUND` ;
- `EMERGENCY_ALREADY_STABILIZED` ;
- `EMERGENCY_TRIAGE_ASSESSMENT_REQUIRED` ;
- `EMERGENCY_TRIAGE_ASSESSED_AT_FUTURE`.

Aucun contenu clinique n’est journalisé.

## Rollback

Le rollback applicatif consiste à revenir au commit précédent. La migration V64 est additive ; la table peut rester inutilisée sans affecter les anciennes urgences. Aucune migration destructive inverse n’est prévue.

## SemVer

MINOR : nouvel endpoint, nouveau modèle append-only et nouveau panneau UI rétrocompatibles.
# Conception technique — Historique des états de lit

## Architecture

L'incrément ajoute un agrégat de journal distinct du modèle courant `BedEntity` :

```text
SpatialController
      │
      ├── SpatialService ── BedEntity / BedRepository
      │          │
      │          └── BedStateChangeService
      │                        │
      │                        └── BedStateChangeRepository
      │                                      │
      │                                      └── bed_state_changes
      │
HospitalizationDischargeWorkflowService ─────┘
```

Le journal ne pilote pas l'état courant. Il enregistre l'intention validée dans la même transaction que la mutation du lit.

## Transactionnalité

Pour les commandes manuelles :

1. charger le lit dans le tenant courant ;
2. vérifier les préconditions de transition ;
3. vérifier la compatibilité du motif ;
4. insérer l'événement ;
5. modifier et sauvegarder le lit ;
6. écrire l'audit générique existant.

L'événement et l'état courant sont annulés ensemble si une étape échoue.

Pour le transfert et le départ physique, l'événement de nettoyage est écrit dans la transaction du workflow qui clôt l'affectation.

## Modèle relationnel

`bed_state_changes` possède une FK composite `(bed_id, organization_id)` vers `beds(id, organization_id)`. Cette contrainte interdit l'association d'un événement à un lit d'un autre établissement.

Les index couvrent :

- la chronologie d'un lit ;
- l'analyse par motif dans un établissement.

## Append-only

Aucun repository applicatif ne propose `update` ou `delete` métier. Les corrections devront être modélisées ultérieurement par un événement compensatoire.

La FK utilise temporairement `ON DELETE CASCADE` parce que le module spatial permet encore la suppression physique d'un lit inutilisé. Cette dette est rattachée à GAP-012 ; l'archivage du lit doit précéder toute conservation réglementaire indépendante.

## Acteur et source

- `actor_id` référence logiquement l'utilisateur, sans FK forte afin de préserver l'historique après désactivation ou suppression future du compte.
- `actor_display_name` constitue le snapshot lisible.
- en absence d'utilisateur authentifié, le snapshot vaut `Système Joprelys`.
- `source` distingue commande manuelle, transfert, départ physique et compatibilité legacy.

## Motifs

`BedStateReasonCode` est un enum applicatif fermé. `BedStateChangeService` contrôle les couples transition/motif et empêche l'utilisation manuelle des motifs automatiques.

Le schéma SQL ne duplique pas toute la matrice de transitions : cette logique dépend de l'ancienne et de la nouvelle valeur et reste centralisée dans le service. Les contraintes SQL garantissent néanmoins axe, source, valeurs différentes et acteur non vide.

## API

### Commande spécialisée

```json
{
  "status": "MAINTENANCE",
  "reasonCode": "MAINTENANCE_CORRECTIVE",
  "note": "Frein du lit défectueux"
}
```

### Consultation

```text
GET /api/spatial/beds/{bedId}/state-history
```

La réponse est triée par `occurredAt` décroissant.

## Compatibilité

Les clients Angular existants utilisent des valeurs par défaut codifiées pour ne pas casser leur compilation. L'interface de sélection explicite des motifs doit devenir le consommateur cible avant retrait de ces valeurs par défaut.

L'endpoint `/status` ne requiert pas `reasonCode` et produit `LEGACY_SUPERVISION`.

## Tests

- validation unitaire des motifs ;
- tests des politiques de transition existantes avec le nouveau collaborateur ;
- contrôle par réflexion des permissions ;
- migration PostgreSQL 16 et intégrité tenant ;
- suites H2/PostgreSQL complètes ;
- tests et build Angular.

## Risques et suites

- supprimer les valeurs par défaut frontend après livraison du sélecteur de motifs ;
- ajouter pagination à l'historique si le volume réel l'exige ;
- modéliser turnover et ordres de travail ;
- ajouter événements compensatoires ;
- migrer vers la stratégie temporelle UTC/timestamptz de GAP-038.

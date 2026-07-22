# Spécification fonctionnelle — Historique des états de lit

## Objectif

Permettre à l'établissement d'expliquer chaque changement de capacité et de préparation d'un lit sans se limiter à son état courant.

## Acteurs

- **Responsable hospitalisation** : ouvre ou ferme la capacité d'un lit.
- **Agent d'hygiène** : démarre et termine un nettoyage.
- **Technicien de maintenance** : démarre et termine une maintenance.
- **Médecin / infirmier autorisé au transfert** : le transfert déclenche automatiquement le nettoyage de l'ancien lit.
- **Responsable hospitalisation autorisé au départ physique** : le départ déclenche automatiquement le nettoyage du lit libéré.
- **Lecteur hospitalisation** : consulte la chronologie.

## Axes suivis

### Capacité

- `OPEN` : le lit est compté dans la capacité ouverte.
- `CLOSED` : le lit est temporairement retiré de la capacité ouverte.

### Préparation

- `READY` : le lit est prêt du point de vue opérationnel.
- `CLEANING` : le lit est dans le circuit de nettoyage.
- `MAINTENANCE` : le lit est indisponible pour maintenance.

L'occupation reste dérivée de l'affectation active et n'est pas modifiée directement par ce journal.

## Catalogue de motifs

### Capacité

| Transition | Motifs autorisés |
|---|---|
| `OPEN → CLOSED` | fermeture temporaire, manque de personnel, sécurité, autre |
| `CLOSED → OPEN` | réouverture validée |

Le motif « autre » impose une note.

### Nettoyage

| Transition | Motifs autorisés |
|---|---|
| `READY → CLEANING` manuel | routine, isolement, incident |
| `READY → CLEANING` automatique | transfert ou départ physique |
| `CLEANING → READY` | nettoyage terminé |

Le motif « incident » impose une note.

### Maintenance

| Transition | Motifs autorisés |
|---|---|
| `READY → MAINTENANCE` | préventive, corrective, sécurité |
| `MAINTENANCE → READY` | maintenance terminée |

## Historique affiché

Chaque ligne contient :

- date et heure ;
- axe modifié ;
- ancienne et nouvelle valeur ;
- motif ;
- note ;
- acteur ;
- source manuelle ou automatique.

## Règles de sécurité

- la lecture requiert `HOSPITALIZATION_READ` ;
- chaque commande conserve sa permission spécialisée ;
- les motifs automatiques ne peuvent pas être soumis manuellement ;
- l'isolation tenant reste obligatoire ;
- aucun nom de patient n'est stocké dans l'historique du lit.

## Compatibilité

L'endpoint historique `/api/spatial/beds/{id}/status` reste disponible pour les consommateurs existants. Ses modifications sont enregistrées sous la source `LEGACY_SUPERVISION` afin de ne pas inventer un motif métier plus précis que celui réellement fourni.

## Limites de l'incrément

- pas de tâche assignable de turnover ;
- pas d'ordre de travail de maintenance ;
- pas de pièce jointe ni preuve de contrôle ;
- pas de correction formelle d'un événement erroné ;
- suppression physique legacy du lit encore possible.

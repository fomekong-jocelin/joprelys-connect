# HOS-BED-002-C — Spécification fonctionnelle capacité et préparation des lits

## Objectif

Distinguer les notions suivantes sans casser les écrans ni les intégrations utilisant encore le statut historique du lit :

1. **lit installé** : lit configuré et physiquement répertorié ;
2. **lit ouvert** : lit inclus dans la capacité exploitable du service ;
3. **lit prêt** : lit ouvert dont l'état de préparation est `READY` ;
4. **lit occupé** : lit possédant une affectation active ;
5. **lit disponible** : lit ouvert, prêt, non affecté et projeté `FREE`.

## Axes métier

### Capacité

- `OPEN` : le lit participe à la capacité ouverte ;
- `CLOSED` : le lit reste installé mais ne peut pas recevoir de patient.

Une fermeture peut représenter une fermeture saisonnière, une réduction temporaire de capacité ou une décision organisationnelle. Elle ne supprime pas le lit.

### Préparation

- `READY` : prêt à recevoir un patient ;
- `CLEANING` : en nettoyage ou désinfection ;
- `MAINTENANCE` : indisponible pour intervention technique.

### Usage

- `OCCUPIED` : une affectation active existe ;
- `UNASSIGNED` : aucune affectation active n'existe.

L'usage est dérivé des affectations actives et n'est pas modifiable directement depuis l'endpoint de statut.

## Compatibilité du statut historique

Le champ historique `status` reste exposé :

| Axes réels | Projection legacy |
|---|---|
| ouvert + prêt + non affecté | `FREE` |
| ouvert + prêt + affecté | `OCCUPIED` |
| préparation nettoyage | `CLEANING` |
| préparation maintenance | `MAINTENANCE` |
| fermé + préparation prête | `MAINTENANCE` par sécurité |

Un client ancien ne verra donc jamais un lit fermé comme `FREE`.

## Règles métier

1. Un lit nouvellement créé est `OPEN`, `READY`, `UNASSIGNED` et projeté `FREE`.
2. Un lit affecté ne peut pas être fermé, nettoyé ou placé en maintenance manuellement.
3. Un lit fermé ne peut pas être déclaré `FREE` ; il doit d'abord être rouvert.
4. L'admission et le transfert ne peuvent réclamer qu'un lit `OPEN`, `READY` et `FREE`.
5. La fermeture d'un lit prêt conserve son état de préparation afin qu'une réouverture puisse le restaurer comme `FREE`.
6. La fermeture ne supprime ni le lit, ni son historique, ni sa chambre.
7. Le passage à `OCCUPIED` reste réservé à l'admission ou au transfert.
8. L'occupation est comptée à partir de `bed_assignments` actifs, pas à partir du seul statut legacy.
9. Le taux d'occupation est calculé sur la capacité ouverte : `occupés / ouverts`.
10. Les anomalies historiques ne sont pas corrigées silencieusement.

## Compteurs du service

| Compteur | Définition |
|---|---|
| `totalBedsCount` | lits installés/configurés |
| `openBedsCount` | lits avec `capacityStatus=OPEN` |
| `readyBedsCount` | lits ouverts avec `readinessStatus=READY`, occupés ou non |
| `occupiedBedsCount` | lits ayant une affectation active |
| `availableBedsCount` | lits ouverts, prêts, non affectés et `FREE` |

## Parcours d'exploitation

### Fermer temporairement un lit

1. vérifier qu'aucune affectation active n'existe ;
2. choisir « Fermer » ;
3. le lit reste visible comme installé ;
4. il sort des compteurs ouvert, prêt et disponible ;
5. l'admission et le transfert le refusent.

### Rouvrir un lit prêt

1. choisir « Ouvrir » ;
2. si sa préparation est `READY`, il redevient immédiatement disponible ;
3. si sa préparation est `CLEANING` ou `MAINTENANCE`, il reste indisponible jusqu'à remise à `READY`.

### Fin de nettoyage ou maintenance

1. le lit doit être ouvert ;
2. choisir « Mettre libre » ;
3. la préparation devient `READY` ;
4. le lit devient disponible seulement s'il n'est pas affecté.

## Scénarios d'acceptation

- un lit libre ouvert et prêt peut être admis ;
- un lit fermé ne peut pas être admis même si son statut historique avait été `FREE` ;
- un lit en nettoyage ou maintenance ne peut pas être admis ;
- un lit affecté ne peut pas être fermé ;
- fermer puis rouvrir un lit prêt restaure sa disponibilité ;
- les compteurs installé, ouvert, prêt, occupé et disponible peuvent avoir des valeurs différentes ;
- le statut legacy reste présent dans les réponses existantes ;
- les nouveaux champs sont disponibles en français et en anglais dans l'interface.

## Hors périmètre

- motif structuré et durée de fermeture ;
- responsable hygiène ou maintenance dédié ;
- preuve de désinfection ;
- fermeture planifiée avec dates d'effet ;
- réservation anticipée ;
- sortie physique et turnover complet ;
- capacité théorique de chambre distincte des lits installés ;
- historisation append-only des changements d'axes.

# HOS-BED-001-D — Spécification fonctionnelle des chevauchements historiques

## Objectif

Garantir qu'un lit ne puisse jamais être attribué à deux séjours sur des périodes qui se chevauchent, y compris après clôture, import ou correction rétroactive.

## Utilisateurs concernés

- bed manager / responsable hospitalisation ;
- cadre infirmier ;
- DBA PostgreSQL ;
- équipe backend et exploitation ;
- auditeur autorisé.

## Définitions

- **Période valide** : intervalle semi-ouvert `[assigned_at, released_at)`.
- **Affectation active** : `released_at IS NULL`, donc borne haute infinie.
- **Périodes adjacentes** : `released_at A = assigned_at B`; elles ne se chevauchent pas.
- **Chevauchement** : les deux périodes ont au moins un instant en commun.
- **VALID** : période retenue comme source de vérité et soumise à la contrainte.
- **QUARANTINED** : période historique conservée mais écartée de la garantie opérationnelle après arbitrage explicite.

## Règles métier

1. Deux affectations `VALID` du même lit et du même établissement ne se chevauchent jamais.
2. Deux périodes adjacentes sont autorisées.
3. Deux lits différents peuvent porter des périodes identiques.
4. Une affectation active entre en conflit avec toute période qui commence après son début.
5. Une correction rétroactive est soumise aux mêmes contrôles qu'une création.
6. Une affectation active ne peut jamais être mise en quarantaine.
7. Une quarantaine exige un motif non vide, un acteur identifié et une date.
8. Aucune ligne n'est supprimée ou corrigée automatiquement.
9. La migration d'activation s'arrête si au moins une paire `VALID` se chevauche.
10. Le choix de la période à mettre en quarantaine appartient au DBA et au bed manager, sur preuve documentée.

## Parcours de déploiement

```text
Copie anonymisée / sauvegarde
→ V79 : métadonnées de quarantaine
→ préflight en lecture seule
→ arbitrage DBA + bed manager
→ quarantaine manuelle des seules lignes approuvées
→ V80 : extension + contrainte d'exclusion
→ tests de non-régression
```

Si le préflight ne trouve aucun conflit, V80 peut s'activer directement après V79.

## Scénarios d'acceptation

### Périodes chevauchantes

Étant donné une affectation `08:00–12:00`, lorsqu'une affectation du même lit `10:00–14:00` est insérée, alors PostgreSQL refuse l'écriture et conserve la première période.

### Périodes adjacentes

Étant donné une affectation `08:00–12:00`, lorsqu'une affectation du même lit `12:00–16:00` est insérée, alors l'écriture est acceptée.

### Lits différents

Étant donné deux lits du même établissement, lorsque chacun reçoit une affectation `08:00–12:00`, alors les deux écritures sont acceptées.

### Correction rétroactive

Étant donné deux périodes adjacentes, lorsque le début de la seconde est déplacé avant la fin de la première, alors la correction est refusée dans la même instruction SQL.

### Préflight de migration

Étant donné deux périodes historiques `VALID` incompatibles, lorsque V80 est lancée, alors la migration échoue avec le nombre de paires détectées et aucune période n'est modifiée.

### Quarantaine approuvée

Étant donné une période clôturée explicitement approuvée, lorsque le script de quarantaine est exécuté avec son motif et son acteur, alors la ligne reste présente, passe à `QUARANTINED` et V80 peut être relancée si aucun autre conflit `VALID` ne subsiste.

### Quarantaine non autorisée

Étant donné une affectation active ou un ID absent, lorsque la quarantaine est demandée, alors toute la transaction est annulée.

## Erreurs attendues

- migration bloquée : message `HOS-BED-001-D bloque V80` avec nombre de paires ;
- insertion/correction chevauchante : violation de la contrainte `ex_bed_assignments_valid_period_no_overlap` ;
- quarantaine invalide : exception SQL descriptive et rollback complet.

## Sécurité et confidentialité

Le préflight ne restitue aucun nom, téléphone, diagnostic ou donnée clinique. Il expose uniquement les identifiants techniques, le lit, le séjour et les bornes nécessaires à l'arbitrage. L'accès aux résultats doit rester limité au DBA et aux responsables habilités.

## Hors périmètre

- réservation anticipée de lit ;
- interface de correction historique ;
- migration globale vers `TIMESTAMPTZ` ;
- décision automatique sur la période correcte ;
- suppression physique d'une affectation ;
- workflow complet de sortie physique et turnover.

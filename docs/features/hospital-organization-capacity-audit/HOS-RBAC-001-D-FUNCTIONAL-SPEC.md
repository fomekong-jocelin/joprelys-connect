# HOS-RBAC-001-D — Spécification fonctionnelle

## Problème métier

`HOSPITALIZATION_MANAGE` représente plusieurs intentions différentes et n'est plus utilisé par les opérations hospitalières courantes depuis HOS-RBAC-001-B/C. Le conserver comme permission attribuable à un rôle personnalisé maintient une autorité ambiguë et trop large qui n'a plus de signification fonctionnelle cible.

## Objectif

Le catalogue d'autorisations hospitalières ne doit exposer que des permissions correspondant à une intention métier explicite. `HOSPITALIZATION_MANAGE` doit disparaître complètement du catalogue actif et des affectations persistées.

## Utilisateurs concernés

- administrateurs de clinique qui configurent des rôles personnalisés ;
- professionnels utilisant des rôles personnalisés ;
- équipes sécurité/DBA chargées des migrations et de la recette.

## Règles fonctionnelles

1. `HOSPITALIZATION_MANAGE` n'est plus une permission disponible dans le catalogue RBAC.
2. Aucun rôle système ne la reçoit.
3. Aucun rôle personnalisé existant ne conserve cette permission après V86.
4. Le retrait est fail-closed : aucune permission dédiée n'est attribuée automatiquement en remplacement.
5. Un administrateur doit attribuer explicitement les permissions nécessaires selon l'intention : admission, note, consentement, soin, administration médicamenteuse, consommable, transfert, sortie, départ physique ou opérations de lit.
6. Les URL et payloads des API hospitalisation ne changent pas dans cette tâche.
7. Les mentions historiques de `HOSPITALIZATION_MANAGE` peuvent rester dans les documents qui décrivent explicitement l'ancien état, mais aucune documentation d'état courant ne doit la présenter comme permission disponible ou temporairement maintenue.

## Résultat attendu pour un rôle personnalisé legacy

Avant V86 :

```text
ROLE_CUSTOM_X
└── HOSPITALIZATION_MANAGE
```

Après V86 :

```text
ROLE_CUSTOM_X
└── aucune permission hospitalière ajoutée automatiquement
```

Le rôle reste présent ; seule l'association à la permission obsolète disparaît. L'administrateur attribue ensuite les permissions dédiées réellement nécessaires.

## Cas limites

- rôle personnalisé possédant `HOSPITALIZATION_MANAGE` + permissions dédiées : seules les permissions dédiées restent ;
- rôle personnalisé possédant uniquement `HOSPITALIZATION_MANAGE` : il perd les droits hospitaliers d'écriture jusqu'à reconfiguration explicite ;
- base greenfield où la permission n'existe jamais : V86 est idempotente au niveau de l'effet métier (`DELETE` sur zéro ligne) ;
- rôle système : aucune perte inattendue car les rôles système utilisent déjà les permissions dédiées.

## Critères d'acceptation

- [ ] permission absente de la liste RBAC administrable ;
- [ ] aucune route hospitalière ne dépend de la permission retirée ;
- [ ] aucune régression des permissions dédiées de HOS-RBAC-001-B/C ;
- [ ] suppression persistante vérifiée sur upgrade ;
- [ ] aucune élévation automatique de privilège ;
- [ ] documentation d'état courant alignée.

## Hors périmètre

- conception des habilitations professionnelles ;
- ABAC unité/relation de soin ;
- migration générale des rôles personnalisés vers un nouveau modèle de données ;
- suppression du champ historique `users.role`.

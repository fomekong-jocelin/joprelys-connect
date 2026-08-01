# Design technique — Annuaire patient mobile (MOB-2809)

## Architecture

- `PatientDirectoryItem` porte le modèle de lecture de résultat.
- `PatientDirectoryApi` encapsule l'appel relatif `GET /api/patients?q=...` via
  le client réseau central MOB-2804.
- Le dashboard orchestre l'état de recherche et la navigation ; il ne décide ni
  des droits ni du contenu métier.
- L'ouverture du résultat délègue au parcours dossier patient MOB-2810.

## Responsabilités

- Backend : filtrage tenant, autorisation, recherche et données retournées.
- Flutter data/domain : mapping et propagation des erreurs normalisées.
- Flutter presentation : saisie, états visuels, sélection et navigation.

## Configuration et UI

- aucune URL absolue ou valeur d'environnement dans le widget ;
- tokens, thèmes et rayons fournis par le design system central ;
- libellés provenant de l'i18n FR/EN ;
- aucun stockage local supplémentaire de données patient.

## Tests attendus

- sérialisation des résultats ;
- encodage du paramètre de recherche ;
- états chargement/vide/erreur ;
- navigation vers MOB-2810 ;
- absence de régression sur la file active.

## Dette connue

La composition annuaire/file active se trouve encore dans
`active_queue_section.dart`, au-dessus de la limite de 500 lignes. Son découpage
reste suivi par TASK-20260801/C.

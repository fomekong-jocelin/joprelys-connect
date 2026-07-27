# TASK-20260727 — UX Linked Evidence (#192-E)

## Type

P0 — fidélité clinique / UX ambient scribe / anti-hallucination

## Objectif

Permettre au médecin de relire dans la Consultation Joprelys une projection clinique déterministe issue du Clinical Fact Ledger, de remonter chaque élément à sa preuve dans le transcript FINAL, puis de valider explicitement la version relue.

Cette UX ne remplace pas le formulaire clinique manuel et ne copie aucune donnée dans celui-ci automatiquement.

## Architecture

```text
Transcript FINAL canonique
        ↓
Extraction incrémentale structurée (#196)
        ↓
Clinical Fact Ledger (#193)
        ↓
Projection déterministe + Linked Evidence (#204)
        ↓
UX de relecture #207
        ↓
Validation projectionVersion
        ↓
Stale-check transactionnel (#205)
        ↓
Snapshot médical validé
```

## Contrats backend consommés

- `POST /api/ai/consultations/{visitId}/facts/extract`
- `GET /api/ai/consultations/{visitId}/facts/note-projection`
- `POST /api/ai/consultations/{visitId}/facts/note-projection/validations`
- `GET /api/ai/consultations/{visitId}/facts/note-projection/validations`

Le chargement médecin est fail-closed : `extractNewFacts -> projection`. Si l’extraction des nouveaux items FINAL échoue, une projection possiblement incomplète n’est pas présentée comme actuelle.

## UX

### Note clinique sourcée

Sections déterministes :

1. Histoire de la maladie actuelle ;
2. Antécédents ;
3. Allergies ;
4. Constantes ;
5. Évaluation diagnostique ;
6. Médicaments ;
7. Examens et demandes ;
8. Plan / conduite à tenir.

Chaque fait affiche, selon disponibilité :

- concept clinique ;
- valeur(s) et unité ;
- polarité ;
- autorité clinique ;
- temporalité ;
- latéralité ;
- fréquence ;
- voie.

### Linked Evidence

La preuve est repliée par défaut pour préserver la lisibilité mobile.

À l’ouverture :

- locuteur ;
- timestamp début/fin ;
- quote verbatim du transcript FINAL ;
- indication de preuve principale.

### Politique audio

Aucun faux lecteur audio n’est affiché.

À ce jour :

- le navigateur conserve temporairement les chunks chiffrés dans IndexedDB ;
- après ACK serveur, le vault supprime le chunk local ;
- le backend conserve le hash et le journal du chunk, pas les octets audio bruts.

L’UX expose donc uniquement les preuves réellement disponibles durablement : quote FINAL + locuteur + timestamps. Un lecteur audio ne pourra être ajouté qu’après définition et implémentation d’une politique de rétention serveur explicite.

## Validation médicale

- Le bouton `Valider cette version` envoie exactement la `projectionVersion` affichée.
- Un `validationId` UUID est généré pour l’idempotence de la requête.
- Si le backend répond `AI_CLINICAL_NOTE_PROJECTION_STALE`, la version courante est rechargée et une nouvelle relecture est obligatoire.
- Une validation existante de la version courante est affichée comme `Validée`.
- Aucune projection vide n’est validable.

## Navigation / concurrence

- chaque chargement est associé à la visite courante et à une génération locale ;
- une réponse HTTP tardive d’une visite précédente ne peut pas écraser la visite active ;
- un résultat de validation n’est appliqué que si la `projectionVersion` affichée est toujours celle relue au déclenchement.

## Design system

- composants/surfaces Joprelys (`ui-card`, `ui-button`, `ui-link`) ;
- tokens centralisés `--app-*`, `--text-*`, `--brand-*` ;
- aucun thème local forcé ;
- light/dark natif ;
- rayons centralisés ;
- mobile-first ;
- aucun tableau horizontal ;
- preuves repliables pour éviter les layout shifts massifs.

## Internationalisation

Catalogues dédiés :

- `web/src/assets/i18n/features/linked-evidence/fr.json`
- `web/src/assets/i18n/features/linked-evidence/en.json`

Chargement centralisé dans `I18nService`.

## Critères d’acceptation

- [x] client API fortement typé ;
- [x] extraction incrémentale déclenchée avant projection ;
- [x] projection déterministe affichée sans texte inventé côté frontend ;
- [x] preuve transcript FINAL consultable par fait ;
- [x] locuteur et timestamps visibles ;
- [x] aucun faux lecteur audio ;
- [x] validation de la version exactement relue ;
- [x] stale-check 409 traité par rechargement et nouvelle relecture ;
- [x] historique utilisé pour reconnaître une version déjà validée ;
- [x] isolation des réponses lors d’un changement de visite ;
- [x] FR/EN ;
- [x] light/dark via tokens ;
- [x] mobile-first ;
- [x] tests du contrat HTTP ;
- [x] tests de preuve, validation, stale-check et navigation ;
- [x] tests Angular complets verts sur le HEAD final ;
- [x] build Angular production vert sur le HEAD final ;
- [x] fusion dans `main` uniquement après le gate Ready final.

## Non-régression

Cette incision ne modifie pas :

- le pipeline microphone ;
- la Dictée ;
- WebRTC Realtime ;
- le transcript ledger ;
- le fact ledger backend ;
- les endpoints de validation #205 ;
- le formulaire clinique manuel ;
- ordonnance et examens.

## Hors périmètre volontaire

- rolling operations `KEEP / ADD / REPLACE / REMOVE` : chantier séparé de #192 ;
- rétention et lecture serveur de l’audio brut ;
- benchmark clinique 5/15/30/60 min : #192-F.

## Gate CI

Run GitHub Actions **#1593** sur le HEAD exact `6f8203985e818c048b43bd23a9d8395b1c67c8cd` :

- `Detect changed stacks` : SUCCESS ;
- tests Angular : SUCCESS ;
- build Angular production : SUCCESS ;
- backend : SKIPPED car aucun changement `backend/**` ;
- aucun commit ajouté après le gate ;
- PR #207 fusionnée dans `main` au commit `5c949cb4db3cf24299ce2ec63c4f92b7b0e14180`.

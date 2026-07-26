# AI-MEM-MED-001 — Mémoire clinique gouvernée et référentiel médicament

## Statut

- **Epic** : IA clinique / Sécurité de consultation
- **Type** : Feature architecture + sécurité
- **Priorité** : P0
- **Statut** : IN PROGRESS
- **PR** : #154
- **Branche** : `agent/clinical-memory-medication-reference`

## Objectif

Améliorer la continuité du copilote clinique sans augmenter son autonomie décisionnelle :

1. mémoriser uniquement ce que le professionnel a réellement accepté ou confirmé ;
2. normaliser les noms de médicaments vers des concepts stables grâce à des sources autoritatives ;
3. conserver un comportement sûr et disponible lorsque les sources externes sont désactivées ou indisponibles.

## Périmètre A — Mémoire clinique gouvernée

- Mémoire structurée attachée à la session de consultation.
- Faits provenant uniquement du brouillon accepté.
- Réponses provenant uniquement de clarifications explicitement résolues.
- Exclusion des propositions en attente ou refusées.
- Remplacement d'une ancienne réponse lorsqu'une même clarification est résolue à nouveau.
- Snapshot système renouvelé, sans accumulation de plusieurs mémoires concurrentes.
- Transmission multi-provider des messages système dynamiques, y compris Gemini.
- Instruction de ne pas reposer une question déjà résolue, sauf contradiction explicite du tour courant.

## Périmètre B — Référentiel médicament

- Contrat `MedicationReferencePort` indépendant des fournisseurs.
- Résultat normalisé comprenant source, identifiant concept, nom canonique, type de terme, synonymes et statut actif.
- Service de résolution avec priorité des sources et cache.
- Résultat dégradé explicite en cas de panne réseau.
- Référentiel entièrement désactivable.
- Adaptateur RxNorm optionnel : recherche exacte ou normalisée via Prescribable RxNorm puis lecture des propriétés du concept.
- Comparaison conservatrice :
  - `SAME_CONCEPT` seulement si deux résolutions partagent un identifiant dans une même source ;
  - `UNKNOWN` dans tous les autres cas.

## Sources initiales

### RxNorm / RxNav — U.S. National Library of Medicine

Usage prévu : normalisation des concepts, noms canoniques, synonymes et types de termes.

Conformément à la demande de la NLM, tout produit utilisant ces données devra afficher une attribution indiquant que les données sont publiques, que la NLM n'est pas responsable du produit et qu'elle ne l'approuve ni ne le recommande.

### WHO ATC/DDD — tranche ultérieure

Usage prévu : classification internationale et regroupements thérapeutiques, sous réserve des conditions de licence et de redistribution applicables.

### openFDA — tranche ultérieure et enrichissement uniquement

Usage potentiel : données de libellés et avertissements. openFDA ne devra jamais être utilisé seul pour prendre une décision de soin.

## Hors périmètre

- Recommandation thérapeutique autonome.
- Validation automatique d'une ordonnance.
- Déduction qu'une absence de correspondance signifie « médicament différent » ou « absence de risque ».
- Interaction médicament–médicament complète.
- Allergies par classe thérapeutique.
- Ajustement pédiatrique, rénal ou hépatique.
- Posologie calculée automatiquement.
- Référentiel local africain/francophone complet.
- Stockage distribué de la mémoire de session.

## Configuration

```text
JOPRELYS_MEDICATION_REFERENCE_ENABLED=false
JOPRELYS_MEDICATION_REFERENCE_CACHE_TTL_MINUTES=1440
JOPRELYS_MEDICATION_REFERENCE_MAX_CANDIDATES=5
JOPRELYS_RXNORM_ENABLED=false
JOPRELYS_RXNORM_BASE_URL=https://rxnav.nlm.nih.gov
```

Les deux drapeaux globaux et RxNorm doivent être activés pour autoriser les appels réseau.

## Critères d'acceptation — mémoire

- [x] Une proposition refusée n'entre jamais dans la mémoire.
- [x] Une proposition acceptée met à jour les faits mémorisés.
- [x] Une opération CLEAR acceptée supprime le fait correspondant.
- [x] Une clarification résolue est mémorisée avec question, champ, réponse et date.
- [x] Une nouvelle réponse à la même question remplace l'ancienne.
- [x] Un seul message système de mémoire gouvernée est conservé dans l'historique provider.
- [x] Gemini reçoit les messages système dynamiques comme OpenAI.
- [ ] Le moteur empêche déterministiquement la répétition exacte d'une question déjà résolue.
- [ ] La mémoire de session est exposée à l'audit sans données administratives inutiles.

## Critères d'acceptation — référentiel

- [x] Le référentiel est désactivé par défaut.
- [x] Une requête vide ne déclenche aucun appel fournisseur.
- [x] Les sources sont interrogées selon leur priorité.
- [x] Une panne d'une source permet de poursuivre vers la suivante.
- [x] Une panne de toutes les sources retourne un résultat dégradé sans exception métier.
- [x] Deux requêtes textuellement équivalentes partagent le cache.
- [x] Une équivalence positive exige un identifiant partagé.
- [x] L'absence d'identifiant partagé retourne `UNKNOWN`.
- [x] RxNorm utilise les endpoints officiels de recherche et de propriétés.
- [ ] Le garde doublon de prescription utilise l'équivalence normalisée lorsque le référentiel est activé.
- [ ] Les alertes distinguent clairement preuve locale, preuve référentielle et source indisponible.

## Scénarios de test

### MEM-01 — Acceptation
1. Le modèle propose un symptôme.
2. Le professionnel accepte.
3. Vérifier que le fait apparaît dans le brouillon et la mémoire.

### MEM-02 — Refus
1. Le modèle propose un diagnostic.
2. Le professionnel refuse.
3. Vérifier que ni le brouillon ni la mémoire ne contiennent ce diagnostic.

### MEM-03 — Question résolue
1. Joprelys demande la durée.
2. Le professionnel répond « trois jours ».
3. Vérifier que la question et la réponse sont mémorisées.
4. Vérifier qu'une sortie provider suivante reçoit ce contexte.

### MED-01 — Référentiel désactivé
1. Laisser les drapeaux à `false`.
2. Rechercher un nom.
3. Vérifier `MEDICATION_REFERENCE_DISABLED` et zéro appel réseau.

### MED-02 — Concept partagé
1. Résoudre une marque et son principe actif vers le même identifiant.
2. Vérifier `SAME_CONCEPT`.

### MED-03 — Absence de preuve
1. Résoudre deux noms sans identifiant partagé.
2. Vérifier `UNKNOWN`, jamais `DIFFERENT`.

### MED-04 — Source indisponible
1. Simuler une erreur réseau RxNorm.
2. Vérifier que la consultation reste utilisable et que le résultat est marqué dégradé.

## Risques et garde-fous

| Risque | Garde-fou |
|---|---|
| Hallucination stockée comme mémoire | Seules les décisions humaines et réponses résolues alimentent la mémoire |
| Ancienne information devenue fausse | Synchronisation complète depuis le brouillon accepté après chaque acceptation |
| Dépendance forte à une API américaine | SPI multi-source, feature flags, cache et mode non bloquant |
| Faux sentiment de sécurité | Aucun statut `DIFFERENT`; absence de preuve = `UNKNOWN` |
| Nom local absent de RxNorm | Résultat non résolu, futur référentiel local/francophone |
| Utilisation clinique abusive d'openFDA | Enrichissement seulement, avertissement explicite |
| Conditions de licence ATC | Validation juridique avant import ou redistribution |

## Definition of Done

- [x] Mémoire structurée implémentée.
- [x] Gouvernance acceptation/refus implémentée.
- [x] Support multi-provider des messages dynamiques.
- [x] SPI médicament et service cache/fallback implémentés.
- [x] Adaptateur RxNorm optionnel implémenté.
- [x] Configuration désactivée par défaut.
- [x] Tests unitaires mémoire, cache, fallback, équivalence et adaptateur ajoutés.
- [ ] Maven strict vert sur le HEAD final.
- [ ] Garde doublon branché au référentiel avec tests de non-régression.
- [ ] Audit/observabilité des résolutions externes.
- [ ] Attribution NLM visible dans la documentation produit avant activation en production.

# AI-MED-SAFETY-001 — Medication Safety Engine et référentiel RxNorm

## Statut

- **Epic** : IA clinique / Sécurité de prescription
- **Type** : Feature sécurité clinique
- **Priorité** : P0
- **PR** : #155
- **Branche** : `agent/medication-safety-engine-v1`

## Objectif

Créer une couche déterministe de sécurité médicament indépendante du LLM et du fournisseur IA, capable de normaliser un nom de médicament, d’identifier ses principes actifs et ses classes de référence, puis d’imposer une confirmation médicale lorsqu’un conflit démontrable est détecté.

Le moteur ne doit jamais substituer, recommander ou prescrire un traitement. Il produit uniquement des signaux de sécurité qui passent par le système de clarification et de validation Joprelys.

## Référentiels et responsabilités

### RxNorm / RxClass

Utilisés pour :
- identifier un concept médicament ;
- récupérer les principes actifs RxNorm ;
- détecter qu’une marque et un générique partagent le même ingrédient ;
- récupérer les classes ATC comme métadonnées explicatives.

RxNorm/RxClass n’est **pas** considéré comme une base d’interactions médicamenteuses.

### Interaction provider

Une interface séparée `MedicationInteractionProvider` porte les futures interactions autoritatives.

La configuration par défaut est `none`, ce qui signifie explicitement :
- aucune interaction n’est annoncée comme vérifiée ;
- aucune absence d’interaction n’est déduite ;
- le moteur ne fabrique aucune règle pharmacologique à partir du LLM.

## Architecture

```text
Prescription explicitement dictée
          ↓
AiMedicationSafetyGuard
          ↓
MedicationSafetyEngine
    ├── contrôles locaux exacts
    ├── MedicationKnowledgeProvider
    │      └── RxNorm / RxClass
    └── MedicationInteractionProvider
           └── none aujourd’hui
          ↓
Finding déterministe
          ↓
Clarification obligatoire
          ↓
Confirmation du professionnel
```

## Périmètre inclus

- Abstraction `MedicationKnowledgeProvider`.
- Implémentation RxNorm/RxClass.
- Résolution stricte/normalisée du nom, sans fuzzy matching décisionnel.
- Extraction des principes actifs RxNorm.
- Extraction des classes ATC comme métadonnées.
- Cache borné configurable.
- Timeouts réseau configurables.
- Contrôles locaux prioritaires sans dépendance réseau.
- Détection d’allergie exacte au nom du médicament.
- Détection de doublon exact d’un traitement actif.
- Détection de doublon par principe actif entre deux noms différents.
- Détection d’allergie lorsque le médicament proposé et le terme allergique résolu partagent le même ingrédient RxNorm.
- Mode fail-closed configurable si médicament non résolu ou référentiel indisponible.
- Frontière séparée pour les interactions médicamenteuses.
- Internationalisation FR/EN des messages de confirmation.

## Hors périmètre

- Recommandation thérapeutique.
- Substitution automatique.
- Calcul de dose.
- Interprétation d’une allergie de classe uniquement à partir d’une classe ATC commune.
- Interactions médicamenteuses sans source autoritative branchée.
- Contre-indications par pathologie.
- Ajustements grossesse, insuffisance rénale ou hépatique.
- Référentiel local des marques africaines non présentes dans RxNorm.

## Critères d’acceptation

- [x] Les contrôles exacts fonctionnent même si RxNorm est indisponible.
- [x] Le moteur ne dépend pas du LLM pour identifier un doublon ou une allergie exacte.
- [x] Deux noms différents partageant un même ingrédient RxNorm provoquent une confirmation.
- [x] Une allergie résolue vers le même ingrédient RxNorm provoque une confirmation.
- [x] Une classe ATC commune seule ne provoque pas de blocage clinique.
- [x] Aucun fuzzy matching n’est utilisé pour une décision de sécurité.
- [x] Un médicament non résolu provoque une confirmation lorsque `fail-closed=true`.
- [x] Une indisponibilité du référentiel provoque une confirmation lorsque `fail-closed=true`.
- [x] L’absence de provider d’interactions est représentée explicitement par `UNAVAILABLE`.
- [x] Le pipeline de prescription historique reste soumis à la confirmation du professionnel.
- [ ] Tests d’adaptateur RxNorm avec réponses simulées.
- [ ] CI Maven stricte verte sur le HEAD final.

## Scénarios de recette

### MS-01 — Doublon exact
Traitement actif : `Amlodipine`.

Nouvelle prescription : `Amlodipine 5 mg`.

Attendu : confirmation avant proposition.

### MS-02 — Marque / générique, même ingrédient
Traitement actif : `Amoxicilline`.

Nouvelle prescription : marque ou produit résolu par RxNorm contenant l’amoxicilline.

Attendu : confirmation pour principe actif déjà présent.

### MS-03 — Allergie par ingrédient
Allergie documentée : terme résolu vers l’ingrédient `amoxicillin`.

Nouvelle prescription : produit RxNorm contenant `amoxicillin`.

Attendu : confirmation de sécurité avant proposition.

### MS-04 — Médicament local non résolu
Prescription : marque non connue du référentiel.

Avec `fail-closed=true` :

Attendu : Joprelys demande de confirmer le nom exact ; aucune substitution ni correction automatique.

### MS-05 — Référentiel indisponible
Simuler timeout ou indisponibilité RxNorm.

Attendu : les contrôles exacts locaux continuent de fonctionner ; une prescription nécessitant le référentiel est retenue pour vérification manuelle si `fail-closed=true`.

### MS-06 — Classe ATC commune
Deux médicaments de la même classe ATC mais ingrédients différents.

Attendu : aucun conflit automatique sur la seule base de la classe ATC.

### MS-07 — Interactions
`interaction-provider=none`.

Attendu : Joprelys n’affirme ni présence ni absence d’interaction pharmacologique.

## Risques et garde-fous

| Risque | Garde-fou |
|---|---|
| Faux positif par fuzzy matching | Aucun fuzzy décisionnel |
| Faux sentiment de sécurité sur interactions | Provider séparé, `none` explicite |
| RxNorm absent pour une marque locale | Mode fail-closed + confirmation manuelle |
| Référentiel externe lent | Timeouts + cache |
| Classe ATC interprétée comme allergie | Classes non décisionnelles en V1 |
| LLM invente une relation médicament | Décision dans moteur déterministe uniquement |

## Configuration

- `AI_MEDICATION_SAFETY_ENABLED`
- `AI_MEDICATION_SAFETY_FAIL_CLOSED`
- `AI_MEDICATION_KNOWLEDGE_PROVIDER`
- `AI_MEDICATION_INTERACTION_PROVIDER`
- `RXNORM_BASE_URL`
- `RXNORM_CONNECT_TIMEOUT_MILLIS`
- `RXNORM_READ_TIMEOUT_MILLIS`
- `RXNORM_CACHE_MINUTES`

## Definition of Done

- [x] Contrat de connaissance médicament provider-agnostique.
- [x] Contrat d’interactions séparé.
- [x] Adaptateur RxNorm/RxClass.
- [x] Moteur déterministe.
- [x] Intégration au garde de prescription.
- [x] Tests métier des règles fortes.
- [ ] Tests HTTP simulés de l’adaptateur RxNorm.
- [ ] CI globale verte.
- [ ] Recette avec une liste de marques réellement utilisées au Cameroun.
- [ ] Choix et validation d’un provider d’interactions autoritatif avant activation des interactions.

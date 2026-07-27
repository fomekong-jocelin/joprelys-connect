# Joprelys Clinical AI Benchmark V1 — #213

## But

Décider objectivement si le planner rolling #211 peut être branché vers le moteur déterministe #208 sans dégrader la fidélité clinique du pipeline #196.

## Règle CI

La CI ne contacte **aucun modèle externe**. Elle évalue uniquement des sorties candidates versionnées/capturées contre un gold synthétique. Les générations modèle seront produites par un runner manuel séparé, hors CI.

## Corpus

Le corpus V1 est synthétique, sans PHI, FR prioritaire et matérialisable sur des fenêtres de 5, 15, 30 et 60 minutes.

Chaque scénario décrit :

- des tours transcript avec locuteur et offsets ;
- des faits initiaux éventuels ;
- les faits effectifs gold ;
- les opérations rolling gold ;
- les preuves exactes ;
- les faits explicitement critiques.

Les scénarios longs sont matérialisés avec des distracteurs conversationnels non cliniques déterministes entre les événements cliniques. La durée n'est donc pas un simple label : les timestamps et le volume de contexte progressent jusqu'à la durée cible.

## Matching factuel

Un fait est un vrai positif clinique uniquement si son identité relationnelle correspond au gold :

`factType + authority + conceptCode + conceptText + polarity + valuePrimary + valueSecondary + unitCode + temporality + laterality + frequency + route`

La preuve est scorée séparément afin qu'une bonne valeur clinique avec une mauvaise citation ne soit jamais masquée.

## Champs critiques

Le gold marque explicitement les faits critiques. En plus, tout candidat de type suivant est considéré critique par défaut :

- `VITAL` ;
- `MEDICATION` ;
- `ALLERGY` ;
- `ASSESSMENT` ;
- `PLAN` ;
- `ORDER`.

Un fait critique candidat sans correspondance gold exacte compte comme `unsupportedCriticalClaim`.

## Opérations rolling

Le scorer évalue séparément :

- `KEEP` — cible exacte ;
- `ADD` — résultat clinique exact ;
- `REPLACE` — cible exacte + résultat clinique exact ;
- `RETRACT` — cible exacte.

L'absence d'opération sur un fait n'est **jamais** interprétée comme une suppression.

## Gates absolus GO/NO-GO

Aucun score agrégé ne peut compenser une erreur critique. GO interdit si :

- `unsupportedCriticalClaims > 0` ;
- `falseCriticalRetracts > 0` ;
- `falseCriticalReplaces > 0` ;
- rupture critique concept↔nombre/unité ;
- rupture médicament↔dose↔unité↔fréquence↔voie.

Precision/recall globales et preuve sont reportées en plus, mais ne masquent jamais ces gates absolus.

## Comparaison #196 / #211

Une capture benchmark indique son pipeline (`FACT_EXTRACTION_196` ou `ROLLING_PLANNER_211`), son modèle, la version du prompt/schema et le scénario. Les deux pipelines sont scorés avec le même moteur et le même gold.

## Données réelles

Interdites dans le dépôt. Toute future capture issue d'un environnement clinique doit être préalablement synthétisée/désidentifiée et validée avant versioning.

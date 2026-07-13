# Spécification fonctionnelle — Triage ABCDE et réévaluations

## Problème métier

Le dossier d’urgence conserve actuellement un triage initial synthétique, mais ne permet pas de tracer de manière structurée les cinq axes ABCDE ni leurs réévaluations successives. Une urgence peut évoluer rapidement ; chaque constat doit rester daté, attribué et non écrasé.

## Utilisateurs

- infirmier d’urgence ;
- médecin urgentiste ;
- administrateur clinique habilité ;
- lecteur clinique autorisé pour la consultation de l’historique.

## Objectifs

1. Démarrer les soins sans dépendre d’une identité définitive ou d’un paiement.
2. Documenter le triage initial puis chaque réévaluation.
3. Conserver l’historique clinique complet et l’orientation recommandée à chaque étape.
4. Empêcher les modifications après stabilisation du dossier.

## Parcours

```text
Admission urgente
→ triage initial existant
→ création automatique d’une évaluation INITIAL
→ consultation du panneau ABCDE
→ saisie d’une réévaluation complète
→ enregistrement horodaté et séquencé
→ éventuelles réévaluations supplémentaires
→ stabilisation et orientation finale
```

## Axes ABCDE

- A — Airway : non évalué, libre, à risque, obstrué.
- B — Breathing : non évalué, adéquat, détresse, insuffisance.
- C — Circulation : non évalué, stable, compromis, choc.
- D — Disability : non évalué, alerte, réponse à la voix, réponse à la douleur, inconscient.
- E — Exposure : non évalué, aucune anomalie critique, traumatisme, hypothermie, hyperthermie, autre anomalie.

## Données complémentaires

- niveau de triage ;
- état hémodynamique ;
- tension artérielle ;
- fréquence cardiaque ;
- fréquence respiratoire ;
- saturation en oxygène ;
- température ;
- score de Glasgow ;
- douleur sur 10 ;
- orientation recommandée ;
- notes cliniques ;
- heure clinique et auteur.

## Règles métier

- Le journal est append-only : une évaluation enregistrée n’est jamais modifiée ni supprimée par l’API métier.
- L’évaluation initiale est créée automatiquement pour toute nouvelle urgence.
- Les urgences historiques reçoivent une évaluation initiale basée uniquement sur leurs données existantes ; les axes absents restent `NOT_ASSESSED`.
- Une réévaluation exige les cinq axes ABCDE.
- L’heure clinique ne peut pas être future.
- Une urgence stabilisée n’accepte plus de réévaluation.
- L’orientation recommandée n’écrase pas l’orientation finale de stabilisation.
- Le frontend ne calcule aucun niveau de triage ni diagnostic.
- Toutes les lectures et écritures sont tenantées et autorisées.

## Cas limites

- mauvaise connexion : aucun doublon lié à une simple actualisation de lecture ; la création de réévaluation reste une commande explicite ;
- deux professionnels enregistrent simultanément : verrouillage du dossier et séquences distinctes ;
- données historiques incomplètes : affichage « non évalué », sans extrapolation ;
- patient URG-TEMP ensuite rapproché : l’historique reste attaché à l’urgence contributrice et visible via le contexte canonique existant.

## Critères d’acceptation

Voir `docs/ai/tickets/STORY-2302-EMERGENCY-TRIAGE-ABCDE.md`.

## Accessibilité et design

- mobile-first ;
- cibles tactiles lisibles ;
- navigation clavier et focus visible ;
- aucun sens transmis uniquement par la couleur ;
- composants `.ui-card`, `.ui-input` et boutons partagés ;
- rayons 4 à 8 px maximum ;
- thèmes light/dark ;
- textes FR/EN.
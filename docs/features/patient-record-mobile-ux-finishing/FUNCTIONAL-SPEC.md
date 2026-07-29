# FUNCTIONAL SPEC — Finition UX du dossier patient mobile

## Problème métier

Le dossier patient doit permettre au professionnel de retrouver rapidement l’identité et les informations utiles sans lui imposer plusieurs niveaux de lecture redondants. Après la refonte progressive de la PR #226, la recette mobile montre encore un accordéon administratif superflu, des titres trop longs et une hiérarchie d’actions insuffisamment claire.

## Utilisateurs concernés

- médecins ;
- infirmiers et personnels cliniques habilités ;
- agents disposant des droits de lecture du dossier patient.

## Objectif

Faire de `Fiche d’identité` la vraie surface d’identité du patient, limiter les accordéons aux informations secondaires et donner une priorité visuelle évidente à l’action métier principale.

## Parcours attendu

### Arrivée dans le dossier patient

Le professionnel voit immédiatement :

- l’identité du patient dans le bandeau ;
- les numéros DPU / établissement ;
- les alertes de sécurité critiques lorsqu’elles existent ;
- l’action métier principale disponible (`Ouvrir une visite` ou `Démarrer la consultation`).

### Fiche d’identité

La section `Fiche d’identité` affiche directement les informations administratives utiles sans demander l’ouverture d’un accordéon supplémentaire.

Le contact d’urgence peut rester secondaire et replié dans le composant administratif existant.

### Informations secondaires

Deux surfaces restent repliables :

- `Contexte d’urgence` ;
- `Informations médicales` pour allergies, antécédents et vaccinations.

Les titres sont courts, explicites et tiennent sur une ligne sur les téléphones courants à partir de 360 px.

### Actions d’en-tête

Sur mobile :

1. action métier principale en pleine largeur ;
2. ligne secondaire avec `Synthèse PDF` et `Retour` lorsqu’un PDF est disponible ;
3. si le PDF n’est pas disponible, `Retour` reste une action secondaire compacte.

Sur desktop :

- actions regroupées et alignées ;
- action métier principale visuellement prioritaire ;
- `Synthèse PDF` puis `Retour` en secondaires.

## Règles fonctionnelles

- Les alertes critiques et le Break-Glass ne sont jamais masqués par cette finition.
- Le contexte d’urgence conserve son comportement existant : historique replié par défaut, urgence active automatiquement révélée.
- Les informations médicales longitudinales restent repliées par défaut.
- Les permissions existantes déterminent toujours les actions et sections visibles.
- Aucune donnée clinique ou administrative n’est supprimée.
- Aucun endpoint, payload ou statut métier ne change.

## Internationalisation

Libellés cibles :

| Clé | FR | EN |
|---|---|---|
| `patient.urgTemp.emergency.title` | Contexte d’urgence | Emergency context |
| `patient.urgTemp.profile.medicalSection` | Informations médicales | Medical information |

Les autres libellés existants sont réutilisés.

## Critères d’acceptation

- [ ] `Informations administratives` n’apparaît plus comme accordéon.
- [ ] Le résumé administratif est rendu directement dans `Fiche d’identité`.
- [ ] `Contexte d’urgence` reste sur une ligne à 360 px.
- [ ] `Informations médicales` reste sur une ligne à 360 px.
- [ ] Les chevrons restent alignés et accessibles.
- [ ] L’action primaire est visuellement distincte et placée avant les actions secondaires sur mobile.
- [ ] Aucun libellé de bouton ne revient à la ligne.
- [ ] FR/EN, light/dark et focus clavier sont préservés.

## Cas limites

- Patient sans droit `CLINICAL_READ` : pas de Synthèse PDF ni section clinique inaccessible.
- Patient déjà en visite : `Démarrer la consultation` remplace `Ouvrir une visite` comme action principale.
- Patient avec urgence active : contexte urgence peut s’ouvrir automatiquement.
- Patient avec identité provisoire : la carte de régularisation reste visible avant les informations d’identité.

## Hors périmètre

- refonte du contenu des consultations ;
- refonte laboratoire/hospitalisation ;
- changement du PDF ;
- changement du modèle de consentement ;
- backend et base de données.

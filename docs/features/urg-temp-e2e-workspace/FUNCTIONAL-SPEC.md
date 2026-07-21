# Workspace URG-TEMP et continuité de bout en bout

## Objectif

Offrir aux équipes d’urgence un parcours lisible et continu depuis l’arrivée d’un patient non identifié jusqu’à son hospitalisation, sans imposer de rupture entre les écrans ni perdre la provenance des données.

## Parcours cible

```text
Accueil urgence
  → création URG-TEMP
  → triage et réévaluations
  → réanimation / soins immédiats
  → incapacité, base légale, tiers et effets personnels
  → documents vérifiables
  → rapprochement ou décision reportée
  → DPU canonique
  → hospitalisation liée à l’urgence
  → facturation différée
```

## Workspace urgence

Le tiroir d’une urgence active présente cinq espaces :

1. **Synthèse** — arrivée, triage, état hémodynamique, constantes et prochaine étape ;
2. **Identité** — identifiants, statut, observations et action de rapprochement ;
3. **Soins** — ABCDE, réévaluations et chronologie de réanimation ;
4. **Médico-légal** — capacité, base d’urgence, tiers et effets personnels ;
5. **Documents** — lot vérifiable et accès à la continuité hospitalière.

Le numéro `URG-TEMP` et le statut d’identité restent visibles dans l’en-tête du dossier.

## Navigation du parcours

### Vers le rapprochement

Depuis une urgence provisoire, l’action « Rapprocher l’identité » ouvre la file de rapprochement avec :

- le patient source présélectionné ;
- l’identifiant de l’urgence conservé dans le contexte de navigation.

### Après la décision

Après `CREATE_NEW_DPU`, `LINK_EXISTING_DPU`, `DEFER` ou une correction :

- le résultat reste visible ;
- le DPU canonique peut être ouvert ;
- l’urgence source est résolue ;
- l’utilisateur peut poursuivre vers l’hospitalisation sans rechercher manuellement le patient.

### Vers l’hospitalisation

L’action ouvre :

```text
/patients/{canonicalPatientId}/hospitalizations?emergencyId={emergencyId}
```

Le panneau d’hospitalisation :

- conserve l’urgence source ;
- sélectionne uniquement des services autorisant les chambres ;
- propose uniquement les lits libres ;
- crée ou réutilise la visite de continuité ;
- génère le lot documentaire ;
- évite une double admission si la génération documentaire échoue après l’admission.

### Stabilisation

Lorsqu’une urgence est stabilisée avec l’orientation `ADMISSION` ou `OR_DIRECT`, le workspace poursuit directement vers l’hospitalisation au lieu de renvoyer vers une liste où l’urgence stabilisée ne serait plus visible.

## Scénarios couverts

1. patient inconscient arrivé seul ;
2. patient amené par un tiers ;
3. triage et réanimation avant formalités administratives ;
4. hospitalisation et finance différée ;
5. reprise de conscience et création d’un nouveau DPU ;
6. rapprochement avec un DPU existant ;
7. plusieurs candidats et décision reportée ;
8. erreur réseau et rejeu avec la même clé d’idempotence ;
9. tentative cross-tenant refusée ;
10. correction d’un mauvais rapprochement avec conservation de l’historique.

## États et erreurs

- les chargements sont localisés par panneau ;
- une erreur de candidats n’efface pas l’historique ;
- une réponse obsolète est ignorée après changement de patient ;
- une erreur documentaire après admission est signalée comme telle ;
- un bouton de continuité est désactivé tant que l’urgence source n’est pas résolue ;
- aucun UUID technique n’est demandé à l’utilisateur.

## Accessibilité et responsive

- boutons et onglets utilisables au clavier ;
- focus visible ;
- tiroir plein écran sur mobile ;
- grille d’onglets adaptée au mobile ;
- textes FR/EN ;
- thèmes clair/sombre ;
- aucun composant Angular Material.

## Sécurité

- permissions existantes conservées ;
- décision humaine obligatoire pour le rapprochement ;
- isolation tenant appliquée côté backend ;
- aucune fusion automatique ;
- traçabilité append-only des décisions ;
- aucune réécriture des données historiques lors du rapprochement.

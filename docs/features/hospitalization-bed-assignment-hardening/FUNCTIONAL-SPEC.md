# FUNCTIONAL-SPEC — Durcissement de l’attribution du lit

## Objectif

Garantir que les deux parcours d’admission proposés au praticien utilisent la même définition d’un lit réellement admissible et qu’un échec documentaire après création du séjour reste visible et récupérable sans créer un second séjour.

## Périmètre livré dans cette tâche

- admission normale : proposer uniquement un lit disponible, ouvert et prêt ;
- admission normale et continuité urgence : proposer uniquement les médecins actifs affectés au service sélectionné ;
- continuité urgence : conserver l’écran après un échec documentaire ;
- permettre une régénération documentaire sans rejouer l’admission ;
- backend : refuser un praticien responsable inactif, hors établissement ou non affecté à l’unité au moment de l’admission.

## Hors périmètre

Les statuts de demande/préadmission/réservation/arrivée, le handoff, la compatibilité clinique patient-lit et la file d’attente sans lit restent dans `HOS-ADM-001`, `HOS-MOV-001` et `HOS-PATH-001`.

## Critères d’acceptation

- Un lit `available=true` mais `capacityStatus != OPEN` ou `readinessStatus != READY` n’est jamais proposé dans une admission normale.
- Si le séjour est créé mais le lot documentaire échoue, le praticien voit l’avertissement et une action de régénération.
- La régénération réussie ferme le parcours sans POST d’admission supplémentaire.
- Un praticien désactivé, d’un autre établissement ou sans affectation active à l’unité est refusé côté backend.
- Une collision de lit ou une erreur de validation reste exprimée sans exposer de détail interne.

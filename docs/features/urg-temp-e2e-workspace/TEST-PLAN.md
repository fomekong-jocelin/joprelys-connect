# Plan de qualification E2E — URG-TEMP

## Préconditions

- un établissement de recette ;
- un compte Accueil avec `PATIENT_WRITE` ;
- un compte Urgence avec droits de triage et médico-légal ;
- un compte autorisé au rapprochement `PATIENT_MERGE` ;
- un compte autorisé à l’hospitalisation et aux documents ;
- un service `HOSPITALIZATION` ou `EMERGENCY` avec un lit libre ;
- un DPU vérifié candidat au rapprochement ;
- aucun outil de suivi tiers sur les écrans cliniques.

## Campagne automatisée

### Admission et urgence

- création atomique patient provisoire + urgence ;
- idempotence de la demande d’admission ;
- création sans nom, date de naissance ou téléphone fictif ;
- arrivée seule et arrivée accompagnée ;
- triage initial et réévaluations ;
- réanimation avant régularisation.

### Rapprochement

- présélection depuis le workspace urgence ;
- candidats multiples sans sélection automatique ;
- création d’un nouveau DPU ;
- lien vers un DPU existant ;
- décision reportée ;
- conservation de la même clé lors d’un rejeu réseau ;
- refus cross-tenant ;
- correction avec événement append-only ;
- résolution de l’urgence source après décision.

### Documents et hospitalisation

- onglet Documents accessible ;
- génération idempotente du lot ;
- navigation vers l’hospitalisation avec `emergencyId` ;
- stabilisation `ADMISSION` et `OR_DIRECT` poursuivant le parcours ;
- service non spatial absent ;
- lit non libre absent ;
- création de la visite de continuité ;
- lien urgence ↔ visite ↔ hospitalisation ;
- facture provisoire marquée à régulariser ;
- agrégation depuis le DPU canonique.

### Non-régression

- admission normale inchangée ;
- urgence d’un patient déjà identifié ;
- consultation, prescription et examens ;
- transfert de lit ;
- sortie et libération du lit ;
- historique et correction du rapprochement ;
- sélecteurs de langue sans fuite de drapeaux sur les boutons métier.

## Recette manuelle principale

1. Ouvrir l’accueil et choisir le parcours urgence.
2. Sélectionner « Patient non identifié ».
3. Créer le dossier avec uniquement les observations disponibles.
4. Vérifier le numéro `URG-TEMP` et l’ouverture de l’urgence.
5. Saisir le triage ABCDE et une réévaluation.
6. Ajouter un acte de réanimation.
7. Ajouter la capacité, la base légale, un tiers et un effet personnel.
8. Ouvrir l’onglet Documents et vérifier les états avant génération.
9. Ouvrir le rapprochement depuis le workspace.
10. Vérifier la présélection du patient et les candidats expliqués.
11. Enregistrer une décision avec preuve et justification.
12. Vérifier la carte de résultat et l’alias conservé.
13. Poursuivre vers l’hospitalisation.
14. Choisir un service autorisé, un lit libre et un médecin.
15. Valider l’admission.
16. Vérifier l’hospitalisation, la visite de continuité et le lot documentaire.
17. Télécharger le billet d’entrée.
18. Créer une facture sans paiement.
19. Vérifier l’historique clinique, documentaire et financier sur le DPU canonique.

## Variantes obligatoires

- patient arrivé seul ;
- patient accompagné avec déclaration non vérifiée ;
- décision `DEFER` puis reprise ;
- `CREATE_NEW_DPU` ;
- `LINK_EXISTING_DPU` ;
- correction du lien ;
- coupure réseau simulée avant réponse ;
- tentative avec un compte d’un autre établissement ;
- mobile 320/375 px ;
- tablette 768 px ;
- desktop 1024/1366/1920 px ;
- thèmes clair et sombre ;
- français et anglais ;
- navigation clavier.

## Preuves attendues

- identifiants métier du patient, de l’urgence, de la visite et de l’hospitalisation ;
- capture des candidats et de la justification ;
- événement de rapprochement ;
- numéros, versions et hashes documentaires ;
- facture et statut de régularisation ;
- preuve que les éléments d’origine n’ont pas été renumérotés ;
- résultat des tests Angular, Maven et PostgreSQL 16.

## Décision de clôture

La story peut être clôturée lorsque :

- les tests automatisés et builds sont verts ;
- le parcours principal est réalisable sans recherche manuelle du patient entre les étapes ;
- les variantes critiques sont couvertes ;
- aucune régression bloquante n’est observée ;
- la recette humaine est consignée par les métiers concernés.

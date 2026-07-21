# Plan de tests — continuité URG-TEMP

## Objectif

Valider que la continuité urgence → hospitalisation → documents → finance fonctionne sans régression des admissions normales, du typage spatial, du rapprochement et de l'isolation tenant.

## Tests automatisés backend

### Admission

- admission normale avec `visitId` existant ;
- admission depuis `emergencyId` sans visite : création contrôlée d'une visite ;
- conservation de `emergencyId` dans l'hospitalisation ;
- liaison de la visite à l'urgence ;
- refus d'une urgence appartenant à un autre DPU canonique ;
- refus cross-tenant ;
- refus d'une seconde hospitalisation pour la même urgence ;
- refus d'une hospitalisation active sous une identité source ou canonique ;
- refus d'un lit absent, non spatial ou non libre ;
- concurrence de réservation du même lit.

### Documents

- génération du lot minimal ;
- contenus issus du triage, de la réanimation et du dossier médico-légal ;
- numéro, hash, version, URL et QR renseignés ;
- rejeu idempotent sans duplication ;
- billet d'entrée persisté ;
- agrégation sur le DPU canonique avec `originPatientId` ;
- conservation après rapprochement et correction.

### Finance

- facture provisoire créée sans paiement initial ;
- statut `REGULARIZATION_PENDING` pour `PROVISIONAL_URGENCY` et `DECLARED` ;
- statut `RESOLVED` pour `VERIFIED` ;
- agrégation des factures source et canonique ;
- références et lignes inchangées après rapprochement ;
- refus d'une visite étrangère au contexte canonique ;
- absence de duplication lors du rejeu.

### Migration

- migration PostgreSQL 16 depuis le schéma précédent ;
- factures historiques qualifiées `RESOLVED` ;
- colonne obligatoire sans défaut persistant ;
- index et clé étrangère créés ;
- compatibilité H2 de test.

## Tests automatisés Angular

- seuls les services `allowsRooms=true` sont proposés ;
- les services administratifs et médico-techniques sont absents ;
- seuls les lits `FREE` sont proposés ;
- affichage du numéro URG-TEMP et du bandeau d'identité provisoire ;
- payload d'admission contient `emergencyId` sans exiger `visitId` ;
- génération documentaire déclenchée après admission ;
- échec documentaire présenté sans proposer une seconde admission ;
- annulation retire le contexte d'urgence ;
- rechargement de l'historique après succès ;
- FR/EN, light/dark, clavier et responsive.

## Recette navigateur

### Scénario principal

1. Créer un patient inconscient en mode URG-TEMP.
2. Vérifier le numéro provisoire.
3. Compléter le triage et au moins une réévaluation.
4. Ajouter un acte de réanimation.
5. Ajouter un tiers, une incapacité/base légale et un effet personnel.
6. Rapprocher le patient avec un DPU existant.
7. Ouvrir le DPU canonique puis l'onglet Hospitalisation avec le contexte d'urgence.
8. Sélectionner un service d'hospitalisation et un lit libre.
9. Valider l'admission.
10. Vérifier l'hospitalisation, le lien d'urgence et la visite.
11. Vérifier le lot documentaire et télécharger le billet d'entrée.
12. Créer une facture sans paiement.
13. Vérifier l'historique clinique, documentaire et financier depuis le DPU canonique.

### Non-régression

- admission normale sans urgence ;
- transfert de lit ;
- sortie et libération du lit ;
- création d'une facture pour un patient vérifié ;
- rapprochement reporté ;
- correction d'un mauvais rapprochement ;
- isolation entre deux établissements.

## Matrice d'écrans

- desktop : 1024, 1366, 1440, 1920 px ;
- tablette : 768 px ;
- mobile : 320 et 375 px ;
- thème clair et sombre ;
- français et anglais ;
- navigation clavier et focus visible.

## Critères de passage

- CI Maven stricte verte ;
- tests Angular et build production verts ;
- migrations H2/PostgreSQL vertes ;
- aucune régression des admissions normales ;
- recette principale réussie sur l'environnement de recette ;
- preuve de la continuité des numéros/hashes et de la non-duplication ;
- validation métier manuelle documentée avant clôture définitive.

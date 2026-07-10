# Spécification fonctionnelle — Poste caissier et file d’encaissement

## 1. Problème utilisateur

Le poste caisse actuel permet d’ouvrir et clôturer une session, de suivre les mouvements et de consigner des dépenses. Il ne fournit toutefois aucune liste des factures qui attendent un règlement patient. Le caissier doit connaître le patient ou revenir au workspace Factures, ce qui ralentit le traitement et augmente le risque d’erreur.

## 2. Acteurs

- **CAISSIER** : traite les règlements et consulte sa session.
- **AGENT_ACCUEIL** : peut assurer le rôle de caisse dans les petites structures.
- **ADMIN_CLINIQUE** : supervise et peut réaliser les mêmes opérations.
- **DAF** : peut consulter et intervenir selon les droits existants.

Les rôles MEDECIN et INFIRMIER n’accèdent pas à la file d’encaissement.

## 3. Préconditions

- utilisateur authentifié dans une organisation active ;
- facture validée ;
- part patient restante strictement positive ;
- session de caisse active pour confirmer un règlement.

## 4. Contenu de la file

Chaque entrée présente :

- nom du patient ;
- numéro DPU ;
- téléphone lorsqu’il est disponible ;
- numéro de facture ;
- date de validation ou, à défaut, date de création ;
- montant total de la facture ;
- reste patient à encaisser ;
- état `À encaisser` ou `Paiement partiel` ;
- action principale `Encaisser`.

La file est triée du dossier le plus ancien au plus récent afin de traiter d’abord les attentes les plus longues.

## 5. Éligibilité

### Inclus

- `PATIENT_DUE` ;
- `PATIENT_PARTIALLY_PAID`.

### Exclus

- `NOT_YET_DUE` ;
- `INSURANCE_DUE` ;
- `SETTLED` ;
- `CANCELLED` ;
- toute facture dont le reste patient est nul ou négatif.

## 6. Parcours principal

1. Le caissier ouvre l’onglet Caisse.
2. Le système vérifie la session active et charge la file.
3. Le caissier recherche éventuellement un patient, un DPU ou une facture.
4. Il sélectionne `Encaisser` sur une entrée.
5. La modale propose le reste patient comme montant par défaut.
6. Le caissier choisit le moyen de règlement.
7. Pour un chèque ou un virement, il saisit obligatoirement la référence.
8. Le système enregistre le paiement dans la session active.
9. Le reçu numéroté est chargé et affiché.
10. La file est rechargée : une facture soldée disparaît, une facture partiellement payée reste avec son nouveau solde.

## 7. Session fermée

Lorsque la session est fermée :

- l’ouverture de session reste l’action prioritaire ;
- aucun règlement ne peut être confirmé ;
- la file ne présente pas d’action active d’encaissement ;
- un message explique clairement la précondition.

## 8. Recherche et filtres

Une recherche unique filtre sans nouvel appel réseau sur :

- le nom du patient ;
- le DPU ;
- le numéro de facture ;
- le téléphone.

Un filtre permet de distinguer :

- toutes les entrées ;
- les factures jamais réglées ;
- les paiements partiels.

## 9. États d’interface

- **Chargement** : squelette et message annoncé aux technologies d’assistance.
- **Erreur** : message explicite et bouton `Réessayer`.
- **Vide global** : aucune facture patient à encaisser.
- **Recherche vide** : aucune entrée ne correspond aux critères.
- **Enregistrement** : action bloquée et libellé de progression.
- **Succès** : reçu numéroté avec montant et moyen de règlement.

## 10. Règles de saisie

- montant > 0 ;
- montant ≤ reste patient ;
- référence obligatoire pour `CHECK` et `BANK_TRANSFER` ;
- espaces de référence supprimés en début et fin ;
- une seule soumission active à la fois.

## 11. Responsive et accessibilité

- cartes sur mobile, disposition plus dense sur tablette/desktop ;
- cibles tactiles de 44 px minimum ;
- statut et montant non communiqués uniquement par la couleur ;
- focus visible avec les tokens du design system ;
- modale avec `role="dialog"`, `aria-modal`, titre associé et fermeture Échap ;
- focus placé dans la modale puis restauré sur l’entrée de file ;
- textes FR/EN persistés ;
- support light/dark et `prefers-reduced-motion`.

## 12. Hors périmètre

- part assurance et bordereaux ;
- paiement groupé de plusieurs factures ;
- remboursement ou avoir ;
- impression PDF du reçu ;
- résolution des écarts par la DAF ;
- pagination serveur avant retour d’expérience pilote.
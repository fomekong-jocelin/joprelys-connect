# Plan de tests — File d’encaissement caissier

## 1. Backend — service

### Éligibilité

- inclut une facture `VALIDATED` avec `PATIENT_DUE` ;
- inclut une facture `PARTIALLY_PAID` avec reste patient positif ;
- exclut `PENDING` et `PROFORMA` ;
- exclut `INSURANCE_DUE` ;
- exclut `SETTLED` et `CANCELLED` ;
- exclut un reste patient inférieur ou égal à la tolérance financière.

### Tri

- trie d’abord par `validatedAt` croissant ;
- utilise `createdAt` comme ordre secondaire stable.

### Enrichissement patient

- expose nom, DPU et téléphone ;
- ignore prudemment une facture dont le patient n’existe plus au lieu d’exposer une donnée incohérente.

## 2. Backend — API et sécurité

- `CAISSIER` reçoit `200` ;
- `AGENT_ACCUEIL`, `ADMIN_CLINIQUE` et `DAF` reçoivent `200` ;
- `MEDECIN` et `INFIRMIER` reçoivent `403` ;
- utilisateur non authentifié reçoit `401` ;
- une organisation ne voit jamais la file d’une autre organisation ;
- le DTO ne contient aucune donnée clinique ou part assurance inutile.

## 3. Backend — paiement existant

Régression :

- paiement total retire la facture de la file ;
- paiement partiel conserve la facture avec le nouveau solde ;
- paiement supérieur au reste patient renvoie une erreur ;
- paiement sans session active est refusé ;
- paiement chèque/virement sans référence est refusé si le contrat backend l’exige ;
- mouvement et reçu sont créés dans la session courante.

## 4. Frontend — composant de file

- affiche un état de chargement distinct ;
- affiche une erreur et déclenche `Réessayer` ;
- affiche l’état vide global ;
- affiche l’état vide de recherche ;
- rend l’identité, la facture, la date et le reste patient ;
- filtre par nom, DPU, téléphone et numéro de facture ;
- filtre les dossiers jamais réglés et partiellement réglés ;
- calcule les indicateurs de file sans modifier les montants ;
- ouvre la modale avec le reste patient comme montant maximum ;
- désactive l’action pendant le paiement ;
- recharge la file après succès ;
- affiche le reçu numéroté ;
- conserve un message explicite si le reçu ne peut pas être chargé après un paiement réussi.

## 5. Frontend — modale

- accepte le contrat minimal de facture ;
- initialise le montant avec le reste patient ;
- bloque montant nul, négatif ou supérieur au maximum ;
- exige une référence pour chèque et virement ;
- expose `role="dialog"`, `aria-modal` et un titre associé ;
- se ferme avec Échap ;
- place le focus sur le premier champ ;
- restaure le focus sur l’action `Encaisser` à la fermeture.

## 6. Responsive et design

Contrôles manuels :

- 360 px : cartes empilées, aucun débordement, action pleine largeur ;
- 768 px : grille lisible et filtres alignés ;
- 1440 px : file dense sans étirement excessif ;
- light/dark : contrastes, surfaces et statuts lisibles ;
- zoom 200 % : aucune perte d’information ;
- `prefers-reduced-motion` : animations décoratives neutralisées.

## 7. Accessibilité clavier

- ordre de tabulation logique ;
- focus visible ;
- recherche et filtres libellés ;
- statut non communiqué uniquement par couleur ;
- ouverture/fermeture de modale sans perte de focus ;
- messages chargement, erreur et succès annoncés.

## 8. Commandes de validation

### Backend

```bash
cd backend
./mvnw clean verify -B -Dspring.profiles.active=test
```

Cette commande doit inclure H2 et PostgreSQL 16 via Testcontainers.

### Frontend

```bash
cd web
npm ci
npm test
npm run build
```

## 9. Critères de sortie

- tous les tests automatisés verts ;
- aucun test désactivé ou contourné ;
- CI backend et frontend verte ;
- QA visuelle responsive/light/dark effectuée ;
- validation du flux par Product/DAF ;
- ticket et changelog à jour avant fusion.
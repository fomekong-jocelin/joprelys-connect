# Plan de tests — Triage ABCDE

## Backend — service

- création automatique d’une évaluation initiale ;
- valeurs `NOT_ASSESSED` quand le bloc ABCDE initial est absent ;
- conservation des valeurs explicites quand le bloc est fourni ;
- ajout d’une réévaluation avec séquence suivante ;
- refus d’une heure future ;
- refus après stabilisation ;
- ordre chronologique stable ;
- deux créations concurrentes sans séquence dupliquée.

## Backend — API et sécurité

- GET historique avec `EMERGENCY_READ` ;
- POST avec `EMERGENCY_WRITE` ;
- refus sans permission ;
- urgence inexistante ;
- lecture et écriture cross-tenant impossibles ;
- validation des enums et bornes des constantes ;
- réponse `201` avec en-tête `Location`.

## Base et migrations

- Flyway H2 ;
- Flyway PostgreSQL 16 ;
- présence de la table, des contraintes et des index ;
- reprise d’une urgence historique ;
- aucune régression des migrations V58 à V63.

## Frontend

- chargement et affichage de l’historique ;
- état vide si aucune ligne ;
- état erreur avec nouvelle tentative ;
- formulaire avec cinq axes ABCDE ;
- validation des bornes ;
- enregistrement puis rafraîchissement ;
- bouton désactivé pendant la requête ;
- traductions FR/EN ;
- absence de texte métier visible codé en dur ;
- rendu mobile, light/dark et focus clavier.

## Non-régression

- admission normale ;
- urgence avec patient identifié ;
- urgence URG-TEMP ;
- idempotence de l’admission provisoire ;
- réanimation ;
- stabilisation ;
- rapprochement DPU et timeline canonique ;
- dossier médico-légal.

## Commandes attendues

```bash
cd backend
./mvnw clean verify -B -Dspring.profiles.active=test

cd web
npm test
npm run build
```

`npm run lint` est exécuté uniquement si le script existe dans `package.json`; son absence doit être signalée sans inventer de commande.

## Recette manuelle

1. Créer une urgence URG-TEMP.
2. Ouvrir le dossier et vérifier l’évaluation initiale.
3. Ajouter une réévaluation ABCDE complète.
4. Vérifier l’ordre et les heures.
5. Ajouter une seconde réévaluation avec une autre orientation recommandée.
6. Stabiliser le dossier.
7. Vérifier qu’une nouvelle réévaluation est refusée.
8. Vérifier avec un utilisateur sans permission.
9. Vérifier avec un utilisateur d’un autre établissement.
10. Contrôler mobile 360 px, tablette 768 px, desktop, light/dark, FR/EN et clavier.
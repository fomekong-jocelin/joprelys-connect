# TEST-PLAN — Agenda personnel du médecin

## 1. Backend

### Authentification et autorisation

- requête sans token → `401` ;
- patient authentifié → `403` ;
- infirmier authentifié → `403` ;
- médecin actif avec `APPOINTMENT_READ_OWN` → `200` ;
- agent d’accueil avec `APPOINTMENT_READ` mais sans `APPOINTMENT_READ_OWN` → `403` ;
- médecin désactivé → `401`, rejeté pendant l’authentification ;
- matrice catalogue : médecin autorisé, accueil et administrateur clinique exclus.

### Isolation

- le médecin A voit ses rendez-vous ;
- le médecin A ne voit jamais ceux du médecin B ;
- un rendez-vous d’un autre établissement n’est jamais retourné.

### Période

- intervalle valide → résultats triés par heure croissante ;
- `to == from` → `400 VALIDATION_ERROR` ;
- `to < from` → `400 VALIDATION_ERROR` ;
- période de 93 jours → `400 VALIDATION_ERROR` ;
- borne `from` incluse ;
- borne `to` exclue.

### Données

- tous les statuts sont retournés ;
- la réponse contient uniquement les champs contractuels ;
- le nom et le numéro local patient sont présents ;
- aucune coordonnée ou donnée clinique n’est sérialisée.

## 2. Frontend

- charge la semaine courante à l’initialisation ;
- affiche les rendez-vous regroupés par jour ;
- trie les rendez-vous d’une journée par heure ;
- navigation semaine précédente / suivante / aujourd’hui ;
- affiche l’état vide ;
- affiche l’erreur et permet une relance ;
- actualise automatiquement toutes les 30 secondes ;
- détruit le polling à la destruction du composant ;
- traduit les statuts et libellés FR/EN ;
- route et menu visibles uniquement avec `APPOINTMENT_READ_OWN`.

## 3. Commandes attendues

```bash
cd backend
./mvnw clean verify

cd ../web
npm run test
npm run build
npm run i18n:check
```

## 4. Recette fonctionnelle

1. Se connecter comme patient et réserver un créneau futur.
2. Se connecter comme médecin associé dans un autre navigateur.
3. Ouvrir « Mon agenda ».
4. Vérifier l’apparition du rendez-vous, au chargement ou au plus tard après 30 secondes.
5. Vérifier le nom patient, l’heure, le motif et le statut.
6. Se connecter comme un autre médecin et confirmer l’absence du rendez-vous.
7. Annuler côté patient et vérifier le statut annulé après actualisation.
8. Se connecter comme agent d’accueil et confirmer que « Mon agenda » n’est ni affiché ni accessible.

## 5. Critère de sortie

La PR ne passe en revue que si les suites automatiques sont vertes et si la recette croisée patient/médecin est signée par QA.

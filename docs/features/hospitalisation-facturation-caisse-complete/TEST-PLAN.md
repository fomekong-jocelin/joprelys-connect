# TEST-PLAN — Hospitalisation, Facturation et Caisse complètes

## 1. Objectif

Définir les vérifications nécessaires pour livrer les modules hospitalisation, facturation, caisse et comptabilité minimale sans régression sur les flux existants.

## 2. Tests backend attendus

| Domaine | Cas | Niveau |
|---|---|---|
| Hospitalisation | Admission impossible sans lit libre | Integration |
| Hospitalisation | Ajout soin impossible sur séjour clôturé | Integration |
| Hospitalisation | Administration médicament horodatée et auditée | Unit + Integration |
| Bloc | Validation CRO crée actes facturables | Unit + Integration |
| Facturation | Devis converti en facture sans perte de lignes | Unit + Integration |
| Facturation | Facture validée immuable | Unit + Integration |
| Facturation | Avoir requis pour correction après validation | Unit |
| Caisse | Paiement impossible sans session ouverte | Integration |
| Caisse | Clôture calcule solde théorique et écart | Unit + Integration |
| Caisse | Dépense > 100 000 FCFA requiert double visa | Unit + Integration |
| Assurance | Bordereau mensuel liste créances tiers payant | Integration |
| Comptabilité | Facture validée génère débit 411 / crédit 706 | Unit |
| Comptabilité | Encaissement caisse génère débit 571 / crédit 411 | Unit |
| Sécurité | Rôles caissier/DAF/soignant isolés | Security |
| Multi-tenant | Aucun accès cross-tenant aux factures/caisse | Integration |

## 3. Tests Angular attendus

| Écran / composant | Cas |
|---|---|
| Séjour hospitalier | Affichage onglets entrée, soins, bloc, sortie |
| Feuille de soins | États loading, empty, erreur, ajout réussi |
| Facturation | Devis, facture, validation, reçu |
| Caisse | Ouverture, encaissement, clôture, écart |
| Créances | Filtres patient/assurance/statut |
| i18n | Clés FR/EN présentes |
| Thème | Light/dark sans contraste bloquant |

## 4. Commandes

```bash
cd backend
./mvnw test
./mvnw clean verify

cd ../web
npm run test
npm run build
```

## 5. Critères de réussite

- [ ] Aucun test existant ne régresse.
- [ ] Tous les endpoints critiques ont au moins un test succès, un test validation et un test autorisation.
- [ ] Les règles de caisse et d'immuabilité facture ont des tests unitaires dédiés.
- [ ] Les composants Angular dépassant 500 lignes sont refactorés.
- [ ] Build Angular OK.
- [ ] Tests backend OK.

## 6. Tests non fonctionnels

- Vérifier l'absence de secrets dans les logs.
- Vérifier que les erreurs financières ne renvoient pas de stack trace.
- Vérifier que les exports/PDF ne contiennent que les données du tenant courant.
- Vérifier que les actions sensibles sont auditées.

## 7. Historique

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Codex | Création du plan de test cible |

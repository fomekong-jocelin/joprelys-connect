# Plan de test — Postes métier hospitalisation et caisse

## Tests métier backend

- Admission : lit disponible, concurrence, droits, création et audit.
- Soins/médicaments : séjour actif, auteur, horodatage, correction contrôlée et tenant.
- Bloc/sortie : consentement, transitions autorisées, génération documentaire et interdiction après sortie selon règle validée.
- Caisse : session obligatoire, espèces/chèque/virement ventilés, reçu unique, dépense sous/sur seuil, dépôt, clôture et écart.
- RBAC : chaque endpoint sensible couvert en autorisé et refusé.

## Tests Angular

- Composants présentateurs : données, états vides, chargement, erreur et droits.
- Façades : appels aux contrats read model, pas de calcul métier local.
- i18n : clés FR/EN présentes pour tous les nouveaux libellés.
- Accessibilité : navigation clavier, focus visible, libellés, erreurs liées aux champs, tableaux sémantiques et contraste light/dark.

## E2E critiques

1. Admission → transfert → transmissions → sortie → document de sortie.
2. Patient hospitalisé → soin/administration autorisée → trace dans la timeline.
3. Facture avec tiers payant → encaissement patient → reçu → assurance due → règlement assurance → état soldé.
4. Ouverture caisse → paiement espèces → dépense/dépôt autorisé → clôture avec et sans écart → traitement DAF.

## Seuils de sortie

- `backend/mvnw test` et, avant release, `backend/mvnw clean verify` réussis ;
- `web/npm run test -- --watch=false` et `web/npm run build` réussis ;
- aucune non-conformité P0/P1 de sécurité, RBAC ou audit ;
- validation métier des scénarios cliniques et DAF consignée dans le ticket.

## Résultats du premier incrément

- `web/npm run build` : OK.
- `web/npm run test -- --watch=false` : OK, 23 fichiers et 111 tests.
- `HospitalizationStayHeaderComponent` : rendu du contexte clinique et émission de l'action de transfert couverts.
- Panneaux notes, soins, médicaments et consommables : chargement de trace et enregistrement vers les API existantes couverts.

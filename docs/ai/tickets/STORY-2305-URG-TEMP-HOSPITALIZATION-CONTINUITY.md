# STORY-2305 — Continuité URG-TEMP vers hospitalisation

- GitHub : #46
- PR : #96
- Statut : IN REVIEW
- Priorité : P0
- Impact : backend, Angular, PostgreSQL, documents, facturation

## Livré sur la branche

- [x] admission depuis une urgence sans visite préalable ;
- [x] lien explicite urgence ↔ visite ↔ hospitalisation ;
- [x] résolution canonique et prévention des doublons source/cible ;
- [x] admission sur structure typée et lit libre ;
- [x] cinq documents d'urgence vérifiables ;
- [x] billet d'entrée persisté et versionné ;
- [x] statut financier différé ;
- [x] agrégation canonique hospitalisations/documents/factures ;
- [x] interface de continuité avec avertissement d'identité provisoire ;
- [x] textes FR/EN ;
- [x] premiers tests backend et Angular ;
- [x] spécifications fonctionnelle, technique et plan de tests.

## Validation requise

- [ ] CI backend Maven stricte verte sur le dernier commit ;
- [ ] tests Angular et build production verts sur le dernier commit ;
- [ ] migration PostgreSQL 16 verte ;
- [ ] recette manuelle sur l'environnement de recette ;
- [ ] contrôle du lot documentaire ;
- [ ] contrôle de la facture `REGULARIZATION_PENDING` ;
- [ ] non-régression admission normale, transfert et sortie ;
- [ ] revue métier avant fusion.

## Conditions de clôture

L'issue #46 sera clôturée uniquement après :

1. CI complète verte ;
2. fusion de la PR #96 ;
3. recette de continuité documentée ;
4. absence de régression bloquante.

# Spécification — Corrections hospitalières
Les professionnels autorisés à admettre ou transférer doivent pouvoir lire les unités, espaces, lits et médecins affectés de leur établissement sans administrer la structure ou le personnel. Les projections ne contiennent pas coordonnées privées, biométrie ni données médicales inutiles.
Les séjours ne sont jamais affichés comme absents après un échec de chargement. Les formulaires distinguent chargement, absence de disponibilité, erreur et soumission. Un double clic n'émet qu'une admission ; un 409 invite à actualiser la disponibilité.
Prise en charge/reprise/libération/sauvegarde consultation partagent une exclusion transactionnelle par visite. Une version de consultation périmée doit rester refusée.
Décision utilisateur du 2026-10-04 (« je suis ta recommandation ») : une prescription validée est obligatoire. Le soignant sélectionne une ligne de prescription ACTIVE, non expirée, du patient canonique et de son établissement. Le serveur refuse une référence absente, inconnue, d'un autre patient/tenant, brouillon, annulée ou expirée et un médicament divergent. Aucun chemin de saisie libre sans prescription n'est ajouté. La dose effectivement administrée reste une trace textuelle ; aucune règle numérique de posologie n'est inventée.
Une panne de source financière empêche le retour d'un précalcul présenté comme complet. L'absence normale de données ne constitue pas une panne.
Consentements et CRO sont des composants dédiés ; textes FR/EN et tokens centralisés, rayon maximum 8px.
Acceptation : tests négatifs permissions, concurrence, médicaments, pannes financières et erreurs UI ; tests complets Angular et backend pertinents verts. Recette visuelle est une gate séparée, jamais remplacée par tests mockés.

Hors périmètre : changement des règles de rattachement à une visite historique, réservation, handoff, compatibilité clinique du lit, nouvelle politique d'urgence ou migration des anciennes administrations sans référence.

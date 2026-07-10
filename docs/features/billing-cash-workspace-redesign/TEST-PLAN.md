# TEST-PLAN — Workspace Facturation & Caisse

| Parcours | Vérification | Niveau |
|---|---|---|
| Visite déjà facturée | Formulaire création absent ou désactivé, détail visible après sélection | Angular / E2E |
| Patient non réglé | Action d'encaissement, montant restant et session contrôlés | Angular / Integration |
| Paiement partiel | État et solde mis à jour sans dépasser le restant | Backend / E2E |
| Tiers-payant | Part patient distincte de la part assurance | Backend / Angular |
| Bordereau | `DRAFT → SENT → PAID`, référence obligatoire au paiement | Integration / E2E |
| Facture soldée | Aucune action d'encaissement restante | Angular |
| Avoir | Action séparée, confirmation et audit | Angular / Backend |
| Annulation facture | Modale maison, aucun `confirm()`, Échap et blocage pendant la requête | Angular |
| Création devis | `patientId`, `visitId` et lignes transmis ; succès/erreur visibles | Angular |
| Deep-link facture | Indicateur de chargement rendu avant le panneau de détail | Angular / QA |
| Accessibilité | Clavier, focus, contraste, labels et responsive | QA |
| Ligne de devis dans un panneau étroit | Champs contenus, retour en grille 2 puis 1 colonne, action « Supprimer » visible | Angular / QA visuelle |

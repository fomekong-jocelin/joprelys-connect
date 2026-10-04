# ADR — Prescription validée obligatoire pour l'administration hospitalière

Date : 2026-10-04. Statut : accepté par l'utilisateur (« je suis ta recommandation ») pour FIX-20261004-HOSPITAL-PATH ; review clinique de livraison reste requise.

## Contexte

L'administration pouvait enregistrer un nom libre et une référence optionnelle non contrôlée. Les permissions distinguent prescription médicale et administration soignante. Le défaut permettait une référence inexistante ou appartenant à un autre patient/établissement.

## Décision

Rendre la référence obligatoire et vérifier côté serveur le patient canonique, le tenant, le statut ACTIVE, l'expiration et la cohérence du nom. Le front propose les lignes admissibles. Aucun chemin sans prescription n'est autorisé dans ce lot. Verrouiller le séjour et les prescriptions pendant la validation et l'insertion. Garder la dose administrée en texte, sans inventer une règle pharmacologique.

## Conséquences

Contrat POST cassant : clients web/mobile/intégrations doivent envoyer prescriptionItemId. Lot MAJOR candidat selon SEMANTIC-VERSIONING.md ; pas de release préparée. Angular adapté dans le même lot ; aucun consommateur hospitalier trouvé côté Flutter actuel. Les administrations historiques restent lisibles, sans migration destructive ni contrainte FK rétroactive. Un éventuel protocole d'urgence sans prescription doit faire l'objet d'une décision clinique et d'un workflow dédié ultérieur.

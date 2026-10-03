# Parcours patient — pré-enregistrement → constantes → consultation

## Contexte
Audit praticien du 2026-10-03 (`docs/ai/tickets/TICKET-20261003-PATIENT-JOURNEY-AUDIT.md`). Ce document décrit les règles métier corrigées le jour même et celles qui restent à arbitrer.

## Règles en vigueur

### Pré-enregistrement
- Les onglets « En attente », « Validées » et « Rejetées » interrogent chacun le serveur avec leur statut. La pagination et le total sont donc exacts.
- Une demande validée garde le lien vers le dossier patient créé ou réconcilié (`validatedPatientId`). Depuis la liste ou le tiroir, l'agent peut :
  - télécharger la fiche d'admission ;
  - ouvrir le dossier pour admettre le patient (ouverture de la visite).
- Le QR code d'admission est généré par le serveur. L'identifiant de la clinique n'est plus transmis à un service tiers.

### Admission
- Le brouillon d'admission reste dans l'onglet (`sessionStorage`). Il est purgé à la déconnexion et à chaque changement d'utilisateur.
- Le parcours normal ne propose plus l'orientation « Urgences » : les urgences passent par le parcours Urgences.
- Seuls des médecins et infirmiers peuvent être choisis comme praticien principal, en priorité ceux du service choisi.

### Constantes
- La systolique doit être strictement supérieure à la diastolique. Le contrôle est fait par le serveur et signalé à l'écran.
- La glycémie est saisie en g/L. Au-delà de 3 g/L, un avertissement non bloquant rappelle l'unité (1 g/L ≈ 5,5 mmol/L).
- Chaque saisie est tracée dans l'audit (`VISIT_VITALS_RECORDED` : auteur, heure, valeurs saisies).
- Le médecin voit l'heure de la dernière mesure dans l'écran de consultation.

### Consultation
- L'auteur de la consultation reste le praticien qui l'a créée. Un enregistrement ultérieur ne le change plus.
- Si la consultation a été modifiée sur un autre poste depuis son ouverture, l'enregistrement est refusé (409). Le praticien doit recharger : aucun écrasement silencieux.
- Un seul bon d'examens actif par visite et par type d'examen. Un nouvel enregistrement ajoute seulement les examens manquants, sans toucher aux examens déjà en cours de traitement.
- Un échec de chargement de la visite affiche un message au lieu d'un écran vide.

## Restant à arbitrer (PO + médecin référent)
- **PJ-02** : états de visite intermédiaires (attente constantes / prêt médecin / en consultation chez Dr X).
- Historique multi-mesures des constantes (réévaluations) dans un modèle dédié.
- **PJ-05** : fusion des deux formulaires d'admission et référentiel unique orientation / service.
- **PJ-07** : recherche patient dans l'admission.
- **PJ-08** : file filtrée par médecin ou service, avec alertes sur les constantes anormales.
- **PJ-10** : refactor des composants trop volumineux.

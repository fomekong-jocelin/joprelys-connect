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

## Parcours de prise en charge (livré le 2026-10-03, à valider par le médecin référent)

### Étapes d'une visite active
Le statut administratif (`EN_COURS` / `TERMINEE` / `ANNULEE`) est inchangé. Une **étape** s'y ajoute :

| Étape | Entrée | Sortie |
|---|---|---|
| Attente constantes | Création de la visite | Première mesure de constantes |
| Prêt pour le médecin | Constantes saisies, ou patient remis dans la file | Prise en charge par un praticien |
| En consultation chez Dr X | « Démarrer la consultation » ou premier enregistrement de la consultation | « Remettre dans la file » ou clôture de la visite |

- Un médecin peut prendre en charge un patient sans constantes (urgence ressentie) : l'étape est un repère, pas un verrou.
- Un patient en consultation chez Dr X ne peut pas être ouvert par un confrère sans **reprise explicite**. La reprise est confirmée à l'écran et tracée dans l'audit (`VISIT_CONSULTATION_TAKEN_OVER`).
- Enregistrer une consultation sur un patient pris en charge par un confrère est refusé (409).
- Seul le praticien qui a le patient en charge peut le remettre dans la file.

### Constantes
- Chaque saisie ajoute une **mesure horodatée et signée** à l'historique de la visite. La dernière mesure reste affichée en premier.
- Une nouvelle mesure part des valeurs de la précédente : l'infirmier corrige ce qui a changé.
- L'historique est visible dans le tiroir de la file et dans l'écran de consultation.
- **Alertes** calculées par le serveur, selon des seuils **adultes** à valider par le médecin référent (non adaptés à la pédiatrie) :

| Paramètre | Avertissement | Critique |
|---|---|---|
| Température | ≥ 38,0 °C ou < 35,5 °C | ≥ 40,0 °C ou < 35,0 °C |
| SpO2 | < 94 % | < 90 % |
| Tension | ≥ 140/90 ou systolique < 90 | ≥ 180/110 ou systolique < 80 |
| Pouls | > 100 ou < 50 bpm | > 130 ou < 40 bpm |
| Fréquence respiratoire | > 20 ou < 12 /min | > 30 ou < 10 /min |
| Glycémie | ≥ 2,0 ou < 0,70 g/L | ≥ 3,0 ou < 0,54 g/L |
| Douleur | ≥ 7/10 | — |

### File d'attente
- Filtres : **Toute la file**, **Mes patients** (praticien principal ou patient en consultation chez moi), **Mon service** (service de la visite = une unité à laquelle je suis affecté).
- Chaque ligne affiche l'étape, le praticien en charge et les alertes. Une alerte critique est signalée par une bordure rouge.
- Le résumé compte les patients par étape et le nombre d'alertes critiques.
- L'heure affichée est l'heure d'arrivée déclarée (et non l'heure de saisie).

### Admission
- **Un seul formulaire de visite** pour l'admission unifiée et pour l'admission depuis le dossier patient : motif, orientation (codes communs), service (catalogue de l'établissement ou « Autre »), praticien principal (médecins et infirmiers du service choisi), date et heure d'arrivée.
- **Recherche du patient existant** par nom, téléphone ou numéro de dossier, dès 2 caractères. Chaque résultat affiche le numéro de dossier, la date de naissance et le téléphone pour distinguer les homonymes.
- Les visites créées avant l'harmonisation gardent leur orientation en texte libre, qui est affichée telle quelle.

## Restant à arbitrer (PO + médecin référent)
- Validation clinique des étapes et des seuils d'alerte, et seuils pédiatriques (âge du patient).
- Tri de la file par gravité plutôt que par heure d'arrivée (aujourd'hui, les alertes sont signalées mais l'ordre reste l'heure d'arrivée).
- Rattachement de `visits.service_name` (texte) aux unités structurées (identifiant), pour un filtre « Mon service » exact.

# EPIC-0006 — Génération PDF & Vérification par QR Code

## 1. Objectif métier

Générer des documents médicaux officiels (fiches de consultation/ordonnances) infalsifiables au format PDF, et permettre à des tiers de vérifier instantanément leur authenticité en ligne via un QR code, tout en protégeant le secret médical.

## 2. Périmètre

### Inclus

- Génération automatique du PDF combiné de consultation et de prescription lors de la clôture clinique de la visite.
- Génération d'un QR code unique embarqué dans le PDF contenant l'URL de vérification publique.
- Rangement et stockage des fichiers PDF générés sur le serveur.
- Page web publique de vérification affichant : statut du document (valide, annulé, remplacé), numéro du document, clinique d'émission, date, nom du médecin émetteur, et nom/prénom du patient.
- Masquage strict des informations médicales sensibles sur la page publique (pas de diagnostic, pas de liste de médicaments, pas d'antécédents).

### Exclus

- Signature cryptographique sur blockchain ou stockage décentralisé IPFS.
- Envoi automatique du PDF par SMS ou E-mail au patient.

## 3. Utilisateurs concernés

- Médecin (génère et remet le document)
- Patient (reçoit le document physique/numérique)
- Vérificateur externe (pharmacien, laboratoire, assureur, administration)

## 4. User stories

| ID | Titre | Priorité | SP | Statut | Sprint cible |
|---|---|---|---:|---|---|
| STORY-0601 | Génération et stockage du PDF de consultation | P0 | 3 | BACKLOG | SPRINT-0003 |
| STORY-0602 | Page publique de vérification d'authenticité | P0 | 3 | BACKLOG | SPRINT-0004 |
| STORY-0603 | Révocation et annulation de documents | P1 | 2 | BACKLOG | SPRINT-0004 |

## 5. Dépendances

- EPIC-0005 (Consultation & Prescription) pour disposer des données à intégrer dans le PDF.
- Hébergement web ou IP publique configurée pour que les QR codes pointent vers une URL accessible.

## 6. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Fuite de secret médical sur la page publique | Très Fort | Ne stocker aucune donnée sensible dans les paramètres d'URL du QR code (utiliser un identifiant opaque UUID). Réaliser un audit strict du contrôleur public de vérification. |

## 7. Définition de succès

- [ ] Un scan de QR code sur mobile ouvre la page de vérification en moins de 2 secondes.
- [ ] La page publique n'affiche aucune donnée sensible (aucun médicament, aucun diagnostic).
- [ ] Si un document est révoqué par le médecin, la page de vérification affiche clairement le statut "ANNULÉ".

## 8. Estimation globale

| Élément | Valeur |
|---|---|
| Total story points | 8 |
| Effort senior | 2.5j |
| Effort intermédiaire | 3.2j |
| Effort junior | 5j |
| Nombre de sprints estimé | 1 |

## Impact version / SemVer

| Champ | Valeur |
|---|---|
| Impact version | MINOR |
| Justification | Ajout de la génération PDF, intégration QR code et portail public de vérification |
| Breaking change | Non |
| Release cible | v0.5.0 |

# FUNCTIONAL-SPEC — Gestion Spatiale (Lits & Chambres)

## 1. Résumé métier

L'objectif de la gestion spatiale est de fournir une cartographie en temps réel des services (Wards), chambres (Rooms) et lits (Beds) de la clinique. Cela permet d'optimiser l'occupation des lits, de suivre les admissions et les transferts de patients, et d'éviter les erreurs d'affectation concurrentes.

## 2. Objectifs

- [x] Cartographie de la clinique par services, chambres et lits.
- [x] Suivi en temps réel du statut de chaque lit (Libre, Occupé, En nettoyage, En maintenance).
- [x] Affectation obligatoire d'un lit libre lors d'une hospitalisation.
- [x] Gestion des transferts de lits et de services pour les patients hospitalisés.
- [x] Visualisation synthétique (Plan de la clinique) de l'occupation des lits.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| **Agent d'accueil** | Consulter les lits disponibles pour orienter les patients. | Lecture seule |
| **Infirmier / Major** | Assigner un lit, transférer un patient, changer le statut d'un lit (ex. nettoyage). | Écriture / Modification |
| **Médecin** | Voir où sont hospitalisés ses patients. | Lecture seule |
| **Admin Clinique** | Configurer les services, chambres et lits (capacité, confort). | Administration complète |

## 4. Périmètre

### Inclus

- CRUD des structures physiques : Service (Ward), Chambre (Room), Lit (Bed).
- Cycle de vie d'un lit : `FREE` ➔ `OCCUPIED` ➔ `CLEANING` ➔ `FREE` (ou `MAINTENANCE`).
- Blocage strict de l'affectation concurrente d'un lit (verrouillage optimiste).
- Écran de transfert de lit (changement de chambre/lit au cours d'un séjour).
- Tableau de bord visuel de l'occupation spatiale de la clinique.

### Exclus

- Plan graphique 2D/3D dynamique (drag-and-drop sur plan de masse). Remplacé par une grille hiérarchique réactive par service/chambre.
- Gestion des coûts d'hébergement (déportée dans le module Facturation).

## 5. Parcours utilisateur

1. **Admission** : Lors de l'hospitalisation d'un patient, le soignant clique sur "Admettre". Le système lui présente uniquement les lits libres du service concerné.
2. **Transfert** : Le patient change de chambre pour confort ou nécessité clinique. Le soignant initie un "Transfert", choisit le nouveau lit libre, et le système met à jour les deux lits automatiquement.
3. **Sortie & Nettoyage** : Lors de la sortie (discharge), le lit passe automatiquement à l'état `CLEANING`. Une fois désinfecté, le soignant le remet à l'état `FREE` via un bouton rapide.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| **BR-SPACE-001** | Un lit ne peut être affecté qu'à un seul patient actif à la fois. | P0 | FR-SPACE-001 |
| **BR-SPACE-002** | Une hospitalisation requiert obligatoirement l'affectation d'un lit au statut `FREE`. | P0 | FR-SPACE-001 |
| **BR-SPACE-003** | Le passage d'un lit à l'état `OCCUPIED` doit être transactionnel et protégé contre les accès concurrents. | P0 | Technique |
| **BR-SPACE-004** | Un lit libéré passe par défaut au statut `CLEANING` avant de pouvoir être réassigné. | P1 | Processus Clinique |

## 7. Critères d’acceptation

- [ ] L'affichage du plan de la clinique montre en temps réel l'occupation par service (avec compteurs de lits occupés/totaux).
- [ ] Le formulaire d'admission empêche la validation si le lit choisi a été réservé entre-temps par un autre utilisateur (message d'erreur convivial sans plantage).
- [ ] La traçabilité des transferts de lits (historique) est consultable dans le dossier d'hospitalisation du patient.

## 8. Cas limites / erreurs attendues

| Cas | Comportement attendu |
|---|---|
| Concurrence sur le même lit | Levée d'une erreur d'affectation avec rechargement de la liste des lits disponibles. |
| Lit en nettoyage sélectionné | Bloqué au niveau du validateur backend et frontend. |

## 9. Textes / i18n

| Clé | Français | English |
|---|---|---|
| `spatial.ward` | Service | Ward |
| `spatial.room` | Chambre | Room |
| `spatial.bed` | Lit | Bed |
| `spatial.status.free` | Libre | Free |
| `spatial.status.occupied` | Occupé | Occupied |
| `spatial.status.cleaning` | En nettoyage | Cleaning |
| `spatial.status.maintenance` | En maintenance | Maintenance |

## 10. Impacts UI / branding

| Point | Impact |
|---|---|
| Nom de l’app | Non |
| Logo | Non |
| Thème light/dark | Oui (les statuts colorés des lits doivent s'adapter au thème sombre) |
| Composants réutilisables | Oui (composant Grid/Badge d'occupation de lits) |

## 12. Suivi des lits en vue globale (UX-20261105-B, S1)

- **But** : savoir d'un coup d'œil où se trouvent les lits libres et lesquels préparer, sans choisir une chambre après l'autre.
- **Écran** (`/clinic/spatial`) : une synthèse « N lits libres sur M lits ouverts · occupation X % », puis toutes les chambres d'hospitalisation actives, chacune avec ses lits.
- **Un état par lit** :
  - Un lit occupé est toujours affiché « Occupé ».
  - Sinon : Fermé, puis En nettoyage, puis En maintenance.
  - Sinon : Libre si le serveur le déclare disponible, Indisponible dans le cas contraire.
- **Filtres** : un bouton par état, avec son nombre de lits. Il n'affiche que les chambres concernées et un second appui revient à « Tous ». Un état sans lit est désactivé.
- **Actions** (selon les permissions `BED_CLEANING_MANAGE`, `BED_MAINTENANCE_MANAGE`, `BED_OPERATIONAL_STATUS_MANAGE`) :
  - lit libre : menu « Changer l'état » (nettoyage, maintenance, fermeture) ;
  - lit en nettoyage ou en maintenance : bouton direct pour terminer ;
  - lit fermé : « Rouvrir le lit » ;
  - lit occupé : aucune action ;
  - la fermeture est confirmée et rappelle le nombre de lits libres restant.
- **États** :
  - chargement : squelette ;
  - aucune chambre : lien vers la configuration ;
  - échec : message et « Réessayer » ;
  - conflit (le lit a changé entre-temps) : message, puis la liste est rafraîchie ;
  - toujours : « Actualiser » et l'heure de mise à jour.

## 11. Historique des mises à jour

| Date | Auteur | Changement |
|---|---|---|
| 2026-07-08 | Antigravity | Création initiale |
| 2026-11-05 | Claude | § 12 : suivi des lits en vue globale (UX-20261105-B S1) |

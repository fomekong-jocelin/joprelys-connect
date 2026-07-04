# FUNCTIONAL-SPEC — Télétransmission d'ordonnances à AllôPharma - STORY-1601 & STORY-1602

## 1. Résumé métier

Afin de simplifier le parcours patient et d'accélérer la délivrance et le remboursement des médicaments, Joprelys Connect permet aux patients et aux médecins prescripteurs de télétransmettre une ordonnance active directement à la plateforme partenaire AllôPharma. Cette transmission électronique sécurisée évite au patient de présenter une ordonnance papier et permet à AllôPharma de préparer la commande en amont.

## 2. Objectifs

- Offrir un mécanisme de télétransmission sécurisé et tracé pour chaque ordonnance.
- Exposer des API REST pour initier la transmission.
- Intégrer un bouton de transmission et des indicateurs de statut visuels sur les portails patient et professionnel.

## 3. Utilisateurs / acteurs concernés

| Acteur | Besoin | Droits / limites |
|---|---|---|
| Patient | Télétransmettre son ordonnance à AllôPharma depuis son portail | Uniquement ses propres ordonnances actives. |
| Médecin | Télétransmettre l'ordonnance qu'il vient de rédiger lors d'une consultation | Uniquement les ordonnances créées au sein de son établissement. |
| AllôPharma (Plateforme externe) | Recevoir de manière structurée et sécurisée les ordonnances via API | Authentifiée par clé API ou jeton. |

## 4. Périmètre

### Inclus

- Base de données : nouveaux champs dans `prescriptions` (`transmission_status`, `transmitted_at`).
- Client d'intégration simulé (`AlloPharmaClient`) pour poster l'ordonnance.
- Bouton interactif et indicateurs de statut sur l'interface du patient et du médecin.
- Journalisation de l'audit log de transmission.

### Exclus

- Routage vers des pharmacies physiques tierces non partenaires (AllôPharma uniquement).

## 5. Parcours utilisateur

1. Le médecin crée et valide une ordonnance à la fin d'une consultation.
2. Le médecin ou le patient clique sur "Télétransmettre à AllôPharma".
3. L'ordonnance est envoyée électroniquement à AllôPharma.
4. L'IHM affiche le statut "Transmise à AllôPharma" avec la date et l'heure exactes.

## 6. Règles métier

| ID | Règle | Priorité | Source |
|---|---|---|---|
| RM-1601-1 | Seule une ordonnance au statut `ACTIVE` peut être télétransmise. | P0 | Spécification de cycle de vie |
| RM-1601-2 | Une ordonnance déjà transmise ne peut pas être transmise à nouveau (bouton désactivé). | P0 | Intégrité des données |
| RM-1601-3 | Tout échec de transmission réseau ou API partenaire doit laisser l'ordonnance dans un statut `FAILED` permettant une nouvelle tentative. | P1 | Robustesse |
| RM-1601-4 | La transmission génère un log d'audit `TRANSMIT_PRESCRIPTION`. | P0 | Gouvernance de traçabilité |

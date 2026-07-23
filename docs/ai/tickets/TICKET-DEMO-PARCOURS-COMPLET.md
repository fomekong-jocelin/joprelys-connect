---
id: TICKET-DEMO-PARCOURS-COMPLET
epic: CDC_ALIGN
sprint: SPRINT-0011
type: Documentation
priority: P1
status: DONE
profil_recommande: Tech Lead / Product Owner
estimation_senior: 0.3j
estimation_intermediaire: 0.5j
estimation_junior: 0.8j
assigne: Antigravity
reviewer: Lead Developer
---

# Document de démo — Parcours utilisateur complet

## Objectif

Rédiger un document de démo opérationnel permettant de présenter l'ensemble du parcours métier de Joprelys Connect, de la création d'un patient jusqu'à la saisie des résultats d'examens de laboratoire, en passant par l'admission, la consultation, la prescription et la dispensation en pharmacie.

## Contexte

Le projet dispose aujourd'hui des modules suivants implémentés et fonctionnels :

- Authentification multi-rôles ;
- Gestion des patients et DPU ;
- Visites et constantes vitales ;
- Consultation médicale et prescription ;
- Pharmacie ;
- Laboratoire ;
- Portail patient.

Le guide historique reste la documentation canonique du parcours complet. Les démonstrations datées peuvent lui ajouter un runbook borné sans recréer un second guide concurrent.

## Critères d'acceptation

- [x] Le document couvre tous les rôles intervenant dans le parcours.
- [x] Le document décrit chaque étape avec le rôle connecté, l'écran, les actions et les données de test.
- [x] Le document inclut les prérequis et la configuration initiale.
- [x] Le document inclut un tableau récapitulatif et une checklist avant démo.
- [x] Le document est versionné dans `docs/features/demo-parcours-complet/`.

## Livrables

- `docs/features/demo-parcours-complet/DEMO-PARCOURS-COMPLET.md` — guide canonique complet ;
- `docs/features/demo-parcours-complet/RUNBOOK-CLIENT-20260725.md` — addendum borné pour la démonstration du 25/07/2026, suivi par #127 ;
- `docs/ai/PROJECT-TRACKING.md`.

## Règle de sécurité documentaire

Les indications historiques du guide complet ne doivent pas être interprétées comme une autorisation d'exposer des OTP, mots de passe temporaires ou secrets dans une API, un écran ou des logs. La préparation de comptes suit toujours le comportement sécurisé du code courant et les variables/configurations autorisées.

## Impact version

Aucun bump applicatif : ce ticket est purement documentaire.

## Notes

Le guide canonique date du SPRINT-0011. Le runbook du 25/07/2026 décrit uniquement le périmètre client retenu après livraison des parcours URG-TEMP et hospitalisation. Il ne modifie aucun code source et ne vaut pas UAT globale du CDC.

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
- Authentification multi-rôles (OTP staff)
- Gestion des patients et DPU
- Visites et constantes vitales
- Consultation médicale et prescription
- Pharmacie (vérification et délivrance d'ordonnances)
- Laboratoire (demandes et résultats d'examens)
- Portail patient

Il manquait un guide unique et actionnable pour enchaîner ces modules dans une démonstration cohérente devant un client ou un utilisateur final.

## Critères d'acceptation

- [x] Le document couvre tous les rôles intervenant dans le parcours.
- [x] Le document décrit chaque étape avec le rôle connecté, l'écran, les actions et les données de test.
- [x] Le document inclut les prérequis et la configuration initiale.
- [x] Le document inclut un tableau récapitulatif et une checklist avant démo.
- [x] Le document est versionné dans `docs/features/demo-parcours-complet/`.

## Livrables

- `docs/features/demo-parcours-complet/DEMO-PARCOURS-COMPLET.md`
- Mise à jour de `docs/ai/PROJECT-TRACKING.md`
- Mise à jour de `docs/ai/CHANGELOG.md`

## Impact version

Aucun bump applicatif : ce ticket est purement documentaire.

## Notes

Ce document ne modifie aucun code source. Il s'appuie sur l'état actuel de l'application (SPRINT-0011, fin de l'alignement CDC modules 4-12).

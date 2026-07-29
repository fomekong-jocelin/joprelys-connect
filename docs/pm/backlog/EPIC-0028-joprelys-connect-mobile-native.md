# EPIC-0028 — Joprelys Connect Mobile Native

## Vision

Livrer une application Flutter professionnelle Android-first, iOS-ready, cohérente avec Joprelys Connect web et capable de supporter les parcours cliniques mobiles sensibles, notamment la consultation vocale longue durée avec moteur natif contrôlé.

## Valeur métier

- sécuriser les consultations vocales mobiles ;
- accélérer les parcours terrain ;
- réduire la dépendance au navigateur ;
- disposer d’un poste de travail clinique portable ;
- conserver une seule vérité métier côté backend.

## Priorité

P0 pour fondation + audio clinique.

P1 pour l’élargissement progressif des autres modules.

## Architecture de référence

ADR : `docs/ai/adr/ADR-0004-mobile-flutter-native-architecture.md`.

## Découpage

| ID | User Story | Priorité | SP | Senior | Intermédiaire | Profil recommandé | Dépendances |
|---|---|---:|---:|---:|---:|---|---|
| MOB-2800 | Architecture et documentation mobile de référence | P0 | 3 | 1–1.5j | 2j | Architecte mobile senior | — |
| MOB-2801 | Bootstrap, arborescence feature-first, router et CI minimale | P0 | 5 | 2–3j | 3–4j | Flutter senior | MOB-2800 |
| MOB-2802 | Design system Flutter + light/dark/system + widgets partagés | P0 | 5 | 2–3j | 3–4j | Flutter senior + UX | MOB-2801 |
| MOB-2803 | i18n FR/EN + formats locale + configuration applicative | P0 | 3 | 1–2j | 2–3j | Flutter intermédiaire/senior | MOB-2801 |
| MOB-2804 | Client API, erreurs, correlation, refresh session et réseau | P0 | 5 | 2–3j | 3–4j | Flutter senior | MOB-2801 |
| MOB-2805 | Auth professionnelle, secure storage, biométrie et frontières de session | P0 | 5 | 2–3j | 3–4j | Flutter senior sécurité | MOB-2803, MOB-2804 |
| MOB-2806 | Dashboard chaleureux + file d’attente active existante | P1 | 5 | 2–3j | 3–4j | Flutter intermédiaire + reviewer senior | MOB-2802, MOB-2805 |
| MOB-2807 | Agenda du médecin / rendez-vous | P1 | 5 | 2–3j | 3–4j | Flutter intermédiaire/senior | MOB-2802, MOB-2804 |
| MOB-2808 | Gestion des disponibilités / indisponibilités | P1 | 5 | 2–3j | 3–4j | Flutter intermédiaire/senior | MOB-2807 |
| MOB-2809 | Recherche et liste patients mobile | P0 | 3 | 1–2j | 2–3j | Flutter intermédiaire | MOB-2802, MOB-2804 |
| MOB-2810 | Dossier patient progressif natif | P0 | 5 | 2–3j | 3–4j | Flutter senior santé | MOB-2809 |
| MOB-2811 | Saisie des constantes | P0 | 5 | 2–3j | 3–4j | Flutter intermédiaire + reviewer clinique | MOB-2810 |
| MOB-2812 | Plugin Android foreground microphone | P0 | 5 | 3–4j | 5–6j | Senior Android/Kotlin + Flutter | MOB-2801, MOB-2805 |
| MOB-2813 | Bridge Flutter audio + state machine + interruptions | P0 | 5 | 2–3j | 4–5j | Senior Flutter/platform | MOB-2812 |
| MOB-2814 | Contrat backend audio segmenté, idempotence et ACK | P0 | 5 | 2–4j | 4–5j | Senior Spring + API/sécurité | MOB-2800 |
| MOB-2815 | File locale chiffrée de segments + reprise upload | P0 | 5 | 3–4j | 5–6j | Senior mobile sécurité | MOB-2813, MOB-2814 |
| MOB-2816 | Consultation IA mobile — capture/transcript/corrections | P0 | 5 | 3–4j | 5–6j | Senior Flutter + IA | MOB-2815 |
| MOB-2817 | Consultation IA mobile — propositions et validation médecin | P0 | 5 | 3–4j | 5–6j | Senior full-stack clinique | MOB-2816 |
| MOB-2818 | Prescription & examens | P1 | 5 | 2–3j | 3–4j | Flutter intermédiaire/senior | MOB-2810, MOB-2804 |
| MOB-2819 | Laboratoire mobile | P1 | 5 | 2–3j | 3–4j | Flutter intermédiaire/senior | MOB-2810, MOB-2804 |
| MOB-2820 | Hardening sécurité, observabilité, builds signés et pilote Android | P0 | 5 | 3–5j | 5–7j | Mobile security/DevOps senior | fondation + parcours pilote |

## Charge indicative

Total initial : **99 SP** environ, à livrer sur plusieurs sprints et à réestimer après MOB-2801/MOB-2812.

Cette estimation couvre le produit mobile professionnel, pas uniquement les dix maquettes.

## Séquencement recommandé

### Phase A — Fondation

- MOB-2800
- MOB-2801
- MOB-2802
- MOB-2803
- MOB-2804
- MOB-2805

### Phase B — Premier poste de travail utilisable

- MOB-2806
- MOB-2809
- MOB-2810
- MOB-2811
- MOB-2807

### Phase C — P0 audio natif

- MOB-2812
- MOB-2813
- MOB-2814
- MOB-2815
- MOB-2816
- MOB-2817

### Phase D — Extension clinique

- MOB-2808
- MOB-2818
- MOB-2819
- MOB-2820

## Dépendances critiques

1. MOB-2801 doit être fini avant les écrans métier.
2. MOB-2802/MOB-2803 sont bloquants pour toute UI considérée DONE.
3. MOB-2812 ne doit pas être mélangé à l’écran consultation.
4. MOB-2814 est requis avant d’annoncer une reprise audio fiable après réseau instable.
5. MOB-2820 est obligatoire avant pilote externe.

## Profils recommandés

- 1 senior Flutter/architecture ;
- 1 développeur Flutter intermédiaire ;
- 1 senior Android/Kotlin ponctuel pour foreground audio ;
- 1 senior Spring/API pour le contrat audio ;
- QA mobile avec appareils physiques ;
- reviewer clinique ;
- sécurité/DPO avant persistance locale PHI et pilote.

## Definition of Ready générale

Chaque story doit préciser :

- design/mockup ou comportement ;
- permissions ;
- endpoints ;
- FR/EN ;
- light/dark ;
- loading/empty/error ;
- données locales autorisées ;
- tests ;
- reviewer ;
- stratégie rollback si plateforme/CI.

## Definition of Done générale

- `flutter analyze` vert ;
- `flutter test` vert ;
- build requis vert ;
- FR/EN ;
- light/dark ;
- design tokens ;
- aucun secret/PII dans logs ;
- documentation ;
- review ;
- recette appareil réel pour toute capacité native/audio.

## Risques principaux

| Risque | Niveau | Mitigation |
|---|---|---|
| perte silencieuse audio | Critique | native state machine + service foreground + interruption visible |
| retry audio en double | Élevé | contrat backend idempotent/segmenté |
| fuite PHI locale | Critique | pas de cache DPU générique, chiffrement ciblé, revue DPO |
| dérive web/mobile | Élevé | backend maître + DESIGN.md + contrats partagés |
| dette Flutter monolithique | Élevé | feature-first + limites de taille + review |
| Android OEM/batterie | Élevé | tests appareils physiques, état réel natif, runbook utilisateur |
| publication trop tôt | Moyen | hardening/signing/pilote avant store |

## Impact release

Le cadrage n’entraîne aucun bump.

La première application mobile fonctionnelle publiée constitue une nouvelle capacité rétrocompatible du produit : cible SemVer MINOR, à confirmer au moment de la release.

# EPIC-0024 — Assistant vocal IA de consultation

## 1. Statut et objectif

| Champ | Valeur |
|---|---|
| Statut | IN_PROGRESS — cadrage et fondations |
| Priorité | P1, activation production bloquée |
| Mode | Project Manager + Engineering + Architecture |
| Responsable | Codex / Tech Lead |
| Reviewers | Médecin référent, DPO/conformité, Lead Backend, Lead Frontend, QA |
| Version cible indicative | `0.11.0` (MINOR), après validation de production |

Permettre au médecin authentifié d’ouvrir une visite active depuis son téléphone,
par scan d’un QR code, puis de dicter des observations. L’IA produit uniquement
un brouillon structuré et pose des questions de clarification. Le médecin garde
la main sur chaque correction et valide explicitement avant l’enregistrement via
le contrat de consultation existant.

Ce ticket est la source de vérité. Le fichier historique
`epic-ai-voice-consultation-assistant.md` reste un brouillon de découverte et
ne doit pas servir à déduire l’état livré ou la charge.

## 2. Garde-fous non négociables

- L’IA n’établit pas un diagnostic autonome et ne prescrit pas.
- Aucune suggestion IA n’est persistée sans validation explicite du médecin.
- Le backend valide les rôles, le tenant, la visite active et les longueurs de champs.
- L’audio n’est pas conservé par Joprelys Connect.
- Les contenus médicaux et audios ne sont jamais inscrits dans les logs.
- L’activation production reste désactivée tant que le fournisseur, le contrat de
  traitement des données, la résidence/rétention et le DPO ne sont pas validés.
- Le mode manuel existant doit rester disponible en permanence.

## 3. État réel constaté le 2026-07-17

| Élément | État | Preuve / remarque |
|---|---|---|
| Spécification fonctionnelle initiale | PARTIAL | `docs/features/ai-voice-consultation/FUNCTIONAL-SPEC.md` |
| Port IA combiné | STARTED | `AiProvider` mélange STT et conversation, à séparer |
| Clients OpenAI/Gemini/Claude | EXPERIMENTAL | Non testés, non configurés dans `application.yml` |
| Service de session et extraction | NOT_STARTED | Aucun controller/use case/session manager |
| QR visite | DONE | URL publique dédiée, visite `EN_COURS`, RBAC/tenant et 16 tests verts |
| Scanner Angular | NOT_STARTED | Aucune dépendance ni composant |
| Assistant vocal Angular | NOT_STARTED | Aucun service/panneau |
| Changelog initial | INCORRECT | Déclarait des stories non livrées |
| Dette UI | BLOCKER | templates consultation/dashboard au-dessus des limites projet |

## 4. EPIC → User Stories → Tasks

### STORY-2500 — Cadrage clinique, conformité et architecture

**Story** : En tant qu’équipe produit, nous voulons cadrer l’assistant comme aide
à la saisie afin de protéger le patient et le médecin.

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 3 |
| Estimation senior | 1,5 j |
| Profil | Tech Lead + médecin + DPO |
| Statut | DONE |

#### Critères d’acceptation

- [x] Documentation fonctionnelle et technique initiale créée.
- [x] Contrat API, modèle de données, plan de tests et guide utilisateur initialisés.
- [x] Architecture STT et assistant de brouillon séparée.
- [ ] Fournisseur et sous-traitance de données validés par DPO.
- [ ] Jeu d’évaluation clinique anonymisé accepté par le médecin référent.
- [ ] Seuils de qualité et procédure d’incident validés.

#### Tasks

- [x] TASK-2500-A — Auditer l’epic et le code amorcé.
- [x] TASK-2500-B — Corriger le découpage et l’état réel.
- [x] TASK-2500-C — Produire les documents Documentation First.
- [ ] TASK-2500-D — Valider DPO/fournisseur/rétention/résidence.
- [ ] TASK-2500-E — Constituer les scénarios cliniques anonymisés.

### STORY-2501 — Ports IA et premier adaptateur approuvé

**Story** : En tant que backend, nous voulons séparer transcription et
structuration pour pouvoir changer chaque capacité sans couplage.

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 5 |
| Estimation senior | 3 j |
| Profil | Senior backend / sécurité |
| Statut | IN_PROGRESS |

#### Critères d’acceptation

- [ ] `AudioTranscriptionPort` ne porte que la transcription.
- [ ] `ConsultationDraftAssistantPort` ne porte que l’extraction structurée.
- [ ] Un seul adaptateur de production est activé après validation DPO.
- [ ] Les implémentations non validées restent hors activation production.
- [ ] Timeouts, erreurs 429/5xx et absence de clé sont gérés.
- [ ] Aucun secret ni contenu médical n’est loggé.
- [ ] Tests HTTP par serveur mock, sans appel réseau réel.

### STORY-2502 — Sessions de brouillon clinique interactif

**Story** : En tant que médecin, je veux dicter ou écrire, corriger et relire un
brouillon avant de l’appliquer au formulaire.

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 8 |
| Estimation senior | 5 j |
| Profil | Senior backend santé |
| Statut | BLOCKED |
| Blocage | STORY-2500 DPO + STORY-2501 |

#### Critères d’acceptation

- [ ] Session tenantée et liée à une visite `EN_COURS`.
- [ ] Messages texte et audio bornés et validés.
- [ ] Sortie structurée par schéma strict et allowlist de 8 champs.
- [ ] Correction explicite supportée sans écrasement silencieux.
- [ ] Aucune sauvegarde clinique depuis l’endpoint IA.
- [ ] TTL, limite de tours, rate limiting et terminaison explicite.
- [ ] Tests RBAC, multi-tenant, prompt injection, tailles et indisponibilité.

### STORY-2503 — Scan QR mobile sécurisé

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 5 |
| Estimation senior | 2,5 j |
| Profil | Frontend Angular + backend |
| Statut | TODO |

#### Critères d’acceptation

- [ ] QR généré uniquement pour une visite active et visible par le tenant.
- [ ] Scan dans un composant Angular réutilisable, caméra arrêtée à la fermeture.
- [ ] Confirmation patient/visite avant navigation.
- [ ] Erreurs permission, caméra absente, QR invalide et autre tenant.
- [ ] i18n FR/EN, light/dark, focus, cibles tactiles et rayons sobres.

### STORY-2504 — Panneau vocal de consultation

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 8 |
| Estimation senior | 5 j |
| Profil | Senior Angular / UX santé |
| Statut | BLOCKED |
| Blocage | STORY-2502, STORY-2503 et TASK-2504-0 |

#### Tasks

- [ ] TASK-2504-0 — Découper `consultation.component.html` (684 lignes) avant ajout.
- [ ] TASK-2504-A — Service capture audio et détection des capacités navigateur.
- [ ] TASK-2504-B — Panneau conversationnel partagé.
- [ ] TASK-2504-C — Application contrôlée du brouillon au formulaire.
- [ ] TASK-2504-D — TTS désactivé par défaut, accessibilité, FR/EN, light/dark.

### STORY-2505 — Génération QR de visite côté backend

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 3 |
| Estimation senior | 1 j |
| Profil | Backend intermédiaire |
| Statut | IN_PROGRESS |

#### Critères d’acceptation

- [x] Controller limité au contrat HTTP et à la délégation.
- [x] URL web distincte de l’URL de vérification documentaire.
- [x] Visite `EN_COURS`, RBAC et isolation tenant vérifiés.
- [x] Réponse PNG et contenu QR testés.
- [x] Erreurs 401/403/404/409 testées.

### STORY-2506 — Documentation, sécurité et observabilité

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 5 |
| Estimation senior | 2,5 j |
| Profil | Tech Lead + sécurité + DPO |
| Statut | IN_PROGRESS |

- [x] Documents Documentation First initialisés.
- [x] ADR initial créé.
- [ ] Métriques techniques sans contenu clinique.
- [ ] Runbook provider indisponible / quota / fuite de données.
- [ ] Revue OWASP, DPO et médecin signée.

### STORY-2507 — E2E, évaluation clinique et déploiement progressif

| Attribut | Valeur |
|---|---|
| Priorité / SP | P0 / 5 |
| Estimation senior | 4 j |
| Profil | QA senior + médecin référent |
| Statut | TODO |

- [ ] E2E mobile réel Android/iOS.
- [ ] Matrice navigateur/micro/caméra.
- [ ] Évaluation extraction/correction sur cas anonymisés.
- [ ] Feature flag par clinique et pilote restreint.
- [ ] Rollback immédiat vers le formulaire manuel.

## 5. Estimation et capacité

| Élément | Valeur |
|---|---|
| Total | 42 SP |
| Effort senior réaliste | 25 à 33 jours |
| Effort intermédiaire | 34 à 45 jours |
| Sprints indicatifs | 3 à 4, capacité à confirmer |
| Charge documentation/conformité | 20 à 25 % incluse |

Aucun engagement sur `SPRINT-0015` ou `SPRINT-0016` n’est valide sans capacité
nominative. La priorité projet courante reste le backlog P0 déjà engagé.

## 6. Action plan courant

- [x] Lire les règles et auditer l’existant.
- [x] Identifier les écarts documentation/code/suivi.
- [x] Refaire le découpage en tâches ≤ 2 jours autant que possible.
- [x] Finaliser l’incrément QR backend et ses tests.
- [ ] Refactorer les ports IA et tester les adaptateurs.
- [x] Exécuter les tests Maven ciblés de l’incrément QR.
- [x] Mettre à jour changelog, tracking, backlog et risques.

## 7. Definition of Ready

- [x] Objectif, périmètre et critères documentés.
- [x] Architecture cible initiale documentée.
- [x] Impacts API, sécurité, UI, tests et SemVer évalués.
- [ ] Fournisseur/DPO validés pour les stories traitant des données de santé.
- [ ] Capacité sprint et assignations validées.
- [ ] Jeu de données clinique anonymisé disponible.

## 8. Definition of Done

- [ ] Critères d’acceptation prouvés par tests.
- [ ] Revue médicale, sécurité, DPO et QA terminées.
- [ ] Aucune donnée clinique/audio dans les logs.
- [ ] Mode manuel et rollback vérifiés.
- [ ] Documentation, changelog, suivi et release note à jour.
- [ ] Feature flag pilote validé avant activation générale.

## 9. Reste à faire

Voir les cases non cochées. L’epic n’est pas livrée et ne doit pas être présentée
comme disponible en production.

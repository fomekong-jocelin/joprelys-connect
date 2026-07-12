# QA-20260712-PR52 — Recette manuelle PR #52 : rapprochement URG-TEMP

## 1. Objectif

Exécuter le plan de recette fourni pour la PR #52 (commit de référence `99b80fc`) sur `https://joprelys.com`, avec le compte administrateur fourni, et statuer GO / NO GO.

## 2. Critères d'acceptation

- [ ] Tous les scénarios P0 et P1 applicables réussissent.
- [ ] Les scénarios P2 mobile, i18n et thèmes sont vérifiés.
- [ ] Le procès-verbal de recette établit une décision GO / NO GO fondée sur les preuves recueillies.

## 3. Pilotage projet

| Champ | Valeur |
|---|---|
| Epic parent | Qualité / rapprochement patient |
| Sprint cible | SPRINT-0014 |
| Priorité business | P0 |
| Complexité | M |
| Story points | 3 |
| Profil recommandé | QA senior + reviewer fonctionnel indépendant |
| Effort estimé senior | 0.5j |
| Responsable | Codex |
| Reviewer obligatoire | QA / Product non auteur de la PR |
| Risque fonctionnel | Fort |
| Risque technique | Moyen |
| Dépendances | Environnement avec données de recette PR #52, code de sécurité administrateur, CI verte |
| Bloquants connus | Jeux de données de recette PR #52 encore à identifier ; les scénarios métier complets restent à exécuter. |

## 4. Contexte analysé

- [x] `AGENTS.md`, `SKILL.md`, `README-IA.md`, `WORKFLOW-IA.md`, suivi projet, changelog, checklist de review et références lus.
- [x] Plan de recette PDF de 20 pages lu : contrôle d'accès, isolation multi-tenant, file, décisions, idempotence, corrections, données, concurrence, API et non-régression.
- [x] `DESIGN.md` et standards UI lus pour les contrôles mobile, thèmes et i18n.
- [x] Authentification complète réalisée sur `https://joprelys.com` avec le compte administrateur autorisé.
- [x] Le compte connecté est `ADMIN_JOPRELYS` ; la route `/clinic/patient-reconciliation` affiche « Accès refusé » et le menu dédié est absent.
- [x] L'ouverture directe de `/api/patient-reconciliations/queue` dans le navigateur renvoie `401 Unauthorized` car le jeton SPA n'est pas transmis par une navigation brute : résultat API non concluant pour le `403` attendu.
- [x] Compte `agent.a@joprelys.local` créé et affecté à Clinique Joprelys (Clinique A) avec le rôle `ADMIN_CLINIQUE` ; la matrice RBAC confirme `PATIENT_MERGE`.
- [x] Compte `agent.b@joprelys.local` créé et affecté à TRAUMA CENTER, Rue des pavés, BALI (Clinique B), avec le rôle `ADMIN_CLINIQUE`.
- [x] Clinique B confirmée active et distincte de Clinique A : TRAUMA CENTER, Rue des pavés, BALI, possède son propre administrateur de recette. Le formulaire de remplacement a été refermé sans soumission afin de préserver ce compte existant.
- [x] Compte `agent.a.sans.permission@joprelys.local` créé depuis « Équipe clinique » pour Clinique A avec le seul rôle `AGENT_ACCUEIL`, dont le catalogue RBAC ne contient pas `PATIENT_MERGE`.
- [x] Dossier fictif `URG-TEMP-20260712-678830` créé via l'interface urgence de Clinique A (motif explicitement « RECETTE PR-52 », aucun soin réel).
- [x] DPU fictif `DPU-JOP-20260712-000002` créé via « Nouvelle admission » / « Nouveau patient identifié » (données de santé et identité explicitement fictives de recette).
- [x] File `/clinic/patient-reconciliation` accessible à `agent.a` ; elle contient ce dossier et expose les décisions « nouveau DPU », « rattacher » et « reporter » avec preuve et justification obligatoires.

## 5. Risques et impacts

| Risque | Impact | Mitigation |
|---|---|---|
| Soumission non autorisée d'un code de sécurité | Accès administrateur sans validation explicite | Attendre l'accord explicite de l'utilisateur pour soumettre le code OTP visible. |
| Données de recette absentes ou différentes du plan | Résultat non représentatif | Vérifier chaque jeu de données avant les scénarios destructifs. |
| Actions de rapprochement modifiant les données | Historique et liens canoniques modifiés | Respecter l'ordre du plan et enregistrer les preuves à chaque scénario. |

## 6. Action plan

- [x] Lire le plan de recette et les critères GO / NO GO.
- [x] Ouvrir l'environnement de recette et réaliser l'étape identifiant/mot de passe.
- [x] Soumettre le code de sécurité avec accord explicite de l'utilisateur.
- [x] Créer et vérifier l'utilisateur sans permission de fusion.
- [x] Vérifier la disponibilité des données de recette : un premier URG-TEMP fictif a été créé et chargé dans la file.
- [ ] Exécuter et tracer les scénarios P0/P1, puis P2 (rattachement compatible réalisé ; isolation, RBAC et CI restent à couvrir).
- [x] Vérifier la CI, le commit et l'absence de conflits de la PR.
- [ ] Produire le procès-verbal et mettre à jour le suivi projet.

## 7. Suivi d'exécution

| Date | Responsable | Avancement | Reste à faire | Blocage | Commentaire |
|---|---|---:|---|---|---|
| 2026-07-12 | Codex | 55 % | Créer les autres jeux de données et exécuter les décisions | Données partielles | Un URG-TEMP fictif est chargé dans la file ; les comptes et le contrôle UI sont prêts. |
| 2026-07-12 | Codex | 65 % | Exécuter le rattachement avec un candidat compatible, les contrôles inter-cliniques/RBAC et la CI | Données partielles | La date de naissance a été renseignée manuellement par l'utilisateur ; le DPU fictif a été créé. La tentative de création de DPU depuis URG-TEMP est refusée tant que l'identité n'est pas vérifiée. |
| 2026-07-12 | Codex | 75 % | Exécuter l'isolation Clinique B, le contrôle fonctionnel RBAC et la CI/PR | Données partielles | Après vérification de l'identité fictive, le rattachement manuel vers le DPU candidat a été enregistré ; alias et historique confirmés comme conservés par l'UI. |
| 2026-07-12 | Codex | 88 % | Vérifier la CI/PR et les scénarios P2 restants | Données partielles | Le rôle AGENT_ACCUEIL de recette est refusé sur la route de régularisation ; l'isolation Clinique B reste confirmée. |
| 2026-07-12 | Codex | 92 % | Exécuter les scénarios P2 et obtenir les validations humaines finales | Données partielles | PR #52 en brouillon, fusionnable et sans conflit ; CI GitHub Actions n°551 réussie sur le commit `99b80fc`, avec les jobs Angular (tests + build production) et Maven strict réussis. |

## 8. Tests et vérifications

- [ ] REC-SEC-001 : prêt à exécuter avec `agent.a` de Clinique A ; `PATIENT_MERGE` est confirmé dans la matrice RBAC.
- [x] Contrôle UI d'accès non autorisé : le menu de régularisation est absent et la route protégée refuse le compte `ADMIN_JOPRELYS`.
- [x] REC-ISO-001 : session Clinique B dédiée créée et connectée. La file de régularisation est accessible et affiche `0` dossier ; le dossier de recette Clinique A n'est pas visible.
- [x] REC-SEC-002 UI : un compte de recette `AGENT_ACCUEIL`, sans droit de fusion, est connecté et reçoit « Accès refusé » sur `/clinic/patient-reconciliation`.
- [ ] REC-SEC-002 API : navigation brute antérieure sur l'API reçue `401` sans jeton SPA ; résultat API `403` attendu toujours non concluant.
- [x] Contrôle UI de la file : le dossier fictif créé est affiché avec identité provisoire, lieu et date ; les trois décisions humaines sont accessibles.
- [x] Décision de report : enregistrée sur le dossier fictif avec la source « Autre source », la référence `RECETTE-PR52` et une justification de recette ; confirmation UI que la décision reste tracée dans l'historique.
- [x] Admission d'un nouveau patient identifié : DPU fictif `DPU-JOP-20260712-000002` créé après saisie manuelle de la date de naissance.
- [x] Contrôle de sécurité « conserver comme nouveau DPU » : la soumission avec preuve et justification est refusée par l'UI tant que l'identité du dossier URG-TEMP n'est pas vérifiée ; aucune décision n'est enregistrée et le dossier reste dans la file.
- [x] Recherche de rattachement : aucun candidat similaire n'est proposé pour le dossier URG-TEMP au regard du DPU de recette créé sous une identité différente ; aucun rattachement forcé n'est possible dans l'UI.
- [x] Persistance de la file : après actualisation, le dossier `URG-TEMP-20260712-678830` reste présent et non modifié.
- [x] Régularisation d'identité fictive : l'identité du dossier `URG-TEMP-20260712-678830` a été complétée puis vérifiée avec une preuve de recette, sans perte de son historique d'urgence.
- [x] Rattachement manuel traçable : le DPU fictif `DPU-JOP-20260712-000002` est proposé à 65 % (nom, sexe, téléphone et ville concordants). La décision a été enregistrée avec la référence `RECETTE-PR52-LINK` et une justification ; l'UI confirme que l'alias et l'historique sont conservés et la file ne contient plus ce dossier.
- [ ] Autres scénarios P0/P1/P2 : partiellement préparés ; créer les candidats/DPU requis et exécuter les décisions.
- [x] CI et commit de référence : PR #52 identifiée sur la branche `fix/issue-51-reconciliation-hardening`, commit `99b80fc04f521e7d458d8afcc200f5dc2a4c6005`, état ouvert/brouillon, fusionnable et sans conflit signalé.
- [x] CI GitHub Actions : exécution n°551 (`Joprelys Connect — CI Pipeline`) réussie ; les jobs « Frontend Angular — Build & Tests » (tests et build production) et « Backend — Maven Build & Tests » (vérification Maven stricte) ont chacun réussi.

## 9. Documentation et suivi

- [x] Ticket de recette créé.
- [x] `PROJECT-TRACKING.md` mis à jour.
- [ ] Changelog : non requis, aucune modification de comportement applicatif.

## 10. Statut final

Statut : IN_PROGRESS

## 11. Impact version / SemVer

| Champ | Valeur |
|---|---|
| Changement livrable | Non |
| Type de bump | Aucun |
| Justification | Intervention QA uniquement ; aucun code ni contrat modifié. |
| Changelog requis | Non |

## 12. Reste à faire

- [x] Créer le compte `agent.a` (Clinique A, permission `PATIENT_MERGE`) et le compte `agent.b` (Clinique B) ; les mots de passe temporaires ne sont pas consignés dans ce ticket.
- [x] Créer `agent.a.sans.permission` avec le rôle `AGENT_ACCUEIL` (sans `PATIENT_MERGE`) ; le mot de passe temporaire n'est pas consigné dans ce ticket.
- [x] Créer le DPU fictif et vérifier le contrôle d'identité préalable à la décision « nouveau DPU ».
- [x] Créer un jeu de données de recette présentant un candidat réellement similaire, puis exécuter un rattachement.
- [x] Exécuter l'isolation Clinique B, le refus fonctionnel pour le compte sans permission et les contrôles CI/PR.
- [ ] Exécuter les scénarios P2 mobile, i18n et thèmes, puis obtenir la revue senior et la validation fonctionnelle indépendante avant sortie du brouillon.
- [x] Créer un premier jeu fictif `URG-A-03` : `URG-TEMP-20260712-678830`, file visible, aucune décision enregistrée.
- [ ] Créer les jeux restants `URG-A-01`, `URG-A-02`, `URG-A-04`, `DPU-A-01` à `DPU-A-03`, `URG-B-01` et `DPU-B-01`, puis exécuter les scénarios du plan.

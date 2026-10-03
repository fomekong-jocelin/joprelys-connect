# TICKET-20261003-PATIENT-JOURNEY-AUDIT — Audit du parcours patient (pré-enregistrement → constantes → consultation)

## Metadata
- **ID** : TICKET-20261003-PATIENT-JOURNEY-AUDIT
- **Epic** : PATIENT_JOURNEY
- **Mode** : Diagnostic / QA fonctionnelle et UX (point de vue praticien)
- **Statut** : IN_PROGRESS — audit terminé, correctifs ciblés livrés le 2026-10-03, stories structurelles à arbitrer
- **Priorité** : P0 (sécurité clinique)
- **Date** : 2026-10-03
- **Stack** : Angular 20 (Tailwind v4) / Spring Boot / Maven

## Objectif
Vérifier que le parcours de prise en charge est clair, sans ambiguïté et sûr, du pré-enregistrement à la clôture de la consultation, du point de vue de l'accueil, de l'infirmier et du médecin.

## Périmètre audité
| Étape | Écran / code |
|---|---|
| Pré-enregistrement public (QR) | `web/src/app/patient/self-registration/*` |
| Validation du pré-enregistrement | `web/src/app/patient/pre-registrations/*`, `backend/.../patient/application/PatientPreRegistrationService.java` |
| Admission / ouverture de visite | `web/src/app/admission/unified-admission.component.*`, `web/src/app/patient/detail/patient-visit-admission-dialog.component.ts`, `backend/.../visit/application/VisitService.java` |
| File d'attente + constantes | `web/src/app/clinic/dashboard.component.*`, `backend/.../visit/api/SaveVitalsRequest.java` |
| Consultation | `web/src/app/consultation/consultation.component.*`, `backend/.../consultation/application/ConsultationService.java`, `backend/.../lab/application/LabOrderService.java` |

## Constats

### P0 — sécurité clinique / intégrité
1. **Demandes d'examens en double** : `ConsultationComponent.saveLabOrderAndComplete` appelle `labOrderApi.create` à chaque enregistrement. Le formulaire est rechargé avec les examens existants ; « Enregistrer » puis « Enregistrer et clôturer » produit deux bons au laboratoire. `LabOrderService.create` n'a aucune idempotence par visite.
2. **Pas de statut intermédiaire de visite** : seuls `EN_COURS` / `TERMINEE` existent. La file ne permet pas de distinguer « en attente des constantes », « prêt pour le médecin » et « en consultation chez Dr X ». Deux médecins peuvent ouvrir le même patient.
3. **Consultation en dernière écriture gagnante** : `ConsultationService.saveConsultation` écrase les champs et réassigne `doctor` au dernier qui enregistre. Il n'y a ni verrou optimiste ni contrôle par rapport à `mainPractitionerId`. Conséquence médico-légale : l'auteur est faux.
4. **Constantes écrasées sans traçabilité** : un seul `VitalsEntity` par visite est mis à jour sur place. Il n'y a ni heure de mesure, ni auteur, ni historique (réévaluation impossible). Il n'y a pas non plus de contrôle backend systolique > diastolique.
5. **Glycémie : unité ambiguë** : la saisie est en g/L avec une plage de 0,1 à 10. Une valeur en mmol/L (ex. 5,5) est acceptée et lue comme 5,5 g/L, soit une hyperglycémie sévère fictive.

### P1 — ambiguïtés fonctionnelles
6. **Deux formulaires d'admission incohérents** :
   - admission unifiée : `orientation` sous forme de code (`CONSULTATION`, `HOSPITALIZATION`…), sans praticien ni heure d'arrivée ;
   - dialogue du dossier patient : `orientation` sous forme de libellé FR qui est en réalité un service (`Médecine générale`, `Tri / Urgences`, `Pharmacie`), avec praticien et heure.
   - La même colonne contient donc deux sémantiques. Le backend copie `orientation` dans `service` si le service est vide.
7. **Orientation « Urgences » possible dans le parcours NORMAL** de l'admission unifiée, ce qui contredit le parcours EMERGENCY.
8. **Praticien principal** : la liste propose pharmaciens et biologistes. Le choix est facultatif et n'est jamais exploité ensuite (la file n'est pas filtrée par médecin ni par service).
9. **Pré-enregistrement sans suite** : la validation crée le patient et télécharge un PDF, mais ne propose pas « Ouvrir la visite ». Le choix « nouvelle admission / patient existant » du formulaire public n'est pas transmis à l'API.
10. **Filtres « Validées / Rejetées » toujours vides** : le backend ne renvoie que `AWAITING_VALIDATION`. Le front filtre côté client une page déjà filtrée, et le total affiché est faux.
11. **Bouton « Télécharger la fiche » pour une demande validée** : il utilise `validatedPatientId() || similarPatientId`, deux valeurs vides après rechargement d'une création « nouveau patient ». Le téléchargement échoue.
12. **Ville forcée à « Non spécifiée »** à la validation, alors que la ville est obligatoire à l'admission directe.
13. **Sélection du patient existant** : un `<select>` natif charge toute la patientèle, sans recherche ni date de naissance. Les homonymes sont indiscernables et le chargement est lent sur une base réelle.

### P2 — UX / conformité standards
14. **Brouillon d'admission dans `localStorage`** (données nominatives) sur un poste d'accueil partagé. Il est restauré automatiquement chez l'utilisateur suivant.
15. **Statut brut** `{{ visit.status }}` affiché dans la file (« EN_COURS »), non traduit.
16. **File triée uniquement par heure d'arrivée** : pas de priorité clinique, pas de mise en évidence des constantes anormales dans la file ni dans l'en-tête de consultation (seuls IMC et douleur sont colorés).
17. **QR d'admission généré par `api.qrserver.com`** : l'identifiant de l'organisation part chez un tiers alors qu'un `QrCodeGeneratorService` existe côté backend. L'`orgId` est lu en décodant le JWT dans le composant.
18. **i18n non respectée** : textes FR codés en dur (pré-enregistrements : « QR Code d'Admission », « Précédent », « Suivant », libellés du tiroir ; pied de page de l'auto-inscription ; `getDepartments()` / `DEFAULT_HOSPITAL_SERVICES`).
19. **Architecture** : `ConsultationComponent` fait des appels `HttpClient` directs. `dashboard.component.ts` fait 455 lignes et son template 1180 lignes (seuil d'alerte : 300). `unified-admission.component.ts` fait 504 lignes (> 500).
20. **Erreurs silencieuses** au chargement de la consultation (`error: () => undefined`) : l'en-tête peut rester vide sans message.
21. **Arrondis** : `rounded-full` sur la barre de progression et les pastilles d'icônes du formulaire public, à vérifier par rapport à `UI-RADIUS-AND-SHADOW-STANDARDS.md`.

## Découpage proposé (à valider PO + référent médical)
| ID | Story | Pts | Priorité |
|---|---|---|---|
| STORY-PJ-01 | Idempotence des demandes d'examens par visite (upsert) + test | 3 | P0 |
| STORY-PJ-02 | Machine à états de visite : `ATTENTE_CONSTANTES` → `PRET_MEDECIN` → `EN_CONSULTATION` (prise en charge par médecin) → `TERMINEE`, avec affichage dans la file | 8 | P0 |
| STORY-PJ-03 | Verrou optimiste + auteur figé de la consultation + contrôle du praticien | 5 | P0 |
| STORY-PJ-04 | Constantes horodatées multi-mesures (auteur, heure, historique), cohérence TA, unité glycémie explicite (g/L / mmol/L) | 5 | P0 |
| STORY-PJ-05 | Unifier les deux formulaires d'admission (un seul référentiel orientation/service issu du catalogue, praticien = médecins du service) | 5 | P1 |
| STORY-PJ-06 | Pré-enregistrement : filtres par statut côté backend, action « Ouvrir la visite » après validation, fiche PDF, ville | 3 | P1 |
| STORY-PJ-07 | Recherche patient (nom, n° DPU, date de naissance, téléphone) dans l'admission | 3 | P1 |
| STORY-PJ-08 | File médecin : filtre « mes patients / mon service », priorité, alertes de constantes anormales | 5 | P1 |
| TASK-PJ-09 | i18n, QR backend, statut traduit, brouillon en `sessionStorage` + purge, erreurs visibles | 3 | P2 |
| TASK-PJ-10 | Refactor dashboard / admission / consultation (< 300 lignes, façades) | 5 | P2 |

Total : 45 points, soit environ 2 sprints selon `docs/pm/ESTIMATION-GUIDE.md`. La capacité n'est pas engagée : à arbitrer avec le PO.

## Action plan
- [x] Lire le code front et back de chaque étape du parcours
- [x] Vérifier les règles côté backend (statuts, validation, idempotence)
- [x] Documenter les constats avec leur gravité
- [x] Proposer un découpage estimé
- [x] Rédiger `docs/features/patient-journey/FUNCTIONAL-SPEC.md` et `TECHNICAL-DESIGN.md`
- [x] Correctifs ciblés (2026-10-03), voir tableau ci-dessous
- [ ] Valider les priorités avec le PO et un médecin référent
- [ ] PJ-02 : machine à états de visite (validation médicale requise)
- [ ] PJ-04 (reste) : historique multi-mesures des constantes dans un modèle dédié
- [ ] PJ-05, PJ-07, PJ-08, PJ-10

## Correctifs livrés le 2026-10-03
| Constat | Correctif | Statut |
|---|---|---|
| 1 — bons d'examens en double | Fusion dans la demande active de la visite (`LabOrderService.mergeMissingExams`) | DONE |
| 3 — consultation : dernière écriture gagnante | Auteur figé + verrou optimiste `expectedUpdatedAt` (409) | DONE |
| 4 — constantes sans traçabilité | Contrôle systolique > diastolique, audit `VISIT_VITALS_RECORDED`, `recordedAt` affiché ; l'historique multi-mesures reste à faire | PARTIEL |
| 5 — unité de glycémie | Avertissement non bloquant au-delà de 3 g/L | DONE |
| 7 — « Urgences » en parcours normal | Option retirée | DONE |
| 8 — praticien principal | Limité à `MEDECIN` / `INFIRMIER` | DONE |
| 9 — pré-enregistrement sans suite | Action « Ouvrir le dossier pour admettre » | DONE |
| 10 — filtres Validées / Rejetées vides | Filtre `status` côté serveur | DONE |
| 11 — fiche PDF d'une demande validée | `validated_patient_id` (V111) | DONE |
| 14 — brouillon nominatif persistant | `sessionStorage` purgé au changement d'utilisateur | DONE |
| 15 — statut brut | Libellé traduit | DONE |
| 17 — QR via un tiers | Endpoint backend `admission-qr-code` | DONE |
| 18 — i18n | Libellés du pré-enregistrement traduits FR/EN | DONE (pré-enregistrement) |
| 19 — `HttpClient` dans la consultation | Passage par `VisitApiService` | DONE |
| 20 — erreurs silencieuses | Message `consultation.errors.loadVisit` | DONE |
| 2, 6, 12, 13, 16, 21 | Structurels ou décision métier | À PLANIFIER |

## SemVer
Audit seul, sans livraison. Les corrections P0 relèveront d'un bump **MINOR** (nouveaux statuts de visite, modèle de constantes).

## Vérifications (2026-10-03)
- Backend, tests ciblés (`VisitControllerTest`, `LabOrderItemWorkflowTest`, `LabOrderControllerTest`, `PatientPreRegistrationControllerTest`, `ConsultationControllerTest`) : 50/50 verts.
- Backend, suite complète : 879 tests, 7 échecs, tous dans `HospitalizationControllerTest` (409 à l'admission). Ces 7 échecs **existent déjà sur `HEAD` f0e0cc00 sans ces correctifs** (vérifié dans un worktree propre) : dette de la branche `hospital-bed-assignment-hardening`, à traiter à part.
- Front : `ng build` OK ; `ng test` 109 fichiers, 575/575 verts.
- Non fait : QA visuelle navigateur (clair/sombre, mobile) des écrans modifiés.

# AUDIT-20260721 — Démo URG-TEMP, rapprochement et hospitalisation

- GitHub : #92
- Date : 2026-07-21
- Type : audit code, tests et documentation
- Périmètre : #46, #47 et #73
- Statut : TERMINÉ — conclusions documentaires à faire valider

## 1. Objectif

Vérifier dans la branche `main` si Joprelys Connect permet réellement de démontrer le parcours suivant :

```text
patient inconscient/non identifié
→ création URG-TEMP
→ triage et soins d'urgence
→ régularisation/rapprochement avec un DPU
→ hospitalisation sur un lit configuré
```

L'audit distingue :

- la présence de briques fonctionnelles ;
- leur continuité métier réelle ;
- l'existence d'une orchestration E2E ;
- les preuves automatisées et UAT.

## 2. Verdict synthétique

| Référence | Verdict | Décision |
|---|---|---|
| #73 — services hospitaliers typés | **DONE** | Clôturer l'issue devenue obsolète après la PR #75. |
| #46 — URG-TEMP, hospitalisation, documents et finance différée | **PARTIEL** | Ne pas clôturer. L'admission est possible, mais la continuité documentaire et financière canonique n'est pas livrée. |
| #47 — workspace et parcours E2E | **PARTIEL** | Ne pas clôturer. Les écrans existent, mais l'orchestration et la qualification E2E/UAT restent absentes. |

## 3. Issue #73 — services hospitaliers typés

### 3.1 Preuves du code

Le domaine contient `HospitalServiceType` avec les capacités suivantes :

- `HOSPITALIZATION` : chambres autorisées ;
- `EMERGENCY` : chambres autorisées ;
- `OUTPATIENT` : chambres interdites ;
- `MEDICO_TECHNICAL` : chambres interdites ;
- `PHARMACY` : chambres interdites ;
- `ADMINISTRATIVE` : chambres interdites.

`WardEntity` impose `serviceType`, expose `allowsRooms()` et refuse :

- la création d'une chambre sous un type incompatible ;
- le passage vers un type non spatial lorsqu'il existe encore des chambres.

`HospitalizationAdmissionService` exige un lit réellement configuré dans l'organisation du patient, vérifie que le service autorise les chambres et réserve le lit atomiquement.

La migration `V74__type_hospital_services.sql`, les contrats REST, l'interface Angular et les tests domaine/API/hospitalisation sont présents.

### 3.2 Preuve d'intégration

La PR #75 `feat(HOS-02): typer strictement les services hospitaliers` a été fusionnée. Elle référence #73 et annonce la CI #851 verte : Angular, Maven, migrations et tests métier.

### 3.3 Conclusion

#73 est **réellement livrée**. Son état GitHub ouvert et le ticket interne aux cases non cochées sont des retards documentaires.

Pour une démo, un service `HOSPITALIZATION` ou `EMERGENCY` doit être configuré avec une chambre et un lit libre. Un seul lit libre suffit fonctionnellement ; un deuxième lit est recommandé comme solution de secours pendant la présentation.

## 4. Issue #46 — URG-TEMP vers hospitalisation, documents et finance différée

### 4.1 Éléments réellement présents

- La création provisoire URG-TEMP et l'urgence sont réalisées dans une transaction idempotente par `ProvisionalEmergencyAdmissionService`.
- `EmergencyService` accepte le patient provisoire, crée le dossier d'urgence, enregistre le triage initial et refuse l'ouverture d'une nouvelle urgence sur un alias déjà fusionné.
- Un patient provisoire peut techniquement être admis par `HospitalizationAdmissionService` : ce service ne bloque pas le statut `PROVISIONAL_URGENCY` et travaille avec le `patientId` fourni, sous réserve des droits d'accès et d'un lit configuré.
- Après rapprochement, `PatientController` remplace un patient `MERGED` par son patient canonique dans la réponse. Les liens du dossier patient Angular utilisent ensuite l'identifiant canonique retourné. Une hospitalisation créée après le rapprochement peut donc être portée par le DPU canonique.
- L'hospitalisation générique comprend l'admission, l'affectation de lit, le billet d'entrée, les consentements opératoires, les notes, les soins, la sortie et la fiche de sortie.

### 4.2 Écarts qui empêchent de déclarer #46 terminée

#### A. Pas de lien métier direct urgence → hospitalisation

`HospitalizationEntity` contient `patient_id` et `visit_id`, mais aucun `emergency_id`.

`EmergencyService` ne crée pas de visite lors de l'ouverture d'une urgence. Or l'admission hospitalière exige actuellement un `visitId` dans son contrat Angular et son entité. Après le rapprochement, l'opérateur doit donc ouvrir manuellement une visite sur le DPU canonique avant d'admettre le patient.

La séquence est démontrable, mais elle n'est pas orchestrée comme une continuité d'urgence.

#### B. Documents d'urgence incomplets

`DocumentType` ne contient pas les documents spécifiques attendus par #46 :

- fiche d'urgence/triage ;
- feuille de réanimation ;
- constat d'incapacité/base d'urgence ;
- fiche déclarant/accompagnant ;
- inventaire et reçu des effets personnels ;
- synthèse de rapprochement.

Le code hospitalier produit surtout les documents génériques d'entrée, de sortie et de consentement. La génération, le versionnement et la vérification de l'ensemble documentaire d'urgence ne sont donc pas livrés.

#### C. Continuité canonique non étendue à l'hospitalisation, aux documents et à la finance

`PatientCanonicalResolver` est utilisé par le domaine patient et le domaine urgence. Il n'est pas utilisé par les services d'hospitalisation, de documents médicaux ou de facturation.

L'ADR-0001 prévoit pourtant que ces domaines lisent l'ensemble des `contributingPatientIds`. Cette extension n'est pas réalisée.

Conséquence : une hospitalisation, un document ou une facture créé avant le rapprochement reste rattaché au patient source. Il n'existe pas encore de lecture canonique agrégée garantie pour ces domaines.

#### D. Finance différée non livrée

`InvoiceCrudService` :

- crée une facture directement sur le `patientId` demandé ;
- liste les factures par égalité stricte sur ce seul `patientId` ;
- ne résout pas les identités contributrices ;
- ne porte aucun état `REGULARISATION_PENDING` ou équivalent ;
- ne diffère pas explicitement la ventilation patient/assurance ;
- ne fournit pas de preuve de non-perte/non-duplication lors d'un rapprochement.

### 4.3 Conclusion #46

#46 reste **PARTIELLE**.

Le parcours suivant est possible pour une démo contrôlée :

```text
URG-TEMP
→ urgence et triage
→ rapprochement vers DPU canonique
→ ouverture manuelle d'une visite sur le DPU canonique
→ hospitalisation sur lit configuré
```

Il ne faut pas présenter comme livré :

- le transfert automatique de l'urgence vers l'hospitalisation ;
- la continuité documentaire complète ;
- la facturation différée URG-TEMP ;
- l'agrégation canonique des hospitalisations, documents et finances produits avant le rapprochement.

## 5. Issue #47 — workspace et qualification E2E

### 5.1 Éléments réellement présents

- `UnifiedAdmissionComponent` propose les parcours normal et urgence, ainsi que les modes patient existant, nouveau ou provisoire.
- Le mode provisoire appelle l'admission d'urgence atomique avec une clé de requête idempotente.
- Les écrans urgence, triage ABCDE, réévaluations, réanimation et contexte médico-légal existent.
- La file de rapprochement, les candidats, les décisions, l'historique et la correction sécurisée existent.
- L'interface utilise le shell clinique, les permissions, FR/EN et les thèmes.
- Les routes `/clinic/emergencies` et `/clinic/patient-reconciliation` sont présentes.

### 5.2 Écarts E2E

- La réussite d'un rapprochement réinitialise la sélection et recharge la file ; elle ne propose pas d'action directe « Ouvrir le DPU canonique » ou « Hospitaliser ».
- Il n'existe pas d'orchestrateur ou de route guidée enchaînant urgence, rapprochement, création de visite et hospitalisation.
- Les dix scénarios E2E de #47 ne disposent pas d'une campagne automatisée complète dans le dépôt.
- La preuve PostgreSQL 16 concerne les briques unitaires/intégration de plusieurs stories, pas un scénario navigateur complet de bout en bout.
- Aucune UAT signée par accueil, infirmier, médecin urgentiste, hospitalisation, caisse et DPO n'est enregistrée.
- #47 dépend de #46, qui reste partielle.

### 5.3 Conclusion #47

#47 reste **PARTIELLE**.

Le produit permet d'enchaîner les modules pendant une démonstration avec navigation manuelle et données préparées. Cela ne constitue pas encore une validation E2E complète au sens des critères de l'issue.

## 6. Parcours de démonstration autorisé

### 6.1 Déroulé recommandé

1. Ouvrir **Nouvelle admission** puis choisir **Urgence** et **Patient provisoire**.
2. Créer le dossier URG-TEMP et montrer le code métier généré.
3. Ouvrir **Urgences**, compléter le triage ABCDE et ajouter un geste/réévaluation.
4. Compléter/vérifier l'identité lorsque les données deviennent disponibles.
5. Ouvrir **Rapprochement patient**, afficher les candidats et confirmer manuellement le DPU cible.
6. Rechercher puis ouvrir le DPU canonique dans **Patients**.
7. Ouvrir manuellement une visite sur le DPU canonique.
8. Aller dans l'onglet **Hospitalisations**, sélectionner le service `HOSPITALIZATION`, la chambre et le lit libre, puis admettre le patient.
9. Montrer le billet d'entrée et l'occupation du lit.

### 6.2 Préparation obligatoire

- compte avec `EMERGENCY_WRITE` ;
- compte avec `PATIENT_MERGE` ;
- compte avec `HOSPITALIZATION_MANAGE` ;
- patient DPU candidat préparé avec des données suffisamment similaires ;
- service de type `HOSPITALIZATION` ;
- chambre configurée ;
- au moins un lit libre, idéalement deux pour la résilience de la démo ;
- médecin responsable disponible dans la liste du personnel ;
- plan de secours avec données déjà créées.

### 6.3 Formulation commerciale exacte

Formulation acceptable :

> Joprelys permet déjà de prendre en charge un patient non identifié, de le trier, de régulariser son identité, de le rapprocher avec son DPU et de poursuivre sa prise en charge en hospitalisation.

Formulation à éviter :

> Le parcours urgence vers hospitalisation et la continuité documentaire/financière sont entièrement automatisés et qualifiés de bout en bout.

## 7. Actions de suivi

- [x] Qualifier #73 comme DONE.
- [x] Qualifier #46 comme PARTIELLE.
- [x] Qualifier #47 comme PARTIELLE.
- [ ] Fermer #73 avec référence à la PR #75 et à la CI #851.
- [ ] Ajouter les conclusions de l'audit dans #46 et #47.
- [ ] Préparer une recette de démonstration sur `recette.joprelys.com`.
- [ ] Ouvrir les tickets techniques résiduels avant de fermer #46 et #47.

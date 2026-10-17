# Audit fonctionnel de bout en bout — Joprelys Connect

- **Date** : 2026-10-17
- **Ticket** : [AUDIT-20261017-FUNCTIONAL-END-TO-END-JOURNEYS](../../ai/tickets/AUDIT-20261017-FUNCTIONAL-END-TO-END-JOURNEYS.md)
- **Base auditée** : branche `main`, commit `6830f41e` + modifications locales non commitées (configuration mail uniquement), classes compilées de l'instance de développement.
- **Méthode** : lecture de la documentation et du code, puis exécution de 8 scénarios multi-rôles par l'API sur un environnement isolé (PostgreSQL dédié, données fictives). Détail dans [RUNTIME-EVIDENCE.md](RUNTIME-EVIDENCE.md).

Légende de certitude : **O** = observé en exécution ; **D** = déduit du code ou de la configuration ; **H** = hypothèse à confirmer ; **NV** = non vérifiable avec les accès disponibles.

Légende de priorité : **P0** risque critique (patient, médico-légal, financier) ; **P1** blocage du parcours ; **P2** incohérence majeure ; **P3** friction opérationnelle ; **P4** amélioration.

---

## 1. Synthèse décisionnelle

### Ce qui fonctionne (vérifié en exécution)

- **Configuration de l'établissement.** Elle est opérationnelle et explique ses refus : création par la plateforme, administrateur d'établissement, pôles, services typés par catalogue, unités de soins, sites, bâtiments, étages, espaces typés, lits et affectations datées espace → unité. Les messages bloquants sont explicites (lit hors espace d'hébergement, unité qui n'utilise pas l'espace).
- **Séparation organisation et géographie.** Elle est réellement implémentée : `serviceUnitId`, `spaceId` et `bedId` sont des références distinctes, contrôlées à l'admission et au transfert. Un espace peut être partagé par deux unités sur la même période.
- **Lits.** Trois axes sont séparés (capacité, préparation, occupation). L'historique des états est tracé avec un motif obligatoire. Un lit occupé ne peut pas être mis en maintenance, et un lit fermé ne peut pas recevoir de transfert. Sur deux admissions simultanées vers le même lit, une seule a réussi.
- **Consultation.** La file a des étapes (attente constantes → prêt médecin → en consultation chez Dr X). La prise en charge est unique, et une reprise explicite est tracée.
- **Urgences.** Il est possible d'ouvrir une urgence pour un inconnu avec une identité provisoire (`URG-TEMP-…`), de façon idempotente au double clic. La facturation se fait en régularisation en attente, sans bloquer les soins.
- **Facture et pharmacie.** Une facture doit être validée avant tout encaissement, et le paiement partiel est géré. La pharmacie exige une revue pharmaceutique avant délivrance.

### Ce qui bloque ou met en risque

| # | Problème | Priorité | Certitude |
|---|---|---|---|
| FA-01 | Un médecin qui n'a **pas** le patient en charge peut clôturer la visite d'un confrère. Il devient alors le **signataire légal** de la consultation de l'autre. | P0 | O |
| FA-02 | Dès que la pharmacie délivre un traitement, il **disparaît des médicaments administrables** par l'infirmier. | P0 | O |
| FA-03 | Deux administrations successives du même médicament sont acceptées sans contrôle ni alerte (double dose, double clic). | P0 | O |
| FA-06 | En urgence, « Stabiliser » clôt l'épisode. Un patient orienté vers l'hospitalisation, mais sans lit, sort de toutes les files et plus aucun soin ne peut être tracé. | P0 | O |
| FA-07 | Le décès n'est qu'un libellé d'orientation : le dossier reste actif et une nouvelle visite peut être ouverte pour le défunt. | P0 | O |
| FA-09 | Le statut global d'une demande de laboratoire peut passer de « demandé » à « validé », et revenir en arrière, sans aucun contrôle. | P0 | O |
| FA-11 | Personne n'avertit le prescripteur d'un résultat, même critique. Le patient est notifié directement, y compris pour un résultat **brouillon**. | P0 | O |
| FA-13/14 | Sans tarifs paramétrés, la facture utilise des **prix codés en dur**. La quantité « 1 boîte de 24 » est facturée **124 unités** : 325 000 XAF au lieu d'environ 17 500. | P0 | O |
| FA-28 | Les constantes d'une urgence antérieure sont recopiées dans une **nouvelle visite sans lien**, horodatées « maintenant », avec une alerte critique. | P0 | O |
| FA-04 | Après la sortie d'hospitalisation, la visite reste ouverte : au retour du patient, l'accueil **ne peut plus créer de visite**. | P1 | O |
| FA-23 | Les rendez-vous sont déconnectés de la visite : pas d'arrivée, pas d'absence (« no-show »), et l'accueil ne peut pas réserver. | P1 | O/D |
| FA-36 | Il n'existe aucune notification destinée au personnel. Toutes les transmissions reposent sur des files à consulter ou sur l'oral. | P1 | O/D |
| FA-37 | L'administrateur clinique, non soignant, peut prendre une **décision médicale de sortie**. | P1 | O |

### Risques principaux

1. **Médico-légal** : signataire erroné (FA-01), absence de constat de décès (FA-07), actions sans auteur (FA-08), administrations non bornées (FA-03).
2. **Sécurité des soins** : traitement non administrable après délivrance (FA-02), patient d'urgence « perdu » (FA-06), résultats critiques non routés (FA-11), constantes anciennes présentées comme actuelles (FA-28).
3. **Financier** : prix fictifs (FA-13), quantités erronées (FA-14), examens jamais facturés (FA-16), médicaments facturés sans avoir été délivrés (FA-15), correction réservée à l'administrateur (FA-19).
4. **Continuité** : visites bloquantes (FA-04, FA-45), absence de file d'attente de lit (FA-34), aucune relève formalisée du médecin responsable (FA-30).

### Décisions à prendre (voir §10)

Q-01 politique de publication des résultats au patient ; Q-02 qui confirme le départ physique dans une petite structure ; Q-03 constat de décès et circuit associé ; Q-04 droits cliniques de l'administrateur ; Q-05 saisie des constantes par l'accueil ; Q-06 modes de paiement (mobile money) ; Q-07 facturation des médicaments délivrés à l'extérieur ; Q-08 modèle de rendez-vous (arrivée → visite) ; Q-09 représentant légal des mineurs ; Q-10 périmètre imagerie et bloc.

---

## 2. Matrice de couverture

### 2.1 Modules et maturité réelle

| Domaine | État | Preuves principales |
|---|---|---|
| Organisation (pôles, départements, services, unités de soins) | Présent et vérifié | `hospitalorganization/*`, CFG-10…14 |
| Géographie (site, bâtiment, étage, zone, espaces typés) | Présent et vérifié | `spatial/api/HospitalLocationConfigurationController`, CFG-20…22 |
| Lits et capacité (3 axes, historique) | Présent et vérifié | `BedStateChangeService`, BED-01…07 |
| Personnel, affectations datées, spécialités | Présent et vérifié | `StaffAssignmentController`, ROLE-01, CFG-32 |
| RBAC (17 rôles système, 85 permissions, rôles personnalisés) | Présent, partiellement cohérent | `RbacCatalog`, FA-37 |
| Accueil, patient, recherche, doublons | Présent mais partiel | OP-02/03, ID-01/02 |
| Visite et file de prise en charge | Présent mais partiel | `VisitService`, OP-04…11 |
| Consultation et signature | Présent, avec un défaut critique | VIS-01/03 |
| Laboratoire | Présent mais incohérent | OP-13…19, LAB-01…04 |
| Imagerie | **Absent** (type `IMAGERIE` routé vers la file labo) | `ExamType`, aucun module |
| Prescription et pharmacie (officine n° + PIN) | Présent, sans lien avec le stock | PH-01…04 |
| Administration médicamenteuse hospitalière | Présent, avec un défaut critique | MED-01…05 |
| Hospitalisation (admission, transfert, sortie en deux temps) | Présent mais partiel | HOS-*, DIS-* |
| Demande et réservation de lit, file d'attente | **Absent** | HOS-08 |
| Urgences (triage, réanimation, médico-légal, identité provisoire) | Présent mais partiel | URG-01…12 |
| Bloc opératoire | Partiel (compte rendu opératoire et consentement dans le séjour, sans programmation) | `OperatingReportService` |
| Facturation, caisse, assurance, avoirs | Présent mais incohérent | BIL-*, CSH-* |
| Rendez-vous (portail patient, disponibilités, agenda médecin) | Partiel et déconnecté | RDV-01…03 |
| Portail patient, consentements, accès externes (Connect) | Présent ; **non exécuté** (OTP patient envoyé par e-mail ; les patients de test n'avaient pas d'e-mail) | `PatientPortalController`, `ExternalAccessService` (D) |
| Notifications | Patient uniquement | `NotificationEntity.patientId`, OP-18 |
| Comptabilité et pilotage DAF | Présent ; **non exécuté** | `AccountingController` (D) |
| Mobile Flutter | Présent ; **hors exécution** dans cet audit | `mobile/` (NV) |
| Assistant IA de consultation | Hors périmètre de cet audit (désactivé en environnement de test) | — |

### 2.2 Parcours et scénarios

| Parcours | Rôles suivis | Exécuté (O) | Statique (D) | Non vérifié |
|---|---|---|---|---|
| Configuration jusqu'à la première admission | Plateforme, admin clinique | CFG-01…35 | `DefaultHospitalOrganizationService` | UI de configuration |
| Patient ambulatoire sans rendez-vous | Accueil, IDE, Dr A, Dr B, biologiste, facturation, caissier, pharmacien | OP, LAB, RX, VIS, BIL, CSH, PH | — | UI |
| Patient avec rendez-vous | Patient, Dr A, accueil | RDV-01…03 | `AppointmentEntity.linkVisit` jamais appelé | Portail patient (OTP par e-mail, patients de test sans e-mail) |
| Doublon d'identité | Accueil, admin | OP-03, ID-01/02 | — | Fusion complète |
| Urgence avec patient inconnu | IDE, Dr A, accueil | URG-01…08 | `EmergencyService` | Rapprochement d'identité |
| Décès | Dr A, accueil | URG-09…11 | — | Documents |
| Hospitalisation, concurrence sur un lit, transfert, sortie | Dr B, gestionnaire des lits, IDE, admin | HOS, BED, DIS | `HospitalizationDischargeWorkflowService` | UI tiroir séjour |
| Traitement hospitalier (prescription → délivrance → administration) | Dr B, pharmacien, IDE | MED-01…05 | `HospitalizationMedicationPolicy` | — |
| Corrections financières | Facturation, admin, caissier | BIL-10…13 | `EstimateController` | Clôture de caisse |
| Mineur ou patient accompagné | — | — | Aucun champ de représentant légal (D) | — |
| Interruption réseau et reprise | — | URG-02 (idempotence) | — | Mode dégradé (NV) |

### 2.3 Limites de couverture

- **Pas de recette navigateur** dans cette intervention : l'ergonomie (FR/EN, thèmes, mobile) n'est jugée que par le code et les audits précédents (TICKET-20261003, QA-20261004).
- Portail patient et Connect : non exécutés. L’OTP patient est envoyé par e-mail (DPU + téléphone + date de naissance) et les patients de test n’avaient pas d’e-mail. Constats déduits du code.
- Comptabilité, bordereaux d'assurance, clôture de caisse, relances, webhooks, FHIR et IA : lus partiellement, non exécutés.
- Application mobile Flutter : non auditée en exécution.
- Les seuils cliniques, la conformité réglementaire camerounaise et les règles de facturation conventionnelle ne sont pas jugés. Ils sont formulés en questions (§10).

---

## 3. Cartographie des parcours actuels

### 3.1 Vocabulaire réellement implémenté

| Notion | Objet technique | Création | Clôture | Observations |
|---|---|---|---|---|
| Rendez-vous | `AppointmentEntity` (`CONFIRMED`, `CANCELLED_*`, `COMPLETED`, `NO_SHOW`) | Patient via le portail | Annulation par le patient uniquement | `COMPLETED` et `NO_SHOW` ne sont jamais posés ; `linkVisit` n'est jamais appelé (FA-23) |
| Registre d'accueil | `ReceptionLog` | Accueil | Départ | Indépendant de la visite |
| Visite | `VisitEntity` (`EN_COURS` / `TERMINEE` / `ANNULEE` + `careStage`) | Accueil, médecin, IDE, ou automatiquement à l'admission ou depuis l'urgence | Médecin ou admin (`VISIT_MANAGE`) | Unique par patient ; service en texte libre (FA-24) |
| Consultation | `ConsultationEntity` (brouillon → `VALIDEE`) | Médecin | Scellée à la clôture de la visite | Signataire = celui qui clôture (FA-01) |
| Urgence | `EmergencyEntity` (active tant que `stabilizedAt` est vide) | IDE ou médecin | « Stabiliser » + orientation texte | L'orientation n'a aucun effet (FA-06, FA-07) |
| Admission / séjour | `HospitalizationEntity` (`EN_COURS` → `SORTI` / `SORTI_CONTRE_AVIS`) | Médecin ou gestionnaire des lits | Décision médicale puis départ physique | Pas de demande ni de réservation (FA-34) |
| Acte | Soins quotidiens, administrations, consommables, CRO | IDE ou médecin | — | Non bornés (FA-03) |
| Facture | `InvoiceEntity` (`PENDING` → `VALIDATED` → `PARTIALLY_PAID`/`PAID`…) | Accueil, facturation | Paiement | Une seule par visite (FA-18) |

### 3.2 Circuit ambulatoire observé

```
Accueil ──crée patient──► Patient (pas d'alerte doublon : FA-26)
Accueil ──ouvre visite (service texte, praticien non contrôlé : FA-24)──► Visite EN_COURS / ATTENTE_CONSTANTES
Accueil ou IDE ──constantes (l'accueil peut saisir : FA-27)──► PRET_MEDECIN  [visible : file /api/visits/active]
Dr A ──prend en charge──► EN_CONSULTATION chez Dr A  [Dr B bloqué : OK]
Dr A ──consultation + demande labo──► LabOrder REQUESTED  [visible : file labo, biologiste]
Biologiste ──items prélevé → en cours → résultat → validé──► résultats  [aucune notification à Dr A : FA-11]
Dr A ──ordonnance brouillon──► Prescription DRAFT
Dr B (!) ──clôture visite──► consultation scellée signée par Dr B (FA-01) ; ordonnance ACTIVE
Accueil ──facture──► PENDING (prix par défaut, quantité 124 : FA-13/14 ; examens absents : FA-16)
Facturation ──valide──► VALIDATED → visible en caisse
Caissier ──paiement partiel──► PARTIALLY_PAID
Pharmacien ──vérifie (n° + PIN, sans authentification)──► valide ──► délivre  [stock inchangé : FA-38]
```

Retours et branches : le retour vers le médecin après les résultats n'a ni statut ni tâche. Le médecin doit rouvrir le dossier ; s'il a déjà clôturé, les résultats arrivent sur une visite close et ne sont pas signalés (FA-12).

### 3.3 Circuit hospitalier observé

```
Visite EN_COURS ──Dr B/gestionnaire lits : admission (unité + espace + lit + responsable affecté à l'unité exacte)──► Séjour EN_COURS, lit OCCUPÉ
   └─ concurrent sur le même lit : 409 ; le perdant reste avec une visite ouverte, sans file d'attente (FA-34)
IDE ──transfert (sans motif, sans acceptation, responsable inchangé : FA-30)──► nouveau lit ; ancien lit en NETTOYAGE
IDE ──administre (prescription ACTIVE)──► trace ; 2e administration immédiate acceptée (FA-03)
Pharmacien ──délivre au service──► ordonnance PARTIALLY_DISPENSED → plus administrable (FA-02)
Dr B ──décision de sortie (non corrigeable : FA-32)──► patient toujours dans le lit
Gestionnaire lits ──départ physique (IDE refusé : FA-33 ; aucun contrôle administratif)──► SORTI, lit en NETTOYAGE
   └─ la visite reste EN_COURS (FA-04) → au retour à J7 : « Ce patient possède déjà une visite active »
Agent hygiène ──nettoyage terminé──► lit PRÊT
```

### 3.4 Circuit urgences observé

```
IDE ──urgence provisoire (inconnu, P1)──► URG-TEMP, triage initial, gestes tracés
Dr A ──« Stabiliser » orientation ADMISSION──► urgence close (stabilizedBy = null : FA-08)
   ├─ le front redirige vers l'onglet hospitalisation du patient ; si aucun lit n'est disponible ou si l'utilisateur quitte, le patient n'apparaît plus nulle part (FA-06)
   └─ tout nouveau geste → 409 EMERGENCY_ALREADY_STABILIZED
Dr A ──« Stabiliser » orientation DEATH──► dossier ACTIVE, visite ouvrable (FA-07)
Urgence clôturée (sortie) puis nouvelle visite programmée → constantes de l'urgence recopiées (FA-28)
```

---

## 4. Diagnostic de configuration

### 4.1 Parcours minimal vérifié jusqu'à la première hospitalisation

| Étape | Acteur | Prérequis | Contrôle système observé |
|---|---|---|---|
| 1. Créer l'établissement | Admin plateforme | — | OK (CFG-01) |
| 2. Créer l'administrateur d'établissement | Admin plateforme | Établissement | Mot de passe temporaire par mail + OTP (CFG-02/03) |
| 3. Créer pôles, services (catalogue national) et unités de soins | Admin clinique | — | Hiérarchie contrôlée ; un service hors catalogue est refusé : « Type de service inconnu » (CFG-14) |
| 4. Créer site, bâtiment, étage | Admin clinique | — | OK |
| 5. Créer les espaces (chambre avec profil d'hébergement, box de consultation…) | Admin clinique | Localisation | 21 types d'espace |
| 6. Créer les lits | Admin clinique | Espace avec profil d'hébergement | Refus explicite sinon (CFG-24) |
| 7. Affecter les espaces aux unités (daté) | Admin clinique | Unité + espace | Sans affectation : « L'unité n'utilise pas cet espace » (HOS-01) |
| 8. Inviter le personnel (rôle + affectation d'unité) | Admin clinique | Unités | Numéro d'Ordre obligatoire pour un médecin |
| 9. Affecter le médecin responsable à l'**unité exacte** d'admission | Admin clinique | — | Un médecin affecté au service parent est refusé (HOS-03) |
| 10. Paramétrer les tarifs et conventions | Facturation / admin | — | **Aucun contrôle** : prix par défaut silencieux (FA-13) |
| 11. Ouvrir une caisse | Caissier | — | « Caisse Principale » créée automatiquement (OK) |
| 12. Stock pharmacie | Pharmacien | — | Facultatif et sans lien avec la délivrance (FA-38) |

### 4.2 Réponses aux questions de configuration

- **Un service peut-il utiliser plusieurs espaces ? Un espace peut-il être partagé ?** Oui et oui. Les affectations datées espace → unité acceptent le partage simultané (CFG-31). Rien n'empêche cependant un partage involontaire : il n'y a ni avertissement ni règle « partage autorisé » sur l'espace.
- **L'utilisateur distingue-t-il service, salle et lit ?** L'API oui. À la création de visite, en revanche, la liste « services » mélange services de consultation, unité d'hospitalisation et laboratoire (OP-01, FA-25). La visite n'enregistre ensuite qu'un libellé texte.
- **Les choix sont-ils compatibles avec le site, le service, la date, la capacité et les droits ?** Oui pour l'admission et le transfert (unité active, espace actif, affectation à date, lit ouvert, prêt et libre). Non pour la visite (texte libre, praticien non contrôlé) ni pour la compatibilité patient-chambre (sexe, âge, isolement : absente, GAP-014 toujours ouvert).
- **Une salle inactive, un lit indisponible ou une affectation expirée sont-ils utilisables ?** Ils sont refusés à l'admission et au transfert. Mais **une unité peut être désactivée avec un patient hospitalisé dedans** (CFG-34), et le séjour reste actif dans une unité inactive.
- **Disponibilité, réservation, occupation, nettoyage et maintenance sont-ils distingués ?** Oui, sauf la **réservation**, qui n'existe pas.
- **Peut-on modifier l'organisation sans incohérence ?** Partiellement. Les affectations sont datées et historisées, mais la désactivation d'unité ne vérifie ni séjours, ni affectations de personnel, ni affectations d'espaces (FA-29). Les visites portent un libellé texte qui ne suit pas les renommages (FA-24).
- **Le système explique-t-il les configurations manquantes ?** Oui pour la structure et les lits. **Non pour les tarifs** (FA-13) ni pour les médecins sans affectation : un médecin non affecté reste proposable comme praticien principal d'une visite (OP-04).

### 4.3 Recommandation : tableau de mise en service

Un écran « Préparation à la mise en service », côté backend `GET /api/organization-readiness`, calcule les points suivants et bloque l'activation des parcours concernés :

| Contrôle | Bloque |
|---|---|
| Au moins un service de consultation actif avec un médecin affecté | Ouverture de visite |
| Chaque unité d'hospitalisation a au moins un espace affecté actif, un lit ouvert et un médecin affecté | Admission dans cette unité |
| Tarif `CS` et tarif de séjour pour chaque niveau de confort utilisé | Génération de facture |
| Une caisse active | Encaissement |
| Un utilisateur avec `LAB_QUEUE_READ` si des demandes de laboratoire internes sont possibles | Prescription d'examen interne |

Avant correction, la facture est générée avec 15 000 / 10 000 / 2 500 XAF inventés. Après correction, la facturation voit « Tarifs manquants : CS, ROOM_STANDARD, Ceftriaxone » et ne peut pas valider la facture tant qu'elle n'a pas saisi les prix ou qu'un administrateur n'a pas complété la grille.

---

## 5. Matrice des responsabilités et permissions

### 5.1 Points d'entrée et transmissions par rôle

| Rôle | Point d'entrée | Reçoit de | Fin de tâche | Transmet à | Découverte par le destinataire |
|---|---|---|---|---|---|
| Agent d'accueil | Patients, pré-enregistrements, registre, file de visites | Patient, pré-enregistrement | Visite ouverte | IDE / médecin | File `/visits/active` (polling), pas de notification |
| Infirmier | Tableau de bord (file), urgences, capacité | Accueil | Constantes saisies → `PRET_MEDECIN` | Médecin | Étape visible dans la file |
| Médecin | File, consultation, urgences | IDE | Clôture de visite | Labo, pharmacie, facturation | Files respectives ; **aucun retour actif** |
| Biologiste | File labo | Médecin | Résultat validé | Médecin, patient | Patient notifié ; **médecin non notifié** (FA-11) |
| Pharmacien | Vérification d'ordonnance (n° + PIN) | Patient (papier ou portail) | Délivrance | — | Pas de file d'ordonnances destinées à la pharmacie interne |
| Gestionnaire des lits | Capacité | Médecin (oral) | Admission, départ physique | Hygiène | Lit en `CLEANING` visible dans la vue capacité |
| Agent d'hygiène | Capacité | Départ, transfert | Lit `READY` | Admission | Vue capacité |
| Facturation | Facturation | Visite close (implicite) | Facture validée | Caisse | File d'encaissement |
| Caissier | Caisse | Facturation | Paiement | — | File d'encaissement |
| DAF | Pilotage | — | Écarts de caisse | — | — |
| Admin clinique | Configuration | — | — | — | — |

### 5.2 Chevauchements et manques

| Constat | Détail | Réf. |
|---|---|---|
| Admin clinique = super-soignant | `ADMIN_CLINIQUE` = toutes les permissions sauf signature et prescription : décision de sortie, `CLINICAL_WRITE`, `LAB_ORDER_WRITE`, administration médicamenteuse, stabilisation d'urgence, accès d'urgence au dossier. | FA-37 (O : RBAC-01) |
| Accueil et clinique | `AGENT_ACCUEIL` a `VISIT_VITALS_WRITE`. | FA-27 (O) |
| Fin de visite impossible pour l'accueil | Seul `VISIT_MANAGE` (médecin, admin) clôt ou annule. Un patient parti sans être vu laisse une visite ouverte qui bloque la suivante. | FA-45 (D) |
| Départ physique | Réservé au `RESPONSABLE_HOSPITALISATION` ; l'IDE est refusé. Une petite clinique sans gestionnaire des lits doit cumuler les rôles. | FA-33 (O) |
| Corrections financières | Avoir et annulation : `BILLING_INVOICE_CANCEL`, détenu par l'admin seul (ni DAF, ni secrétaire comptable). | FA-19 (O) |
| Doublons | Visibles et traitables uniquement avec `PATIENT_MERGE` (admin). L'accueil ne peut même pas signaler un doublon. | FA-26 (O) |
| Rendez-vous | `APPOINTMENT_WRITE` (accueil) n'est utilisé par aucun endpoint. | FA-23 (D/O) |
| Rôles absents | Responsable de service, technicien d'imagerie ou radiologue, personnel de bloc, gestionnaire d'assurance, préparateur en pharmacie. Les codes `IMAGING_TECHNICIAN` et `LAB_TECHNICIAN` existent comme rôles d'**affectation**, sans permission associée. | FA-40 (D) |
| Relève | Une prise en charge de visite se reprend explicitement (bien). Un médecin responsable de séjour ne peut **jamais** être changé (pas d'endpoint). | FA-30 (D) |
| Plateforme et établissement | L'admin plateforme doit sélectionner un établissement pour la structure (CFG-04, bien séparé). | — |

---

## 6. Registre des constats

Format : **Parcours / étape / rôles — Scénario — Actuel → Attendu — Preuve — Conséquence — Cause — Solution — Critères d'acceptation — Dépendances**. Les identifiants de test (OP-xx, HOS-xx…) renvoient à [RUNTIME-EVIDENCE.md](RUNTIME-EVIDENCE.md).

### P0 — Risques critiques

#### FA-01 — Clôture et signature d'une visite par un médecin qui n'a pas le patient en charge
- **Parcours** : ambulatoire / clôture / médecins. **Certitude** : O (VIS-01, VIS-03).
- **Scénario** : Dr A prend en charge et rédige la consultation. Dr B appelle `POST /api/visits/{id}/close`.
- **Actuel** : 200. La consultation est scellée avec `signedBy` = Dr B et `doctorName` = Dr A, et l'ordonnance de Dr A est signée par Dr B.
- **Attendu** : 409 « Patient en consultation chez Dr A ». Seul le médecin en charge, ou celui qui a repris explicitement, peut signer.
- **Conséquence** : un médecin endosse juridiquement un acte qu'il n'a pas fait ; la traçabilité médico-légale est faussée.
- **Cause** : `VisitService.closeVisit` → `VisitClinicalClosureService.signClinicalActs` vérifie seulement `requirePhysician(actor)`, sans appeler `VisitCareFlowUseCase.ensureConsultationOwnership`.
- **Solution** : à la clôture, exiger `actor == visit.consultingPractitionerId`. Sinon, renvoyer une erreur proposant « Reprendre la prise en charge », ce qui trace `VISIT_CONSULTATION_TAKEN_OVER`. Après reprise, le signataire est le repreneur, et l'auteur des sections déjà rédigées est conservé dans l'historique.
- **Critères d'acceptation** : (1) Dr B → 409 sans reprise ; (2) après reprise, signature par Dr B avec audit de reprise ; (3) une visite sans prise en charge ne peut être clôturée que par un médecin qui la prend en charge dans la même transaction.
- **Dépendances** : aucune.

#### FA-02 — Un traitement délivré par la pharmacie n'est plus administrable
- **Parcours** : hospitalisation / administration / médecin, pharmacien, IDE. **Certitude** : O (MED-04, MED-05).
- **Actuel** : après délivrance partielle, l'ordonnance passe à `PARTIALLY_DISPENSED`, et `eligible-medications` renvoie 0 ligne.
- **Attendu** : la délivrance alimente l'administration, elle ne la bloque pas.
- **Conséquence** : l'IDE ne peut plus tracer la Ceftriaxone ; risque d'omission de dose ou de traçabilité hors système.
- **Cause** : `HospitalizationMedicationPolicy.eligibleItems` filtre `status == "ACTIVE"`, alors que `PharmacyService` réécrit le même champ `status` avec l'état de délivrance. Introduit par la BREAKING CHANGE « administration médicamenteuse hospitalière » (CHANGELOG 2026-10-04).
- **Solution** : séparer `prescription.status` (DRAFT, ACTIVE, CANCELLED, EXPIRED) de `dispensationStatus` (NONE, PARTIAL, FULL). L'éligibilité à l'administration = ordonnance ACTIVE, non expirée, ligne non arrêtée. La délivrance n'est pas un prérequis tant que Q-07 n'est pas tranchée.
- **Critères d'acceptation** : les mêmes données que MED-01…05 donnent MED-05 = 1 ligne ; la vérification en pharmacie montre la délivrance cumulée ; migration des ordonnances existantes `*_DISPENSED` vers ACTIVE + `dispensationStatus`.
- **Dépendances** : contrat API pharmacie (MAJOR candidat).

#### FA-03 — Administrations multiples sans contrôle
- **Certitude** : O (MED-03). Deux administrations de 1 g de Ceftriaxone à quelques secondes d'intervalle sont acceptées.
- **Attendu** : (a) idempotence par clé client, contre le double clic ; (b) alerte bloquante « Dernière administration il y a 2 min ; posologie 1 g/24 h », levable avec un motif tracé.
- **Cause** : `HospitalizationCareService`, sans plan d'administration ni contrôle d'intervalle.
- **Solution** : introduire un **plan d'administration**, généré à la signature (fréquence structurée : dose, unité, intervalle, durée). Chaque administration réalise une prise planifiée (statuts PLANIFIÉE, ADMINISTRÉE, NON_ADMINISTRÉE avec motif, REPORTÉE). Une administration hors plan exige un motif.
- **Critères d'acceptation** : un double clic produit une seule ligne ; la 2e administration dans l'intervalle → 409 avec un message ; un forçage avec motif est tracé.
- **Dépendances** : posologie structurée (aujourd'hui texte libre `posology`), question clinique Q-11.

#### FA-06 — Urgence : clôture avant prise en charge par l'unité d'aval
- **Certitude** : O (URG-06, URG-07, URG-08) + D (`EmergencyService.stabilizeEmergency`, `emergency-dashboard.component.ts:182`).
- **Actuel** : « Stabiliser » pose `stabilizedAt` et l'orientation (texte). L'urgence sort de `/active`. Tout geste ultérieur → 409. Le front navigue vers l'onglet hospitalisation, sans création atomique.
- **Attendu** : le patient reste sous la responsabilité des urgences jusqu'à son **acceptation** par l'aval (admission créée, consultation ouverte, sortie effective).
- **Solution** : remplacer « Stabiliser » par deux actions.
  1. **Décider l'orientation** (médecin). Statut `ORIENTATION_DECIDEE`, avec une destination structurée : unité cible, consultation, transfert externe, retour à domicile ou décès. L'urgence reste active, l'IDE continue de tracer les soins, et le tableau des urgences affiche « En attente de lit — MI — depuis 2 h 10 ».
  2. **Sortie des urgences**, déclenchée par l'événement d'aval : admission créée (même transaction), départ confirmé ou transfert remis. Statut `SORTIE_URGENCES` avec auteur.
- **Critères d'acceptation** : URG-07 = urgence visible avec l'orientation en attente ; URG-08 = soin accepté ; l'admission depuis l'urgence clôt l'urgence dans la même transaction ; `stabilizedBy`/`orientedBy` est renseigné.
- **Dépendances** : FA-34 (demande de lit), FA-07.

#### FA-07 — Le décès n'est pas modélisé
- **Certitude** : O (URG-09, URG-10, URG-11).
- **Actuel** : orientation `DEATH` en texte. Le patient reste `ACTIVE` et une visite est ouvrable pour le défunt.
- **Attendu** : événement « Décès constaté » (date et heure, médecin constatant, lieu, circonstances), statut patient `DÉCÉDÉ`, clôture automatique des visites et séjours actifs, annulation des rendez-vous futurs, blocage de toute nouvelle visite, portail patient fermé ; documents remis aux proches selon la réglementation.
- **Questions** : Q-03 (constat, certificat, délai, chambre mortuaire — le type d'espace `MORGUE` existe).
- **Critères d'acceptation** : URG-10 = statut DÉCÉDÉ ; URG-11 = 409 ; le séjour et le lit sont libérés par le circuit de départ (départ du corps).

#### FA-09 — Statut global de la demande de laboratoire modifiable sans règles
- **Certitude** : O (OP-16 : REQUESTED → VALIDATED = 200 ; OP-17 : VALIDATED → REQUESTED = 200).
- **Cause** : `LabOrderService.updateStatus` applique le statut et le propage à tous les items, sans `validateItemTransition` ni `validatePaymentGate`.
- **Conséquence** : une demande peut apparaître « validée » sans aucun résultat ; un résultat validé peut être rouvert silencieusement.
- **Solution** : réserver `PATCH /{id}/status` à l'annulation globale (motif obligatoire) ; toute autre progression se fait par item avec les transitions existantes. Le statut global reste toujours calculé (`recalculateOrderStatus`).
- **Critères d'acceptation** : OP-16 et OP-17 → 409 ; annulation globale avec motif → items non terminés `CANCELLED`, items validés inchangés.

#### FA-11 — Résultats : pas de routage vers le prescripteur, notification patient prématurée
- **Certitude** : O (OP-18 : `/api/notifications` est interdit au médecin ; OP-19 : un brouillon critique notifie le patient) + D (`LabResultService.getPatientResults` ne filtre pas `DRAFT` ; `notifyPatient` est appelé quel que soit le statut).
- **Attendu** :
  1. Tout résultat validé crée une **tâche « Résultat à prendre en compte »** pour le prescripteur, ou à défaut pour le médecin en charge.
  2. Un résultat critique crée une tâche prioritaire et une notification au prescripteur et au médecin de garde de l'unité.
  3. Le médecin **accuse réception** (`acknowledgedBy`, `acknowledgedAt`, commentaire facultatif).
  4. Le patient ne voit que les résultats VALIDÉS, selon la politique Q-01 (immédiat, différé ou après prise en compte du médecin).
- **Critères d'acceptation** : un brouillon n'est ni visible ni notifié au patient ; un résultat validé apparaît dans la corbeille « Résultats à voir » de Dr A ; un résultat critique non acquitté au-delà d'un délai paramétrable est escaladé.
- **Dépendances** : FA-36 (notifications du personnel).

#### FA-13 — Prix par défaut codés en dur
- **Certitude** : O (BIL-04 : consultation 15 000, médicament 2 500 par unité, sans aucun tarif paramétré) + D (`InvoicePrecalculationService`, `ConventionTariffService.getTariff(key, default)`).
- **Solution** : supprimer les valeurs par défaut. Un tarif manquant fait apparaître une ligne « À tarifer » (prix nul, drapeau `tariffMissing`), qui bloque la validation de la facture avec la liste des clés manquantes. Contrôle de mise en service (§4.3).
- **Critères d'acceptation** : sans grille, la facture affiche « 2 lignes à tarifer » et la validation est refusée ; avec la grille, les prix viennent de la grille.

#### FA-14 — Quantité de médicament mal interprétée
- **Certitude** : O (BIL-03 : « 1 boite de 24 » → 124).
- **Cause** : `addPrescriptionItems`, `replaceAll("[^\\d.]", "")`.
- **Solution** : quantité structurée sur la ligne d'ordonnance (`quantityValue` + `quantityUnit` : boîte, comprimé, flacon), saisie à la prescription. La facturation des médicaments repose sur la **délivrance** (Q-07), pas sur l'ordonnance.
- **Critères d'acceptation** : aucune quantité dérivée d'un texte libre ; « 1 boîte de 24 » = 1 boîte.

#### FA-28 — Constantes d'urgence recopiées dans une visite sans lien
- **Certitude** : O (URG-12 : une urgence pour entorse, clôturée avec retour à domicile ; une visite « diabète » ouverte ensuite reçoit TA 190/110, pouls 99, `recordedAt` = maintenant et une alerte critique).
- **Cause** : `VisitService.propagateEmergencyVitalsIfApplicable`. Le filtre `createdAt < 24 h || stabilizedAt == null || visitId == null` rattache n'importe quelle urgence sans visite, quel que soit son âge.
- **Solution** : ne propager que si l'urgence est **active** et que la visite est créée **depuis** cette urgence (lien explicite `emergencyId` dans la requête). Conserver l'heure et l'auteur réels de la mesure d'urgence.
- **Critères d'acceptation** : URG-12 → aucune constante recopiée ; une visite créée depuis une urgence active reprend les mesures avec leur horodatage d'origine.

### P1 — Blocages de parcours

#### FA-04 — La sortie d'hospitalisation ne clôture pas la visite
- **Certitude** : O (DIS-09 : visite `EN_COURS` après départ physique ; DIS-10 : nouvelle visite refusée « visite active en cours »).
- **Conséquence** : au retour du patient, l'accueil est bloqué et ne peut rien faire seul (FA-45).
- **Solution** : le départ physique (ou la sortie administrative, cf. FA-33) clôture la visite liée dans la même transaction. Une visite liée à un séjour actif ne peut pas être clôturée séparément (FA-05).
- **Critères d'acceptation** : DIS-09 = `TERMINEE` ; DIS-10 = 201.

#### FA-05 — Visite clôturable pendant un séjour actif
- **Certitude** : O (HOS-07). **Solution** : 409 « Visite liée au séjour HOS-… en cours ; elle sera clôturée à la sortie ».

#### FA-10 — Examen de contrôle fusionné silencieusement
- **Certitude** : O (OP-14 : une seconde NFS, motif « contrôle après traitement », renvoie la même demande avec toujours 2 items).
- **Cause** : `LabOrderService.create` fusionne par visite et type d'examen, et `mergeMissingExams` ignore un nom déjà présent.
- **Solution** : ne fusionner que tant que la demande est `REQUESTED` et qu'aucun prélèvement n'a eu lieu. Au-delà, créer une nouvelle demande. Un examen identique sur une demande en cours → 409 avec le choix « Répéter l'examen » (flag explicite).
- **Critères d'acceptation** : une NFS de contrôle après validation de la première crée une nouvelle demande ; un double enregistrement de la consultation ne duplique pas (non-régression PJ-01).

#### FA-15 — Médicaments facturés depuis l'ordonnance
- **Certitude** : D. `addPrescriptionItems` facture toutes les lignes de l'ordonnance de la visite, même délivrées en officine externe ou non délivrées. **Question** : Q-07. **Solution recommandée** : facturer les médicaments sur les **délivrances** internes et les **consommables** du séjour ; une ordonnance externe n'est jamais facturée.

#### FA-16 — Examens jamais facturés ; garde de paiement inactive
- **Certitude** : O (BIL-02) + D. `InvoiceItemType` n'a pas de type examen, et aucun code ne pose `AWAITING_PAYMENT`. **Solution** : ajouter le type `LAB_EXAM` / `IMAGING_EXAM`, avec un tarif par examen du catalogue. Paramètre d'établissement « paiement avant prélèvement » (oui/non, sauf urgence et hospitalisation) qui pose `AWAITING_PAYMENT` et libère la demande au paiement.

#### FA-18 — Une seule facture par visite
- **Certitude** : O (BIL-05). Un séjour long, un complément après correction ou une avance sont impossibles.
- **Solution** : passer de « une facture par visite » à « un **dossier de facturation** par épisode » (visite ou séjour). Il porte plusieurs factures (intermédiaire, finale, complémentaire) et des **acomptes**, imputés sur la facture finale.
- **Critères d'acceptation** : une facture intermédiaire à J7 et une facture finale à la sortie couvrent chaque jour une seule fois ; un acompte encaissé apparaît comme déduit.

#### FA-19 — Corrections financières réservées à l'administrateur ; pas de remboursement
- **Certitude** : O (BIL-10/11 : 403 pour la secrétaire comptable ; BIL-12 : facture partiellement payée non annulable) + D (aucun remboursement lié à un avoir ; la caisse n'a que des mouvements OUT libres).
- **Solution** : donner `BILLING_INVOICE_CANCEL` (avoir) à `SECRETAIRE_COMPTABLE`, avec validation DAF au-delà d'un seuil. Ajouter « Rembourser » sur l'avoir : mouvement de caisse lié à l'avoir et au paiement d'origine, dans la session ouverte, avec reçu.

#### FA-23 — Rendez-vous déconnecté de la visite
- **Certitude** : O (RDV-03 : aucun endpoint de réservation pour le personnel ; agenda médecin en lecture seule) + D (`AppointmentEntity.linkVisit` sans appelant ; `COMPLETED` et `NO_SHOW` jamais posés).
- **Solution cible** : voir §7.2.

#### FA-24 — Service et praticien de visite non contrôlés
- **Certitude** : O (OP-06 : orientation `N_IMPORTE_QUOI`, service « Service inexistant », praticien principal = pharmacien → 200). C'est une divergence avec la documentation (PJ-05 et PJ-08 indiqués DONE) : le contrôle n'existe que dans le front.
- **Solution** : `serviceUnitId` (FK `organizational_units`) obligatoire sur la visite ; orientation = enum ; praticien = médecin ou IDE **affecté** au service (ou à ses unités filles) à la date d'arrivée. Libellé du service conservé en instantané (snapshot).
- **Critères d'acceptation** : OP-06 → 400 ; OP-04 (médecin non affecté) → 400 ; les files « Mon service » reposent sur l'identifiant, pas sur le libellé.

#### FA-26 — Doublons non signalés à la création
- **Certitude** : O (OP-03 : homonyme strict créé en 201 ; ID-01 : détecté a posteriori ; ID-02 : accueil 403).
- **Solution** : à la création, `POST /api/patients` renvoie 409 `POTENTIAL_DUPLICATE` avec les candidats (nom normalisé + date de naissance, ou téléphone). L'accueil choisit « C'est ce patient » (utilise le dossier existant) ou « Nouveau patient, motif : … » (création tracée et file de doublons). L'accueil peut **signaler** un doublon, mais la fusion reste réservée à `PATIENT_MERGE`.

#### FA-29 — Désactivation d'unité avec des patients
- **Certitude** : O (CFG-34). **Solution** : refuser tant qu'il existe un séjour actif, une visite active rattachée, une affectation d'espace ou de personnel en cours. Proposer « Fermer la capacité » (lits fermés) comme étape préalable.

#### FA-30 — Transfert sans demande ni relève ; médecin responsable figé
- **Certitude** : O (BED-04 : transfert par l'IDE sans motif ; BED-05 : responsable inchangé dans une unité où il n'est pas affecté) + D (aucun usage de `setResponsiblePractitionerId`).
- **Solution** : demande de transfert (motif obligatoire, unité cible) → **acceptation** par l'unité cible (cadre ou médecin) → exécution du mouvement. À l'acceptation, désignation du nouveau responsable. Endpoint « Changer le médecin responsable » (motif : relève, absence, transfert), historisé.

#### FA-32 — Décision de sortie non corrigeable
- **Certitude** : O (DIS-03 : 409). **Solution** : « Annuler la décision de sortie » (médecin, motif : aggravation, erreur) tant que le départ physique n'est pas confirmé ; historique conservé.

#### FA-34 — Pas de demande d'hospitalisation ni de file d'attente de lit
- **Certitude** : O (HOS-05 : le perdant de la course au lit garde une visite ouverte ; HOS-08 : aucune file). **Solution** : voir §7.3.

#### FA-36 — Pas de notification ni de tâche pour le personnel
- **Certitude** : D (`NotificationEntity.patientId` uniquement) + O (OP-18).
- **Solution** : un module « Tâches et alertes » pour le personnel, adressé à un utilisateur, un rôle dans une unité, ou une file. Il porte un type, une priorité, une échéance, un statut (`À_FAIRE`, `PRISE_EN_CHARGE` par X, `TERMINÉE`) et un lien vers l'objet. Il sert à FA-11, FA-30, FA-34 et FA-06. Badge dans le menu.

#### FA-37 — Administrateur clinique avec pouvoirs cliniques
- **Certitude** : O (RBAC-01 : l'admin décide d'une sortie médicale) + D (`RbacCatalog.systemRoles`, `without(all, …)`).
- **Solution** : `ADMIN_CLINIQUE` limité à l'administration (utilisateurs, RBAC, structure, espaces, paramètres, audit, lecture des indicateurs). Les droits cliniques ne s'obtiennent que par un rôle soignant explicitement cumulé (Q-04). Les accès d'urgence restent possibles via `PATIENT_EMERGENCY_ACCESS`, tracés.

#### FA-40 — Imagerie absente
- **Certitude** : D. Une demande `IMAGERIE` arrive dans la file du laboratoire (biologiste) ; il n'y a ni modalité, ni compte rendu, ni rôle. Une évolution de périmètre (Q-10) est **nécessaire si l'imagerie est proposée commercialement**. À défaut, l'option `IMAGERIE` doit être retirée du formulaire de prescription interne.

#### FA-45 — Fin de visite impossible pour l'accueil
- **Certitude** : D (`VISIT_MANAGE` : médecin et admin ; `cancelVisit` sans motif, sans audit, sans cascade).
- **Scénario** : le patient part avant d'être vu. La visite reste `EN_COURS` et bloque la suivante.
- **Solution** : action « Le patient est parti » / « Annuler la visite » pour l'accueil, autorisée uniquement avant toute donnée clinique (étape `ATTENTE_CONSTANTES` ou `PRET_MEDECIN`). Motif obligatoire (PARTI_SANS_ÊTRE_VU, ERREUR, REFUS) ; annulation en cascade des demandes non commencées ; facture `PENDING` annulée ; audit.

### P2 — Incohérences majeures

| ID | Constat | Certitude / preuve | Solution courte |
|---|---|---|---|
| FA-08 | Actions sans auteur : `stabilizeEmergency` (pas d'utilisateur), audit d'upload labo avec `user = null` et validateur en texte libre. | O (URG-06 `stabilizedBy: null`) + D | Auteur résolu côté serveur, obligatoire ; `validatorUserId` = utilisateur connecté. |
| FA-12 | Une visite peut être clôturée avec des examens non terminés, sans signalement. | O (VIS-04) | Récapitulatif de clôture : « 1 examen en attente — résultats routés vers Dr A » (tâche FA-11) ; confirmation explicite. |
| FA-17 | Consultation facturée pour toute visite (séjour « Chute » sans consultation), prix unique quel que soit le service. | O (DIS-07) | Facturer la consultation si une consultation est signée, au tarif du service ou de la spécialité. |
| FA-27 | L'accueil peut saisir les constantes. | O (OP-07) | Q-05 ; par défaut retirer `VISIT_VITALS_WRITE` du rôle accueil. |
| FA-33 | Départ physique sans contrôle administratif (facture, documents, résultats en attente) ; IDE refusé. | O (DIS-04/05) | Liste de contrôle de sortie non bloquante, sauf règles choisies ; permission paramétrable (Q-02). |
| FA-35 | Pas de liste des séjours actifs par unité (aucun endpoint, aucun menu). | D (`HospitalizationController`) | `GET /api/hospitalizations?unitId=&status=` + écran « Patients de mon unité ». |
| FA-38 | Délivrance sans impact sur le stock (GAP-026 toujours ouvert). | O (PH-04) | Décrément du stock de la pharmacie délivrante, dans la même transaction. |
| FA-39 | Vérification d'ordonnance publique par n° + PIN à 4 chiffres : nom du patient, du médecin et médicaments exposés sans authentification. Verrouillage en mémoire (GAP-027). | O (PH-01) + D | PIN de 6 caractères ou plus, limitation persistante ; réponse publique minimale ; détails uniquement pour un pharmacien authentifié. |
| FA-42 | Pas de représentant légal ni de statut mineur dans le dossier patient (seulement les tiers d'urgence). | D | Q-09 ; `legalRepresentatives[]` + règles d'accès au portail et de consentement. |
| FA-31 | Règles d'appartenance incohérentes : la file « Mon service » hérite service → unités, mais la responsabilité de séjour exige l'unité exacte. | O (HOS-03) + D | Une règle unique : affectation au service = éligibilité à ses unités filles, sauf restriction explicite. |

### P3 — Frictions opérationnelles

| ID | Constat | Preuve | Solution |
|---|---|---|---|
| FA-20 | Séjour facturé sur l'espace courant uniquement, minimum 1 « nuit », fuseau du serveur. | D | Facturer par segment d'affectation de lit ; fuseau de l'établissement. |
| FA-21 | Modes de paiement : espèces, chèque, virement ; pas de mobile money. | D (`PaymentMethod`) | Q-06. |
| FA-22 | Assurance = convention « nom + % » : pas d'adhésion patient, de plafond, d'accord préalable ni d'exclusion. | D | Évolution de périmètre. |
| FA-25 | La liste « services » de la visite propose Laboratoire et l'unité d'hospitalisation. | O (OP-01) | Filtrer sur les services consultants actifs (flag catalogue). |
| FA-41 | Bloc opératoire limité au compte rendu opératoire, au consentement et aux implants dans le séjour : pas de programme opératoire, de salle, de SSPI ni de check-list. | D (`OperatingReportService`) | Évolution de périmètre (Q-10). |
| FA-43 | Un paramètre manquant renvoie 500 au lieu de 400. | O (log `MissingServletRequestParameterException`) | Gestion dans `GlobalExceptionHandler`. |
| FA-44 | Kinésithérapie absente du catalogue national (déjà signalé le 2026-10-04). | O (CFG-14) | Migration catalogue. |

---

## 7. Parcours cibles recommandés

### 7.1 Principes

1. **Un épisode, des états séparés.** L'état clinique (visite, consultation, séjour) est distinct de l'état administratif (arrivée, admission, sortie), de l'état financier (dossier de facturation) et de l'occupation (lit). Chaque transition déclare explicitement ses effets sur les autres domaines.
2. **Toute transmission est une tâche** avec un destinataire (utilisateur, ou rôle dans une unité), un statut et une preuve de prise en charge (FA-36).
3. **Le backend décide.** Chaque règle du front a son équivalent serveur (FA-24).

### 7.2 Rendez-vous → arrivée → visite

| Avant | Après |
|---|---|
| Le patient réserve sur le portail. Le jour J, l'accueil crée une visite comme pour un patient sans rendez-vous. Le rendez-vous reste `CONFIRMED` indéfiniment ; l'agenda du Dr A montre tout en « confirmé ». | L'accueil ouvre « Rendez-vous du jour » et clique « Patient arrivé » : la visite est créée et pré-remplie (motif, médecin, service), le rendez-vous passe à `ARRIVÉ` (lien `visitId`). À la clôture de la visite, le rendez-vous passe à `COMPLETED`. 30 min après l'heure, un rendez-vous sans arrivée passe à « En retard » ; l'accueil peut marquer `NO_SHOW`, ou le système le fait à la fin de la journée. L'accueil peut réserver, déplacer et annuler (`CANCELLED_BY_CLINIC` + motif) au nom du patient. |

Règles : une seule visite par rendez-vous ; l'arrivée est refusée si le patient a une visite active (proposer de la réutiliser) ; un `NO_SHOW` est réversible le jour même avec un motif.

### 7.3 Demande d'hospitalisation et file d'attente de lit

| Avant | Après |
|---|---|
| Dr B choisit directement un lit. S'il est pris, 409, et le patient reste « nulle part » avec une visite ouverte. Le gestionnaire des lits est prévenu oralement. | Dr B crée une **demande d'hospitalisation** (unité cible, motif, urgence, contraintes : isolement, sexe). Statut `EN_ATTENTE_LIT`. Une tâche apparaît dans la file « Demandes de lit » du gestionnaire et du cadre de l'unité. Le gestionnaire **réserve** un lit (`RÉSERVÉ` pour une durée paramétrable ; il n'est plus proposé à d'autres). L'IDE de l'unité clique « Patient arrivé dans l'unité » : séjour `EN_COURS`, lit `OCCUPÉ`, visite liée. Sans lit, la demande reste visible avec son ancienneté et le patient reste dans sa file d'origine (urgences ou consultation). |

Statuts : `DEMANDÉE` → `LIT_RÉSERVÉ` → `ADMIS` | `ANNULÉE` (motif) | `RÉORIENTÉE`. Une réservation expirée revient à `DEMANDÉE`, avec alerte.

### 7.4 Résultats d'examens

| Avant | Après |
|---|---|
| Le biologiste valide. Le patient reçoit « valeur critique — attention médicale immédiate ». Dr A, qui a déjà clôturé la visite, n'est pas averti. | À la validation, une tâche « Résultat à voir » est créée pour Dr A (« critique » s'il y a lieu ; notification immédiate du médecin de garde de l'unité). Dr A ouvre la corbeille, voit le résultat, clique « Pris en compte » (et peut planifier un rappel). Le patient voit le résultat selon Q-01 (par exemple après prise en compte, ou 48 h après validation). |

### 7.5 Fin d'épisode

| Événement | Effet clinique | Effet administratif | Effet financier | Effet occupation |
|---|---|---|---|---|
| Patient parti sans être vu (accueil) | Visite `ANNULÉE` (motif) | Rendez-vous `NO_SHOW` ou inchangé | Facture `PENDING` annulée | — |
| Clôture de consultation (médecin en charge) | Consultation signée par son auteur | Visite `TERMINÉE` sauf séjour actif | Lignes « consultation » prêtes | — |
| Décision médicale de sortie | Diagnostic de sortie et consignes ; annulable | Tâche « Préparer la sortie » (documents, facture) | Facture finale calculée | Lit toujours occupé |
| Départ physique | — | Séjour `SORTI` ; **visite `TERMINÉE`** | Solde visible, non bloquant sauf paramètre | Lit `NETTOYAGE` |
| Décès constaté | Constat signé | Patient `DÉCÉDÉ` ; rendez-vous annulés ; portail fermé | Dossier gelé | Départ du corps → lit `NETTOYAGE` |

---

## 8. Backlog de correction

Le backlog détaillé (objectif, règles, critères d'acceptation, tests, dépendances, estimation) se trouve dans [EPIC-0029](../../pm/backlog/EPIC-0029-functional-journey-coherence.md). Ordre recommandé :

| Lot | Stories | Constats | SP |
|---|---|---|---|
| Lot 1 — sécurité clinique immédiate | S01 signature par le médecin en charge ; S02 statut de délivrance séparé ; S03 contrôle d'administration ; S04 transitions labo ; S05 propagation des constantes d'urgence | FA-01, 02, 03, 09, 28 | 21 |
| Lot 2 — continuité des épisodes | S06 fin de visite (sortie, départ, annulation par l'accueil) ; S07 orientation d'urgence et file d'attente ; S08 décès ; S09 annulation de la décision de sortie ; S10 désactivation d'unité | FA-04, 05, 06, 07, 32, 29, 45 | 26 |
| Lot 3 — facturation fiable | S11 tarifs obligatoires et quantités structurées ; S12 facturation des examens ; S13 corrections financières et remboursement | FA-13, 14, 15, 16, 19 | 18 |
| Lot 4 — transmissions | S14 tâches et alertes du personnel ; S15 corbeille de résultats ; S16 visite liée à une unité | FA-36, 11, 24, 12 | 21 |
| Lot 5 — évolutions de parcours | S17 rendez-vous → arrivée ; S18 demande de lit et réservation ; S19 transfert accepté et relève ; S20 doublons à la création ; S21 RBAC admin | FA-23, 34, 30, 26, 37 | 31 (S18 à découper) |

---

## 9. Plan de recette de bout en bout

Voir [TEST-PLAN.md](TEST-PLAN.md). Il reprend les 8 scénarios exécutés, avec leurs résultats attendus **après** correction, rôle par rôle, et ajoute les scénarios non couverts (mineur, rendez-vous complet, portail patient, imagerie si retenue).

---

## 10. Questions métier bloquantes

| ID | Question exacte | Proposition | Conséquences | À valider par |
|---|---|---|---|---|
| Q-01 | Quand un résultat validé devient-il visible pour le patient : immédiatement, après prise en compte par le prescripteur, ou après un délai ? Avec quelle règle pour les valeurs critiques ? | Visible après prise en compte, ou 72 h après validation au plus tard ; jamais avant pour un résultat critique. | Les patients voient plus tard leurs résultats normaux ; le prescripteur doit acquitter. | Médecin référent, DPO |
| Q-02 | Qui confirme le départ physique dans une structure sans gestionnaire des lits ? | Permission accordable à l'IDE de l'unité via le rôle ou un rôle personnalisé ; par défaut gestionnaire des lits + cadre. | Plus de souplesse, et risque de libération prématurée moindre grâce à la liste de contrôle. | Cadre infirmier |
| Q-03 | Quel circuit après un décès : constat par le seul médecin, certificat, délais, déclaration à l'état civil, chambre mortuaire ? | Événement « Décès constaté » par un médecin, document de constat, départ du corps tracé. | Les documents réglementaires doivent être spécifiés localement. | Direction médicale, juriste |
| Q-04 | L'administrateur d'établissement doit-il disposer de droits cliniques par défaut ? | Non. Droits cliniques uniquement par un rôle soignant cumulé. | Les démos et petites structures doivent cumuler explicitement les rôles. | PO, DPO |
| Q-05 | L'agent d'accueil peut-il saisir des constantes, par exemple poids et taille seulement ? | Retirer par défaut ; option « Constantes administratives » (poids et taille) si nécessaire. | L'IDE devient le seul acteur des constantes vitales. | Cadre infirmier |
| Q-06 | Quels modes de paiement doivent être supportés (Orange Money, MTN MoMo, carte) ? Avec rapprochement automatique ? | Ajouter `MOBILE_MONEY` avec opérateur et référence de transaction obligatoires ; rapprochement manuel dans un premier temps. | Ventilation de la caisse et de la clôture à adapter. | DAF |
| Q-07 | Les médicaments sont-ils facturés à la prescription, à la délivrance interne ou à l'administration ? | À la délivrance interne (ambulatoire) et à l'administration / consommable (séjour) ; jamais pour une ordonnance externe. | Une délivrance obligatoire pour facturer ; la pharmacie devient un acteur de la facturation. | DAF, pharmacien |
| Q-08 | Le rendez-vous est-il obligatoire pour certains services ? L'absence doit-elle être facturée ou pénalisée ? | Arrivée → visite ; `NO_SHOW` non facturé, compteur visible. | Nouvelle file « Rendez-vous du jour » à l'accueil. | PO, accueil |
| Q-09 | Comment représenter le mineur et son représentant légal (consentements, accès au portail, majorité à 18 ou 21 ans) ? | Représentant légal structuré, portail accessible au représentant jusqu'à la majorité paramétrée. | Impacte le portail, les consentements et Connect. | Juriste, DPO |
| Q-10 | L'imagerie et la programmation du bloc sont-elles dans le périmètre commercial actuel ? | Si non : masquer `IMAGERIE` et le bloc ; si oui : epic dédié. | Évite une promesse fonctionnelle non tenue. | PO |
| Q-11 | Quelle structure de posologie et quel intervalle minimal entre deux administrations (par molécule ou par posologie) ? | Posologie structurée (dose, unité, fréquence) ; intervalle déduit de la fréquence ; forçage avec motif. | Saisie de prescription plus contrainte. | Médecin référent, pharmacien |

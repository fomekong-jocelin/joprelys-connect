# Preuves d'exécution — audit fonctionnel du 2026-10-17

## Environnement

- Backend Joprelys (classes compilées de `main` @ `6830f41e` + configuration locale), lancé séparément sur le port **8095**.
- Base PostgreSQL 17 dédiée **`joprelys_audit_e2e_20261017`**, migrations Flyway appliquées sur une base vide. L'instance de développement (8080, base `joprelys`) n'a pas été touchée.
- Clés IA vides (aucun appel externe) ; e-mails capturés par MailDev local (1025/1080) ; toutes les identités sont fictives (`@example.test`).
- Scénarios rejouables : [scenarios/](scenarios/) (`python -I s1_setup.py` … `s8_misc.py`, dans l'ordre ; `harness.py` lit les OTP dans MailDev).

## Résultats

123 vérifications, 33 en écart, dont 4 artefacts du harnais (signalés). Une vérification « OK » signifie que la vérification s'est exécutée et que son libellé décrit le comportement constaté ; pour les libellés descriptifs (« … : comportement », « … accepté ? »), le constat est dans la colonne détail.

| ID | Vérification | Résultat | Constat | Détail (extrait de la réponse) |
|---|---|---|---|---|
| CFG-01 | Plateforme crée un établissement | OK |  | (201, 'e2e8d025-8234-4df5-b30b-dd7ea506e00d') |
| CFG-02 | Plateforme crée l administrateur établissement | OK |  | (201, {'id': '6b604724-f443-478e-959a-17d84588e50f', 'email': 'admin242025@example.test', 'displayName': 'Admin Audit', 'role': 'ADMIN_CLINIQUE', 'enabled': True, 'organizationId': 'e2e8d025-8234-4df5-b30b-dd7ea506e00d', |
| CFG-03 | Admin établissement se connecte (mot de passe temporaire + OTP) | OK |  |  |
| CFG-04 | Plateforme et structure d un établissement : comportement | OK |  | (400, "{'detail': 'Sélectionnez un établissement à administrer.', 'instance': '/api/hospital-organization/units', 'status': 400, 'title': 'Sélectionnez un ét") |
| CFG-05 | Catalogue national des services disponible | OK |  | (200, 23) |
| CFG-10 | Créer un pôle | OK |  | 201 |
| CFG-11 | Créer service Médecine générale (catalogue) | OK |  | 201 |
| CFG-12 | Créer service Urgences sans pôle (racine) | OK |  | (201, '') |
| CFG-13 | Créer unité de soins rattachée au service | OK |  | 201 |
| CFG-14 | Service hors catalogue (Kinésithérapie) refusé | OK | FA-44 | (400, {'detail': 'Type de service inconnu ou inactif.', 'instance': '/api/hospital-organization/units', 'status': 400, 'title': 'Type de service inconnu ou inactif.'}) |
| CFG-20 | Créer site/bâtiment/étage | OK |  | 201 |
| CFG-21 | Créer chambre (HOSPITAL_ROOM) avec profil hébergement | OK |  | (201, '') |
| CFG-22 | Créer box de consultation (CONSULTATION_ROOM) | OK |  | (201, '') |
| CFG-23 | Créer 3 lits | OK |  | {'101-A': ('FREE', 'OPEN', 'READY'), '101-B': ('FREE', 'OPEN', 'READY'), '102-A': ('FREE', 'OPEN', 'READY')} |
| CFG-24 | Lit dans un espace sans profil hébergement : refusé | OK |  | (400, {'detail': "Seul un espace doté d'un profil d'hébergement peut recevoir des lits.", 'instance': '/api/spatial/configuration/beds', 'status': 400, 'title': "Seul un espace doté d'un profil d'hébergement peut recevoi |
| ROLE-01 | Invitation de 10 collaborateurs (rôles système) | OK |  | ['accueil', 'ide', 'bio', 'pharma', 'caisse', 'lits', 'compta', 'drA', 'drB', 'drC'] |
| ROLE-02 | Chaque collaborateur se connecte | ARTEFACT |  | Artefact du harnais : mail d'invitation de Dr C reçu après la tentative de lecture ; connexion vérifiée ensuite (OK). |
| OP-01 | Accueil : options de visite (services, praticiens) | OK | FA-25 | {'services': ['Hospitalisation MI - Aile A', 'Laboratoire', 'Médecine générale', 'Urgences'], 'practitioners': [{'id': '4e7cbab2-9ebf-4ed4-b281-a315da6f99e6', 'displayName': 'Dr A Généraliste', 'role': 'MEDECIN', 'unitNa |
| OP-02 | Accueil crée un patient | OK |  | 201 |
| OP-03 | Création d un doublon évident (même nom/date/téléphone) : bloquée ou signalée | ÉCART | FA-26 | (201, "{'id': '0da880c0-f2ec-4e19-a9b7-d9cd4ec57865', 'organizationId': 'e2e8d025-8234-4df5-b30b-dd7ea506e00d', 'globalPatientNumber': 'DPU-JOP-20261017-000002', 'localPatientNumber': 'PAT-20261017-000002', 'fullName': ' |
| OP-04 | Visite avec praticien non affecté au service (Dr C) : acceptée ? | OK | FA-24 | (200, '6f58c945-22e5-43c9-ba1c-fa6d00e66bc6') |
| OP-05 | Deuxième visite active pour le même patient refusée | OK |  | (400, "{'detail': 'Ce patient possède déjà une visite active en cours.', 'instance': '/api/visits', 'status': 400, 'title': 'Ce patient possède déjà une visi") |
| OP-06 | Visite avec orientation/service libres et praticien = pharmacien : refusée | ÉCART | FA-24 | (200, "{'id': 'bd69bf56-939b-4cda-b118-0c1b52010032', 'visitNumber': 'VIS-20261017-000002', 'patientId': '0da880c0-f2ec-4e19-a9b7-d9cd4ec57865', 'patientName': 'MBARGA Fictif Paul', 'patientDpu': 'DPU-JOP-20") |
| OP-07 | Agent d accueil peut saisir les constantes | OK | FA-27 | (200, "{'temperature': 39.4, 'weight': None, 'height': None, 'pulse': 118, 'systolic': 120, 'diastolic': 80, 'spo2': 95, 'glycemia': None, 'respiratoryRate':") |
| OP-08 | Infirmier saisit les constantes | OK |  | 200 |
| OP-09 | File médecin : étape et alertes visibles | OK |  | {'careStage': 'PRET_MEDECIN', 'status': 'EN_COURS', 'alerts': None, 'vitalAlerts': None, 'mainPractitionerId': '6f58c945-22e5-43c9-ba1c-fa6d00e66bc6', 'service': 'Médecine générale'} |
| OP-10 | Dr A prend en charge | OK |  | (200, "{'id': 'a00e1736-83a0-4f98-ba1b-dcf87e7bac44', 'visitNumber': 'VIS-20261017-000001', 'patientId': '038046e7-db71-4a05-91c7-48eb5227d555', 'patientName") |
| OP-11 | Dr B bloqué (prise en charge unique) | OK |  | (409, "{'detail': 'Patient déjà en consultation chez Dr A Généraliste.', 'instance': '/api/visits/a00e1736-83a0-4f98-ba1b-dcf87e7bac44/take-charge', 'status'") |
| OP-12 | Dr A enregistre la consultation | OK |  | 200 |
| OP-13 | Dr A prescrit GE + NFS | OK |  | (201, 'REQUESTED') |
| OP-14 | Seconde NFS (contrôle) dans la même visite : nouvelle demande créée | ÉCART | FA-10 | (201, True, 2) |
| OP-15 | Biologiste voit la demande dans la file | OK |  | (200, 1) |
| OP-16 | Statut global REQUESTED -> VALIDATED direct (saut d étapes) refusé | ÉCART | FA-09 | (200, 'VALIDATED') |
| OP-17 | Retour arrière VALIDATED -> REQUESTED refusé | ÉCART | FA-09 | (200, 'REQUESTED') |
| OP-18 | Le prescripteur dispose d une notification / corbeille de résultats | ÉCART | FA-11/FA-36 | (403, {'error': {'code': 'ACCESS_DENIED', 'message': "Vous n'êtes pas autorisé à effectuer cette action.", 'trace_id': 'trc_80b7099bd517491bb314'}, 'detail': "Vous n'êtes pas autorisé à effectuer cette action."}) |
| OP-19 | Biologiste téléverse un résultat BROUILLON critique | OK | FA-11 | (201, None) |
| LAB-01 | Medecin voit le resultat avec son statut (brouillon distingue) | OK |  | [('Goutte épaisse', 'DRAFT', 'CRITICAL')] |
| LAB-02 | Statut de la demande apres resultat brouillon | OK |  | ('RESULT_AVAILABLE', [('NFS', 'REQUESTED'), ('Goutte épaisse', 'RESULT_AVAILABLE')]) |
| LAB-03 | Cycle d un examen (prelevement -> valide) par items | OK |  | [('SAMPLE_COLLECTED', 200), ('IN_PROGRESS', 200), ('RESULT_AVAILABLE', 200), ('VALIDATED', 200)] |
| LAB-04 | Resultat valide televerse | OK |  | 201 |
| RX-01 | Dr A redige l ordonnance (brouillon) | OK |  | (200, 'DRAFT') |
| VIS-01 | Dr B (non pris en charge) ne peut pas cloturer/signer la visite de Dr A | ÉCART | FA-01 | (200, "{'id': 'a00e1736-83a0-4f98-ba1b-dcf87e7bac44', 'visitNumber': 'VIS-20261017-000001', 'patientId': '038046e7-db71-4a05-91c7-48eb5227d555', 'patientName': 'Mbarga Fictif Paul', 'patientDpu': 'DPU-JOP-20") |
| VIS-03 | Signataire de la consultation | OK | FA-01 | {'doctorId': '4e7cbab2-9ebf-4ed4-b281-a315da6f99e6', 'doctorName': 'Dr A Généraliste', 'status': 'VALIDEE', 'signedBy': 'bb12c379-0da5-4296-bb26-4257ed77c706', 'signedAt': '2026-10-17T13:07:36.356030Z', 'signedContentHas |
| RX-02 | Ordonnance active apres cloture | OK |  | {'status': 'ACTIVE', 'prescriptionNumber': 'ORD-20261017-000001', 'pinCode': '6206'} |
| VIS-04 | Cloture de visite alors qu un examen reste en brouillon : bloquee ou signalee | ÉCART | FA-12 | ('RESULT_AVAILABLE', [('Goutte épaisse', 'RESULT_AVAILABLE'), ('NFS', 'VALIDATED')]) |
| BIL-01 | Pre-calcul facture : lignes proposees | ARTEFACT |  | Artefact du harnais : paramètres envoyés dans le corps au lieu de la query. Rejoué en query : 200, mêmes lignes que BIL-04. Révèle FA-43 (500 au lieu de 400). |
| BIL-02 | Les examens de laboratoire apparaissent dans la facture | ÉCART | FA-16 | 0 |
| BIL-03 | Quantite medicament "1 boite de 24" interpretee correctement | ÉCART | FA-14 | None |
| BIL-04 | Accueil cree la facture de la visite | OK | FA-13/FA-14 | (201, ('PENDING', 325000.0)) |
| BIL-05 | Deuxieme facture meme visite refusee (complement impossible) | OK | FA-18 | (409, "{'detail': 'Une facture existe déjà pour cette visite.', 'instance': '/api/invoices', 'status': 409, 'title': 'Une facture existe déjà pour cette visi") |
| CSH-01 | Caisse principale disponible sans parametrage | ARTEFACT |  | Non-défaut : la liste était vide avant la première ouverture ; « Caisse Principale » est créée automatiquement à l'ouverture (CSH-02). |
| CSH-02 | Caissier ouvre la session | OK |  | (201, "{'id': '9afef060-d22e-4363-9a7c-52d032e34368', 'cashRegisterId': 'af544aaf-dc13-432c-bb6b-1c238c2687c9', 'cashRegisterName': 'Caisse Principale', 'ope") |
| CSH-03 | Facture non validee visible dans la file d encaissement | OK |  | (200, False) |
| CSH-04 | Paiement sur facture non validee | OK |  | (400, "{'detail': 'La facture doit être validée avant tout encaissement. Statut actuel : PENDING.', 'instance': '/api/invoices/2a784f3f-aaf2-4d6d-91d2-330af76304f8/payments', 'status': 400, 'title': 'La fact") |
| BIL-06 | Facturation valide la facture | OK |  | (200, 'VALIDATED') |
| CSH-03b | Facture validee visible dans la file d encaissement | OK |  | 200 |
| CSH-05 | Paiement partiel apres validation | OK |  | (200, "{'id': 'e72496af-ddd3-4ab6-aac3-71e80f2a1980', 'invoiceId': '2a784f3f-aaf2-4d6d-91d2-330af76304f8', 'amount': 5000, 'paymentMethod': 'CASH', 'referenceNumber': None, 'receivedByUserId': '25da3dff-0fa6") |
| CSH-06 | Statut facture apres paiement partiel | OK |  | {'totalAmount': 325000.0, 'status': 'PARTIALLY_PAID'} |
| PH-01 | Verification ordonnance par numero + PIN sans authentification | OK | FA-39 | (200, "{'prescriptionId': 'dcbc6be5-201f-4939-8929-7ab069057eb8', 'prescriptionNumber': 'ORD-20261017-000001', 'status': 'ACTIVE', 'patientName': 'Mbarga Fictif Paul', 'doctorName': 'Dr A Généraliste', 'issu") |
| PH-02 | Delivrance sans validation pharmaceutique refusee | OK |  | (409, "{'detail': 'Une validation pharmaceutique est requise avant la délivrance.', 'instance': '/api/public/pharmacy/prescriptions/dispense', 'status': 409,") |
| PH-03 | Pharmacien delivre apres validation | OK |  | (204, 'None') |
| PH-04 | Stock pharmacie impacte par la delivrance | ÉCART | FA-38 | (200, '[]') |
| HOS-01 | Admission avant affectation espace->unite : message de configuration explicite | OK |  | (409, "L'unité sélectionnée n'utilise pas cet espace à la date de l'admission.") |
| HOS-02 | Options de placement exposees avant affectation | OK |  | {'units': 6, 'serviceCatalog': 23, 'spaces': 2, 'assignments': 0, 'staff': 4} |
| CFG-30 | Affectations datees espace->unite (CH-101 -> UH-MI-A, CH-102 -> MI) | OK |  | (201, 201) |
| CFG-31 | Espace partage simultanement par deux unites (CH-101 aussi -> MG) | OK | §4.2 | (201, 'accepte') |
| HOS-03 | Medecin affecte au service parent (MI) refuse comme responsable de l unite de soins fille | OK | FA-31 | (409, "Le praticien responsable n'est pas affecté à l'unité sélectionnée.") |
| CFG-32 | Admin affecte Dr B a l unite de soins | OK |  | (201, "{'id': 'd1507673-aaba-4daf-8ca3-258d0074267c', 'organizationalUnitId': '2e3384df-0149-420c-b340-d55231698052', 'assignme") |
| HOS-04 | Infirmier ne peut pas admettre (pas HOSPITALIZATION_ADMIT) | OK |  | 403 |
| HOS-05 | Deux admissions simultanees sur le meme lit : une seule reussit | OK | FA-34 | {'jean': 201, 'claire': 409} |
| HOS-06 | Sejour visible : unite, espace, lit, responsable | OK |  | {'currentSpaceId': '11fe4001-685f-4f97-b0d8-9266f1906461', 'serviceName': 'Hospitalisation MI - Aile A', 'spaceName': 'Chambre 101', 'bedNumber': '101-A', 'status': 'EN_COURS', 'responsiblePractitionerId': 'bb12c379-0da5 |
| BED-01 | Lit occupe mis en maintenance : refuse | OK |  | (409, "Le statut d'un lit affecté ne peut pas être modifié manuellement. Utilisez le transfert ou la sortie physique.") |
| BED-02 | Fermeture capacitaire d un lit libre (motif obligatoire) | OK |  | 200 |
| BED-03 | Transfert vers lit ferme refuse | OK |  | (409, "Le lit demandé n'est pas ouvert, prêt et disponible.") |
| BED-04 | Infirmier transfere vers MI/CH-102 sans motif ni acceptation de l unite receveuse | OK | FA-30 | (200, "{'id': '830d17e5-f668-447b-bb83-5d731588282e', 'hospitalizationId': '442da157-d120-4d3e-bb0f-f97b90965acb', 'bedId': '2b26bd8f-cdf3-4564-bc66-38e20d50e4bc', 'as") |
| BED-05 | Responsable inchange apres transfert vers une unite ou il n est pas affecte | OK | FA-30 | {'serviceName': 'Médecine interne', 'spaceName': 'Chambre 102', 'bedNumber': '102-A', 'responsiblePractitionerId': 'bb12c379-0da5-4296-bb26-4257ed77c706'} |
| BED-06 | Ancien lit passe en nettoyage apres transfert (historique) | OK |  | [('READINESS', 'CLEANING', 'CLEANING_AFTER_TRANSFER')] |
| CFG-33 | Desactivation d un service hebergeant un patient : bloquee | OK |  | (409, "Désactivez d'abord les unités enfants actives.") |
| HOS-07 | Cloture de la visite d un patient hospitalise : bloquee ou geree | ÉCART | FA-05 | (200, "{'id': '96a7080a-72c0-465b-88f3-a27fba2d9cd1', 'visitNumber': 'VIS-20261017-000004', 'patientId': '24627c14-06e3-4182-8d") |
| DIS-01 | Depart physique avant decision medicale refuse | OK |  | 409 |
| DIS-02 | Decision medicale de sortie | OK |  | 200 |
| DIS-03 | Correction/annulation de la decision de sortie possible | ÉCART | FA-32 | (409, 'Une décision médicale de sortie existe déjà pour cette hospitalisation.') |
| DIS-04 | Infirmier confirme le depart physique | ÉCART | FA-33 | 403 |
| DIS-05 | Gestionnaire des lits confirme le depart physique (facture non reglee) | OK | FA-33 | 200 |
| DIS-06 | Visite cloturee automatiquement a la sortie | OK |  | TERMINEE |
| DIS-07 | Facture de sejour pre-calculee (sejour < 1 jour, transfert) | OK | FA-17 | [('Consultation - Chute', 15000.0, 1.0), ('Frais de séjour (STANDARD, espace Chambre 102, 1 nuits)', 10000.0, 1.0)] |
| DIS-08 | Lit libere passe en nettoyage | OK |  | [('READINESS', 'CLEANING', 'CLEANING_AFTER_DEPARTURE'), ('CAPACITY', 'OPEN', 'CAPACITY_REOPENING'), ('CAPACITY', 'CLOSED', 'CAPACITY_STAFFING_SHORTAGE')] |
| HOS-08 | Patient sans lit : aucune file d attente de lit, visite EN_COURS orpheline | OK | FA-34 | ATTENTE_CONSTANTES |
| BED-07 | Infirmier ne peut pas declarer un lit nettoye (agent hygiene requis) | OK |  | 403 |
| HOS-09 | Admission de Claire sur 101-B | OK |  | (201, "{'id': '2d70d6e3-6ce7-4147-81d3-c5b0a56f44e3', 'patientId': '85793cfa-a4ca-4a82-b585-87ad08a1f1e3', 'organizationId': 'e") |
| CFG-34 | Desactivation d une unite de soins avec patient hospitalise : bloquee | ÉCART | FA-29 | (200, None) |
| CFG-35 | Sejour actif dans une unite desactivee | OK | FA-29 | ('EN_COURS', 'Hospitalisation MI - Aile A') |
| DIS-09 | Visite cloturee apres depart physique | ÉCART | FA-04 | EN_COURS |
| DIS-10 | Retour du patient a J7 : nouvelle visite possible | ÉCART | FA-04 | (400, 'Ce patient possède déjà une visite active en cours.') |
| URG-01 | Infirmier ouvre une urgence pour patient inconnu (identite provisoire) | OK |  | (201, "{'id': '6402276f-ca8a-4f28-b5a7-5f10ef239c7c', 'organizationId': 'e2e8d025-8234-4df5-b30b-dd7ea506e00d', 'patientId': '7ff528dc-3978-48c1-b6bf-ea52b1f04ce5', 'patientName': 'URG-TEMP-20261017-811616',") |
| URG-02 | Double clic (meme requestId) idempotent | OK |  | (201, True) |
| URG-03 | Facturation possible sur identite provisoire (regularisation en attente) | OK |  | (201, 'REGULARIZATION_PENDING') |
| URG-04 | Infirmier trace un geste de reanimation | OK |  | (200, "{'id': '0ce1ba16-43ed-4863-af18-8a394fa3c7f7', 'emergencyId': '6402276f-ca8a-4f28-b5a7-5f10ef239c7c', 'actionType': 'MED") |
| URG-05 | Infirmier ne peut pas cloturer l urgence (EMERGENCY_STABILIZE medecin) | OK |  | 403 |
| URG-06 | Medecin "stabilise" avec orientation hospitalisation | OK | FA-06/FA-08 | (200, {'orientation': 'ADMISSION', 'stabilizedAt': '2026-10-17T13:11:48.912602600Z', 'stabilizedBy': None, 'visitId': None}) |
| URG-07 | Patient oriente hospitalisation mais sans lit : reste visible dans une file | ÉCART | FA-06 | 0 |
| URG-08 | Soins traces apres "stabilisation" en attente de lit | ÉCART | FA-06 | (409, 'EMERGENCY_ALREADY_STABILIZED') |
| URG-09 | Deces enregistre via "stabiliser" orientation DEATH | OK |  | 200 |
| URG-10 | Dossier patient marque decede (statut, date) | ÉCART | FA-07 | {'status': 'ACTIVE', 'identityStatus': 'PROVISIONAL_URGENCY', 'deceasedAt': None} |
| URG-11 | Ouverture d une visite pour un patient decede refusee | ÉCART | FA-07 | (200, 'VIS-20261017-000005') |
| URG-12 | Nouvelle visite : constantes de l urgence precedente recopiees sans horodatage | OK | FA-28 | ({'temperature': 37.0, 'weight': None, 'height': None, 'pulse': 99, 'systolic': 190, 'diastolic': 110, 'spo2': None, 'glycemia': None, 'respiratoryRate': None, 'painScale': None, 'bmi': None, 'recordedAt': '2026-10-17T13 |
| RDV-01 | Medecin declare ses disponibilites | OK |  | (200, "{'id': '3863fe9e-67ee-4bd6-b901-2bc622c5670b', 'doctorId': '4e7cbab2-9ebf-4ed4-b281-a315da6f99e6', 'weekday': 7, 'startTime': '08:00', 'endTime': '12:") |
| RDV-02 | Agenda medecin accessible | ARTEFACT |  | Artefact du harnais : paramètres from/to manquants → 500 (FA-43). Rejoué avec paramètres : 200. |
| RDV-03 | Accueil peut reserver un RDV pour un patient (APPOINTMENT_WRITE) | ÉCART | FA-23 | (403, 'aucun endpoint personnel de reservation') |
| RX-10 | Signature de l ordonnance sans cloturer la visite | OK |  | (200, "{'id': '8100cbb2-0321-4775-b41d-123d46221579', 'consultationId': 'f99d5d77-74df-48e2-8fc0-c8bae4b0f69c', 'prescriptionNu") |
| HOS-10 | Admission apres remise en etat du lit | OK |  | (201, "{'id': '12f350eb-a4bd-4db7-9056-ef1e4adb58a7', 'patientId': '9d0edac0-68bf-485b-9d20-401a0a5f8591', 'organizationId': 'e2e8d025-8234-4df5-b30b-dd7ea50") |
| MED-01 | Infirmier voit la Ceftriaxone a administrer | OK |  | (200, [('Ceftriaxone', '1 g IV/24h')]) |
| MED-02 | Administration J1 tracee | OK |  | (201, "{'id': '7d45cc16-1963-4d32-a446-6ea6ac387a94', 'hospitalizationId': '12f350eb-a4bd-4db7-9056-ef1e4adb58a7', 'prescriptio") |
| MED-03 | Deuxieme administration immediate (double dose / double clic) bloquee ou alertee | ÉCART | FA-03 | (201, "{'id': '75bbcf7d-7d91-4a9f-abdc-b6011b6a174f', 'hospitalizationId': '12f350eb-a4bd-4db7-9056-ef1e4adb58a7', 'prescriptio") |
| MED-04 | Pharmacie delivre 3 flacons au service | OK |  | 204 |
| MED-05 | Apres delivrance, le traitement reste administrable | ÉCART | FA-02 | (200, 0) |
| BIL-10 | Facturation emet un avoir pour corriger une facture partiellement payee | ÉCART | FA-19 | (403, '{\'error\': {\'code\': \'ACCESS_DENIED\', \'message\': "Vous n\'êtes pas autorisé à effectuer cette action.", \'trace_id\': \'trc_e65dd64549364e96a4af\'}, \'detail\': "Vous n\'') |
| BIL-11 | Secretaire comptable peut annuler une facture | ÉCART | FA-19 | (403, '{\'error\': {\'code\': \'ACCESS_DENIED\', \'message\': "Vous n\'êtes pas autorisé à effectuer cette action.", \'trace_id\': \'trc_671999586ebe4d51a45f\'}') |
| BIL-12 | Annulation d une facture deja encaissee partiellement (admin) | OK | FA-19 | (400, "{'detail': 'Seule une facture en attente peut être annulée directement. Créez un avoir pour une facture validée ou réglée.', 'instance': '/api/invoices/2a784f3f") |
| BIL-13 | Remboursement du paiement deja encaisse : circuit present | ÉCART | FA-19 | aucun endpoint de remboursement / mouvement lie a l avoir |
| ID-01 | Doublon Mbarga detecte dans la file des doublons | OK |  | (200, "[{'id': '7f2611d2-26b0-402b-a41b-b96fcb194f69', 'sourcePatient': {'id': '038046e7-db71-4a05-91c7-48eb5227d555', 'organizationId': 'e2e8d025-8234-4df5-b30b-dd7ea506e00d', 'globalPatientNumber': 'DPU-JO") |
| ID-02 | Agent d accueil voit / signale les doublons | ÉCART | FA-26 | 403 |
| RBAC-01 | Administrateur clinique (non soignant) peut decider une sortie medicale | ÉCART | FA-37 | (200, "{'id': '12f350eb-a4bd-4db7-9056-ef1e4adb58a7', 'patientId': '9d0edac0-68bf-485b-9d20-401a0a5f8591', 'organizationId': 'e") |

## Données complémentaires

- Facture FAC-20261017-000001 (BIL-04) : `Consultation - Fièvre depuis 3 jours` 15 000 × 1 ; `Médicament : Artemether-Lumefantrine (20/120 mg)` 2 500 × **124** → total **325 000 XAF**, sans aucun tarif paramétré.
- VIS-03 : consultation `doctorName = Dr A Généraliste`, `signedBy = bb12c379…` (= Dr B Interniste).
- Concurrence HOS-05 : deux requêtes parallèles sur le lit 101-A → {jean: 201, claire: 409}.
- Journal backend : `scratchpad/audit-env/backend.log` (session d'audit, non versionné).

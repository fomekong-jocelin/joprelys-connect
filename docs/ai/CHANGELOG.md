# Changelog

Tous les changements notables du projet doivent être documentés ici.

Le format suit l'esprit de Keep a Changelog et le versioning suit Semantic Versioning.

> Le détail historique complet antérieur à cette consolidation est conservé **sans modification** dans `docs/ai/CHANGELOG-HISTORY-THROUGH-20260723.md`. Le présent fichier reste le registre actif à maintenir à partir du 23 juillet 2026.

## [Unreleased]

- **TICKET-20261003-PATIENT-JOURNEY-AUDIT — Correctifs ciblés du parcours patient (pré-enregistrement → constantes → consultation)** :
  - un seul bon d'examens actif par visite : un nouvel enregistrement de la consultation ne crée plus de doublon au laboratoire ;
  - consultation : l'auteur reste figé et un enregistrement concurrent est refusé (409, `expectedUpdatedAt`) au lieu d'un écrasement silencieux ;
  - constantes : contrôle serveur systolique > diastolique, audit `VISIT_VITALS_RECORDED`, heure de mesure (`recordedAt`) visible du médecin, avertissement sur l'unité de glycémie ;
  - pré-enregistrements : filtres par statut côté serveur, lien `validatedPatientId` (Flyway V111), action « Ouvrir le dossier », QR code généré par le backend (plus d'appel à `api.qrserver.com`), libellés traduits FR/EN ;
  - admission : brouillon en `sessionStorage` (purgé à chaque changement d'utilisateur), plus d'orientation « Urgences » dans le parcours normal, praticien principal limité aux médecins et infirmiers ;
  - file d'attente : statut de visite traduit ; consultation : erreur de chargement affichée, appels HTTP déplacés dans `VisitApiService` ;
  - **SemVer** : nouveaux champs d'API et migration additive, rétrocompatibles → candidat `MINOR` (0.10.1 → 0.11.0).

- **BUG-20261003 — Correction du démarrage Spring Boot sans IA (`FinalClinicalReview`)** :
  - ajout de `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")` sur `FinalClinicalReviewController`, `FinalClinicalReviewService` et `OpenAiFinalClinicalReviewGateway` ;
  - élimination de l'`UnsatisfiedDependencyException` liée à l'absence du bean `AiConsultationService` lorsque `joprelys.ai.enabled=false` ;
  - déblocage des migrations Flyway V88 à V110 sur environnement local ;
  - **SemVer** : correctif rétrocompatible candidat `PATCH`.

- **BUG-20260809 — Clarification de la navigation clinique** :
  - remplacement des libellés ambigus par `Services & unités de soins`, `Suivi des lits` et `Configurer chambres & lits` ;
  - regroupement sous `Établissement` et `Capacité d’accueil` ;
  - **SemVer** : correctif rétrocompatible candidat `PATCH`.

- **BUG-20260809 — Filtrage du médecin responsable par service** :
  - les modales d’admission normale et de continuité urgence proposent uniquement les médecins actifs affectés à l’unité sélectionnée ;
  - l’admission normale impose maintenant l’ordre service → médecin et désactive le médecin avant la sélection du service ;
  - le choix est réinitialisé lors d’un changement de service s’il devient invalide ;
  - **SemVer** : correctif rétrocompatible candidat `PATCH`.

- **DIAG-20260809 — Durcissement du parcours d’attribution du lit** :
  - les admissions normales et la continuité urgence ne proposent que les lits disponibles, ouverts et prêts ;
  - le backend contrôle le tenant, l’activation, le rôle `MEDECIN` et l’affectation active du praticien responsable ;
  - un échec de génération documentaire reste visible et récupérable sans rejouer l’admission ;
  - les workflows de demande, réservation, arrivée et handoff restent dans les lots `HOS-ADM`, `HOS-MOV` et `HOS-PATH` ;
  - **SemVer** : candidat `MINOR`, aucun bump ni release préparé.

- **BUG-20260804 — Support ordonnances/examens, captation continue non-bloquante et chunking strict backend** :
  - **Assistant IA Mobile** : ajout du support des prescriptions et examens complémentaires (`prescriptions` et `labOrders`) dans `ConsultationNote`, extrait par l'IA et affiché dans l'aperçu clinique `ClinicalAcceptedPreview`. Notification explicite du praticien si des ordonnances/examens sont détectés pour leur saisie dans les modules dédiés.
  - **Performance Écoute** : passage de l'analyse progressive (`analyzeProgressiveSegment`) en mode non-bloquant (`unawaited`) dans `ClinicalVoiceProgressiveCoordinator` pour éviter tout blocage de la boucle de sérialisation audio.
  - **Robustesse Backend** : refonte de `AiClinicalCaptureRebuildService` pour empêcher tout scindage arbitraire au milieu d'un segment (`splitLongText` supprimé).
  - **SemVer** : correctif rétrocompatible candidat PATCH.


- **BUG-20260802 — Fiabilisation end-to-end de l'assistant vocal mobile** :
  - **Diagnostic P0** : courses entre reprise Android et recovery, déduplication insuffisante des fenêtres ASR, restauration de transcripts `ANALYZED`, marqueur d'effacement ignoré et `eventId` backend inutilisé identifiés comme causes des répétitions, retours d'anciens textes et pertes aux frontières.
  - **Cible du correctif** : une seule écoute active, fusion bornée des replays récents, restauration limitée à `PENDING_REVIEW`, effacement durable, analyse realtime idempotente et working set Vitals filtré.
  - **Sécurité clinique** : contrats SOAP, permissions et validation explicite du praticien inchangés ; aucun diagnostic ni constante n'est appliqué automatiquement.
  - **SemVer** : correctif rétrocompatible candidat PATCH ; tests et recette Android encore requis avant clôture.

- **PR #258 / TASK-20260801 — Fusion et réconciliation de la livraison mobile** :
  - **Historique sécurisé** : les 17 commits de `fix/mobile-dashboard-layout-overflow` sont fusionnés dans `main` au commit `e6f7a348` ; la branche est supprimée localement et sur `origin`.
  - **IDs canoniques** : annuaire `MOB-2809`, dossier/historique `MOB-2810`, constantes `MOB-2811`, assistant capture/transcription `MOB-2816` et notes SOAP `MOB-2821`. Les anciens usages restent documentés uniquement dans la table de transition d'EPIC-0028.
  - **SemVer** : les anciennes cibles `v1.1.0` sont remplacées par un candidat MINOR `0.11.0` ; aucune release ni aucun bump n'est préparé par cette fusion.

- **TASK-20260801 / ADR-0005 — Contrat SOAP unifié Angular, Flutter et Spring Boot** :
  - **Contrat canonique simplifié** : Flutter consomme désormais `GET/POST /api/visits/{id}/consultation` avec `symptoms`, `clinicalExam`, `diagnosis`, `conclusion`, `advice`, `followUp` ; un vrai `404` n'est plus masqué comme une note absente.
  - **Diagnostic unique** : Angular, Flutter, Spring et l'assistant IA n'exposent plus `suspectedDiagnosis` ni `finalDiagnosis`. V109 archive les anciennes valeurs, retient la valeur la plus aboutie disponible, puis supprime les deux colonnes actives redondantes. L'orchestration Angular des prescriptions, feedbacks et libellés a été extraite en services/facade dédiés afin de ramener `ConsultationComponent` sous 500 lignes.
  - **Sécurité clinique** : la dictée locale mobile ne fabrique plus de diagnostic ni de plan de soins à partir d'une simple mention de fièvre ; elle conserve uniquement le texte dicté et les constantes explicitement reconnues.
  - **Découpage Flutter** : `consultation_notes_sheet.dart` passe de 573 à 236 lignes avec extraction de l'en-tête, du formulaire, des contrôleurs et des champs cliniques réutilisables. L'assistant vocal de 819 lignes est séparé en orchestration (379), surface d'écoute (326) et transcription (152), sans fichier au-dessus de la limite dure de 500.
  - **Vérifications** : 15 tests Angular ciblés et le build production sont verts ; l'analyse Dart ciblée ne remonte aucun diagnostic. Les tests Maven sont bloqués par le parent Spring Boot absent du cache ; les tests Flutter runtime complets n'ont pas terminé dans le délai d'outillage.

- **MOB-2809 — Annuaire & Recherche Globale des Patients Mobile** :
  - **Recherche en temps réel & API** : création de `PatientDirectoryItem` et `PatientDirectoryApi` consommant `GET /api/patients?q={query}` pour rechercher n'importe quel patient de l'établissement par nom, numéro DPU, téléphone ou numéro temporaire.
  - **Interface utilisateur** : ajout d'une barre de recherche dynamique et d'un sélecteur d'onglets à deux vues (*File d'attente active* vs *Annuaire global de tous les patients*) avec cartes récapitulatives et accès direct au dossier médical (`PatientHistorySheet`).

- **MOB-2821 / MOB-2810 — Alignement UI SOAP Web Angular & Connexion Backend Réelle** :
  - **Interface SOAP (ConsultationNotesSheet)** : refonte complète de la modale SOAP pour s'aligner sur l'éditeur Web Angular `clinical-note-editor` (sections numérotées 1, 2, 3, 4 avec sous-titres d'aide i18n, badge motif de consultation, accès rapide à l'assistant vocal et zone de texte clinique stylisée).
  - **Historique Médical (PatientHistoryApi & Sheet)** : correction du paramètre d'appel (`patientId` au lieu de `visitId`), élimination des exceptions brutes 404 affichées en rouge à l'utilisateur, et ajout d'un écran d'erreur ergonomique avec bouton de réessai.
  - **Gestion REST** : `ConsultationApi` distingue désormais le `204` (visite valide sans consultation) du `404` (visite introuvable) ; les autres gateways conservent leur politique documentée propre.

- **MOB-2810 — Historique Médical & Chronologie des Visites Patient** :
  - **Domaine & Gateway** : création de `PatientMedicalHistory`, `MedicalAntecedent`, `PatientAllergy`, `PastVisitSummary` et `PatientHistoryApi`.
  - **Interface utilisateur** : création du composant modal `PatientHistorySheet` avec onglets séparés pour les antécédents/allergies (avec badges de sévérité) et la chronologie des consultations passées.
  - **Tests & Conformité** : création de `patient_history_api_test.dart` et validation de la suite avec 68/68 tests Flutter verts.

- **MOB-2816 — Assistant Vocal Clinique & Dictée Intelligente** :
  - **Moteur d'extraction** : création de `ClinicalDictationParser` capable d'extraire automatiquement en Regex les constantes médicales explicitement dictées et de conserver le texte clinique sans inférer de diagnostic ou de plan de soins.
  - **Interface utilisateur** : création du composant modal `ClinicalVoiceAssistantSheet` et ajout du bouton `[🎙️ Assistant vocal]` dans la modale des constantes.
  - **Tests & Conformité** : création de `clinical_dictation_parser_test.dart` et validation de la suite avec 66/66 tests Flutter verts.

- **MOB-2821 — Saisie & consultation mobile des notes cliniques (SOAP)** :
  - **Gateway & REST** : création de `ConsultationGateway` et `ConsultationApi`, désormais alignés sur les endpoints canoniques `GET/POST /api/visits/{id}/consultation`.
  - **Interface SOAP** : création du composant modal `ConsultationNotesSheet` structuré en quatre rubriques cliniques et aligné sur les six champs utiles du contrat backend simplifié.
  - **i18n & Thèmes** : support complet FR/EN et Thèmes Sombre/Clair sans aucun texte dur.
  - **Tests** : création de `consultation_api_test.dart`, ensuite étendu à la sérialisation détaillée, aux endpoints, au `204` et à la propagation du `404` ; la preuve 63/63 reste historique et doit être remplacée par un gate exact-HEAD.

- **MOB-2811 — Saisie & consultation mobile des constantes patient** :
  - **Gateway & API** : création de `VitalsGateway` et `VitalsApi` consommant `GET /api/visits/{id}/vitals` et `POST /api/visits/{id}/vitals`.
  - **Composant UI** : création de `PatientVitalsSheet` (modale de saisie 10 constantes vitales, calcul d'IMC à chaud, validation et messages d'erreur).
  - **Raccordement Dashboard** : clic sur la carte patient (`_ActiveVisitCard`) déclenchant la modale et actualisant la file d'attente à l'enregistrement.
  - **Tests** : ajouts des tests unitaires `vitals_api_test.dart` (validation JSON, calcul d'IMC).

- **BUG-20260730-MOBILE-DASHBOARD-UI-REDESIGN — Refonte visuelle du dashboard mobile Flutter** :
  - **En-tête & Ambiance** : suppression du dégradé radial avec spot bleu dur ; ajout du marqueur d'espace clinique médical `[+] ESPACE CLINIQUE` et hiérarchisation fluide des titres.
  - **Cartes métriques** : refonte des conteneurs `En attente`, `Prêts`, `À évaluer` avec bordures subtiles (`accent.withValues(alpha: 0.3)`), micro-icônes d'état et typographie Montserrat bold.
  - **Cartes de visite patient** : réarchitecturation de `_ActiveVisitCard` avec avatar patient, badge de constantes en haut à droite, motif de consultation lisible et correction de la répétition du préfixe DPU (`DPU DPU-` -> `DPU-`).
  - **Conformité & Tests** : 58 tests Flutter et goldens verts, aucune régression sur le routing, l'i18n FR/EN ou la logique métier.

- **FIX-20260730-MOBILE-AUTH-WEB-VISUAL-ALIGNMENT — Auth Flutter alignée sur l’expérience web** :
  - **Composition** : préférences compactes, lockup de marque réutilisable, titre hors carte, libellés de champs stables et CTA pleine largeur selon les tokens Joprelys.
  - **Accueil professionnel** : suppression du badge technique `MOB-2805` et des réglages dupliqués ; identité, rôle, biométrie et déconnexion sont hiérarchisés dans des surfaces sobres.
  - **Design system / i18n** : ajout du bouton destructif secondaire, du champ à label externe et des textes FR/EN de l’accueil sans nouveau contrat API ni logique métier frontend.
  - **Qualité** : 53 tests Flutter verts, analyse `lib`/`test` sans erreur, build web réussi et goldens sombres `393 × 852` validées dans `design-qa.md`. Le build APK reste à confirmer sur CI, le sandbox local ne pouvant ni verrouiller le cache Gradle utilisateur ni télécharger Gradle 8.14.

- **MOB-2804 — Client API Flutter, erreurs, corrélation et résilience réseau** :
  - **Configuration** : ajout de `APP_ENV` / `API_BASE_URL`, validation stricte de la base URL et HTTPS obligatoire en recette/production ; aucune valeur sensible n’est placée dans `--dart-define`.
  - **Client central** : Dio est encapsulé par `ApiClient` et injecté via Riverpod ; headers JSON, locale, bearer conditionnel, idempotence et `X-Trace-Id` sont gérés hors des widgets et des features métier.
  - **Sessions** : port `ApiSessionAccess`, distinction professionnel/patient, coordination d’un seul refresh concurrent et replay borné ; un `403` ne déclenche jamais de refresh/logout et le patient n’utilise jamais le refresh professionnel.
  - **Résilience / erreurs** : retry unique limité aux requêtes sûres ou explicitement idempotentes, mapping de l’enveloppe Joprelys, de `ProblemDetail` et des erreurs Dio, sans log de token, cookie, PII ou body clinique.
  - **Frontière thème préservée** : aucun changement de `AppTheme`, `AppDesignTokens` ou widget partagé ; `core/network` reste non visuel et les DTO métier restent feature-scoped.
  - **Validation runtime** : run #2046 (`30522369209`) vert sur `0c2670a3545bedc60d2f241e9452bc7a6a594de8` : pub get, format, analyze, tests Flutter et APK debug. Gate final exact-HEAD requis après clôture documentaire.

- **MOB-2803 — Internationalisation Flutter FR/EN et formats locale** :
  - **i18n native** : activation de `flutter_localizations`, `gen_l10n` et ARB FR/EN avec français comme fallback produit ; la locale système FR/EN est reconnue et la langue peut être changée à chaud via Riverpod.
  - **Formats cohérents** : ajout d’un formateur central limité aux dates, heures et nombres ; `fr_FR` et `en_GB` sont initialisés au bootstrap et `intl 0.20.2` est verrouillé par le lockfile Flutter 3.44.6.
  - **Fondation localisée** : suppression des libellés utilisateur codés en dur de la page de fondation et branchement des delegates/locales sur `MaterialApp.router`.
  - **Frontière thème préservée** : `AppTheme` et `AppDesignTokens` sont volontairement inchangés ; l’i18n ne transforme pas le thème en registre de styles métier et les futures compositions visuelles restent feature-scoped, comme sur le web.
  - **Validation finale** : run #2030 (`30517927222`) vert sur le HEAD exact `5363c491585b0f4944f941a2576730c28c9c9775`, puis fusion squash de la PR #251 dans `main` au commit `43e3d053`.

- **FIX-20260730-PUBLIC-SELF-REGISTRATION-UX / #247 — Pré-enregistrement public progressif et sorties explicites** :
  - **Charge cognitive mobile** : remplacement du formulaire monolithique de cinq sections par un parcours progressif en quatre étapes, avec une seule étape affichée à la fois et conservation des valeurs lors des retours arrière.
  - **Cohérence Joprelys Connect** : topbar alignée sur le shell applicatif, sélection explicite `FR | EN`, thème clair/sombre et dictionnaire i18n de feature dédié.
  - **Navigation de sortie** : accès permanent au site Joprelys et à `/patient/login`, y compris sur l'écran de succès, sans modification du contrat API, du captcha, du `orgId`, de la DB ni du RBAC.
  - **Validation** : tests Angular ciblés ajoutés ; gate frontend et recette visuelle mobile/desktop requis avant fusion.

- **MOB-2802 — Design system Flutter Joprelys, thèmes light/dark/system et widgets partagés** :
  - **Tokens centralisés** : création de `AppDesignTokens` pour les couleurs sémantiques, espacements, rayons, tailles tactiles et ombres, avec `DESIGN.md` comme source de vérité commune web/mobile.
  - **Thèmes** : `AppTheme.light` / `AppTheme.dark` et `ThemeController` Riverpod `system | light | dark` branchés dans `JoprelysApp` ; aucun chargement réseau de police n’est ajouté.
  - **Primitives UI** : `AppButton`, `AppTextField`, `AppCard`, `AppBadge`, `AppPageHeader`, états loading/empty/error et `AppConfirmDialog`, avec rayons sobres et actions tactiles principales >= 44 px.
  - **Sécurité fonctionnelle** : aucun faux contenu clinique, aucune donnée patient, aucun secret, aucun appel réseau et aucune permission native ajoutés dans ce lot.
  - **Validation finale** : run #1994 (`30514193739`) vert sur le HEAD exact `f851926981d3526f26f47389afe0897437ba7928`, puis fusion squash de la PR #246 dans `main` au commit `311f60a1`.

- **MOB-2801 — Première fondation runtime Flutter & CI mobile** :
  - **Bootstrap applicatif** : remplacement du compteur Flutter généré par le flux `main -> bootstrap -> ProviderScope -> JoprelysApp`, avec configuration minimale centralisée et aucune logique métier clinique dans la présentation.
  - **Navigation** : intégration de Riverpod et `go_router` avec une seule route de fondation neutre `/` ; aucun faux écran clinique ni donnée patient n’est exposé avant livraison des stories métier correspondantes.
  - **Baseline mobile** : CI pinée sur Flutter `3.44.6` / Dart `3.12.2`, `flutter_riverpod` résolu en `3.4.2`, `go_router` en `17.3.0`, et `pubspec.lock` applicatif versionné.
  - **CI mobile** : détection dédiée de `mobile/**` et gate `flutter pub get`, Dart format, `flutter analyze`, `flutter test`, puis `flutter build apk --debug` ; une modification du workflow partagé force aussi les gates backend et frontend.
  - **Validation finale MOB-2801** : run #1972 (`30502470416`) vert sur le HEAD exact `92fc92eb7c95ac1a7d33769cf57c658d6558579d`, puis fusion squash de la PR #244 dans `main` au commit `ca245a74`.

- **FIX-20260729-PATIENT-RECORD-MOBILE-UX-FINISHING — Finition de la hiérarchie mobile du dossier patient** :
  - **Fiche d’identité unifiée** : les informations administratives sont désormais rendues directement dans la Fiche d’identité ; l’accordéon redondant « Informations administratives » disparaît sans suppression de donnée.
  - **Accordéons compacts** : les intitulés deviennent `Contexte d’urgence`, `Informations médicales` et `Contact d’urgence`, avec titres courts sur une ligne et sous-titre d’urgence déplacé dans le contenu déplié.
  - **Actions hiérarchisées** : sur mobile, `Ouvrir une visite` / `Démarrer la consultation` occupe la première ligne complète ; `Synthèse PDF` et `Retour` deviennent les actions secondaires. Les permissions existantes restent inchangées.
  - **Design / tests** : alignement de `DESIGN.md`, FR/EN et ajout de tests ciblés sur la divulgation progressive, l’urgence active et la hiérarchie des actions. Gate frontend final requis avant fusion de la PR #228.

- **TASK-20260728-P0-VOICE-CLARIFICATION-CONFLICT-HOTFIX — Correction du blocage de finalisation "Finalisation en cours..." et gestion des conflits 409** :
  - **Résolution du blocage de finalisation** : la présence d'une clarification en attente ne bloque plus indéfiniment la finalisation d'une session lorsque le pipeline audio est inactif ; le clic sur "Terminer" force la cloture propre et l'application du brouillon.
  - **Gestion résiliente des conflits 409** : ajout de `AI_CLARIFICATION_PENDING` dans les états suspendus non fatals du coordinateur de tours Angular, évitant les crashs réseau et les alertes d'erreur intempestives lors d'interactions simultanées avec l'assistant.
  - **Support du double-clic de secours** : si la finalisation reste en cours en raison d'une revue bloquante, un second clic sur "Terminer" applique directement les données accumulées sans perdre l'état clinique.

- **TASK-20260728-P0-VOICE-HOTFIX-B — Realtime conversationnel fiable et Dictée passive** :
  - **Suppression des aberrations vocales** : retrait de `response.create`, de la consigne « lisez le message approuvé mot pour mot » et du double canal `SpeechSynthesis`/Realtime. Le transport OpenAI Realtime ne produit plus de réponse assistant autonome ni d’audio.
  - **Conversation clinique Realtime** : une question courte de clarification ou un message clinique validé par le backend peut être lu une seule fois via le TTS Joprelys dédié. La reprise de parole du médecin interrompt la lecture sans couper le microphone.
  - **Dictée réellement passive** : aucun message IA n’est vocalisé en Dictée Consultation ou Constantes ; le texte entendu reste affiché pour relecture.
  - **Correction sans perte** : une confiance ASR absente n’est plus transformée en zéro. Tout transcript non vide reste durable et visible ; la dernière phrase Realtime expose toujours « Corriger », y compris quand l’ASR se dit confiant. Les constantes dictées peuvent aussi être corrigées puis réanalysées.
  - **Capture continue** : le backlog, l’analyse, une proposition, une clarification ou une restitution TTS ne coupent plus le sender WebRTC. Seules la pause explicite, la finalisation et une panne de persistance fail-closed peuvent l’arrêter.
  - **Finalisation drainante** : « Terminer » bloque les nouveaux tours, conserve l’éditeur et les files, attend les ACK/analyses/décisions encore en cours, applique le brouillon au formulaire puis quitte le Realtime.
  - **Architecture** : extraction du support de connexion WebRTC et création d’un service de restitution clinique unique ; les contrôleurs et le bridge restent sous la limite de 500 lignes par classe.
  - **Validation** : 102 fichiers/517 tests Angular, 6 tests backend Realtime, 816 tests Maven complets, contrôle i18n et build production verts. Recette clinique réelle Chrome desktop/Android encore requise.

- **FIX-20260728-UNIFIED-VOICE-LISTENING-SURFACE — Surface d’écoute identique pour Consultation/Constantes en Dictée/Realtime** :
  - **Composant partagé** : extraction de `VoiceListeningSurfaceComponent`, purement présentationnel, avec badge IA, arrêt, microphone central, halos, rubans d’ondes animés, état d’écoute et bandeau conseil contextualisé.
  - **Quatre intégrations** : remplacement des variantes visuelles dans Consultation/Dictée, Consultation/Realtime, Constantes/Dictée et Constantes/Realtime sans modifier les moteurs audio ni les contrats API.
  - **Design system et accessibilité** : tokens centralisés light/dark, rayons sobres, action tactile de 44 px minimum, focus visible, i18n FR/EN et réduction des animations selon la préférence système.
  - **Anti-déformation** : l’historique Realtime Consultation reste séparé dans une zone fixe et scrollable.
  - **Validation** : 46 tests ciblés, 507 tests Angular, contrôle i18n et build production verts ; comparaison visuelle interactive encore requise sur une session authentifiée.

- **FEAT-20260728-UI-MOCKUPS-AI-VOICE-DICTATION — Intégration du composant Soft UI de dictée vocale IA avec zone de transcription fixe scrollable (anti-déformation)** :
  - **Composant carte vocale Soft UI (`RealtimeVoiceControllerComponent` & `VoiceWaveVisualizerComponent`)** : Implémentation fidèle de la carte de dictée vocale soft (`pasted-image-4.png`) intégrant le badge `✦ IA en cours...`, le bouton sobre `⏹ Arrêter`, l'icône micro à halo lumineux et le ruban d'ondes sinusoïdales fluides.
  - **Stabilité de mise en page anti-déformation (Anti-CLS)** : Le flux de transcription en direct s'affiche désormais sous le bloc du micro dans un conteneur à hauteur fixe bornée (`h-32 max-h-32 overflow-y-auto`) avec défilement automatique vers le bas (`scrollToBottom()`). L'écran ne s'allonge et ne se rétrécit plus pendant la dictée.
  - **Thèmes CSS centralisés & i18n FR/EN** : Utilisation exclusive des tokens de `DESIGN.md` (`var(--app-surface)`, `var(--app-border)`, `var(--brand-primary)`, `var(--text-primary)`), compatibilité transparente avec les thèmes Light/Dark et clés de traduction `aiInProgress`, `listenNaturally`, `tipDictateNaturally`, `tipVitalsNaturally`.

- **BUG-20260728-LOGIN-FLASHING-SESSION-CARD-REMOVAL — Suppression de la carte temporaire de session/rôle au login et redirection directe dashboard** :
  - **Suppression du flash UI (`login.component.html`)** : Elimination de la carte temporaire d'identité (`login-session-card`) qui affichait le nom, l'email et le rôle de l'utilisateur pendant les quelques millisecondes de redirection vers le tableau de bord.
  - **Protection par guard (`web/src/app/auth/login.guard.ts`)** : Ajout du guard `loginGuard` sur les routes d'authentification (`path: ''` et `path: 'patient/login'`) dans `app.routes.ts` pour rediriger immédiatement tout utilisateur déjà connecté vers `/dashboard` (ou `/patient/dashboard`) sans charger ni faire flasher la page de login.

- **BUG-20260728-LOGIN-PASSWORD-TOGGLE-EYE — Bouton d'affichage / masquage du mot de passe (Icône œil)** :
  - **Interactivité & Sécurité Visuelle (`LoginComponent`)** : Ajout du bouton réactif d'affichage/masquage du mot de passe avec le signal `showPassword` et bascule dynamique du type d'input (`password` / `text`).
  - **Accessibilité & i18n FR/EN** : Intégration d'un bouton accessible avec `aria-label` et `title` dynamiques ("Afficher le mot de passe" / "Masquer le mot de passe" en FR, "Show password" / "Hide password" en EN).
  - **Design System & Responsive CSS** : Positionnement absolu propre de l'icône dans la zone d'input avec `padding-right: 2.85rem` pour éviter tout chevauchement du texte, gestion des états hover/focus-visible et intégration automatique aux thèmes light et dark.


- **BUG-20260728-DOCTOR-AVAILABILITY-MOBILE-GRID-AND-WEEKDAY-BUG — Correctifs UX & Backend Disponibilités (Mobile-First, Modale Unifiée par Radio, i18n 100% FR/EN & Trailing Slash API)** :
  - **Simplification UX (Bouton & Modale d'ajout unifiée par boutons radio)** : Remplacement des 2 boutons d'en-tête redondants par un seul bouton principal `+ Ajouter` (`availability.modal.addButton`). Ouverture d'une modale réactive unique intégrant un sélecteur par boutons radio entre *Disponibilité (Plage de consultation)* et *Indisponibilité (Absence / Congé)* pour basculer de manière fluide les champs du formulaire.
  - **Internationalisation 100% FR/EN (`WeeklyAvailabilityGridComponent`)** : Suppression de toutes les chaînes de texte en français codées en dur dans le calendrier (onglets, boutons "Modifier", "Désactiver", créneaux rapides "Matin", "A-M", "Jour", "Personnalisé..."). Extension de `WeeklyAvailabilityGridLabels` et liaison intégrale aux dictionnaires `features/availability/{fr,en}.json`.
  - **Épurage de la vue d'aperçu des créneaux** : Suppression du paragraphe de texte explicatif technique (règle de calcul des 7 jours et durée de créneau) dans le composant accordéon d'aperçu des créneaux pour offrir une vue minimaliste et directe.
  - **Correction du débordement mobile (Responsive & Anti-Overflow)** : Ajout des contraintes `w-full min-w-0 max-w-full` sur l'hôte `@Component` de `WeeklyAvailabilityGridComponent`, `CardComponent` et `availability-page.component.html` pour éliminer le scroll horizontal forcé sur écran mobile (< 768px). Le bandeau d'onglets de jours supporte un défilement horizontal interne autonome (`shrink-0`).
  - **Synchronisation strict jour / date (`AvailabilityPageComponent`)** : Élimination du désalignement entre le jour de semaine (`formWeekday`) et la date de début de validité (`formValidFrom`) lors de la sélection d'une date ou d'un créneau dans la grille.
  - **Résilience Backend (`AvailabilityController.java`)** : Support de `@RequestMapping({"/api/availabilities", "/api/availabilities/"})` et des sous-routes avec/sans slash final sous Spring Boot 3.

- **FEAT-20260728-UNIVERSAL-VOICE-FUSION — Évolution R1 : Micro Ambiant Universel Unifié (Note Clinique + Constantes)** :
  - **Unification du Flux Vocale Ambiant** : Élimination de la dualité des assistants vocaux sur la page de consultation. Le micro universel principal (`app-voice-assistant-panel`) gère désormais l'écoute unique et réalise l'extraction simultanée de la note clinique, des prescriptions, des examens ET des constantes vitales (`vitals`).
  - **Routage Automatique Dual (Formulaire + Constantes)** : Mise à jour de `applyAiDraft` dans `consultation.component.ts` pour que la validation globale d'un brouillon applique à la fois les champs de la note et prépare le bloc de validation des constantes (`pendingVitalsProposal`).
- **STORY-20260728-CLINICAL-VOICE-FLUIDITY — Ergonomie Clinique & Visualiseur d'Ondes Vocales Animées** :
  - **Visualiseur d'Ondes Sonores Animées (`VoiceWaveVisualizerComponent`)** : Création d'un composant réactif affichant des vagues sonores organiques pulsatiles et fluides réagissant dynamiquement au volume sonore pendant l'écoute.
  - **Validation Global "1-Clic" (`AiProposalPanelComponent`)** : Ajout du bouton principal *Tout valider (X modifications en 1 clic)* au sommet du panneau de propositions IA pour appliquer l'ensemble des révisions en attente d'un seul coup sans imposer de clics répétitifs.
  - **Épurage de la Vue Consultation** : Isolation du bloc d'audit technique ("Note clinique sourcée / SHA-256") dans un accordéon pliable discret `<details>` intitulé *Traçabilité & Preuves cliniques (Optionnel)* pour désencombrer la surface de travail du médecin.
- **FIX-20260728-TRANSCRIPTION-UX-SURGICAL — Correctifs chirurgicaux UX Dictée / Transcription Temps Réel (Constantes & Consultation)** :
  - **Historique & Feedback File d'attente (BUG-03 & BUG-08)** : Ajout d'un panneau d'historique scrollable horodaté (`transcriptHistory`) dans le contrôleur Realtime Consultation pour conserver la visibilité sur l'ensemble des phrases captées. Repositionnement du badge de file d'attente (`queuedCount`) et de traitement au cœur du composant de transcription avec animations visuelles (`pulse` / `spin`).
  - **Staging des Constantes (BUG-04 & ERG-02)** : Suppression de la sauvegarde automatique/directe en base de données lors de la détection de constantes dans le panneau consultation. Les constantes sont désormais présentées sous forme de proposition à valider/rejeter explicitement par le médecin (`pendingVitalsProposal`), et le message de confirmation liste le nombre de champs mis à jour.
  - **Avertissements & Polling (BUG-05 & BUG-06)** : Isolation de l'avertissement de validation des constantes dans un signal dédié (`vitalsWarning`) avec bouton de fermeture pour éviter son effacement par le busy-state. Conditionnement du polling à 4 secondes (`this.session()`) pour supprimer les requêtes réseau superflues sans session active.
  - **Ergonomie Mobile Overlay (BUG-07)** : Ajustement des contraintes d'affichage CSS (`bottom-14`, `max-h-[55dvh]`) de l'assistant de constantes en mode non-intégré pour empêcher le masquage des boutons de soumission/action du formulaire principal sur mobile.
- **#215 / PR #216 — P0 sûreté Dictée/Realtime Consultation & Constantes** : séparation capture durable / analyse clinique afin que le médecin puisse continuer à parler pendant les traitements et revues, conservation des transcripts non vides même avec confiance ASR faible ou absente, journal durable distinct `CONSULTATION` / `VITALS`, files ordonnées et dédupliquées, arrêt fail-closed sans suppression lorsque la persistance durable échoue, revue humaine des transcripts incertains, propositions de constantes non appliquées avant validation explicite, fusion du brouillon Consultation protégeant les saisies médecin plus récentes et les lignes existantes de prescription/examens, et simplification mobile-first des surfaces vocales. Flyway V106 ; gates Angular/Maven requis avant fusion.
- **PR #206 — GitHub Actions coût V2** : adoption d'un cycle Draft-first avec gate lourd explicite au passage Ready, `ubuntu-slim` pour les jobs légers de détection/fraîcheur, et blocage volontaire d'un gate devenu stale après ajout de commits. Maven `clean verify`, tests Angular et build production restent obligatoires avant fusion de la stack concernée.
- **PR #205 / P0-192-D — Validation médecin + stale-check transactionnel** : persistance append-only des validations de projection clinique, verrou pessimiste de la visite avant recomposition, rejet `409` d'une projection devenue stale et snapshot relationnel des `factId` validés par section/position. Flyway V104 et Maven strict validés avant fusion.
- **PR #204 / P0-192 — Projection clinique déterministe sourcée / Linked Evidence** : ajout d'une projection de note construite exclusivement depuis les faits cliniques effectifs et le transcript FINAL canonique. Chaque entrée expose sa preuve exacte (`transcriptItemId`, locuteur, offsets audio, offsets caractères, quote verbatim), les sections sont typées et déterministes, et un `projectionVersion` SHA-256 permet le futur stale-check de validation médecin. Aucun LLM n'est appelé pour rédiger ou reformuler la note.
- **PR #196 / P0-192 — Extraction contrôlée des faits cliniques** : ajout de `@ConditionalOnProperty(name = "joprelys.ai.enabled", havingValue = "true")` sur `ClinicalFactExtractionService` pour éviter l'échec de chargement du contexte Spring dans les tests d'intégration non-IA (351 erreurs de tests résolues). Réconciliation complète de la branche de la PR #196 (`feat/ai-controlled-fact-extraction-p0-192`) avec `main@90d5a3c`.

- **BUG-20260724-ADMISSION-FREE-TEXT-SERVICE-ORIENTATION-FIX — Sélecteurs contrôlés pour l'admission et la visite** : remplacement des champs à saisie libre `<input>` d'Orientation et Service dans le formulaire d'admission patient (`app-unified-admission`) par des éléments `<select>` contrôlés. L'orientation propose les orientations cliniques normées (Consultation, Spécialisée, Urgences, Hospitalisation, Ambulatoire, De Jour, Bilan, Autre) et le service s'alimente dynamiquement via `HospitalOrganizationApiService.listServiceCatalog()`.
- **Persistance et restauration anti-perte de brouillon d'admission (`joprelys_admission_draft`)** : sauvegarde automatique en temps réel de tous les champs saisis de l'admission dans `localStorage`. En cas d'interruption, de rafraîchissement ou de reconnexion suite à une fin de session, le brouillon est restauré à 100% à l'ouverture du formulaire avec une bannière d'information et option d'effacement. Exemption explicite de purge pour les clés de brouillon dans `AuthTokenStorageService`.
- **Maintien de session actif en arrière-plan (`AuthSessionKeepAliveService`)** : rafraîchissement proactif du jeton JWT à l'approche de l'expiration (sous 15 minutes) pendant que l'utilisateur travaille sur un formulaire, éliminant tout risque de déconnexion inopinée pendant la saisie.

- **HOS-ORG-001-A / #130 / PR #133 — organisation hospitalière structurée** : ajout d'un bounded context `hospitalorganization` séparé de `spatial`, avec hiérarchie facultative `POLE → DEPARTMENT → SERVICE → CARE_UNIT`. Une petite clinique peut créer directement un service sous l'établissement sans niveau factice.
- **Flyway V87 — référentiels organisationnels** : ajout de `hospital_service_catalog`, `medical_specialty_catalog`, `organizational_unit_type_catalog` et `organizational_units`. Les codes sont stables ; le code d'unité est unique par tenant ; le parent est protégé par FK composite tenant.
- **Catalogue services FR/EN** : 14 types initiaux contrôlés couvrant notamment médecine générale, médecine interne, maternité/gynécologie-obstétrique, pédiatrie, urgences, chirurgie générale, cardiologie, réanimation, anesthésie, bloc, laboratoire, imagerie, pharmacie et hospitalisation polyvalente.
- **Catalogue spécialités FR/EN** : 10 spécialités initiales contrôlées pour préparer HOS-STAFF-001-A sans nouvelle saisie libre.
- **Permission `ORGANIZATION_STRUCTURE_MANAGE`** : capacité dédiée à la gestion des pôles, départements, services et unités ; attribuée aux profils administratifs prévus, non accordée par défaut aux métiers cliniques.

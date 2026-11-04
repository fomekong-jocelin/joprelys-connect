# AUDIT-20261104 — Audit complet : sécurité, sécurité clinique, parcours et interfaces

- **Date** : 2026-11-04. **Base auditée** : `main` @ `e5114415` (après EPIC-0029 lots 1-3 et le système de dialogues).
- **Branche de correction** : `fix/audit-20261104-security-clinical-ux` (non commitée, non poussée).
- **Méthode** : lecture du code, en 4 volets parallèles. Chaque constat indique une preuve `fichier:ligne` et un scénario. Les correctifs ont été vérifiés par les suites de tests (voir § 6). Aucune donnée patient réelle n'a été utilisée.
  - backend AppSec (OWASP Top 10 / ASVS) ;
  - sécurité clinique et cohérence des parcours ;
  - web Angular (UX, WCAG 2.2 AA, i18n) ;
  - mobile Flutter (OWASP MASVS, UX terrain).
- **Limite** : aucune règle clinique (posologie, seuils, scores) n'est inventée. Toute règle de ce type est marquée **à valider par un professionnel de santé**.
- **Décisions** : [ADR-20261104](../../architecture/adr/ADR-20261104-audit-security-clinical-hardening.md).
- **Suite** : [EPIC-0030](../../pm/backlog/EPIC-0030-audit-20261104-remediation.md).

Légende des statuts :
- ✅ corrigé sur la branche ;
- ◐ partiellement corrigé, le reste est dans EPIC-0030 ;
- ⏳ non traité, au backlog (référence EPIC-0030).

## 1. Constat général

Les fondations sont solides :
- JWT vérifié strictement, refresh tokens tournants avec détection de rejeu ;
- `@PreAuthorize` partout, multi-établissement par `@TenantId`, SQL toujours paramétré ;
- signature scellée des consultations et ordonnances ;
- administration des médicaments idempotente avec fenêtre de sécurité ;
- thèmes et tokens centralisés.

Les risques se concentraient sur cinq points :

1. **Une permission technique suffisait à poser un acte médical.** L'administrateur clinique ou un infirmier pouvait rédiger une consultation, décider une sortie ou déclarer un décès. Un médecin pouvait aussi signer l'ordonnance d'un confrère.
2. **Le patient voyait des données non validées.** Brouillons de résultats, diagnostics non signés, et une alerte « attention médicale immédiate » envoyée pour un simple résultat anormal.
3. **Des soins restaient possibles après un décès** : délivrance, examens, administrations.
4. **Le PIN public d'ordonnance** (4 chiffres, compteur en mémoire non atomique) permettait une force brute vers des données de santé nominatives.
5. **Côté interface**, une erreur de chargement des allergies s'affichait comme « aucune allergie », le triage était pré-rempli au niveau le plus grave, et des actions irréversibles n'avaient pas de confirmation.

Les quatre points cliniques les plus lourds demandent une décision médicale et un changement de contrat d'API. Ils sont **planifiés, non corrigés** :
- contrôle d'allergie et de doublon à la prescription ;
- posologie structurée ;
- prescriptions multiples en hospitalisation ;
- corbeille de résultats du prescripteur.

## 2. Sécurité clinique (backend)

| ID | Prio | Constat | Statut | Correctif ou suite |
|---|---|---|---|---|
| CLIN-01 | P0 | Aucun contrôle d'allergie ni de doublon thérapeutique sur une ordonnance manuelle (seulement pour les propositions IA). | ⏳ A1 | Alerte bloquante à la signature et à la validation pharmaceutique, forçage motivé. |
| CLIN-02 | P0 | Posologie en texte libre ; signature possible sans fréquence, durée ni voie ; aucun poids. | ⏳ A2 | Posologie structurée (à valider par un professionnel de santé). |
| CLIN-03 | P0 | Une seule ordonnance par séjour ; les ordonnances ambulatoires actives deviennent administrables pendant l'hospitalisation. | ⏳ A3 | Prescriptions de séjour et arrêt ligne par ligne. |
| CLIN-04 | P0 | Brouillons visibles par le patient (export, résumé) ; alerte d'urgence envoyée pour un résultat simplement anormal. | ✅ ◐ | Ce qui est corrigé :<br>- le patient n'exporte que les résultats validés ;<br>- le résumé ne montre que les consultations signées et la **dernière version validée** des résultats critiques ;<br>- le message patient est gradué : critique = « à revoir rapidement avec un médecin » plus une consigne d'urgence ; anormal = « parlez-en avec votre médecin ».<br>Reste : alerte au prescripteur (A4). |
| CLIN-05 | P1 | Labo : la saisie vaut validation ; un examen est validable sans résultat ; un PDF perdu ne laisse aucune trace ; import partenaire probablement inopérant (contexte d'établissement absent). | ◐ | Ce qui est corrigé :<br>- une saisie sans statut reste un brouillon ;<br>- un examen ne se valide plus par simple changement de statut ;<br>- l'échec de création du PDF est journalisé.<br>Reste (A5) : permission de validation distincte, heure de prélèvement obligatoire, motif de correction, import partenaire. |
| CLIN-06 | P1 | Ordonnance signée ou annulée par n'importe quel médecin, à tout état, ou créée sur une visite close. | ✅ | Seul le praticien en charge signe ou annule ; annulation depuis `DRAFT`/`ACTIVE` seulement ; visite `EN_COURS` exigée (409). Motif d'annulation : A3. |
| CLIN-07 | P1 | L'administrateur peut déclarer un décès via l'orientation « Décès » des urgences. | ◐ | Médecin obligatoire (403). Reste : heure du décès saisie plutôt que l'heure du clic (A6). |
| CLIN-08 | P1 | Un infirmier ou l'administrateur peut rédiger la consultation médicale. | ✅ | Médecin obligatoire (403). Les infirmiers utilisent les transmissions infirmières. |
| CLIN-09 | P1 | Administration : heure, dose et auteur libres, pas de statut « non administré ». | ◐ | Heure future refusée (tolérance 5 min). Reste : dose structurée et statuts (A7). |
| CLIN-10 | P1 | Triage initial en texte libre ; constantes initiales des urgences sans borne ; bornes des constantes de visite trop étroites ; unité de glycémie implicite. | ◐ | Ce qui est corrigé :<br>- triage limité à RED, ORANGE, YELLOW, GREEN ;<br>- bornes de plausibilité **physique** sur les constantes initiales (FC 0 acceptée pour un arrêt cardiaque).<br>Reste : A6. |
| CLIN-11 | P1 | Identitovigilance : demande d'examen sur la visite d'un autre patient ; doublons mal détectés ; sexe en texte libre. | ◐ | La visite doit appartenir au même patient, identité canonique comprise (400). Reste : FA-26, S20. |
| CLIN-12 | P1 | Pharmacie : forme « Comprimé » et substitution autorisée codées en dur ; quantité négative acceptée ; substitution interdite non contrôlée. | ◐ | Ce qui est corrigé :<br>- forme, voie, fréquence, durée et posologie réelles renvoyées ;<br>- quantité > 0 ;<br>- substitution refusée si le prescripteur l'interdit (409).<br>Reste : décrément du stock (FA-38). |
| CLIN-13 | P1 | Des actes restent possibles après un décès. | ✅ | Délivrance, validation pharmaceutique et demande d'examen refusées. Toute administration datée après l'heure du décès est refusée ; une administration antérieure peut encore être tracée. |
| CLIN-14 | P2 | Allergie supprimée sans motif ; sévérité « MEDIUM » posée par le système. | ⏳ A8 | — |
| CLIN-15 | P2 | Décisions IA (accepter/rejeter) gardées en mémoire, sans trace d'origine. | ⏳ B4 | — |
| CLIN-16 | P2 | Facturation : lignes manuelles non dédupliquées ; l'accueil peut modifier les tarifs. | ⏳ B7 | — |
| FA-37 | — | L'administrateur clinique garde des permissions d'actes médicaux. | ◐ | Actes médicaux bloqués côté serveur (ADR-20261104 D-01). Recentrage RBAC : S21. |
| FA-39 | — | PIN d'ordonnance faible. | ✅ | Voir SEC-01. |
| FA-08, FA-11 | — | Auteur manquant, résultat non routé vers le prescripteur. | ◐ | La décision de sortie a toujours un médecin auteur. Reste : A4, A5. |
| FA-12, FA-23, FA-24, FA-26, FA-27, FA-30, FA-38, FA-42 | — | Voir AUDIT-20261017. | ⏳ lot D | FA-27 et FA-38 attendent une décision métier (Q-05, unité de dispensation). |

## 3. Sécurité applicative (backend)

| ID | Gravité | Constat | Statut | Correctif ou suite |
|---|---|---|---|---|
| SEC-01 | Critique | Force brute du PIN d'ordonnance :<br>- 4 chiffres tirés par `Math.random` ;<br>- verrouillage en mémoire non atomique ;<br>- oracle 404/401. | ◐ | Ce qui est corrigé :<br>- 6 chiffres tirés par `SecureRandom` ;<br>- tentatives réservées atomiquement en base (3 essais, 15 min de blocage, conservé au redémarrage) ;<br>- comparaison en temps constant ;<br>- 401 identique que l'ordonnance existe ou non (V121).<br>Reste (B9) : identifiant opaque dans le QR, PIN haché, réponse réduite pour un anonyme. |
| — | Critique | Secret JWT par défaut versionné. | ✅ | Plus aucune valeur par défaut hors dev. En dev, secret aléatoire tiré à chaque démarrage. L'application refuse de démarrer avec l'ancien secret publié. **Changer le secret sur tout serveur qui l'a utilisé.** |
| SEC-02 | Élevée | Bris de glace inter-établissements sans garde-fou ; oracle DPU → UUID. | ◐ | Motif obligatoire de 10 à 500 caractères (serveur et web). Reste (B1) : autorisation par utilisateur, épisode actif ou seconde validation, alerte au DPO, oracle DPU. |
| SEC-03 | Élevée | L'administrateur clinique peut s'attribuer le rôle AUDITEUR inter-établissements. | ✅ | AUDITEUR et `AUDIT_CROSS_TENANT_READ` réservés à la plateforme ; un administrateur ne peut plus modifier ses propres rôles (403, écran en lecture seule). |
| SEC-04 | Élevée | Journal d'audit des lectures perdu (transaction `readOnly`) : **confirmé par un test**. | ✅ | Écriture d'audit en `REQUIRES_NEW` ; entité `Persistable`. |
| SEC-05 | Élevée | OTP et anti-force brute : compteurs en mémoire non atomiques, `X-Forwarded-For` client accepté tel quel. | ◐ | Comparaison en temps constant, compteurs atomiques purgés, IP résolue par Tomcat derrière un proxy de confiance, BCrypt factice contre l'énumération. Reste : persistance multi-instance (B8). |
| SEC-07 | Élevée | Jeton sans `sid` et repli de résolution par e-mail. | ◐ | Repli par e-mail supprimé ; secret par défaut supprimé. Refus des jetons sans `sid` à faire après un outillage de test adossé aux sessions. |
| SEC-06, 09, 11, 13, 16, 19, 20 | Moyenne / faible | Énumération sur le portail ; écriture clinique inter-établissements ; données de santé dans les logs ; erreurs 500 (FA-43) ; sessions non révoquées après réinitialisation du mot de passe ; origines WebSocket `*` ; secret de webhook relu. | ✅ / ◐ | Corrigés : 06, 11, 13, 16. Partiels : 09 (reste labo, ordonnance autorisée, infos médicales), 19 (jeton WebSocket en query, B5), 20 (nom complet sur la vérification publique de document, PDF de test suivis). Détail dans le ticket. |
| SEC-08 | Moyenne | Signature, cachet et numéro d'ordre usurpables ; fichiers servis publiquement. | ⏳ B2 | — |
| SEC-10 | Moyenne | Allergies et antécédents exposés aux rôles administratifs ; `GET /api/patients` non paginé. | ⏳ B3 | — |
| SEC-12 | Moyenne | IA : ni consentement ni pseudonymisation des transcriptions. | ⏳ B4 | — |
| SEC-14 | Moyenne | Intégration labo : une seule clé pour tous les établissements, validateur falsifiable. | ⏳ A5 | — |
| SEC-15, 17, 18, 21 | Faible | Captcha trivial ; upload de consentement non typé ; `validate-on-migrate: false` ; springdoc 2.x sous Boot 4, pas d'OWASP dependency-check. | ⏳ | À planifier en dette sécurité. |
| Dépendances web | — | Angular 22.2.0 vulnérable. | ✅ | Angular 22.2.4 / CLI 22.2.5. `npm audit --omit=dev` : 0 vulnérabilité. Garde CI ajoutée. Restent 5 vulnérabilités d'outillage de dev, non livrées (bug `npm audit fix`). |
| Scripts SSH | — | `AutoAddPolicy` dans les scripts d'exploitation. | ✅ | `RejectPolicy` et clés d'hôte connues. |

### 3 bis. Correctifs d'authentification et d'infrastructure

Ces correctifs ont été développés dans un worktree isolé puis intégrés à la branche. Le détail point par point est dans le ticket [AUDIT-20261104](../../ai/tickets/AUDIT-20261104-FULL-SECURITY-CLINICAL-UX.md#sécurité--authentification-et-infrastructure).

Un constat supplémentaire est apparu pendant les tests : l'import labo partenaire ne retrouve la demande que si le contexte d'établissement est déjà posé. En production anonyme, il échoue donc probablement. Il est ajouté à A5.

## 4. Web Angular

| ID | Gravité | Constat | Statut | Correctif ou suite |
|---|---|---|---|---|
| UX-01 | Bloquant | Une erreur de chargement des allergies, antécédents, examens ou lits s'affiche comme « aucune donnée ». | ✅ | État d'erreur avec « Réessayer ». Clôture et ordonnance bloquées tant que les allergies ne sont pas chargées. |
| UX-02 | Bloquant | QR code patient généré par un service tiers ; validité calculée côté client. | ⏳ B6 | — |
| UX-03 | Majeur | Saisie perdue en quittant l'écran. | ⏳ C2 | — |
| UX-04 | Majeur | Actions irréversibles sans confirmation. | ✅ | Confirmation avec récapitulatif pour la clôture, la délivrance, la validation labo et la fermeture d'un lit. Focus sur « Annuler » en ton danger. Libellé explicite « Enregistrer l'orientation ». |
| UX-05 | Majeur | Triage pré-rempli RED / SHOCK / AMBULANCE. | ✅ | Valeurs vides et obligatoires ; ancien brouillon neutralisé. |
| UX-06 | Majeur | Redirection vers la vitrine ; le guard laisse passer sur erreur réseau. | ✅ | `/login` avec `returnUrl` (URL internes uniquement) ; page « Serveur indisponible » avec « Réessayer » ; fil d'Ariane vers le tableau de bord du rôle. |
| UX-07 | Majeur | Navigation ambiguë, caisse en double. | ⏳ C1 | — |
| UX-08 | Majeur | Pas de page 404 ; lien mort `/legal/consent`. | ✅ | — |
| UX-09 | Majeur | i18n incomplète, clés brutes affichées. | ◐ | Clés brutes corrigées ; `lang` du document suit la langue. Reste : C3. |
| UX-10 | Majeur | Montants « 125,000 FCFA », dates au format US. | ◐ | Locale `fr`, pipes `xaf` et `appDate` (`dd/MM/yyyy`). Reste : 18 montants de facturation et changement de locale à chaud. |
| UX-11 | Majeur | Contrastes insuffisants en thème clair. | ✅ | `--text-muted` 5,4:1, primaire 5,0:1 (DESIGN.md mis à jour). |
| UX-12 | Majeur | Accessibilité des formulaires et des interactions. | ◐ | Boutons icône libellés ; sélection du dossier principal en `radiogroup` ; accordéons en `<button>` ; lien Profil dans le tiroir mobile. Reste : C5. |
| UX-13 | Majeur | Erreurs avalées. | ✅ | Messages et « Réessayer » ; erreurs de facturation persistantes ; `console.error` d'URL patient retirés. |
| UX-14 | Majeur | Contrôles métier tenus par le front (double visa, horodatages). | ⏳ B7 | — |
| UX-15 à UX-18 | Mineur | Design system, chargement i18n, sécurité front, divers. | ⏳ C3, C4 | — |

## 5. Mobile Flutter

| ID | Gravité | Constat | Statut | Correctif ou suite |
|---|---|---|---|---|
| MOB-01 | Élevée | Jeton dans l'URL WebSocket et dans les logs. | ◐ | Logs expurgés et limités au mode debug. Reste : ticket WebSocket côté serveur (B5). |
| MOB-02 | Élevée | Une coupure réseau pendant le refresh efface la session sans prévenir. | ✅ | Seul un 401/403 expire la session ; l'interface est prévenue. |
| MOB-03 | Élevée | La dictée échoue après 15 min (jeton figé). | ✅ | Le jeton est relu et rafraîchi à chaque connexion. |
| MOB-04 | Élevée | La file active du médecin précédent reste visible après déconnexion. | ✅ | État lié à la session. |
| MOB-05 | Élevée | Captures d'écran non bloquées. | ✅ | `FLAG_SECURE` sur Android. Masque iOS : C6. |
| MOB-06 | Élevée | Sauvegarde Android activée ; PDF de résultats en clair et jamais effacés. | ✅ | Sauvegarde désactivée ; PDF temporaires purgés à la déconnexion. |
| MOB-07 | Élevée | L'écran de verrouillage plante pendant une dictée. | ✅ | Verrou en surcouche dans `MaterialApp.router`, test widget à l'appui. |
| MOB-08, MOB-09 | Élevée / moyenne | Release signée avec la clé debug, pas d'obfuscation ; verrouillage faible. | ⏳ C6 | **Bloquant avant toute diffusion d'APK.** |
| MOB-10 | Moyenne | Saisie perdue au verrouillage. | ◐ | Le verrou en surcouche ne détruit plus la saisie. Brouillons : C2. |
| MOB-11, MOB-12 | Moyenne | Codes techniques affichés ; annuaire patients chargé en entier. | ⏳ C7 | — |
| MOB-13 | Moyenne | Brouillons de dictée conservés après déconnexion. | ✅ | — |
| MOB-19 | Faible | Clavier qui apprend les saisies ; nom de l'app. | ◐ | Apprentissage du clavier désactivé sur les champs cliniques ; nom « Joprelys Connect » sur Android et iOS. |
| MOB-14 à MOB-18, MOB-20 | Moyenne / faible | Dictée en arrière-plan, i18n, accessibilité, code mort, dépendances. | ⏳ C3, C4, C5 | — |

## 6. Vérifications

Toutes les commandes ont été lancées sur la branche. Les résultats chiffrés sont dans le [ticket](../../ai/tickets/AUDIT-20261104-FULL-SECURITY-CLINICAL-UX.md#vérifications).

**Non testé** :
- rendu réel dans un navigateur et sur un appareil (contraste perçu, focus clavier, tiroir mobile, verrou pendant une dictée, `FLAG_SECURE`) ;
- migration V121 sur PostgreSQL ;
- comportement multi-instance.

## 7. Points vérifiés et corrects (inutile de les ré-auditer)

- **JWT** : HS256 imposé, `iss`/`aud`/`exp` vérifiés, autorités relues en base.
- **Refresh token** : `SecureRandom`, haché, rotation avec détection de rejeu.
- **Autorisations** : `@PreAuthorize` sur tous les endpoints métier ; multi-établissement par `@TenantId`.
- **Injection et fichiers** : aucune injection SQL ou JPQL ; protection contre le path traversal et vérification des octets magiques à l'upload.
- **Dossier clinique** : consultation et ordonnance scellées après signature ; administration idempotente avec fenêtre de sécurité et forçage motivé ; résultats validés versionnés ; décès avec heure future refusée et révocation motivée.
- **IA** : citations exigées et validation explicite du médecin.
- **Mobile** : jetons dans le Keystore, audio jamais écrit sur disque, HTTPS imposé.
- **Web** : architecture standalone et signals, appels API relatifs, aucun Angular Material, tokens d'arrondi conformes.

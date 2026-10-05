# BUG-20261005-LANDING-CONTACT-DELIVERY

Date : 2026-10-05. Modes : Diagnostic + Engineering. Priorité : P1.
Objectif : rendre fiable la demande de démonstration publique et corriger les contacts.
Profil : full-stack. Reviewer : Tech Lead + utilisateur. Estimation indicative : 0,5–1 jour ; correction bornée hors sprint, aucun engagement de capacité.

## Diagnostic

- Angular absorbe les erreurs HTTP puis affiche un succès même avec une réponse nulle.
- Le stockage local de coordonnées ne dispose d'aucun mécanisme de synchronisation et ne constitue pas un envoi.
- Le backend persiste les demandes sans notifier l'équipe.
- Le numéro WhatsApp est dupliqué et incorrect ; le footer utilise une autre adresse mail.

## Critères d'acceptation

- Tous les contacts de la landing visent +237691501780 et contact@joprelys.com via configuration centrale.
- Une demande valide est persistée ; une notification SMTP est tentée après commit, avec contenu et référence.
- Une panne SMTP n'annule pas l'enregistrement ; aucun email n'est envoyé sur rollback.
- Une panne HTTP n'affiche jamais de succès : saisie conservée, reprise et alternatives disponibles en FR/EN.
- Pas de coordonnées dans le localStorage ou les logs ; validation serveur conservée et email optionnel validé.

## Actions

- [x] Lire gouvernance, standards, design et analyser le flux existant.
- [x] Créer spécification et conception avant code.
- [x] Corriger contacts, états et validation Angular.
- [x] Ajouter notification SMTP après commit et validation email serveur.
- [x] Tester succès, HTTP/SMTP en erreur, validation, commit/rollback et doubles clics.
- [x] Exécuter tests ciblés, build et i18n ; documenter preuves et limites.
- [x] Mettre à jour suivi, changelog et checklist.
- [ ] Déployer puis vérifier la réception SMTP et le rendu navigateur.

Statut : READY_FOR_REVIEW — correctif local et vérifications automatisées terminés.

## Versionnement demandé par l'utilisateur

Le 2026-10-05, commit et push autorisés sur `main`. Lot limité au présent correctif, tests et documentation ; travaux teaser exclus. Base locale et GitHub vérifiée : `59d2d12ac36daad6e6ca50a0e38ff95948373f8d`. L'environnement refusait initialement l'écriture de `.git/index.lock` : publication via le connecteur GitHub du commit `59fc39d8783b12373d5f79cc9e318f5195c54f9c`, vérifié sur la branche distante.

Après levée des restrictions et demande « tente le commit local », `git fetch` et `git add` ont réussi. L'arbre indexé a été vérifié identique au commit publié (`3438095b95f4397954c13cb7f2e6cf9bfe3581db`). `git commit` a recréé localement le même commit `59fc39d8` en conservant ses métadonnées : pas de doublon ni de divergence du correctif. Synchronisation locale résolue ; ce complément de suivi est versionné séparément. Aucun code nouveau, aucune release/version/tag préparé ; tests précédents toujours applicables.

## Preuves

### Complément UX mobile — capture utilisateur du 2026-10-05

Diagnostic : le CTA de soumission affiche un libellé long sur une largeur mobile contrainte, avec `white-space: nowrap`, centrage flex et padding trop faible. Son texte dépasse de part et d'autre. L'utilisateur demande la suppression du bouton e-mail du formulaire et un CTA plus aéré.

Critères : aucun bouton e-mail dans la carte du formulaire ; CTA principal intégralement visible, padding intérieur et autour de l'action, cible tactile ≥44px, libellé concis FR/EN sans retour à la ligne ; WhatsApp et envoi API existants préservés. Vérifier 320/360/390px et desktop en light/dark. Le mail de notification et le contact du footer restent applicables.

- [x] Examiner capture utilisateur, styles et standards de boutons ; actualiser specs avant code.
- [x] Supprimer le CTA e-mail, raccourcir les libellés et ajouter le padding.
- [x] Adapter les assertions de parcours et vérifier tests/build/i18n/rendu mobile.
- [x] Actualiser suivi et changelog avec les résultats.

Correction bornée Angular, sans impact API/DB/SMTP/Flutter/CI ni capacité sprint ; candidat PATCH, aucun bump applicatif.

### Complément mail — capture utilisateur du 2026-10-05

Diagnostic : la notification des demandes emploie `SimpleMailMessage` et ignore le gabarit HTML existant des mails de compte. La capture confirme la réception réelle du mail, mais sa présentation est brute et la fonction apparaît comme un code (`directeur`).

Critères : reprendre le même logo inline, carte, bandeau cyan et footer que les mails existants ; afficher la référence et les coordonnées en lignes lisibles, fonction libellée, message préservé ; HTML avec alternative texte UTF-8, champs visiteurs échappés, aucun script/HTML injecté. Destinataire, persistance et notification après commit inchangés. Extraire le cadre et l'envoi MIME partagés pour éviter deux chartes divergentes ; mails de compte conservés.

- [x] Lire la structure des mails existants et documenter l'impact avant code.
- [x] Réutiliser le cadre/envoi des mails et créer le contenu de demande de démonstration.
- [x] Adapter les tests MIME, échappement HTML, compte et commit/rollback.
- [x] Vérifier le rendu mail avec données fictives et mettre à jour le suivi.

Spring Boot/Maven concerné, aucune dépendance/migration/API nouvelle. OWASP : HTML échappé et en-têtes non contrôlés par le visiteur ; configuration SMTP 12-Factor conservée. Candidat PATCH ; recette de clients mail à confirmer après déploiement.

- Angular : 120 fichiers / 663 tests verts ; build production et i18n verts.
- Maven wrapper hors ligne dans cache workspace : 19 tests ciblés verts, aucun échec/erreur/skip ; tous les sources et tests backend compilés, intégration H2 et SMTP simulé.
- `git diff --check` vert ; `.gitignore` vérifié, caches/artefacts ignorés. Aucun script lint existant.
- Premier essai Maven bloqué par réseau/cache ; résolu par copie du cache machine dans `.m2-test-cache`. Premier test Angular email ajusté pour reproduire de vrais événements input, puis suite complète verte.

## Impacts

Angular et Spring Boot concernés ; endpoint/response existants conservés, aucune migration, Flutter et CI/CD inchangés. OWASP : validations serveur, HTML utilisateur échappé et alternative texte, destinataire fixé par configuration, pas de fuite de coordonnées dans les logs. 12-Factor : SMTP et destinataire externalisés sans secret nouveau. SemVer : candidat PATCH 0.10.2 depuis VERSION 0.10.1 ; aucun bump/release demandé.

## Reste à faire

Review et déploiement, puis recette SMTP réelle et navigateur mobile/desktop FR/EN light/dark. Vérifier les paramètres `MAIL_*` et `JOPRELYS_CONTACT_EMAIL`. Pas d'outbox/retry automatique ; panne SMTP à traiter depuis la demande persistée. Dette template/CSS landing préexistante hors correctif, >500 lignes.

## Fichiers concernés

- Angular : `web/src/app/core/config/app-brand.config.ts`, `web/src/app/landing/landing-demo.service{,.spec}.ts`, `landing-page.component.{ts,html,spec.ts}`, `landing-i18n.ts`.
- Backend : module `lead` (controller, DTO, service, contrat RegisterDemoRequest, événement, port/listener et implémentation SMTP), `notification/infrastructure/mail/MailSenderConfig.java`, `src/main/resources/application.yml`.
- Tests : `PublicDemoRequestControllerTest`, `SmtpDemoRequestNotificationTest`, `MailSenderConfigTest`.
- Documentation : ce ticket, `docs/features/landing-contact-delivery/`, ADR après commit, suivi, changelog et checklist review.

## Preuves des compléments UI / mail

16 tests Angular ciblés, build production et i18n verts ; 22 tests Maven ciblés verts, aucun skip. Contrôle Chromium du CTA : 16 scénarios 320/360/390/1280px, FR/EN, light/dark, cible 44px et texte dans le padding. Aperçu du mail généré par les factories Java : 6 scénarios FR/EN, 320/390/900px sans débordement et logo présent ; captures inspectées. Tests MIME, échappement, rôles, alternative texte et commit/rollback verts. Voir TEST-PLAN pour commandes, preuves locales et ajustements des fixtures.

Compléments bornés UI et infrastructure mail existante : aucun changement API/DB/Flutter/CI, aucun impact de capacité sprint. Candidat PATCH sans bump ni release. Reste : review, déploiement web/backend et recette du nouveau mail dans les clients réels.

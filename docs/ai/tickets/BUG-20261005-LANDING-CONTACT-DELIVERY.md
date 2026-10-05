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

Le 2026-10-05, commit et push autorisés sur `main`. Lot limité au présent correctif, tests et documentation ; travaux teaser exclus. Base locale et GitHub vérifiée : `59d2d12ac36daad6e6ca50a0e38ff95948373f8d`. L'environnement refuse l'écriture de `.git/index.lock` : publication via le connecteur GitHub, sans modification des métadonnées Git locales. Le résultat de publication est vérifié sur la branche distante et fourni dans le compte rendu final. La synchronisation du dépôt local reste à effectuer dans un environnement autorisant les écritures Git ; aucune release/version/tag préparé.

## Preuves

- Angular : 120 fichiers / 663 tests verts ; build production et i18n verts.
- Maven wrapper hors ligne dans cache workspace : 19 tests ciblés verts, aucun échec/erreur/skip ; tous les sources et tests backend compilés, intégration H2 et SMTP simulé.
- `git diff --check` vert ; `.gitignore` vérifié, caches/artefacts ignorés. Aucun script lint existant.
- Premier essai Maven bloqué par réseau/cache ; résolu par copie du cache machine dans `.m2-test-cache`. Premier test Angular email ajusté pour reproduire de vrais événements input, puis suite complète verte.

## Impacts

Angular et Spring Boot concernés ; endpoint/response existants conservés, aucune migration, Flutter et CI/CD inchangés. OWASP : validations serveur, email texte brut, destinataire fixé par configuration, pas de fuite de coordonnées dans les logs. 12-Factor : SMTP et destinataire externalisés sans secret nouveau. SemVer : candidat PATCH 0.10.2 depuis VERSION 0.10.1 ; aucun bump/release demandé.

## Reste à faire

Review et déploiement, puis recette SMTP réelle et navigateur mobile/desktop FR/EN light/dark. Vérifier les paramètres `MAIL_*` et `JOPRELYS_CONTACT_EMAIL`. Pas d'outbox/retry automatique ; panne SMTP à traiter depuis la demande persistée. Dette template/CSS landing préexistante hors correctif, >500 lignes.

## Fichiers concernés

- Angular : `web/src/app/core/config/app-brand.config.ts`, `web/src/app/landing/landing-demo.service{,.spec}.ts`, `landing-page.component.{ts,html,spec.ts}`, `landing-i18n.ts`.
- Backend : module `lead` (controller, DTO, service, contrat RegisterDemoRequest, événement, port/listener et implémentation SMTP), `notification/infrastructure/mail/MailSenderConfig.java`, `src/main/resources/application.yml`.
- Tests : `PublicDemoRequestControllerTest`, `SmtpDemoRequestNotificationTest`, `MailSenderConfigTest`.
- Documentation : ce ticket, `docs/features/landing-contact-delivery/`, ADR après commit, suivi, changelog et checklist review.

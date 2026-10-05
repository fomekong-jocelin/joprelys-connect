# Vérification — BUG-20261005-LANDING-CONTACT-DELIVERY

## Automatisation

- Angular : réponse succès avec référence, panne HTTP sans faux succès et reprise, réponse nulle, timeout, absence de nouvelle persistance navigateur, double soumission, champs blancs, email optionnel invalide/vide, contacts accessibles avant soumission, WhatsApp FR/EN.
- Backend H2 : accès public et champs obligatoires, validation email, notification seulement après commit, aucune notification sur rollback, demande conservée si SMTP échoue.
- SMTP mock : destinataire par défaut/surchargé, contenu complet et champs optionnels, objet/headers indépendants de la saisie, propagation de l'erreur vers listener. Configuration SMTP : defaults et overrides SSL/STARTTLS/timeouts préservés.

Commandes : `npm.cmd test -- --watch=false`, `npm.cmd run build`, `npm.cmd run i18n:check` dans web ; Maven wrapper avec `-Dtest=PublicDemoRequestControllerTest,SmtpDemoRequestNotificationTest,MailSenderConfigTest,SmtpAccountMailServiceTest,AccountMailTemplateFactoryTest`.

## Preuves disponibles

- Angular : 120 fichiers / 663 tests verts ; build production et i18n (47 clés shell) verts.
- Pas de script lint dans package.json ; TypeScript compilé par build/tests.
- Premier essai Maven bloqué par réseau ; cache initial incomplet. Cache machine copié dans `.m2-test-cache` ignoré : 19 tests ciblés verts, zéro échec/erreur/skip, compilation complète des sources/tests et intégration H2.
- `git diff --check` vert ; caches et builds ignorés. Recette navigateur/SMTP réelle non effectuée, suite Maven exhaustive/PostgreSQL non lancée car le changement est couvert par les tests ciblés.

## Recette déployée à effectuer

Vérifier `MAIL_HOST/PORT/USERNAME/PASSWORD`, destinataire `JOPRELYS_CONTACT_EMAIL=contact@joprelys.com` et réception effective ; ne pas mettre les identifiants SMTP dans Git. Tester en FR/EN, light/dark, mobile/desktop : formulaire valide, email invalide, panne API/reprise, lien WhatsApp prérempli vers +237691501780 et mailto. Aucun envoi réel réalisé par les tests.

Une erreur SMTP conserve la demande mais ne déclenche pas de nouvelle tentative automatique : retrouver la référence dans `demo_requests` et traiter la demande en exploitation. Des anciens backups navigateur éventuels ne sont pas envoyés automatiquement et ne sont pas supprimés par le correctif.

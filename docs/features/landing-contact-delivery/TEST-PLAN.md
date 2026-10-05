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

## Compléments UI et mail — vérifications locales finales

- Angular ciblé : 2 fichiers / 16 tests verts après changement du CTA, absence du bouton e-mail et maintien du mail footer/WhatsApp. Build production et i18n (47 clés shell) verts. La suite historique de 663 tests ci-dessus n'a pas été relancée pour ce complément borné.
- Maven ciblé : 22 tests, zéro échec/erreur/skip, sur les cinq classes listées plus `DemoRequestMailTemplateFactoryTest`. Compilation complète sources/tests, H2 commit/rollback, panne SMTP ; contenu MIME HTML/texte UTF-8, logo CID, destinataires/en-têtes fixes, rôle traduit et champs échappés. Mails de compte et configuration SMTP couverts par leurs tests existants.
- Playwright/Chromium sur build local : 16 scénarios, largeurs 320/360/390/1280px × FR/EN × light/dark. Mesures : texte dans le padding, cible ≥44px, espace autour, aucun mailto dans la carte. Pas d'appel API réel ; réponses simulées.
- Mails fictifs produits par les vraies factories Java : FR/EN à 320/390/900px, logo chargé et aucun débordement horizontal ; captures mobile/desktop inspectées. Preuves locales ignorées : `.ai-tmp/landing-ui-metrics.json`, `landing-cta-mobile.png`, `mail-render-metrics.json`, `demo-mail-fr-{390,900}.png`.
- Premier contrôle mobile trop serré à 320px : libellé raccourci puis contrôle repris. Premier test MIME optionnel lisait le message avant finalisation de ses headers : fixture corrigée avec `saveChanges()`, suite reprise verte.

La réception de l'ancien mail est confirmée par la capture utilisateur. Aucun envoi SMTP réel pendant ces vérifications ; rendu Outlook/Gmail/webmail du nouveau message à recetter après déploiement. Le lien e-mail à vérifier est désormais celui du footer.

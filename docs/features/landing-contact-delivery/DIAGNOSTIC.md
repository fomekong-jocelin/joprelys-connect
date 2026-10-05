# Diagnostic — formulaire public et contacts

Ticket : BUG-20261005-LANDING-CONTACT-DELIVERY, 2026-10-05.

## Causes observées dans le code initial

1. `LandingDemoService.submitDemo` interceptait les erreurs HTTP avec `of(null)` ; `LandingPageComponent.submitDemoRequest` passait ensuite `demoSuccess` à true indépendamment du résultat. Même son callback d'erreur affichait un succès.
2. Le backup `joprelys_demo_leads_backup` stockait les coordonnées sur l'appareil du visiteur ; aucun flux de synchronisation n'existait. Cette sauvegarde ne pouvait pas notifier Joprelys.
3. `DemoRequestService` enregistrait en base et dans les logs les coordonnées, sans appel de notification.
4. Le numéro 237691893198 était dupliqué dans le service et le composant. Le footer utilisait support@joprelys.com au lieu du contact fourni.

## Correction et limites

L'API reste la source de vérité pour l'enregistrement ; les erreurs sont visibles et permettent la reprise. Le contact landing est centralisé, les coordonnées ne sont plus ajoutées au stockage local ni aux logs des demandes. Une notification SMTP est tentée après commit, jamais sur rollback.

Les liens WhatsApp/mail restent des actions utilisateur. L'enregistrement et la livraison SMTP sont distincts. Les identifiants SMTP et la réception réelle en production ne peuvent pas être prouvés par les tests avec sender simulé ; recette déployée requise. Les éventuels backups historiques des visiteurs restent sur leurs appareils, sans envoi automatique. Le rate limiting serveur existant est activé par défaut hors profil test ; aucun contrôle d'accès existant n'est affaibli.

## Captures complémentaires du 2026-10-05

La capture mobile révèle le débordement du long libellé de soumission : `nowrap` et centrage flex ne suffisent pas lorsque le texte dépasse la zone disponible. Padding d'action 8px, padding intérieur 16px et libellé FR concis corrigent le problème sans retour à la ligne. Un premier contrôle à 320px a montré que « Demander une démo gratuite » restait trop long ; le libellé final « Demander une démo » est mesuré sans débordement. Le bouton e-mail du formulaire est retiré selon la demande ; le contact du footer reste disponible.

La capture de réception démontre que le SMTP fonctionne sur l'environnement utilisateur, mais le contenu initial `SimpleMailMessage` ignore la charte HTML des mails de compte. La correction réutilise leur cadre et transport MIME avec logo inline, fournit le libellé de fonction et échappe les champs visiteurs. Aperçus fictifs FR/EN vérifiés à 320/390/900px ; la vérification du nouveau mail dans les clients réels reste après déploiement.

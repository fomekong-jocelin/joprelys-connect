# ADR — notification de démonstration après commit

Date : 2026-10-05. Statut : retenu pour ce correctif local. Ticket : BUG-20261005-LANDING-CONTACT-DELIVERY.

La demande publique doit rester enregistrée même si le SMTP échoue. Décision : publier un événement immuable dans la transaction, puis tenter la notification après commit avec l'infrastructure mail existante, un port dédié et délais réseau bornés. Aucun email sur rollback ; logs limités à l'identifiant.

L'API 201 confirme la persistance, pas la livraison du mail. En cas de panne SMTP, la demande reste dans demo_requests et doit être traitée par l'exploitation ; pas de retry automatique. Une outbox durable serait adaptée à une garantie de livraison future, mais impose migration/worker et dépasse ce correctif. Aucun contrat cassant ni nouvelle dépendance.

Complément du 2026-10-05 : la notification réutilise maintenant le cadre HTML et l'envoi MIME des mails de compte, extraits dans `BrandedMailTemplateFactory` et `BrandedSmtpMailSender`. Les corps de compte et de démonstration restent dédiés à leur usage ; aucun changement du contrat applicatif ou du moment de notification. Alternative texte, logo CID et échappement HTML conservés. Pas de nouvelle infrastructure ni dépendance.

# TASK-20260717-PREMIUM-ACCOUNT-EMAILS — E-mails transactionnels premium

**Mode** : Engineering + Product Design + QA Review

**Date** : 2026-07-17

**Statut** : READY FOR DEPLOYMENT

**Priorité** : P1

**Estimation** : 0,5 j senior

**Profil recommandé** : Backend Spring Boot + Email Design

**Reviewer** : Lead Backend + Product Design + QA

**Impact version** : PATCH

## Objectif

Remplacer les e-mails texte bruts de compte, connexion et récupération par des e-mails HTML professionnels, accessibles et cohérents avec la marque Joprelys Connect.

## Critères d'acceptation

- [x] Logo officiel intégré sans dépendre du chargement d'une image distante.
- [x] Mise en page responsive compatible avec les principaux webmails.
- [x] Code ou mot de passe temporaire clairement identifiable.
- [x] Message de sécurité et durée d'expiration visibles.
- [x] Version texte alternative conservée.
- [x] Données utilisateur échappées contre l'injection HTML.
- [x] Les quatre parcours d'e-mail utilisent le modèle partagé.
- [ ] Envoi réel et rendu webmail validés après déploiement.

## Actions

- [x] Créer une fabrique de modèles partagée.
- [x] Intégrer le logo officiel en pièce jointe inline.
- [x] Adapter l'envoi SMTP multipart texte/HTML.
- [x] Ajouter les tests de contenu et de sécurité.
- [x] Mettre à jour la documentation et le suivi.

## Reste à faire

- Déployer le backend.
- Générer un e-mail de récupération et un code de connexion réels.
- Contrôler le rendu dans le webmail Joprelys sur desktop et mobile.

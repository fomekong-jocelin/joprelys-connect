# Review checklist — Permissions dynamiques UI/API

## Architecture et responsabilités

- ✅ Le backend reste l'autorité finale et résout les permissions en base à chaque requête.
- ✅ Une politique Angular partagée aligne les menus et les routes composites.
- ✅ Les sous-composants vérifient leurs permissions exactes avant affichage et avant appel.
- ✅ Aucun contrat de payload, modèle de données ou migration n'est modifié.
- ⚠️ `LabOrdersPageComponent` dépassait déjà 500 lignes et reste à extraire avant évolution
  fonctionnelle ; le correctif P0 est volontairement borné aux contrôles d'accès.

## Sécurité

- ✅ `MEDECIN` ne reçoit plus de droit de facturation ou de caisse par défaut.
- ✅ Aucun droit n'est déduit d'une autre permission côté Angular.
- ✅ `LAB_QUEUE_READ`, `LAB_ORDER_READ` et `LAB_ORDER_WRITE` restent trois capacités distinctes.
- ✅ Les actions de caisse, relance, assurance, export et résolution d'écart sont séparées.
- ✅ Les méthodes de composants refusent aussi l'action lorsqu'elles sont appelées hors UI.
- ✅ Les rôles personnalisés suivent les mêmes permissions que les rôles système.
- ✅ Les tests financiers ne réintroduisent aucun privilège financier au médecin.

## Standards techniques

- ✅ Maven, Spring Security, Angular 22, signaux et Tailwind v4 existants conservés.
- ✅ Aucun Angular Material, Tailwind v3, URL backend codée en dur ou secret ajouté.
- ✅ `application.yml`, `proxy.conf.json` et `.gitignore` vérifiés.
- ➖ DB / migration : aucune évolution.
- ➖ Design visuel : aucun changement de thème, branding ou token.

## Preuves

- ✅ Angular : 56 fichiers, 267 tests, 0 échec.
- ✅ Maven : 453 tests, 0 échec, 0 erreur, 1 ignoré.
- ✅ Build Angular production : vert.
- ✅ i18n shell : 47 clés FR/EN présentes.
- ✅ `git diff --check` : vert.

## Livraison

- ✅ Ticket, diagnostic, spécifications, matrice, plan de tests, changelog, suivi, planning,
  risque et revue sécurité mis à jour.
- ✅ Impact SemVer : PATCH ; `VERSION` inchangé car aucune release n'est préparée.
- ⚠️ Recette humaine multi-rôles et validation RSSI/Tech Lead encore requises avant déploiement.

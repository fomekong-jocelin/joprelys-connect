# QA REVIEW WORKFLOW

## 1. Objectif

Vérifier qu'une tâche est réellement prête à merge/livrer.

## 2. Étapes

1. Lire le ticket.
2. Vérifier les critères d'acceptation.
3. Lire les fichiers modifiés.
4. Vérifier les tests.
5. Vérifier sécurité/régression.
6. Vérifier documentation/changelog.
7. Classer les commentaires P0 à P3.

## 3. Sévérité

| Sévérité | Signification | Action |
|---|---|---|
| P0 | Bloquant sécurité/régression | Rejet immédiat |
| P1 | Problème majeur | Corriger avant merge |
| P2 | Problème mineur | Corriger si simple ou créer ticket |
| P3 | Nit/style | Optionnel |

## 4. Rapport attendu

```markdown
## Verdict
APPROVED / CHANGES_REQUESTED / BLOCKED

## P0

## P1

## P2

## P3

## Tests vérifiés

## Risques restants
```


## Vérifications standards imposés

- Backend Spring Boot : vérifier Maven uniquement (`pom.xml`, `mvnw`) et refuser tout ajout Gradle non justifié par ADR.
- Frontend Angular : vérifier Tailwind CSS et refuser tout ajout Angular Material (`@angular/material`, `Mat*`, `mat-*`, thème Material) non justifié par ADR.
- Backend Spring Boot : vérifier `application.yml` / profils YAML et refuser tout nouveau `application.properties` non justifié par ADR.
- Frontend Angular : vérifier `proxy.conf.json`, `proxyConfig` dans `angular.json` et l’absence d’URL backend hardcodée dans les services.

## Contrôle obligatoire Frontend/Mobile

Pour toute PR Angular ou Flutter, le reviewer doit vérifier :

- intégration au thème central ;
- rendu light et dark ;
- absence de textes visibles hardcodés ;
- présence des traductions `fr` et `en` ;
- absence de nom d’app/logo/branding hardcodé ;
- usage de composants/widgets réutilisables ;
- cohérence des états loading/empty/error ;
- respect Tailwind côté Angular et absence d’Angular Material non justifié.

## Review documentaire obligatoire

La QA/review doit vérifier la documentation autant que le code.

Bloquer la PR/MR si :

- la documentation fonctionnelle n’est pas créée ou mise à jour ;
- la documentation technique n’est pas créée ou mise à jour ;
- les contrats API, migrations, configuration, i18n, thème ou branding ne sont pas documentés alors qu’ils sont impactés ;
- le changelog ou la décision SemVer manque pour un changement livrable.

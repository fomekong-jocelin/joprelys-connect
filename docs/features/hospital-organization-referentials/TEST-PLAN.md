# TEST-PLAN — HOS-ORG-001-A

## Backend domaine/service

- création directe d'un SERVICE à la racine ;
- création POLE → DEPARTMENT → SERVICE → CARE_UNIT ;
- rejet des parents invalides ;
- rejet d'un cycle ;
- rejet parent cross-tenant ;
- rejet code dupliqué dans le tenant ;
- même code autorisé dans deux tenants distincts ;
- SERVICE sans catalogue refusé ;
- SERVICE avec `name` libre refusé ;
- POLE/DEPARTMENT/CARE_UNIT avec catalogue refusé ;
- catalogue service inconnu/inactif refusé ;
- SERVICE persisté avec `name = null` et identité par code catalogue ;
- désactivation avec enfant actif refusée ;
- activation avec parent inactif refusée.

## API / sécurité

- utilisateur sans `ORGANIZATION_STRUCTURE_MANAGE` : lecture/mutation 403 ;
- admin clinique : lecture/mutation autorisée dans son tenant ;
- rôles médecin/infirmier/responsable hospitalisation : pas de permission par défaut ;
- permission non classée platform-only ;
- parent autre tenant : refus sans fuite ;
- validation Bean Validation : 400 ;
- conflit code : 409 ;
- liste explicitement tenant-isolée.

## Migration V87

PostgreSQL 16 :

- migration V1→V87 greenfield ;
- migration V86→V87 ;
- 14 entrées initiales service et 10 spécialités ;
- contraintes FK composites ;
- même code d'unité autorisé dans des tenants différents ;
- SERVICE sans catalogue refusé ;
- SERVICE avec nom localisé/free-text refusé au niveau DB ;
- autre type avec catalogue refusé ;
- aucune modification des anciennes colonnes `users.department`, `users.specialty` ou de `wards`.

## Angular

- chargement catalogues + unités ;
- création SERVICE depuis select catalogue ;
- absence de champ de nom libre pour SERVICE ;
- payload SERVICE avec `name: null` ;
- affichage du service en FR puis EN à partir du catalogue ;
- création POLE/DEPARTMENT/CARE_UNIT avec nom requis ;
- CARE_UNIT sans parent service non soumissible ;
- affichage hiérarchique ;
- désactivation/réactivation ;
- erreurs API affichées sans stack trace ;
- route et navigation conditionnées par `ORGANIZATION_STRUCTURE_MANAGE` ;
- thème light/dark via tokens ;
- 320/375 : formulaires et actions empilés, aucun débordement ;
- 768/1366 : hiérarchie lisible ;
- clavier : labels, focus, boutons accessibles.

## Suites obligatoires

Backend :

```bash
./mvnw clean verify -B --no-transfer-progress -Dspring.profiles.active=test
```

Frontend :

```bash
npm ci --prefer-offline --no-audit --fund=false
npm test -- --watch=false
npm run build
```

Aucun `-DskipTests` ni `-Dmaven.test.skip`.

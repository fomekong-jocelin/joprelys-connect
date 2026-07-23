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
- POLE/DEPARTMENT/CARE_UNIT avec catalogue refusé ;
- service catalogue inconnu/inactif refusé ;
- nom d'un SERVICE dérivé du catalogue ;
- désactivation avec enfant actif refusée ;
- activation avec parent inactif refusée.

## API / sécurité

- utilisateur sans `ORGANIZATION_STRUCTURE_MANAGE` : mutation 403 ;
- admin clinique : mutation autorisée dans son tenant ;
- payload avec UUID d'un parent autre tenant : 404/403 sans fuite ;
- validation Bean Validation : 400 ;
- conflit code : 409 ;
- liste tenant-isolée.

## Migration V87

PostgreSQL 16 :

- migration V1→V87 greenfield ;
- migration V86→V87 ;
- contraintes FK composites ;
- check SERVICE/catalogue ;
- seed catalogue sans doublon ;
- aucune modification des anciennes colonnes `users.department`, `users.specialty`, `wards`.

## Angular

- chargement catalogues + unités ;
- création SERVICE depuis select catalogue ;
- création POLE/DEPARTMENT/CARE_UNIT avec nom requis ;
- affichage hiérarchique ;
- désactivation/réactivation ;
- erreurs API affichées sans stack trace ;
- permission : actions masquées/désactivées sans permission ;
- FR et EN ;
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

# TEST-PLAN — HOS-LOC-001-A

## 1. Migration / PostgreSQL

### V88 preflight

- base greenfield V1→V87 : V88 passe ;
- une ligne `wards` : V88 échoue avant mutation ;
- une ligne `rooms` : V88 échoue ;
- une ligne `beds`/`bed_assignments` : V88 échoue ;
- une ligne `hospitalizations` : V88 échoue ;
- message explique reset/remapping contrôlé ;
- aucune table/colonne du nouveau modèle n'existe après échec V88.

### V89 modèle

- catalogues seedés ;
- hiérarchie tenant-safe ;
- parent géographique cross-tenant refusé ;
- code location/space unique par tenant ;
- même code autorisé dans deux tenants ;
- `SPACE` sans localisation accepté ;
- type inconnu refusé ;
- inpatient profile impossible pour un type non autorisé ;
- bed impossible sans inpatient profile ;
- `beds.room_id` absent ; `beds.space_id` obligatoire ;
- `rooms` et `wards` absents ;
- hospitalizations possède current IDs + snapshots renommés ;
- FK current bed/space cohérente.

### V90 temporal exclusion

PostgreSQL 16 :

- périodes unit-space disjointes autorisées ;
- même couple avec chevauchement refusé ;
- couple différent sur même space simultané autorisé ;
- période ouverte puis nouvelle période chevauchante refusée.

## 2. Domaine géographique

- root SITE/BUILDING/FLOOR/ZONE accepté ;
- niveaux sautés acceptés ;
- parent de rang inférieur/égal refusé ;
- ZONE parent d'un nœud refusé ;
- cycle direct/indirect refusé ;
- parent inactif refusé ;
- activation sous parent inactif refusée ;
- désactivation avec descendants/espaces actifs selon règle documentée.

## 3. Spaces

- création directe sous établissement ;
- création sous location node ;
- type catalogue obligatoire ;
- inpatient profile HOSPITAL_ROOM/ICU_ROOM accepté ;
- inpatient profile consultation/attente/lab refusé ;
- désactivation avec lit actif/assignment courant refusée selon règle ;
- réactivation parent/type valides.

## 4. Unit-space assignments

- unit/space même tenant ;
- cross-tenant 404/403 ;
- validTo <= validFrom : 400 ;
- chevauchement même couple : 409 ;
- deux unités différentes sur même space : autorisé ;
- assignment expiré non considéré actif ;
- assignment futur non considéré actif.

## 5. Beds

- création par `spaceId` ;
- roomId absent des DTO ;
- lit dans consultation refusé ;
- duplicate bedNumber dans même space : 409 ;
- même numéro dans deux spaces : autorisé ;
- claim atomic existant toujours vert ;
- capacité/readiness/cleaning/maintenance non régressés ;
- historique bed assignment non régressé.

## 6. Admission

- request par `serviceUnitId/spaceId/bedId` ;
- aucun constructor/payload legacy ;
- service inexistant/inactif : 404/409 ;
- space inexistant/inactif : 404/409 ;
- assignment service-space absent/expiré : 409 ;
- bed d'un autre space : 409/404 ;
- tenant mismatch : 404 ;
- bed non disponible : 409 ;
- admission nominale crée IDs courants + snapshots dérivés ;
- texte utilisateur ne peut pas substituer service/space/bed ;
- urgence → visite auto : snapshot service dérivé du catalogue HOS-ORG.

## 7. Transfert

- payload target service/space/bed ;
- assignment cible actif obligatoire ;
- bed cible appartient au target space ;
- service-space partagé correctement distingué ;
- ancien lit passe nettoyage selon workflow ;
- nouvelle assignment bed active ;
- current IDs/snapshots séjour mis à jour ;
- concurrence sur bed cible : un seul transfert gagne ;
- décision de sortie bloque toujours le transfert.

## 8. API / RBAC

- `SPATIAL_CONFIGURATION_MANAGE` contrôle configuration ;
- permission ne crée/modifie aucune OrganizationalUnit ;
- métiers non autorisés : 403 ;
- lecture hospitalisation/capacité respecte permissions existantes ;
- routes Ward/Room supprimées : 404 ;
- endpoints bed state existants continuent de fonctionner.

## 9. Angular

### Configuration

- arbre locations ;
- niveaux sautés ;
- création space direct/root ;
- type catalog select ;
- profil hébergement affiché seulement si compatible ;
- lits par space ;
- assignments units ↔ spaces ;
- aucune UI Ward/Room ;
- aucun texte métier type en free-text.

### Admission

- sélection service/unité active ;
- espaces filtrés via assignment actif ;
- lits disponibles du space ;
- payload UUID ;
- changement service reset space/bed ;
- changement space reset bed ;
- absence de fallback string.

### UX

- 320/375 : aucune overflow horizontale, actions empilées ;
- 768 : master/detail utilisable ;
- 1366 : arbre + détail ;
- FR/EN ;
- light/dark ;
- focus visible, labels, Escape/modales ;
- erreurs localisées via mécanisme existant.

## 10. Recherche de dette résiduelle

Avant merge, recherches obligatoires :

```text
WardEntity
RoomEntity
SaveWardRequest
SaveRoomRequest
HospitalServiceType
serviceName dans CreateHospitalizationRequest
roomNumber dans CreateHospitalizationRequest
findConfiguredBed
/api/spatial/wards
/configuration/wards
/configuration/rooms
```

Toute occurrence restante doit être soit :

- un snapshot explicitement documenté ;
- une documentation historique archivée ;
- ou supprimée.

Aucun consommateur applicatif legacy actif n'est accepté.

## 11. Suites obligatoires

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

Interdits : `-DskipTests`, `-Dmaven.test.skip`, suppression/affaiblissement d'un test pour obtenir le vert.

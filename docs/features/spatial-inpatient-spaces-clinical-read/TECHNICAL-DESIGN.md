# TECHNICAL-DESIGN — Endpoint de consultation clinique des espaces d'hébergement

## 1. Séparation API Administration vs Consultation Opérationnelle

Dans le bounded context `spatial` :
- **Administration d'infrastructure** (`/api/spatial/configuration/*`) :
  - Porté par `HospitalLocationConfigurationController`, `HospitalBedConfigurationController`, `InpatientSpaceProfileController`.
  - Protégé par `@PreAuthorize("hasAuthority('SPATIAL_CONFIGURATION_MANAGE')")`.
- **Consultation clinique opérationnelle** (`/api/spatial/*`) :
  - Porté par `SpatialController`.
  - Protégé au niveau méthode par `@PreAuthorize("hasAnyAuthority('HOSPITALIZATION_READ', 'SPATIAL_CONFIGURATION_MANAGE')")`.

## 2. Nouvel Endpoint Backend `SpatialController`

```java
@GetMapping("/inpatient-spaces")
@PreAuthorize("hasAnyAuthority('HOSPITALIZATION_READ', 'SPATIAL_CONFIGURATION_MANAGE')")
public List<FacilitySpaceResponse> listInpatientSpaces(@RequestParam(required = false) UUID organizationId) {
    return spatialService.listInpatientSpaces(organizationId);
}
```

Implémentation dans `SpatialService` :
```java
@Transactional(readOnly = true)
public List<FacilitySpaceResponse> listInpatientSpaces(UUID organizationId) {
    UUID tenantId = tenantResolver.resolveTenant(organizationId);
    return spaceRepository.findAllByOrganizationIdAndActiveTrue(tenantId).stream()
            .filter(space -> space.getInpatientProfile() != null)
            .map(this::toSpaceResponse)
            .toList();
}
```

## 3. Adaptation Frontend Angular `SpatialApiService`

Ajout de la méthode `listInpatientSpaces()` dans `SpatialApiService` :
```typescript
listInpatientSpaces(organizationId?: string): Observable<FacilitySpace[]> {
  const params = this.scopeParams(organizationId);
  return this.http.get<FacilitySpace[]>('/api/spatial/inpatient-spaces', { params });
}
```

Et appel par `SpatialManagementPageComponent.loadSpaces()` pour charger les espaces d'hébergement actifs sans 403 Forbidden.

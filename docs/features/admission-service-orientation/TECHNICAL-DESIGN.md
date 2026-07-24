# Technical Design: Structured Hospital Service and Orientation Selection

## 1. Overview
This technical design addresses the replacement of free-text inputs for `orientation` and `service` in `app-unified-admission` and `patient-detail` modals with structured HTML `<select>` controls backed by `HospitalOrganizationApiService` and standardized vocabulary.

## 2. Architecture & Data Flow

```
[HospitalOrganizationApiService] ---> listServiceCatalog() ---> [UnifiedAdmissionComponent / PatientDetailComponent]
                                                                        |
                                                                        v
                                                          <select formControlName="orientation">
                                                          <select formControlName="service">
```

## 3. Component Updates

### 3.1 `UnifiedAdmissionComponent` (`web/src/app/admission/unified-admission.component.ts` & `.html`)
- Inject `HospitalOrganizationApiService`.
- Load active catalog entries on initialization via `listServiceCatalog()`.
- Merge catalog entries with default hospital services catalog to form `availableServices`.
- Replace free-text `<input formControlName="orientation">` with `<select formControlName="orientation">`.
- Replace free-text `<input formControlName="service">` with `<select formControlName="service">`.
- If "Autre" (`OTHER`) is selected in service, show precision text input `customService`.

### 3.2 `PatientDetailComponent` (`web/src/app/patient/patient-detail.component.ts`)
- Inject `HospitalOrganizationApiService`.
- Enrich `getDepartments()` method with services returned by `listServiceCatalog()`.

### 3.3 Translation Files (`fr.json` & `en.json`)
- Add keys under `admission.orientationOption.*` and `admission.serviceSelectPlaceholder`.

## 4. Anti-Regression & Verification
- Verify Reactive Form controls, validations, and payloads.
- Ensure Angular unit tests pass.
- Verify Angular production build completes without errors.

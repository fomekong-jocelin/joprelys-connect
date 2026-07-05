package com.joprelys.backend.fhir.model;

import java.util.List;

public record FhirBundleDto<T>(
    String resourceType,
    String type,
    int total,
    List<BundleEntry<T>> entry
) {
    public record BundleEntry<T>(
        T resource
    ) {}
}

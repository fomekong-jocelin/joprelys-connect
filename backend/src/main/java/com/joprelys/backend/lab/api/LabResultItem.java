package com.joprelys.backend.lab.api;

public record LabResultItem(
		String analyteName,
		String value,
		String unit,
		String referenceRange,
		String interpretation,
		String comment
) {}

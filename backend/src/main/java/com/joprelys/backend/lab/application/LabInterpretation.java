package com.joprelys.backend.lab.application;

import java.util.Locale;
import java.util.Optional;

/**
 * Interprétation d'un résultat de biologie, telle que validée par le laboratoire.
 *
 * <p>Une interprétation absente n'est jamais présumée normale : elle reste {@link #NOT_INTERPRETED}
 * (relecture clinique du 2026-11-04). Les codes FHIR suivent la terminologie HL7 v3
 * {@code ObservationInterpretation} ; un code non listé ici n'est pas deviné.
 */
public enum LabInterpretation {
	NORMAL,
	LOW,
	HIGH,
	ABNORMAL,
	CRITICAL,
	NOT_INTERPRETED;

	/** Valeur saisie ou transmise sous forme canonique ; absente → {@link #NOT_INTERPRETED}, inconnue → vide. */
	public static Optional<LabInterpretation> fromCanonical(String value) {
		if (value == null || value.isBlank()) {
			return Optional.of(NOT_INTERPRETED);
		}
		try {
			return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException unknown) {
			return Optional.empty();
		}
	}

	/** Code HL7 v3 ObservationInterpretation ; tout autre code (« &lt; », « POS », « IND »…) reste non interprété. */
	public static LabInterpretation fromHl7Code(String code) {
		if (code == null) {
			return NOT_INTERPRETED;
		}
		return switch (code.trim().toUpperCase(Locale.ROOT)) {
			case "N" -> NORMAL;
			case "L", "LU" -> LOW;
			case "H", "HU" -> HIGH;
			case "A" -> ABNORMAL;
			case "LL", "HH", "AA" -> CRITICAL;
			default -> NOT_INTERPRETED;
		};
	}

	/** Valeur en dehors des valeurs de référence, sans être critique. */
	public boolean isOutsideReference() {
		return this == LOW || this == HIGH || this == ABNORMAL;
	}
}

package com.joprelys.backend.lab.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LabInterpretationTest {

	@Test
	void absentInterpretationIsNotInterpretedAndUnknownIsRejected() {
		assertThat(LabInterpretation.fromCanonical(null)).contains(LabInterpretation.NOT_INTERPRETED);
		assertThat(LabInterpretation.fromCanonical("  ")).contains(LabInterpretation.NOT_INTERPRETED);
		assertThat(LabInterpretation.fromCanonical("high")).contains(LabInterpretation.HIGH);
		assertThat(LabInterpretation.fromCanonical("PLUTOT_BON")).isEmpty();
	}

	@Test
	void hl7CodesMapToCanonicalValuesWithoutGuessing() {
		assertThat(LabInterpretation.fromHl7Code("N")).isEqualTo(LabInterpretation.NORMAL);
		assertThat(LabInterpretation.fromHl7Code("LU")).isEqualTo(LabInterpretation.LOW);
		assertThat(LabInterpretation.fromHl7Code("H")).isEqualTo(LabInterpretation.HIGH);
		assertThat(LabInterpretation.fromHl7Code("A")).isEqualTo(LabInterpretation.ABNORMAL);
		assertThat(LabInterpretation.fromHl7Code("HH")).isEqualTo(LabInterpretation.CRITICAL);
		assertThat(LabInterpretation.fromHl7Code("AA")).isEqualTo(LabInterpretation.CRITICAL);
		assertThat(LabInterpretation.fromHl7Code("POS")).isEqualTo(LabInterpretation.NOT_INTERPRETED);
		assertThat(LabInterpretation.fromHl7Code(null)).isEqualTo(LabInterpretation.NOT_INTERPRETED);
	}

	@Test
	void lowHighAndAbnormalAreOutsideReferenceButNotCritical() {
		assertThat(LabInterpretation.LOW.isOutsideReference()).isTrue();
		assertThat(LabInterpretation.HIGH.isOutsideReference()).isTrue();
		assertThat(LabInterpretation.ABNORMAL.isOutsideReference()).isTrue();
		assertThat(LabInterpretation.CRITICAL.isOutsideReference()).isFalse();
		assertThat(LabInterpretation.NORMAL.isOutsideReference()).isFalse();
		assertThat(LabInterpretation.NOT_INTERPRETED.isOutsideReference()).isFalse();
	}
}

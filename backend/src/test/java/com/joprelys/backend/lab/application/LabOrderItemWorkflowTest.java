package com.joprelys.backend.lab.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.joprelys.backend.lab.infrastructure.persistence.ExamType;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderItemEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabOrderStatus;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultEntity;
import com.joprelys.backend.lab.infrastructure.persistence.LabResultStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LabOrderItemWorkflowTest {

    private final LabOrderService service = new LabOrderService(null, null, null, null, null);

    @Test
    void oneExamCanAdvanceWithoutCompletingTheWholeRequest() {
        LabOrderEntity order = orderWith("NFS", "CRP");
        LabOrderItemEntity nfs = order.getItems().get(0);
        LabOrderItemEntity crp = order.getItems().get(1);

        nfs.applyStatus(LabOrderStatus.SAMPLE_COLLECTED, Instant.parse("2026-08-08T10:00:00Z"));
        service.recalculateOrderStatus(order);

        assertThat(nfs.getStatus()).isEqualTo(LabOrderStatus.SAMPLE_COLLECTED);
        assertThat(crp.getStatus()).isEqualTo(LabOrderStatus.REQUESTED);
        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.SAMPLE_COLLECTED);

        nfs.applyStatus(LabOrderStatus.RESULT_AVAILABLE, Instant.parse("2026-08-08T11:00:00Z"));
        service.recalculateOrderStatus(order);

        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.RESULT_AVAILABLE);
        assertThat(crp.getStatus()).isEqualTo(LabOrderStatus.REQUESTED);
    }

    @Test
    void requestBecomesValidatedOnlyWhenEveryExamIsTerminal() {
        LabOrderEntity order = orderWith("NFS", "CRP");
        LabOrderItemEntity nfs = order.getItems().get(0);
        LabOrderItemEntity crp = order.getItems().get(1);
        Instant now = Instant.parse("2026-08-08T11:00:00Z");

        nfs.applyStatus(LabOrderStatus.VALIDATED, now);
        service.recalculateOrderStatus(order);
        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.RESULT_AVAILABLE);

        crp.applyStatus(LabOrderStatus.CANCELLED, now);
        service.recalculateOrderStatus(order);
        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.VALIDATED);
    }

    @Test
    void resultCanBeLinkedToExactlyOneExamItem() {
        LabOrderEntity order = orderWith("NFS", "CRP");
        LabOrderItemEntity crp = order.getItems().get(1);
        LabResultEntity result = new LabResultEntity(
                "EXAM-RES-20260808-000001",
                order,
                crp,
                null,
                "Dr Biologiste",
                LabResultStatus.VALIDATED,
                null,
                "Inflammation",
                null,
                1,
                null,
                "CRP",
                "22",
                "mg/L",
                "< 5",
                "ABNORMAL",
                null,
                null,
                nowMinusHours(2),
                nowMinusHours(1),
                Instant.parse("2026-08-08T12:00:00Z"));

        assertThat(result.getLabOrderItem()).isSameAs(crp);
        assertThat(result.getLabOrderItem().getExamName()).isEqualTo("CRP");
    }

    private LabOrderEntity orderWith(String... exams) {
        return new LabOrderEntity(
                "EXAM-REQ-20260808-000001",
                null,
                null,
                null,
                null,
                ExamType.LABORATOIRE,
                List.of(exams),
                null,
                "NORMALE",
                null);
    }

    private Instant nowMinusHours(long hours) {
        return Instant.parse("2026-08-08T12:00:00Z").minusSeconds(hours * 3600);
    }
}

package com.joprelys.backend.lead.repository;

import com.joprelys.backend.lead.domain.DemoRequest;
import com.joprelys.backend.lead.domain.DemoRequestStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRequestRepository extends JpaRepository<DemoRequest, UUID> {
    List<DemoRequest> findByStatusOrderByCreatedAtDesc(DemoRequestStatus status);
}

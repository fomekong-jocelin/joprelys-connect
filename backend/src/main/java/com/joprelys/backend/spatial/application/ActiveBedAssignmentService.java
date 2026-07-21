package com.joprelys.backend.spatial.application;

import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentEntity;
import com.joprelys.backend.spatial.infrastructure.persistence.BedAssignmentRepository;
import com.joprelys.backend.spatial.infrastructure.persistence.BedEntity;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ActiveBedAssignmentService {

    private final BedAssignmentRepository bedAssignmentRepository;

    public ActiveBedAssignmentService(BedAssignmentRepository bedAssignmentRepository) {
        this.bedAssignmentRepository = bedAssignmentRepository;
    }

    public BedAssignmentEntity assign(UUID hospitalizationId, BedEntity bed, UUID organizationId) {
        if (organizationId == null || !organizationId.equals(bed.getOrganizationId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Le lit sélectionné n'appartient pas à l'établissement du séjour.");
        }
        BedAssignmentEntity assignment = new BedAssignmentEntity(hospitalizationId, bed);
        assignment.setOrganizationId(organizationId);
        try {
            return bedAssignmentRepository.saveAndFlush(assignment);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une affectation active existe déjà pour ce lit ou cette hospitalisation.",
                    exception);
        }
    }
}

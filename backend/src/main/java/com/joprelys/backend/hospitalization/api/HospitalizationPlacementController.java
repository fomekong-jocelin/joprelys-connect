package com.joprelys.backend.hospitalization.api;

import com.joprelys.backend.hospitalization.application.HospitalizationPlacementUseCase;
import com.joprelys.backend.spatial.api.BedResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hospitalizations")
public class HospitalizationPlacementController {
    private final HospitalizationPlacementUseCase queries;

    public HospitalizationPlacementController(HospitalizationPlacementUseCase queries) {
        this.queries = queries;
    }

    @GetMapping("/placement-options")
    @PreAuthorize("hasAnyAuthority('HOSPITALIZATION_ADMIT', 'HOSPITALIZATION_TRANSFER')")
    public HospitalizationPlacementOptions options() {
        return queries.options();
    }

    @GetMapping("/placement-beds")
    @PreAuthorize("hasAnyAuthority('HOSPITALIZATION_ADMIT', 'HOSPITALIZATION_TRANSFER')")
    public List<BedResponse> beds(@RequestParam UUID spaceId) {
        return queries.beds(spaceId);
    }

    @GetMapping("/practitioners")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_READ')")
    public List<HospitalizationPlacementOptions.PractitionerOption> practitioners() {
        return queries.practitioners();
    }

    @GetMapping("/admission-visits/{patientId}")
    @PreAuthorize("hasAuthority('HOSPITALIZATION_ADMIT')")
    public List<HospitalizationPlacementOptions.AdmissionVisit> visits(@PathVariable UUID patientId) {
        return queries.visits(patientId);
    }
}

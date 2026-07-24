package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.api.StaffAssignmentDtos.AssignmentRoleResponse;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.CloseAssignmentRequest;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.SpecialtyAssignmentRequest;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.SpecialtyAssignmentResponse;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.StructureResponse;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.UnitAssignmentRequest;
import com.joprelys.backend.auth.api.StaffAssignmentDtos.UnitAssignmentResponse;
import com.joprelys.backend.auth.application.StaffAssignmentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffAssignmentController {

    private final StaffAssignmentService staffAssignmentService;

    public StaffAssignmentController(StaffAssignmentService staffAssignmentService) {
        this.staffAssignmentService = staffAssignmentService;
    }

    @GetMapping("/assignment-roles")
    @PreAuthorize("hasAnyAuthority('USER_READ', 'USER_MANAGE')")
    public List<AssignmentRoleResponse> listAssignmentRoles(Authentication authentication) {
        return staffAssignmentService.listAssignmentRoles(authentication);
    }

    @GetMapping("/{staffId}/assignments")
    @PreAuthorize("hasAnyAuthority('USER_READ', 'USER_MANAGE')")
    public StructureResponse getStructure(@PathVariable UUID staffId, Authentication authentication) {
        return staffAssignmentService.getStructure(staffId, authentication);
    }

    @PostMapping("/{staffId}/assignments/specialties")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public SpecialtyAssignmentResponse createSpecialty(
            @PathVariable UUID staffId,
            @Valid @RequestBody SpecialtyAssignmentRequest request,
            Authentication authentication) {
        return staffAssignmentService.createSpecialty(staffId, request, authentication);
    }

    @PutMapping("/{staffId}/assignments/specialties/{assignmentId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public SpecialtyAssignmentResponse updateSpecialty(
            @PathVariable UUID staffId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody SpecialtyAssignmentRequest request,
            Authentication authentication) {
        return staffAssignmentService.updateSpecialty(staffId, assignmentId, request, authentication);
    }

    @PostMapping("/{staffId}/assignments/specialties/{assignmentId}/close")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public SpecialtyAssignmentResponse closeSpecialty(
            @PathVariable UUID staffId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CloseAssignmentRequest request,
            Authentication authentication) {
        return staffAssignmentService.closeSpecialty(staffId, assignmentId, request, authentication);
    }

    @PostMapping("/{staffId}/assignments/units")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UnitAssignmentResponse createUnit(
            @PathVariable UUID staffId,
            @Valid @RequestBody UnitAssignmentRequest request,
            Authentication authentication) {
        return staffAssignmentService.createUnit(staffId, request, authentication);
    }

    @PutMapping("/{staffId}/assignments/units/{assignmentId}")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UnitAssignmentResponse updateUnit(
            @PathVariable UUID staffId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UnitAssignmentRequest request,
            Authentication authentication) {
        return staffAssignmentService.updateUnit(staffId, assignmentId, request, authentication);
    }

    @PostMapping("/{staffId}/assignments/units/{assignmentId}/close")
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UnitAssignmentResponse closeUnit(
            @PathVariable UUID staffId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody CloseAssignmentRequest request,
            Authentication authentication) {
        return staffAssignmentService.closeUnit(staffId, assignmentId, request, authentication);
    }
}

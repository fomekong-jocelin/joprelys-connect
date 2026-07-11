package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.application.StaffService;
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
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER_READ', 'USER_MANAGE') or hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN', 'AGENT_ACCUEIL', 'INFIRMIER', 'MEDECIN')")
    public List<StaffResponse> list(Authentication authentication) {
        return staffService.listStaff(authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE') or hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')")
    public InviteStaffResponse invite(@Valid @RequestBody InviteStaffRequest request, Authentication authentication) {
        return staffService.inviteStaff(request, authentication);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_MANAGE') or hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')")
    public StaffResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffRequest request,
            Authentication authentication) {
        return staffService.updateStaff(id, request, authentication);
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('USER_MANAGE') or hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')")
    public StaffResponse toggle(@PathVariable UUID id, Authentication authentication) {
        return staffService.toggleStatus(id, authentication);
    }
}

package com.joprelys.backend.auth.api;

import com.joprelys.backend.auth.rbac.RbacAdministrationService;
import com.joprelys.backend.auth.rbac.RbacStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
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
@RequestMapping("/api/rbac")
public class RbacController {

    private static final String CAN_READ =
            "hasAnyAuthority('RBAC_READ', 'RBAC_MANAGE') or hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";
    private static final String CAN_MANAGE =
            "hasAuthority('RBAC_MANAGE') or hasAnyRole('ADMIN_CLINIQUE', 'ADMIN_JOPRELYS', 'SUPER_ADMIN')";

    private final RbacAdministrationService rbacAdministrationService;

    public RbacController(RbacAdministrationService rbacAdministrationService) {
        this.rbacAdministrationService = rbacAdministrationService;
    }

    @GetMapping("/me")
    public RbacStore.EffectiveAccess me(Authentication authentication) {
        return rbacAdministrationService.myAccess(authentication);
    }

    @GetMapping("/roles")
    @PreAuthorize(CAN_READ)
    public List<RbacStore.RoleView> roles(Authentication authentication) {
        return rbacAdministrationService.listRoles(authentication);
    }

    @GetMapping("/permissions")
    @PreAuthorize(CAN_READ)
    public List<RbacStore.PermissionView> permissions(Authentication authentication) {
        return rbacAdministrationService.listPermissions(authentication);
    }

    @GetMapping("/users")
    @PreAuthorize(CAN_READ)
    public List<RbacAdministrationService.UserAccessView> users(Authentication authentication) {
        return rbacAdministrationService.listUsers(authentication);
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(CAN_MANAGE)
    public RbacStore.RoleView createRole(
            @Valid @RequestBody CreateRoleRequest request,
            Authentication authentication) {
        return rbacAdministrationService.createRole(
                request.code(),
                request.name(),
                request.description(),
                request.assignable(),
                request.permissionCodes(),
                authentication);
    }

    @PutMapping("/roles/{roleId}")
    @PreAuthorize(CAN_MANAGE)
    public RbacStore.RoleView updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        return rbacAdministrationService.updateRole(
                roleId,
                request.code(),
                request.name(),
                request.description(),
                request.assignable(),
                request.enabled(),
                authentication);
    }

    @PutMapping("/roles/{roleId}/permissions")
    @PreAuthorize(CAN_MANAGE)
    public RbacStore.RoleView replaceRolePermissions(
            @PathVariable UUID roleId,
            @Valid @RequestBody ReplacePermissionsRequest request,
            Authentication authentication) {
        return rbacAdministrationService.replaceRolePermissions(
                roleId,
                request.permissionCodes(),
                authentication);
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize(CAN_MANAGE)
    public RbacAdministrationService.UserAccessView replaceUserRoles(
            @PathVariable UUID userId,
            @Valid @RequestBody ReplaceUserRolesRequest request,
            Authentication authentication) {
        return rbacAdministrationService.replaceUserRoles(userId, request.roleIds(), authentication);
    }

    @GetMapping("/audit")
    @PreAuthorize(CAN_READ)
    public List<RbacStore.AuditView> audit(Authentication authentication) {
        return rbacAdministrationService.listAudit(authentication);
    }

    public record CreateRoleRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            boolean assignable,
            Set<String> permissionCodes) {
        public CreateRoleRequest {
            permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        }
    }

    public record UpdateRoleRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 500) String description,
            boolean assignable,
            boolean enabled) {
    }

    public record ReplacePermissionsRequest(Set<String> permissionCodes) {
        public ReplacePermissionsRequest {
            permissionCodes = permissionCodes == null ? Set.of() : Set.copyOf(permissionCodes);
        }
    }

    public record ReplaceUserRolesRequest(@NotEmpty List<UUID> roleIds) {
        public ReplaceUserRolesRequest {
            roleIds = roleIds == null ? List.of() : List.copyOf(roleIds);
        }
    }
}

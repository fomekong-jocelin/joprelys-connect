package com.joprelys.backend.auth.session.application;

import java.util.Set;
import java.util.UUID;

public record SessionActor(
        UUID userId,
        UUID organizationId,
        UUID currentSessionId,
        Set<String> authorities) {

    public SessionActor {
        authorities = Set.copyOf(authorities);
    }

    public boolean canManageOtherUsers() {
        return authorities.contains("AUTH_SESSION_MANAGE");
    }
}

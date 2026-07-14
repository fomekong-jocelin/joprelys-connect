package com.joprelys.backend.auth.session.application;

import java.util.UUID;

public interface RevokeAuthSessionUseCase {

    void revoke(SessionActor actor, UUID sessionId);
}

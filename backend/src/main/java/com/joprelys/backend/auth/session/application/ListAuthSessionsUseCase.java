package com.joprelys.backend.auth.session.application;

import java.util.List;
import java.util.UUID;

public interface ListAuthSessionsUseCase {

    List<AuthSessionView> listOwn(SessionActor actor);

    List<AuthSessionView> listUser(SessionActor actor, UUID targetUserId);
}

package com.joprelys.backend.auth.session.application;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountEntity;

public interface IssueAuthSessionUseCase {
    IssuedAuthSession issue(UserAccountEntity user, SessionClientMetadata metadata);
}

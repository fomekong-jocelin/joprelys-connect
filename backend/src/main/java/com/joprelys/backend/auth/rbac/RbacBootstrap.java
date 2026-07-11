package com.joprelys.backend.auth.rbac;

import com.joprelys.backend.auth.infrastructure.persistence.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RbacBootstrap implements ApplicationRunner {

    private final RbacStore rbacStore;
    private final UserAccountRepository userAccountRepository;

    public RbacBootstrap(RbacStore rbacStore, UserAccountRepository userAccountRepository) {
        this.rbacStore = rbacStore;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        rbacStore.seedCatalog();
        userAccountRepository.findAll().forEach(rbacStore::synchronizeLegacyAssignments);
    }
}

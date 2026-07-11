package com.joprelys.backend.auth.rbac;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RbacPlatformRolePolicyInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public RbacPlatformRolePolicyInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbcTemplate.update(
                "UPDATE roles SET assignable = TRUE WHERE organization_id IS NULL AND code IN (?, ?)",
                RbacCatalog.ROLE_ADMIN_JOPRELYS,
                RbacCatalog.ROLE_SUPER_ADMIN);
    }
}

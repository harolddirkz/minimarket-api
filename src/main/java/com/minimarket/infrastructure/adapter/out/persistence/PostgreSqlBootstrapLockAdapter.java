package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.port.out.BootstrapLockPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlBootstrapLockAdapter implements BootstrapLockPort {

    private static final long ADMIN_BOOTSTRAP_LOCK_ID = 6_521_903_447L;

    private final EntityManager entityManager;

    public PostgreSqlBootstrapLockAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void acquireAdministratorBootstrapLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
                .setParameter("lockId", ADMIN_BOOTSTRAP_LOCK_ID)
                .getSingleResult();
    }
}

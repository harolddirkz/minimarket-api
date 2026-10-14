package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.port.out.UserManagementLockPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class PostgreSqlUserManagementLockAdapter implements UserManagementLockPort {

    private static final long ADMIN_MUTATION_LOCK_ID = 6_521_903_448L;

    private final EntityManager entityManager;

    public PostgreSqlUserManagementLockAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void acquireAdministratorMutationLock() {
        entityManager.createNativeQuery("select pg_advisory_xact_lock(:lockId)")
                .setParameter("lockId", ADMIN_MUTATION_LOCK_ID)
                .getSingleResult();
    }
}

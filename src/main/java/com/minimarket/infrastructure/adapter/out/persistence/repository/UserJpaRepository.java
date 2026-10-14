package com.minimarket.infrastructure.adapter.out.persistence.repository;

import com.minimarket.domain.model.UserRole;
import com.minimarket.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findByIdForUpdate(@Param("id") Long id);

    boolean existsByUsernameIgnoreCase(String username);

    long countByRoleAndActiveTrue(UserRole role);

    long countByRole(UserRole role);
}

package com.minimarket.infrastructure.adapter.out.persistence.mapper;

import com.minimarket.domain.model.User;
import com.minimarket.infrastructure.adapter.out.persistence.entity.UserEntity;

public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(), entity.getUsername(), entity.getPasswordHash(), entity.getFullName(),
                entity.getRole(), entity.isActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.id(), user.username(), user.passwordHash(), user.fullName(), user.role(),
                user.active(), user.createdAt(), user.updatedAt());
    }
}

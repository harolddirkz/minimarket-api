package com.minimarket.infrastructure.adapter.in.rest.mapper;

import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.User;
import com.minimarket.domain.port.in.CreateUserCommand;
import com.minimarket.infrastructure.adapter.in.rest.dto.PageResponse;
import com.minimarket.infrastructure.adapter.in.rest.dto.UserCreateRequest;
import com.minimarket.infrastructure.adapter.in.rest.dto.UserResponse;

public final class UserRestMapper {

    private UserRestMapper() {
    }

    public static CreateUserCommand toCommand(UserCreateRequest request) {
        return new CreateUserCommand(
                request.username(), request.password(), request.fullName(), request.role());
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.id(), user.username(), user.fullName(), user.role(), user.active(),
                user.createdAt(), user.updatedAt());
    }

    public static PageResponse<UserResponse> toResponse(PageResult<User> page) {
        return new PageResponse<>(
                page.content().stream().map(UserRestMapper::toResponse).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}

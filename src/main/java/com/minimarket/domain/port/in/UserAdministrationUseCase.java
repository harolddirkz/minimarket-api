package com.minimarket.domain.port.in;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;

public interface UserAdministrationUseCase {

    User create(CreateUserCommand command);

    PageResult<User> findAll(PageCriteria criteria);

    User findById(Long id);

    User findAuthenticatedUser(Long id);

    User updateStatus(Long actorId, Long userId, boolean active);

    User updateRole(Long actorId, Long userId, UserRole role);

    void resetPassword(Long userId, String newPassword);
}

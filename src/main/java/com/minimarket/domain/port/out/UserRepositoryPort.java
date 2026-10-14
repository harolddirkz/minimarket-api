package com.minimarket.domain.port.out;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByUsername(String username);

    PageResult<User> findAll(PageCriteria pageCriteria);

    boolean existsByUsername(String username);

    long countActiveAdministrators();

    long countAdministrators();
}

package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.User;
import com.minimarket.domain.model.UserRole;
import com.minimarket.domain.port.out.UserRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.minimarket.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.minimarket.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "username");

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public User save(User user) {
        return UserPersistenceMapper.toDomain(repository.save(UserPersistenceMapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(Long id) {
        return repository.findById(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username.trim()).map(UserPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<User> findAll(PageCriteria criteria) {
        Page<UserEntity> page = repository.findAll(
                PageRequest.of(criteria.page(), criteria.size(), DEFAULT_SORT));
        return new PageResult<>(
                page.getContent().stream().map(UserPersistenceMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public boolean existsByUsername(String username) {
        return repository.existsByUsernameIgnoreCase(username.trim());
    }

    @Override
    public long countActiveAdministrators() {
        return repository.countByRoleAndActiveTrue(UserRole.ADMIN);
    }

    @Override
    public long countAdministrators() {
        return repository.countByRole(UserRole.ADMIN);
    }
}

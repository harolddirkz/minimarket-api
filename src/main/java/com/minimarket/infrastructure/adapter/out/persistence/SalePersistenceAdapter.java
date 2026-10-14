package com.minimarket.infrastructure.adapter.out.persistence;

import com.minimarket.domain.model.Sale;
import com.minimarket.domain.model.PageCriteria;
import com.minimarket.domain.model.PageResult;
import com.minimarket.domain.model.SaleFilter;
import com.minimarket.domain.model.SaleListEntry;
import com.minimarket.domain.model.SaleSummary;
import com.minimarket.infrastructure.adapter.out.persistence.entity.SaleEntity;
import com.minimarket.domain.port.out.SaleRepositoryPort;
import com.minimarket.infrastructure.adapter.out.persistence.mapper.SalePersistenceMapper;
import com.minimarket.infrastructure.adapter.out.persistence.repository.SaleJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class SalePersistenceAdapter implements SaleRepositoryPort {

    private final SaleJpaRepository repository;
    private final EntityManager entityManager;

    public SalePersistenceAdapter(SaleJpaRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Override
    public Sale save(Sale sale) {
        return SalePersistenceMapper.toDomain(repository.save(SalePersistenceMapper.toEntity(sale)));
    }

    @Override
    public Optional<Sale> findById(Long id) {
        return repository.findById(id).map(SalePersistenceMapper::toDomain);
    }

    @Override
    public String nextSaleNumber() {
        return repository.nextSaleNumber();
    }

    @Override
    public PageResult<SaleListEntry> findAll(SaleFilter filter, PageCriteria pageCriteria) {
        PageRequest pageable = PageRequest.of(pageCriteria.page(), pageCriteria.size(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        var page = repository.findAll(specification(filter), pageable);
        return new PageResult<>(
                page.getContent().stream().map(SalePersistenceAdapter::toListEntry).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    @Override
    public SaleSummary summarize(SaleFilter filter) {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(Object[].class);
        var sale = query.from(SaleEntity.class);
        query.select(builder.array(builder.count(sale), builder.sum(sale.<BigDecimal>get("total"))));
        Predicate predicate = specification(filter).toPredicate(sale, query, builder);
        if (predicate != null) {
            query.where(predicate);
        }
        Object[] result = entityManager.createQuery(query).getSingleResult();
        long count = (Long) result[0];
        BigDecimal amount = result[1] == null ? new BigDecimal("0.00") : (BigDecimal) result[1];
        return new SaleSummary(count, amount.setScale(2));
    }

    private static Specification<SaleEntity> specification(SaleFilter filter) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<Predicate>();
            if (filter.fromInclusive() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filter.fromInclusive()));
            }
            if (filter.toExclusive() != null) {
                predicates.add(builder.lessThan(root.get("createdAt"), filter.toExclusive()));
            }
            if (filter.userId() != null) {
                predicates.add(builder.equal(root.get("userId"), filter.userId()));
            }
            if (filter.status() != null) {
                predicates.add(builder.equal(root.get("status"), filter.status()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static SaleListEntry toListEntry(SaleEntity entity) {
        return new SaleListEntry(entity.getId(), entity.getSaleNumber(), entity.getUserId(),
                entity.getStatus(), entity.getTotal(), entity.getCreatedAt());
    }
}

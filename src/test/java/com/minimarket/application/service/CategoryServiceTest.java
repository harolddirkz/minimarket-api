package com.minimarket.application.service;

import com.minimarket.domain.exception.ConflictException;
import com.minimarket.domain.model.Category;
import com.minimarket.domain.port.in.CreateCategoryCommand;
import com.minimarket.domain.port.out.CategoryRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepositoryPort repository;

    private CategoryService service;

    @BeforeEach
    void setUp() {
        service = new CategoryService(repository);
    }

    @Test
    void shouldCreateCategory() {
        when(repository.existsByNameIgnoreCase("Lácteos")).thenReturn(false);
        when(repository.save(any(Category.class)))
                .thenReturn(new Category(1L, "Lácteos", "Derivados", true));

        Category result = service.create(new CreateCategoryCommand("  Lácteos  ", "Derivados"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Lácteos");
        assertThat(result.active()).isTrue();
        verify(repository).existsByNameIgnoreCase("Lácteos");
    }

    @Test
    void shouldRejectDuplicatedCategory() {
        when(repository.existsByNameIgnoreCase("Lácteos")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateCategoryCommand("Lácteos", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya existe una categoría con ese nombre")
                .extracting("code")
                .isEqualTo("CATEGORY_ALREADY_EXISTS");
    }
}

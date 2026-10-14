package com.minimarket.infrastructure.adapter.in.rest.controller;

import com.minimarket.infrastructure.adapter.in.rest.ApiResponseFactory;
import com.minimarket.infrastructure.logging.CorrelationIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SystemControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SystemController(new ApiResponseFactory()))
                .addFilter(new CorrelationIdFilter())
                .build();
    }

    @Test
    void shouldReturnSystemInformationWithoutDatabaseAccess() throws Exception {
        mockMvc.perform(get("/api/v1/system/info")
                        .header(CorrelationIdFilter.HEADER_NAME, "system-check-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER_NAME, "system-check-1"))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Información del sistema obtenida correctamente"))
                .andExpect(jsonPath("$.data.application").value("minimarket-api"))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}

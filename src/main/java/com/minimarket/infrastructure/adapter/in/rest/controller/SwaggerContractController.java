package com.minimarket.infrastructure.adapter.in.rest.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerContractController {

    private static final MediaType YAML = MediaType.parseMediaType("application/yaml");
    private static final Resource CONTRACT = new ClassPathResource("swagger.yml");

    @GetMapping(value = "/swagger.yml", produces = "application/yaml")
    public ResponseEntity<Resource> contract() {
        return ResponseEntity.ok()
                .contentType(YAML)
                .body(CONTRACT);
    }
}

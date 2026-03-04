package org.acme.api.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ProductCreateRequest {
    @NotBlank
    public String code;

    @NotBlank
    public String name;

    @NotNull
    public BigDecimal unitValue;

    @NotNull
    @Valid
    public List<ProductComponentRequest> components;
}
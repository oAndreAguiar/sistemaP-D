package org.acme.api.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ProductComponentRequest {
    @NotNull
    public Long rawMaterialId;

    @NotNull
    @Min(1)
    public Long quantityPerUnit;
}
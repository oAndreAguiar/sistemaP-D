package org.acme.api.rawmaterial.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RawMaterialUpdateRequest {
    @NotBlank
    public String name;

    @NotNull
    @Min(0)
    public Long stockQuantity;
}
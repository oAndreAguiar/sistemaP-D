package org.acme.api.product.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductResponse {
    public Long id;
    public String code;
    public String name;
    public BigDecimal unitValue;
    public List<ProductComponentResponse> components;
}
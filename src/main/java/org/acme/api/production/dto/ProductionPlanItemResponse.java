package org.acme.api.production.dto;

import java.math.BigDecimal;

public class ProductionPlanItemResponse {
    public Long productId;
    public String productCode;
    public String productName;

    public Long unitsToProduce;

    public BigDecimal unitValue;
    public BigDecimal totalValue;
}
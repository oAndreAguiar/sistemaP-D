package org.acme.api.production.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductionPlanResponse {
    public BigDecimal totalValue;
    public List<ProductionPlanItemResponse> items;
    public List<RemainingStockItemResponse> remainingStock;
}
package org.acme.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.acme.api.production.dto.ProductionPlanItemResponse;
import org.acme.api.production.dto.ProductionPlanResponse;
import org.acme.api.production.dto.RemainingStockItemResponse;
import org.acme.domain.Product;
import org.acme.domain.ProductComponent;
import org.acme.domain.RawMaterial;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProductionPlannerService {

    // usado pelo endpoint (busca no DB)
    public ProductionPlanResponse plan() {
        List<Product> products = Product.listAll();
        List<RawMaterial> rawMaterials = RawMaterial.listAll();

        Map<Long, Long> stock = rawMaterials.stream()
                .collect(Collectors.toMap(rm -> rm.id, rm -> rm.stockQuantity));

        return plan(products, stock);
    }

    // método PURO (testável) — não acessa DB
    ProductionPlanResponse plan(List<Product> products, Map<Long, Long> stock) {
        Map<Long, Long> workingStock = new HashMap<>(stock);

        List<Product> sorted = new ArrayList<>(products);
        sorted.sort((a, b) -> score(b, workingStock).compareTo(score(a, workingStock)));

        List<ProductionPlanItemResponse> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Product p : sorted) {
            long maxUnits = maxUnitsPossible(p, workingStock);
            if (maxUnits <= 0) continue;

            for (ProductComponent pc : p.components) {
                long current = workingStock.getOrDefault(pc.rawMaterial.id, 0L);
                workingStock.put(pc.rawMaterial.id, current - (pc.quantityPerUnit * maxUnits));
            }

            ProductionPlanItemResponse item = new ProductionPlanItemResponse();
            item.productId = p.id;
            item.productCode = p.code;
            item.productName = p.name;
            item.unitsToProduce = maxUnits;
            item.unitValue = p.unitValue;
            item.totalValue = p.unitValue.multiply(BigDecimal.valueOf(maxUnits));

            total = total.add(item.totalValue);
            items.add(item);
        }

        // remaining stock (com base nos insumos presentes na composição dos produtos)
        List<RemainingStockItemResponse> remaining = products.stream()
                .flatMap(p -> p.components.stream())
                .map(pc -> pc.rawMaterial)
                .filter(Objects::nonNull)
                .distinct()
                .map(rm -> {
                    RemainingStockItemResponse rs = new RemainingStockItemResponse();
                    rs.rawMaterialId = rm.id;
                    rs.rawMaterialCode = rm.code;
                    rs.rawMaterialName = rm.name;
                    rs.remainingQuantity = workingStock.getOrDefault(rm.id, 0L);
                    return rs;
                })
                .toList();

        ProductionPlanResponse resp = new ProductionPlanResponse();
        resp.totalValue = total;
        resp.items = items;
        resp.remainingStock = remaining;
        return resp;
    }

    private BigDecimal score(Product p, Map<Long, Long> stock) {
        BigDecimal weightedCost = BigDecimal.ZERO;

        for (ProductComponent pc : p.components) {
            long s = Math.max(stock.getOrDefault(pc.rawMaterial.id, 0L), 1L);
            BigDecimal weight = BigDecimal.ONE.divide(BigDecimal.valueOf(s), 10, RoundingMode.HALF_UP);
            weightedCost = weightedCost.add(BigDecimal.valueOf(pc.quantityPerUnit).multiply(weight));
        }

        if (weightedCost.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;

        return p.unitValue.divide(weightedCost, 10, RoundingMode.HALF_UP);
    }

    private long maxUnitsPossible(Product p, Map<Long, Long> stock) {
        long max = Long.MAX_VALUE;

        for (ProductComponent pc : p.components) {
            long available = stock.getOrDefault(pc.rawMaterial.id, 0L);
            long possible = available / pc.quantityPerUnit;
            max = Math.min(max, possible);
        }

        return max == Long.MAX_VALUE ? 0 : max;
    }
}
package org.acme.service;

import org.acme.api.production.dto.ProductionPlanResponse;
import org.acme.domain.Product;
import org.acme.domain.ProductComponent;
import org.acme.domain.RawMaterial;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionPlannerServiceTest {

    // Helper to create a product with its components already linked
    private static Product product(Long id, String code, String name, BigDecimal unitValue, ProductComponent... comps) {
        Product p = new Product();
        p.id = id;
        p.code = code;
        p.name = name;
        p.unitValue = unitValue;

        // Ensure the list starts clean (safety)
        p.components.clear();

        for (ProductComponent pc : comps) {
            pc.product = p;      // link component to product
            p.components.add(pc);
        }
        return p;
    }

    // Helper to create a product component (raw material + quantity per unit)
    private static ProductComponent comp(Long rmId, String rmCode, String rmName, long qtyPerUnit) {
        RawMaterial rm = new RawMaterial();
        rm.id = rmId;
        rm.code = rmCode;
        rm.name = rmName;

        ProductComponent pc = new ProductComponent();
        pc.rawMaterial = rm;
        pc.quantityPerUnit = qtyPerUnit;
        return pc;
    }

    @Test
    void shouldPrioritizeHigherReturnAndThenUseRemainingStock() {
        /*
         Scenario:
         - Stock:
           Steel: 5000
           Plastic: 2000

         - Premium (P-A): value 100, consumes 300 steel + 100 plastic
         - Basic   (P-B): value  60, consumes 200 steel + 100 plastic

         Expected:
         - Produce as many Premium units as possible first:
           steel limit: 5000/300 = 16
           plastic limit: 2000/100 = 20
           => Premium = 16

         - Remaining stock:
           steel: 5000 - 16*300 = 200
           plastic: 2000 - 16*100 = 400

         - With the remaining stock we can still produce Basic:
           steel: 200/200 = 1
           plastic: 400/100 = 4
           => Basic = 1

         Total:
         16*100 + 1*60 = 1660
        */

        Map<Long, Long> stock = new HashMap<>();
        stock.put(1L, 5000L); // steel
        stock.put(2L, 2000L); // plastic

        Product premium = product(
                10L, "P-A", "Premium", new BigDecimal("100.00"),
                comp(1L, "RM-STEEL", "Steel", 300L),
                comp(2L, "RM-PLASTIC", "Plastic", 100L)
        );

        Product basic = product(
                11L, "P-B", "Basic", new BigDecimal("60.00"),
                comp(1L, "RM-STEEL", "Steel", 200L),
                comp(2L, "RM-PLASTIC", "Plastic", 100L)
        );

        ProductionPlannerService svc = new ProductionPlannerService();
        ProductionPlanResponse resp = svc.plan(java.util.List.of(premium, basic), stock);

        assertNotNull(resp);
        assertNotNull(resp.items);

        // Premium should appear first with 16 units
        assertTrue(resp.items.size() >= 1);
        assertEquals("P-A", resp.items.get(0).productCode);
        assertEquals(16L, resp.items.get(0).unitsToProduce);

        // Basic should exist with 1 unit (using remaining stock)
        boolean hasBasic = resp.items.stream().anyMatch(i -> "P-B".equals(i.productCode) && i.unitsToProduce == 1L);
        assertTrue(hasBasic);

        // Expected total = 1660.00
        assertEquals(new BigDecimal("1660.00"), resp.totalValue.setScale(2));
    }

    @Test
    void shouldReturnEmptyWhenNoProductCanBeProduced() {
        /*
         Scenario:
         - Stock is insufficient for any production.
         - Product requires 100 units of RM-X but stock is only 10.

         Expected:
         - No items in the plan
         - Total = 0
        */

        Map<Long, Long> stock = new HashMap<>();
        stock.put(1L, 10L);

        Product p = product(
                10L, "P-001", "Impossible", new BigDecimal("50.00"),
                comp(1L, "RM-X", "X", 100L)
        );

        ProductionPlannerService svc = new ProductionPlannerService();
        ProductionPlanResponse resp = svc.plan(java.util.List.of(p), stock);

        assertNotNull(resp);
        assertTrue(resp.items.isEmpty());
        assertEquals(new BigDecimal("0.00"), resp.totalValue.setScale(2));
    }

    @Test
    void shouldChooseTheOneWithBetterReturnWhenCompetingForSameMaterial() {
        /*
         Conflict scenario:
         - Only one raw material with stock = 500.

         Product A:
         - value 90
         - consumes 300
         => produces 1 (total 90)

         Product B:
         - value 80
         - consumes 200
         => produces 2 (total 160)

         Expected:
         - Choose B first because it generates a higher total value using the same resource.
        */

        Map<Long, Long> stock = new HashMap<>();
        stock.put(1L, 500L);

        Product a = product(
                1L, "A", "A", new BigDecimal("90.00"),
                comp(1L, "RM-1", "RM1", 300L)
        );

        Product b = product(
                2L, "B", "B", new BigDecimal("80.00"),
                comp(1L, "RM-1", "RM1", 200L)
        );

        ProductionPlannerService svc = new ProductionPlannerService();
        ProductionPlanResponse resp = svc.plan(java.util.List.of(a, b), stock);

        assertNotNull(resp);
        assertFalse(resp.items.isEmpty());

        // Expect B first with 2 units
        assertEquals("B", resp.items.get(0).productCode);
        assertEquals(2L, resp.items.get(0).unitsToProduce);

        // Expected total = 160.00
        assertEquals(new BigDecimal("160.00"), resp.totalValue.setScale(2));
    }
}
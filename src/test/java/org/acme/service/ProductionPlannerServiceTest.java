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

    // Helper para criar um produto com componentes já ligados
    private static Product product(Long id, String code, String name, BigDecimal unitValue, ProductComponent... comps) {
        Product p = new Product();
        p.id = id;
        p.code = code;
        p.name = name;
        p.unitValue = unitValue;

        // Garante lista limpa (por segurança)
        p.components.clear();

        for (ProductComponent pc : comps) {
            pc.product = p;      // liga o componente ao produto
            p.components.add(pc);
        }
        return p;
    }

    // Helper para criar um componente de produto (matéria-prima + quantidade por unidade)
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
         Cenário:
         - Estoque:
           Steel: 5000
           Plastic: 2000

         - Premium (P-A): valor 100, consome 300 steel + 100 plastic
         - Basic   (P-B): valor  60, consome 200 steel + 100 plastic

         Esperado:
         - Produz o máximo de Premium primeiro:
           steel limita: 5000/300 = 16
           plastic limita: 2000/100 = 20
           => Premium = 16

         - Sobra:
           steel: 5000 - 16*300 = 200
           plastic: 2000 - 16*100 = 400

         - Com o restante, ainda dá para fazer Basic:
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

        // Premium deve aparecer primeiro com 16 unidades
        assertTrue(resp.items.size() >= 1);
        assertEquals("P-A", resp.items.get(0).productCode);
        assertEquals(16L, resp.items.get(0).unitsToProduce);

        // Deve existir Basic com 1 unidade (usando o estoque restante)
        boolean hasBasic = resp.items.stream().anyMatch(i -> "P-B".equals(i.productCode) && i.unitsToProduce == 1L);
        assertTrue(hasBasic);

        // Total esperado = 1660.00
        assertEquals(new BigDecimal("1660.00"), resp.totalValue.setScale(2));
    }

    @Test
    void shouldReturnEmptyWhenNoProductCanBeProduced() {
        /*
         Cenário:
         - Estoque insuficiente para qualquer produção.
         - Produto precisa de 100 unidades de RM-X, mas estoque é 10.

         Esperado:
         - Nenhum item no plano
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
         Cenário de conflito:
         - Apenas uma matéria-prima com estoque 500.

         Produto A:
         - valor 90
         - consome 300
         => produz 1 (total 90)

         Produto B:
         - valor 80
         - consome 200
         => produz 2 (total 160)

         Esperado:
         - Escolher B primeiro, porque com o mesmo recurso gera maior valor total.
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

        // Espera-se B primeiro com 2 unidades
        assertEquals("B", resp.items.get(0).productCode);
        assertEquals(2L, resp.items.get(0).unitsToProduce);

        // Total esperado = 160.00
        assertEquals(new BigDecimal("160.00"), resp.totalValue.setScale(2));
    }
}
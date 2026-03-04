package org.acme.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
        name = "product_component",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_rawmaterial", columnNames = {"product_id", "raw_material_id"})
)
public class ProductComponent extends PanacheEntity {

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    public Product product;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", nullable = false)
    public RawMaterial rawMaterial;

    @NotNull
    @Min(1)
    @Column(name = "quantity_per_unit", nullable = false)
    public Long quantityPerUnit;
}
package org.acme.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
public class Product extends PanacheEntity {

    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 50)
    public String code;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    public String name;

    @NotNull
    @Column(name = "unit_value", nullable = false, precision = 15, scale = 2)
    public BigDecimal unitValue;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    public List<ProductComponent> components = new ArrayList<>();
}
package com.bpdb.dms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "sku", length = 150)
    private String sku;

    @Column(name = "brand", length = 150)
    private String brand;

    @Column(name = "model", length = 150)
    private String model;

    @Column(name = "specs_json", columnDefinition = "jsonb")
    private String specsJson;

    @Column(name = "default_warranty_months")
    private Integer defaultWarrantyMonths;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSpecsJson() {
        return specsJson;
    }

    public void setSpecsJson(String specsJson) {
        this.specsJson = specsJson;
    }

    public Integer getDefaultWarrantyMonths() {
        return defaultWarrantyMonths;
    }

    public void setDefaultWarrantyMonths(Integer defaultWarrantyMonths) {
        this.defaultWarrantyMonths = defaultWarrantyMonths;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}

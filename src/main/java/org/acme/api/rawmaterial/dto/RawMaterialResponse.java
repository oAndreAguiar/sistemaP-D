package org.acme.api.rawmaterial.dto;

public class RawMaterialResponse {
    public Long id;
    public String code;
    public String name;
    public Long stockQuantity;

    public static RawMaterialResponse of(Long id, String code, String name, Long stockQuantity) {
        RawMaterialResponse r = new RawMaterialResponse();
        r.id = id;
        r.code = code;
        r.name = name;
        r.stockQuantity = stockQuantity;
        return r;
    }
}
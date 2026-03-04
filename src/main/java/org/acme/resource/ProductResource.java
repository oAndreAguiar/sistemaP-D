package org.acme.resource;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.api.product.dto.*;
import org.acme.domain.Product;
import org.acme.domain.ProductComponent;
import org.acme.domain.RawMaterial;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    public List<ProductResponse> list() {
        return Product.<Product>listAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public ProductResponse getById(@PathParam("id") Long id) {
        Product p = Product.findById(id);
        if (p == null) throw new NotFoundException("Product not found");
        return toResponse(p);
    }

    @POST
    @Transactional
    public Response create(@Valid ProductCreateRequest req) {
        validateNoDuplicateRawMaterials(req.components);

        Product p = new Product();
        p.code = req.code;
        p.name = req.name;
        p.unitValue = req.unitValue;

        for (ProductComponentRequest cReq : req.components) {
            RawMaterial rm = RawMaterial.findById(cReq.rawMaterialId);
            if (rm == null) throw new BadRequestException("RawMaterial not found: " + cReq.rawMaterialId);

            ProductComponent pc = new ProductComponent();
            pc.product = p;
            pc.rawMaterial = rm;
            pc.quantityPerUnit = cReq.quantityPerUnit;

            p.components.add(pc);
        }

        p.persist();

        return Response.status(Response.Status.CREATED)
                .entity(toResponse(p))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public ProductResponse update(@PathParam("id") Long id, @Valid ProductUpdateRequest req) {
        validateNoDuplicateRawMaterials(req.components);

        Product p = Product.findById(id);
        if (p == null) throw new NotFoundException("Product not found");

        p.name = req.name;
        p.unitValue = req.unitValue;

        // ✅ forma mais segura: apaga no banco antes de inserir (evita conflito UNIQUE)
        ProductComponent.delete("product.id", id);
        ProductComponent.getEntityManager().flush();

        // mantém lista em memória consistente
        p.components.clear();

        for (ProductComponentRequest cReq : req.components) {
            RawMaterial rm = RawMaterial.findById(cReq.rawMaterialId);
            if (rm == null) throw new BadRequestException("RawMaterial not found: " + cReq.rawMaterialId);

            ProductComponent pc = new ProductComponent();
            pc.product = p;
            pc.rawMaterial = rm;
            pc.quantityPerUnit = cReq.quantityPerUnit;

            p.components.add(pc);
        }

        // opcional, mas ajuda a “materializar” antes da resposta
        Product.getEntityManager().flush();

        return toResponse(p);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        boolean deleted = Product.deleteById(id);
        if (!deleted) throw new NotFoundException("Product not found");
    }

    private void validateNoDuplicateRawMaterials(List<ProductComponentRequest> components) {
        Set<Long> seen = new HashSet<>();
        for (ProductComponentRequest c : components) {
            if (c.rawMaterialId == null) throw new BadRequestException("rawMaterialId is required");
            if (!seen.add(c.rawMaterialId)) {
                throw new BadRequestException("Duplicate rawMaterialId in components: " + c.rawMaterialId);
            }
        }
    }

    private ProductResponse toResponse(Product p) {
        ProductResponse r = new ProductResponse();
        r.id = p.id;
        r.code = p.code;
        r.name = p.name;
        r.unitValue = p.unitValue;

        r.components = p.components.stream().map(pc -> {
            ProductComponentResponse cr = new ProductComponentResponse();
            cr.rawMaterialId = pc.rawMaterial.id;
            cr.rawMaterialCode = pc.rawMaterial.code;
            cr.rawMaterialName = pc.rawMaterial.name;
            cr.quantityPerUnit = pc.quantityPerUnit;
            return cr;
        }).toList();

        return r;
    }
}
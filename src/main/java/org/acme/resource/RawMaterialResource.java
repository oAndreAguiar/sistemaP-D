package org.acme.resource;

import org.acme.api.rawmaterial.dto.RawMaterialCreateRequest;
import org.acme.api.rawmaterial.dto.RawMaterialResponse;
import org.acme.api.rawmaterial.dto.RawMaterialUpdateRequest;
import org.acme.domain.RawMaterial;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/raw-materials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RawMaterialResource {

    @GET
    public List<RawMaterialResponse> list() {
        return RawMaterial.<RawMaterial>listAll().stream()
                .map(rm -> RawMaterialResponse.of(rm.id, rm.code, rm.name, rm.stockQuantity))
                .toList();
    }

    @GET
    @Path("/{id}")
    public RawMaterialResponse getById(@PathParam("id") Long id) {
        RawMaterial rm = RawMaterial.findById(id);
        if (rm == null) throw new NotFoundException("RawMaterial not found");
        return RawMaterialResponse.of(rm.id, rm.code, rm.name, rm.stockQuantity);
    }

    @POST
    @Transactional
    public Response create(@Valid RawMaterialCreateRequest req) {
        RawMaterial rm = new RawMaterial();
        rm.code = req.code;
        rm.name = req.name;
        rm.stockQuantity = req.stockQuantity;

        rm.persist();

        return Response.status(Response.Status.CREATED)
                .entity(RawMaterialResponse.of(rm.id, rm.code, rm.name, rm.stockQuantity))
                .build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public RawMaterialResponse update(@PathParam("id") Long id, @Valid RawMaterialUpdateRequest req) {
        RawMaterial rm = RawMaterial.findById(id);
        if (rm == null) throw new NotFoundException("RawMaterial not found");

        rm.name = req.name;
        rm.stockQuantity = req.stockQuantity;

        return RawMaterialResponse.of(rm.id, rm.code, rm.name, rm.stockQuantity);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        boolean deleted = RawMaterial.deleteById(id);
        if (!deleted) throw new NotFoundException("RawMaterial not found");
    }
}
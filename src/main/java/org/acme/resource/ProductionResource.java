package org.acme.resource;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.acme.api.production.dto.ProductionPlanRequest;
import org.acme.api.production.dto.ProductionPlanResponse;
import org.acme.service.ProductionPlannerService;

@Path("/production")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductionResource {

    private final ProductionPlannerService planner;

    public ProductionResource(ProductionPlannerService planner) {
        this.planner = planner;
    }

    @POST
    @Path("/plan")
    public ProductionPlanResponse plan(ProductionPlanRequest req) {
        return planner.plan();
    }
}
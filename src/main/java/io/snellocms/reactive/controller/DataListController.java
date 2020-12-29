package io.snellocms.reactive.controller;


import io.snellocms.reactive.service.MetadataService;
import org.jboss.logging.Logger;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static io.snellocms.reactive.management.AppConstants.*;
import static javax.ws.rs.core.Response.ok;


@Path(DATALIST_PATH)
public class DataListController {

    Logger logger = Logger.getLogger(getClass());


    @Inject
    MetadataService metadataService;

    @GET
    @Path(DATA_LIST_NAMES)
    public Response names() throws Exception {
        return ok(metadataService.names()).build();
    }

    @GET
    @Path(DATA_LIST_METADATA_NAMES)
    public Response metadata(@NotNull String name) throws Exception {
        return ok(metadataService.metadata(name)).build();
    }

    @GET
    @Path(DATA_LIST_FIELD_DEFINITIONS)
    public Response fielddefinitions(@NotNull String name) throws Exception {
        return ok(metadataService.fielddefinitions(name)).build();
    }

    @GET
    @Path(DATA_LIST_CONDITIONS)
    public Response conditions(@NotNull String name) throws Exception {
        return ok(metadataService.conditions(name)).build();
    }

}

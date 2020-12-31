package io.snellocms.reactive.controller;


import io.snellocms.reactive.management.AppConstants;
import io.snellocms.reactive.service.ApiService;
import io.snellocms.reactive.service.documents.DocumentsService;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import org.jboss.resteasy.reactive.RestQuery;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.util.Map;

import static io.snellocms.reactive.management.AppConstants.*;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;


@Path(DOCUMENTS_PATH)
public class DocumentsController {

    Logger logger = Logger.getLogger(getClass());

    @Inject
    ApiService apiService;

    String table = DOCUMENTS;

    @Inject
    DocumentsService documentsService;

    @Context
    Request request;

    @Context
    UriInfo uriInfo;

    @GET
    public Response list(@Nullable @RestQuery(SORT_PARAM) String sort,
                         @Nullable @RestQuery(LIMIT_PARAM) String limit,
                         @Nullable @RestQuery(START_PARAM) String start) throws Exception {
        if (sort != null)
            logger.info(SORT_DOT_DOT + sort);
        if (limit != null)
            logger.info(LIMIT_DOT_DOT + limit);
        if (start != null)
            logger.info(START_DOT_DOT + start);
        Integer l = limit == null ? 10 : Integer.valueOf(limit);
        Integer s = start == null ? 0 : Integer.valueOf(start);
        return ok(apiService.list(table, uriInfo.getQueryParameters(), sort, l, s))
                .header(SIZE_HEADER_PARAM, EMPTY + apiService.count(table, uriInfo.getQueryParameters()))
                .header(TOTAL_COUNT_HEADER_PARAM, EMPTY + apiService.count(table, uriInfo.getQueryParameters())).build();
    }

    @GET
    @Path(UUID_PATH_PARAM)
    public Response fetch(@NotNull String uuid) throws Exception {
        return ok(apiService.fetch(null, table, uuid, UUID)).build();
    }

    @GET
    @Path(UUID_PATH_PARAM + DOWNLOAD_PATH)
    public Response download(@NotNull String uuid) throws Exception {
        Map<String, Object> map = apiService.fetch(null, table, uuid, AppConstants.UUID);
        String path = (String) map.get(DOCUMENT_PATH);
        String mimetype = (String) map.get(DOCUMENT_MIME_TYPE);
        String fileName = (String) map.get(FILE_NAME);
        return Response.ok(documentsService.streamingOutput(path, mimetype))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();


    }

    @GET
    @Path(UUID_PATH_PARAM + DOWNLOAD_PATH + "/{name}")
    public Response downloadWithName(@NotNull String uuid, @NotNull String name) throws Exception {
        Map<String, Object> map = apiService.fetch(null, table, uuid, AppConstants.UUID);
        String path = (String) map.get(DOCUMENT_PATH);
        String mimetype = (String) map.get(DOCUMENT_MIME_TYPE);
        String fileName = (String) map.get(FILE_NAME);
//        return documentsService.streamingOutput(path, mimetype);
        return Response.ok(documentsService.streamingOutput(path, mimetype))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .build();

    }


    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response post(
            @HeaderParam(CONTENT_TYPE)
                    MediaType mediaType,
            @FormParam(FILE)
            @PartType(MediaType.APPLICATION_OCTET_STREAM)
                    InputStream file,
            @FormParam(FILE_NAME)
            @PartType(MediaType.TEXT_PLAIN)
                    String filename,
            @FormParam(TABLE_NAME)
            @PartType(MediaType.TEXT_PLAIN)
                    String table_name,
            @FormParam(TABLE_KEY)
            @PartType(MediaType.TEXT_PLAIN)
                    String table_key) {
        try {
            String uuid = java.util.UUID.randomUUID().toString();
            Map<String, Object> map = documentsService.upload(file, mediaType, filename, uuid, table_name, table_key);
            map = apiService.create(table, map, AppConstants.UUID);
            return ok(map).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return serverError().build();
    }


    @PUT
    @Path(UUID_PATH_PARAM)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response put(
            @PathParam(UUID) String uuid,
            @HeaderParam(CONTENT_TYPE)
                    MediaType mediaType,
            @FormParam(FILE)
            @PartType(MediaType.APPLICATION_OCTET_STREAM)
                    InputStream file,
            @FormParam(FILE_NAME)
            @PartType(MediaType.TEXT_PLAIN)
                    String filename,
            @FormParam(TABLE_NAME)
            @PartType(MediaType.TEXT_PLAIN)
                    String table_name,
            @FormParam(TABLE_KEY)
            @PartType(MediaType.TEXT_PLAIN)
                    String table_key) {
        try {
            Map<String, Object> map = documentsService.upload(file, mediaType, filename, uuid, table_name, table_key);
            map = apiService.merge(table, map, uuid, AppConstants.UUID);
            return ok(map).build();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return serverError().build();
    }

    @DELETE
    @Path(UUID_PATH_PARAM)
    public Response delete(@NotNull String uuid, @Nullable @RestQuery(DELETE_PARAM) String delete) throws Exception {
        Map<String, Object> map = apiService.fetch(null, table, uuid, AppConstants.UUID);
        if (delete != null && delete.toLowerCase().equals(TRUE)) {
            documentsService.delete((String) map.get(DOCUMENT_PATH));
        }
        if (map != null) {
            boolean result = apiService.delete(table, uuid, AppConstants.UUID);
            if (result) {
                return ok().build();
            } else {
                return serverError().build();
            }
        } else {
            return serverError().build();
        }
    }
}

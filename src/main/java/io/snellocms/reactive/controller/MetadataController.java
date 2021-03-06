package io.snellocms.reactive.controller;


import io.snellocms.reactive.service.ApiService;
import org.jboss.logging.Logger;

import javax.annotation.Nullable;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.HashMap;
import java.util.Map;

import static io.snellocms.reactive.management.AppConstants.METADATAS;
import static io.snellocms.reactive.management.AppConstants.METADATA_PATH;

@Path(METADATA_PATH)
public class MetadataController {


    @Inject
    ApiService apiService;

    static String table = METADATAS;
    static String default_sort = " table_name asc ";


    @Inject
    Event eventPublisher;


    Logger logger = Logger.getLogger(getClass());

    @GET
    public Response list(HttpRequest<?> request,
                                @Nullable @RestQuery(SORT_PARAM) String sort,
                                @Nullable @RestQuery(LIMIT_PARAM) String limit,
                                @Nullable @RestQuery(START_PARAM) String start) throws Exception {
        if (sort != null)
            logger.info(SORT_DOT_DOT + sort);
        else
            sort = default_sort;
        if (limit != null)
            logger.info(LIMIT_DOT_DOT + limit);
        if (start != null)
            logger.info(START_DOT_DOT + start);
        Integer l = limit == null ? 10 : Integer.valueOf(limit);
        Integer s = start == null ? 0 : Integer.valueOf(start);
        return ok(apiService.list(table, uriInfo.getQueryParameters(), sort, l, s))
                .header(SIZE_HEADER_PARAM, EMPTY + apiService.count(table, uriInfo.getQueryParameters()))
                .header(TOTAL_COUNT_HEADER_PARAM, EMPTY + apiService.count(table, uriInfo.getQueryParameters()));
    }


    @GET
    @Path(UUID_PATH_PARAM)
    public Response fetch(HttpRequest<?> request, @NotNull String uuid) throws Exception {
        return ok(apiService.fetch(null, table, uuid, UUID));
    }

    @GET
    @Path(UUID_PATH_PARAM_CREATE)
    public Response createTable(@NotNull String uuid) throws Exception {
        Metadata metadata = apiService.createMetadataTable(uuid);
        Map<String, Object> updateMetadataMap = new HashMap<>();
        updateMetadataMap.put(CREATED, true);
        apiService.merge(METADATAS, updateMetadataMap, uuid, UUID);
        return ok(apiService.fetch(null, table, uuid, UUID));
    }

    @GET
    @Path(UUID_PATH_PARAM_DELETE)
    public Response deleteTable(@NotNull String uuid) throws Exception {
        boolean result = apiService.deleteTable(uuid);
        if (result) {
            Map<String, Object> updateMetadataMap = new HashMap<>();
            updateMetadataMap.put(CREATED, false);
            apiService.merge(METADATAS, updateMetadataMap, uuid, UUID);
        }
        return ok(apiService.fetch(null, table, uuid, UUID));
    }

    @GET
    @Path(UUID_PATH_PARAM_TRUNCATE)
    public Response truncateTable(@NotNull String uuid) throws Exception {
        apiService.truncateTable(uuid);
        return ok(apiService.fetch(null, table, uuid, UUID));
    }


    @Post()
    public Response post(@Body String body) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        if (map.get(TABLE_NAME) == null) {
            throw new Exception(MSG_TABLE_NAME_IS_EMPTY);
        }
        if (MetadataUtils.isReserved(map.get(TABLE_NAME))) {
            throw new Exception(MSG_TABLE_NAME_IS_RESERVED);
        }
        map.put(UUID, java.util.UUID.randomUUID().toString());
        map = apiService.create(table, map, UUID);
        eventPublisher.publishEvent(new MetadataCreateUpdateEvent(map));
        return ok(map);
    }

     @PUT
    @Path(UUID_PATH_PARAM)
    public Response put(@Body String body, @NotNull String uuid) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        if (map.get(TABLE_NAME) == null) {
            throw new Exception(MSG_TABLE_NAME_IS_EMPTY);
        }
        if (MetadataUtils.isReserved(map.get(TABLE_NAME))) {
            throw new Exception(MSG_TABLE_NAME_IS_EMPTY);
        }
        map = apiService.merge(table, map, uuid, UUID);
        eventPublisher.publishEvent(new MetadataCreateUpdateEvent(map));
        return ok(map);
    }

    @DELETE
    @Path(UUID_PATH_PARAM)
    public Response delete(HttpRequest<?> request, @NotNull String uuid) throws Exception {
        boolean result = apiService.delete(table, uuid, UUID);
        boolean deleteFieldDefinitionsByMetadataUuid = apiService.deleteFieldDefinitionsByMetadataUuid(uuid);
        if (result && deleteFieldDefinitionsByMetadataUuid) {
            eventPublisher.publishEvent(new MetadataDeleteEvent(uuid));
            return ok();
        }
        return serverError();
    }
}

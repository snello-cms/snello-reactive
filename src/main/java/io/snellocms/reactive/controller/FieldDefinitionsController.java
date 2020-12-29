package io.snellocms.reactive.controller;


import io.snellocms.reactive.management.AppConstants;
import io.snellocms.reactive.service.ApiService;
import org.jboss.logging.Logger;

import javax.annotation.Nullable;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.Map;

import static io.snellocms.reactive.management.AppConstants.FIELD_DEFINITIONS_PATH;


@Path(FIELD_DEFINITIONS_PATH)
public class FieldDefinitionsController {


    @Inject
    ApiService apiService;

    String table = AppConstants.FIELD_DEFINITIONS;
    static String default_sort = " name asc ";


    @Inject
    Event eventPublisher;


    Logger logger = Logger.getLogger(getClass());

    @GET
    @Path(AppConstants.BASE_PATH)
    public Response list(HttpRequest<?> request,
                                @Nullable @RestQuery(AppConstants.SORT_PARAM) String sort,
                                @Nullable @RestQuery(AppConstants.LIMIT_PARAM) String limit,
                                @Nullable @RestQuery(AppConstants.START_PARAM) String start) throws Exception {
        if (sort != null)
            logger.info(AppConstants.SORT_DOT_DOT + sort);
        else
            sort = default_sort;
        if (limit != null)
            logger.info(AppConstants.LIMIT_DOT_DOT + limit);
        if (start != null)
            logger.info(AppConstants.START_DOT_DOT + start);
        Integer l = limit == null ? 10 : Integer.valueOf(limit);
        Integer s = start == null ? 0 : Integer.valueOf(start);
        return ok(apiService.list(table, uriInfo.getQueryParameters(), sort, l, s))
                .header(AppConstants.SIZE_HEADER_PARAM, AppConstants.EMPTY + apiService.count(table, uriInfo.getQueryParameters()))
                .header(AppConstants.TOTAL_COUNT_HEADER_PARAM, AppConstants.EMPTY + apiService.count(table, uriInfo.getQueryParameters()));
    }


    @GET
    @Path(AppConstants.UUID_PATH_PARAM)
    public Response fetch(HttpRequest<?> request, @NotNull String uuid) throws Exception {
        return ok(apiService.fetch(null, table, uuid, AppConstants.UUID));
    }


    @Post()
    public Response post(@Body String body) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        map.put(AppConstants.UUID, java.util.UUID.randomUUID().toString());
        map = apiService.create(table, map, AppConstants.UUID);
        eventPublisher.publishEvent(new FieldDefinitionCreateUpdateEvent(map));
        return ok(map);
    }

     @PUT
    @Path(AppConstants.UUID_PATH_PARAM)
    public Response put(@Body String body, @NotNull String uuid) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        map = apiService.merge(table, map, uuid, AppConstants.UUID);
        eventPublisher.publishEvent(new FieldDefinitionCreateUpdateEvent(map));
        return ok(map);
    }

    @DELETE
    @Path(AppConstants.UUID_PATH_PARAM)
    public Response delete(HttpRequest<?> request, @NotNull String uuid) throws Exception {
        boolean result = apiService.delete(table, uuid, AppConstants.UUID);
        if (result) {
            eventPublisher.publishEvent(new FieldDefinitionDeleteEvent(uuid));
            return ok();
        }
        return serverError();
    }
}

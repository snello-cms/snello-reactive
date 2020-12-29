package io.snellocms.reactive.controller;


import io.snellocms.reactive.management.AppConstants;
import io.snellocms.reactive.service.ApiService;
import org.jboss.logging.Logger;

import javax.annotation.Nullable;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.ws.rs.Path;
import java.util.Map;


@Path(AppConstants.DROPPABLES_PATH)
public class DroppablesController {

    @Inject
    ApiService apiService;

    String table = AppConstants.DROPPABLES;
    String UUID = AppConstants.UUID;

    Logger logger = Logger.getLogger(getClass());


    @Inject
    Event eventPublisher;


    @GET
    public Response list(HttpRequest<?> request,
                                @Nullable @RestQuery(AppConstants.SORT_PARAM) String sort,
                                @Nullable @RestQuery(AppConstants.LIMIT_PARAM) String limit,
                                @Nullable @RestQuery(AppConstants.START_PARAM) String start) throws Exception {
        if (sort != null)
            logger.info(AppConstants.SORT_DOT_DOT + sort);
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
        return ok(apiService.fetch(null, table, uuid, UUID));
    }


    @Post()
    public Response post(@Body String body) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        map.put(UUID, map.get(AppConstants.NAME));
        map = apiService.create(table, map, UUID);
        eventPublisher.publishEvent(new DroppableCreateUpdateEvent(map));
        return ok(map);
    }

     @PUT
    @Path(AppConstants.UUID_PATH_PARAM)
    public Response put(@Body String body, @NotNull String uuid) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        map = apiService.merge(table, map, uuid, UUID);
        eventPublisher.publishEvent(new DroppableCreateUpdateEvent(map));
        return ok(map);
    }

    @DELETE
    @Path(AppConstants.UUID_PATH_PARAM)
    public Response delete(HttpRequest<?> request, @NotNull String uuid) throws Exception {
        boolean result = apiService.delete(table, uuid, UUID);
        if (result) {
            eventPublisher.publishEvent(new DroppableDeleteEvent(uuid));
            return ok();
        }
        return serverError();
    }
}

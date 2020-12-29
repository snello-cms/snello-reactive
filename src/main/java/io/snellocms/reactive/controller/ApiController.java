package io.snellocms.reactive.controller;


import io.snellocms.reactive.model.FieldDefinition;
import io.snellocms.reactive.model.Metadata;
import io.snellocms.reactive.service.ApiService;
import io.snellocms.reactive.util.JsonUtils;
import io.snellocms.reactive.util.TableKeyUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestQuery;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static io.snellocms.reactive.management.AppConstants.*;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.serverError;


@Path(API_PATH)
public class ApiController {

    Logger logger = Logger.getLogger(getClass());

    @Inject
    ApiService apiService;

    @Context
    Request request;

    @Context
    UriInfo uriInfo;

    public ApiController() {
    }

    private void debug(String path) {
        logger.info("------------");
        logger.info("METHOD: " + request.getMethod());
        logger.info("RELATIVE PATH: " + path);
        StringBuffer sb = new StringBuffer();
        uriInfo.getQueryParameters().forEach((param, value) -> sb.append(param + ":" + value));
        if (sb.length() > 0) {
            logger.info("QUERY: " + path);
        }
        logger.info("------------");
        logger.info(uriInfo.getPath());
        logger.info("------------");
        uriInfo.getQueryParameters().forEach((param, value) -> sb.append(param + ":" + value));
        logger.info("------------");
    }

    @GET
    @Path(TABLE_PATH_PARAM)
    public Response list(
            @NotNull String table,
            @Nullable @RestQuery(SORT_PARAM) String sort,
            @Nullable @RestQuery(LIMIT_PARAM) String limit,
            @Nullable @RestQuery(START_PARAM) String start) throws Exception {
        if (sort != null)
            logger.info(SORT_DOT_DOT + sort);
        if (limit != null)
            logger.info(LIMIT_DOT_DOT + limit);
        if (start != null)
            logger.info(START_DOT_DOT + start);
        debug(null);
        Integer l = limit == null ? 10 : Integer.valueOf(limit);
        Integer s = start == null ? 0 : Integer.valueOf(start);
        long count = apiService.count(table, uriInfo.getQueryParameters());
        return ok(apiService.list(table, uriInfo.getQueryParameters(), sort, l, s))
                .header(SIZE_HEADER_PARAM, "" + count)
                .header(TOTAL_COUNT_HEADER_PARAM, "" + count).build();
    }


    @GET
    @Path(TABLE_PATH_PARAM + UUID_PATH_PARAM)
    public Response fetch(@NotNull String table, @NotNull String uuid) throws Exception {
        debug(null);
        String key = apiService.table_key(table);
        return ok(apiService.fetch(uriInfo.getQueryParameters(), table, uuid, key)).build();
    }


    @GET
    @Path(TABLE_PATH_PARAM + UUID_PATH_PARAM + EXTRA_PATH_PARAM)
    public Response get(@NotNull String table, @NotNull String uuid, @NotNull String path,
                        @Nullable @RestQuery(SORT_PARAM) String sort,
                        @Nullable @RestQuery(LIMIT_PARAM) String limit,
                        @Nullable @RestQuery(START_PARAM) String start) throws Exception {
        debug(path);
        if (path == null) {
            throw new Exception(MSG_PATH_IS_EMPTY);
        }
        if (start == null) {
            start = _0;
        }
        if (limit == null) {
            limit = _10;
        }
        logger.info("path accessorio: " + path);
        if (path.contains("/")) {
            String[] pars = path.split(BASE_PATH);
            if (pars.length > 1) {
                return ok(apiService.fetch(uriInfo.getQueryParameters(), pars[0], pars[1], UUID)).build();
            } else {
                return ok(apiService.list(pars[0], uriInfo.getQueryParameters(), sort, Integer.valueOf(limit), Integer.valueOf(start))).build();
            }
        } else {
            MultivaluedMap<String, String> parametersMap = null;
            if (uriInfo.getQueryParameters() != null) {
                parametersMap = uriInfo.getQueryParameters();
            } else {
                parametersMap = new MultivaluedHashMap<>();
            }
            parametersMap.put(table + "_id", Arrays.asList(new String[]{uuid}));
            parametersMap.put("join_table", Arrays.asList(new String[]{table + "_" + path}));
            return ok(apiService.list(path, parametersMap, sort, Integer.valueOf(limit), Integer.valueOf(start))).build();
        }
    }


    @POST
    @Path(TABLE_PATH_PARAM)
    public Response post(String body, @NotNull String table) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        Metadata metadata = apiService.metadataWithFields(table);
        String key = metadata.table_key;
        TableKeyUtils.generateUUid(map, metadata, apiService);
        // CI VUOLE UNA TRANSAZIONE PER TENERE TUTTO INSIEME
        for (FieldDefinition fd : metadata.fields) {
            if ("multijoin".equals(fd.type)) {
                if (map.containsKey(fd.name) && map.get(fd.name) != null) {
                    String join_table_uuids_value = (String) map.get(fd.name);
                    String[] join_table_uuids = join_table_uuids_value.split(",|;");
                    for (String ss : join_table_uuids) {
                        String join_table_name = metadata.table_name + "_" + fd.join_table_name;
                        String table_id = metadata.table_name + "_id";
                        String join_table_id = fd.join_table_name + "_id";
                        Map<String, Object> join_map = new HashMap<>();
                        join_map.put(table_id, map.get(metadata.table_key));
                        join_map.put(join_table_id, ss.trim());
                        apiService.createFromMap(join_table_name, join_map);
                    }
                    //ELIMINO I VALORI NEL CAMPO DI APPOGGIO
                    map.remove(fd.name);
                }
            }
        }
        map = apiService.create(table, map, key);
        return ok(map).build();
    }

    @PUT
    @Path(TABLE_PATH_PARAM + UUID_PATH_PARAM)
    public Response put(String body, @NotNull String table, @NotNull String uuid) throws Exception {
        Map<String, Object> map = JsonUtils.fromJson(body);
        boolean renewSlug = TableKeyUtils.isSlug(apiService.metadata(table));
        String key = apiService.table_key(table);
        if (renewSlug) {
            String fieldSluggable = apiService.slugField(table);
            String toSlugValue = (String) map.get(fieldSluggable);
            String slugged = TableKeyUtils.createSlug(toSlugValue);
            logger.info("toSlugValue: " + toSlugValue + ", old slug: " + uuid);
            if (!uuid.equals(slugged)) {
                logger.info("renew slug");
                TableKeyUtils.generateUUid(map, apiService.metadata(table), apiService);
            } else {
                logger.info(" slug is the same!!");
            }
        }
        // CI VUOLE UNA TRANSAZIONE PER TENERE TUTTO INSIEME
        map = apiService.merge(table, map, uuid, key);
        //DEVO ELIMINARE TUTTI I VALORI
        Metadata metadata = apiService.metadataWithFields(table);
        if (metadata.fields != null && metadata.fields.size() > 0) {
            for (FieldDefinition fd : metadata.fields) {
                if ("multijoin".equals(fd.type)) {
                    String join_table_name = metadata.table_name + "_" + fd.join_table_name;
                    String table_id = metadata.table_name + "_id";
                    apiService.delete(join_table_name, table_id, uuid);
                    if (map.containsKey(fd.name) && map.get(fd.name) != null) {
                        String join_table_uuids_value = (String) map.get(fd.name);
                        String[] join_table_uuids = join_table_uuids_value.split(",|;");
                        for (String ss : join_table_uuids) {
                            String join_table_id = fd.join_table_name + "_id";
                            Map<String, Object> join_map = new HashMap<>();
                            join_map.put(table_id, map.get(metadata.table_key));
                            join_map.put(join_table_id, ss.trim());
                            apiService.createFromMap(join_table_name, join_map);
                        }
                    }
                    //ELIMINO I VALORI NEL CAMPO DI APPOGGIO
                    map.remove(fd.name);
                }
            }
        }
        return ok(map).build();
    }

    @DELETE
    @Path(TABLE_PATH_PARAM + UUID_PATH_PARAM)
    public Response delete(@NotNull String table, @NotNull String uuid) throws Exception {
        debug(null);
        String key = apiService.table_key(table);
        boolean result = apiService.delete(table, uuid, key);
        if (result)
            return ok().build();
        return serverError().build();
    }


}

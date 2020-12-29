package io.snellocms.reactive.service;


import io.snellocms.reactive.model.Condition;
import io.snellocms.reactive.model.FieldDefinition;
import io.snellocms.reactive.model.Metadata;
import io.snellocms.reactive.model.SelectQuery;
import io.snellocms.reactive.repository.JdbcRepository;
import io.snellocms.reactive.util.ParamUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.snellocms.reactive.management.AppConstants.*;
import static io.snellocms.reactive.management.DbConstants.DROP_TABLE;
import static io.snellocms.reactive.management.DbConstants.TRUNCATE_TABLE;


@Singleton
public class ApiService {

    @Inject
    MetadataService metadataService;

    @Inject
    JdbcRepository jdbcRepository;

    public ApiService() {
    }


    public Metadata metadata(String metadata_name) throws Exception {
        Metadata metadata = metadataService.metadataMap().get(metadata_name);
        return metadata;
    }

    public Metadata metadataWithFields(String metadata_name) throws Exception {
        Metadata metadata = metadataService.metadataMap().get(metadata_name);
        metadata.fields = metadataService.fielddefinitions(metadata.table_name);
        return metadata;
    }

    public String slugField(String metadata_name) throws Exception {
        return metadataService.metadataMap().get(metadata_name).table_key_addition;
    }


    public String table_key(String metadata_name) throws Exception {
        Metadata metadata = metadataService.metadataMap().get(metadata_name);
        return metadata.table_key;
    }

    public long count(String table, MultivaluedMap<String, String> httpParameters) throws Exception {
        String alias_condition = null;
        List<Condition> conditions = null;
        if (metadataService.selectqueryMap().containsKey(table)) {
            return 0;
        }
        if (metadataService.metadataMap().containsKey(table)) {
            conditions = metadataService.conditionsMap().get(table);
            Metadata metadata = metadataService.metadataMap().get(table);
            if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
                table = metadata.alias_table;
                alias_condition = metadata.alias_condition;
            }
        }
        return jdbcRepository.count(table, alias_condition, httpParameters, conditions);
    }

    public boolean exist(String table, String table_key, Object uuid) throws Exception {
        return jdbcRepository.exist(table, table_key, uuid);
    }


    public List<Map<String, Object>> list(String table, MultivaluedMap<String, String> httpParameters, String sort, int limit,
                                          int start) throws Exception {
        // select fields:
        String select_fields = ParamUtils.select_fields(httpParameters);
        String alias_condition = null;
        List<Condition> conditions = null;
        if (metadataService.selectqueryMap().containsKey(table)) {
            SelectQuery selectQuery = metadataService.selectqueryMap.get(table);
            if (selectQuery.with_params) {
                return jdbcRepository.list(selectQuery.select_query, httpParameters, conditions, sort, limit, start);
            } else {
                return jdbcRepository.list(selectQuery.select_query);
            }
        }
        if (metadataService.metadataMap().containsKey(table)) {
            Metadata metadata = metadataService.metadataMap().get(table);
            conditions = metadataService.conditionsMap().get(table);
            if (select_fields == null) {
                if (metadata.select_fields != null && !metadata.select_fields.trim().isEmpty()) {
                    //"_SELECT_ * _FROM_ "
                    select_fields = metadata.select_fields;
                }
            } else {
                System.out.println("uso i httpParameters:" + select_fields);
            }

            if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
                table = metadata.alias_table;
                alias_condition = metadata.alias_condition;
            }

            if (metadata.order_by != null && !metadata.order_by.trim().isEmpty()) {
                sort = metadata.order_by;
            }

        }
        return jdbcRepository.list(table, select_fields, alias_condition, httpParameters, conditions, sort, limit, start);
    }

//    public List<Map<String, Object>> list(String table, MultivaluedMap<String, String> httpParameters, String sort, int limit,
//                                          int start) throws Exception {
//        return list(table, getMap(httpParameters), sort, limit, start);
//    }

    public Map<String, Object> create(String table, Map<String, Object> map, String table_key) throws Exception {
        table = initTable(table);
        table_key = metadataService.initTableKey(table, table_key);
        return jdbcRepository.create(table, table_key, map);
    }

    public Map<String, Object> createFromMap(String table, Map<String, Object> map) throws Exception {
        return jdbcRepository.create(table, UUID, map);
    }

    public Map<String, Object> merge(String table, Map<String, Object> map, String key, String table_key) throws Exception {
        table = initTable(table);
        table_key = metadataService.initTableKey(table, table_key);
        return jdbcRepository.update(table, table_key, map, key);
    }

    public boolean truncateTable(String uuid) throws Exception {
        Metadata metadata = metadataService.byUUid(uuid);
        String table;
        if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
            table = metadata.alias_table;
        }
        table = metadata.table_name;
        return jdbcRepository.query(TRUNCATE_TABLE + table, null);
    }

    public boolean deleteTable(String uuid) throws Exception {
        Metadata metadata = metadataService.byUUid(uuid);
        metadata.fields = metadataService.fielddefinitions(metadata.table_name);
        for (FieldDefinition fd : metadata.fields) {
            if ("multijoin".equals(fd.type)) {
                String join_table_name = metadata.table_name + "_" + fd.join_table_name;
                jdbcRepository.query(DROP_TABLE + join_table_name, null);
            }
        }
        //DEVO ELIMINARE TUTTE LE CONDITIONS COLLEGATE
        jdbcRepository.delete(CONDITIONS, "metadata_multijoin_uuid", metadata.uuid);
        if (metadata.already_exist) {
            throw new Exception("metadata was already_exist: we can't destroy!");
        }
        String table;
        if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
            table = metadata.alias_table;
        }
        table = metadata.table_name;
        return jdbcRepository.query(DROP_TABLE + table, null);
    }


    public Map<String, Object> createIfNotExists(String table, Map<String, Object> map, String table_key) throws Exception {
        alreadyExists(table);
        return jdbcRepository.create(table, table_key, map);
    }

    public Map<String, Object> mergeIfNotExists(String table, Map<String, Object> map, String key, String table_key) throws Exception {
        alreadyExists(table);
        return jdbcRepository.update(table, table_key, map, key);
    }


    public void alreadyExists(String table) throws Exception {
        if (metadataService.metadataMap().containsKey(table) || metadataService.selectqueryMap().containsKey(table)) {
            throw new Exception(" metadata or selectquery already exists! ");
        }
    }

    public String initTable(String table) throws Exception {
        if (metadataService.metadataMap().containsKey(table)) {
            Metadata metadata = metadataService.metadataMap().get(table);
            if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
                return metadata.alias_table;
            }
            return metadata.table_name;
        }
        return table;
    }

//    private Map<String, List<String>> getMap(MultivaluedMap<String, String> httpParameters) {
//        if (httpParameters != null) {
//            return httpParameters.asMap();
//        }
//        return null;
//    }


    public Map<String, Object> fetch(MultivaluedMap<String, String> httpParameters, String table, String uuid, String table_key) throws Exception {
        String select_fields = ParamUtils.select_fields(httpParameters);
        if (metadataService.metadataMap().containsKey(table)) {
            Metadata metadata = metadataService.metadataMap().get(table);
            if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
                table = metadata.alias_table;
                table_key = metadata.table_key;
            }
        }
        return jdbcRepository.fetch(select_fields, table, table_key, uuid);
    }

    public boolean delete(String table, String uuid, String table_key) throws Exception {
        if (metadataService.metadataMap().containsKey(table)) {
            Metadata metadata = metadataService.metadataMap().get(table);
            if (metadata.alias_table != null && !metadata.alias_table.trim().isEmpty()) {
                table = metadata.alias_table;
                table_key = metadata.table_key;
            }
        }
        return jdbcRepository.delete(table, table_key, uuid);
    }

    public boolean deleteFieldDefinitionsByMetadataUuid(String uuid) throws Exception {
        List<Object> uuidList = new ArrayList<>();
        uuidList.add(uuid);
        return jdbcRepository.query(DELETE_FROM_FD, uuidList);
    }

    public void batch(String[] queries) throws Exception {
        jdbcRepository.batch(queries);
    }

    public Metadata createMetadataTable(String uuid) throws Exception {
        return metadataService.createTableFromMetadata(uuid);
    }

    public boolean createUserRole(List<Object> values) throws Exception {
        return jdbcRepository.query(jdbcRepository.getUserRoleQuery(), values);
    }

    public boolean query(String query, List<Object> values) throws Exception {
        return jdbcRepository.query(query, values);
    }

}

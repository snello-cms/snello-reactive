package io.snellocms.reactive.repository;


import io.quarkus.runtime.StartupEvent;
import io.snellocms.reactive.model.Condition;
import io.snellocms.reactive.model.FieldDefinition;
import io.snellocms.reactive.model.Metadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

public interface JdbcRepository {


    void onLoad(final StartupEvent event);

    String[] creationQueries();

    Connection getConnection() throws SQLException;

    default void openTransaction(Connection connection) throws Exception {
        connection.setAutoCommit(false);
    }

    default void closeTransaction(Connection connection) throws Exception {
        connection.commit();
    }

    default void rollbackTransaction(Connection connection) throws Exception {
        connection.rollback();
    }


    default boolean executeQuery(Connection connection, String sql) throws Exception {
        Statement statement = connection.createStatement();
        int result = statement.executeUpdate(sql);
        if (result > 0) {
            return true;
        }
        return false;
    }

    long count(String table, String alias_condition, Map<String, List<String>> httpParameters, List<Condition> conditions) throws Exception;

    long count(String select_query, Map<String, List<String>> httpParameters, List<Condition> conditions) throws Exception;

    long count(String select_query) throws Exception;

    boolean exist(String table, String table_key, Object uuid) throws Exception;


    List<Map<String, Object>> list(String table, String sort) throws Exception;

    List<Map<String, Object>> list(String query) throws Exception;

    List<Map<String, Object>> list(String query, Map<String, List<String>> httpParameters, List<Condition> conditions, String sort, int limit, int start) throws Exception;

    List<Map<String, Object>> list(String table, String select_fields, String alias_condition, Map<String, List<String>> httpParameters, List<Condition> conditions, String sort, int limit, int start) throws Exception;

    Map<String, Object> create(String table, String table_key, Map<String, Object> map) throws Exception;

    Map<String, Object> update(String table, String table_key, Map<String, Object> map, String key) throws Exception;

    Map<String, Object> fetch(String select_fields, String table, String table_key, String uuid) throws Exception;

    boolean delete(String table, String table_key, String uuid) throws Exception;

    void batch(String[] queries) throws Exception;

    boolean executeQuery(String sql) throws Exception;

    boolean verifyTable(String tableName) throws Exception;

//    UserDetails login(String username, String password) throws Exception;

    List<String> roles(String username) throws Exception;

    boolean query(String query, List<Object> values) throws Exception;

    String getUserRoleQuery();

    String getJoinTableQuery();

    String escape(String name);

    String fieldDefinition2Sql(FieldDefinition fieldDefinition) throws Exception;

    String createTableSql(Metadata metadata, List<FieldDefinition> fields, List<String> joiQueries, List<Condition> conditions) throws Exception;

}

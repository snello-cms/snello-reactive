package io.snellocms.reactive.repository.postgresql;

import io.quarkus.runtime.StartupEvent;
import io.snellocms.reactive.model.Condition;
import io.snellocms.reactive.model.FieldDefinition;
import io.snellocms.reactive.model.Metadata;
import io.snellocms.reactive.model.events.DbCreatedEvent;
import io.snellocms.reactive.repository.JdbcRepository;
import io.snellocms.reactive.util.ConditionUtils;
import io.snellocms.reactive.util.ParamUtils;
import io.snellocms.reactive.util.SqlHelper;
import org.jboss.logging.Logger;

import javax.enterprise.event.Event;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static io.snellocms.reactive.management.DbConstants.*;
import static io.snellocms.reactive.repository.postgresql.PostgresqlConstants.*;


@Singleton
public class PostgresqlJdbcRepository implements JdbcRepository {

    DataSource dataSource;
    Logger logger = Logger.getLogger(getClass());


    @Inject
    Event eventPublisher;


    public PostgresqlJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }


    public void onLoad(@ObservesAsync StartupEvent event) {
        logger.info("Creation queries at startup: " + event.toString());
        try {
            batch(creationQueries());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        eventPublisher.fireAsync(new DbCreatedEvent());
    }

    @Override
    public String[] creationQueries() {
        return new String[]{
                creationQueryMetadatas,
                creationQueryFieldDefinitions,
                creationQueryConditions,
                creationQueryDocuments,
                creationQueryDraggables,
                creationQueryDroppables,
                creationQueryExtensions,
                creationQuerySelectQueries,
                creationUsersQueries,
                creationRolesQueries,
                creationUserRolesQueries,
                creationUrlMapRulesQueries,
                creationAdminUser,
                creationAdminRole,
                creationAdminUserRole,
                creationLinksQueries,
                creationConditionsViewRole,
                creationConditionsEditRole,
                creationDocumentsViewRole,
                creationDocumentsEditRole,
                creationExtensionsViewRole,
                creationExtensionsEditRole,
                creationFieldDefinitionsViewRole,
                creationFieldDefinitionsEditRole,
                creationLinksViewRole,
                creationLinksEditRole,
                creationMetadatasViewRole,
                creationMetadatasEditRole,
                creationRoleViewRole,
                creationRoleEditRole,
                creationSelectQueryViewRole,
                creationSelectQueryEditRole,
                creationUrlMapRuleViewRole,
                creationUrlMapRuleEditRole,
                creationUserViewRole,
                creationUserEditRole,
                creationDraggableEditRole,
                creationDraggableViewRole,
                creationDroppableEditRole,
                creationDroppableViewRole,
                creationContentsViewRole,
                creationContentsEditRole,
                creationPublicdataEditRole,
                creationChangePasswordTokenQueries
        };
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public long count(String table, String alias_condition, Map<String, List<String>> httpParameters, List<Condition> conditions) throws Exception {
        StringBuffer where = new StringBuffer();
        StringBuffer select = new StringBuffer();
        List<Object> in = new LinkedList<>();
        select.append(COUNT_QUERY);
        if (alias_condition != null)
            where.append(alias_condition);
        boolean withCondition = false;
        try {
            withCondition = ConditionUtils.where(httpParameters, conditions, where, in);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        if (!withCondition) {
            ParamUtils.where(httpParameters, where, in);
        }
        try (
                Connection connection = dataSource.getConnection()) {

            if (where.length() > 0) {
                where = new StringBuffer(_WHERE_).append(where);
            }
            logger.info("query: " + select + PostgresqlSqlUtils.escape(table) + where);
            try (PreparedStatement preparedStatement = connection.prepareStatement(select + PostgresqlSqlUtils.escape(table) + where)) {
                SqlHelper.fillStatement(preparedStatement, in);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        long count = resultSet.getLong(SIZE_OF);
                        logger.info("count:" + count);
                        return count;
                    }
                }
            }
        }
        return 0;
    }


    public long count(String select_query, Map<String, List<String>> httpParameters, List<Condition> conditions) throws Exception {
        return 0;
    }

    public long count(String select_query) throws Exception {
        return 0;
    }

    public boolean exist(String table, String table_key, Object uuid) throws Exception {
        String select = COUNT_QUERY + PostgresqlSqlUtils.escape(table) + _WHERE_ + PostgresqlSqlUtils.escape(table_key) + "= ?";
        List<Object> in = new LinkedList<>();
        in.add(uuid);
        try (Connection connection = dataSource.getConnection()) {
            logger.info("query: " + select);
            try (PreparedStatement preparedStatement = connection.prepareStatement(select)) {
                SqlHelper.fillStatement(preparedStatement, in);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        long count = resultSet.getLong(SIZE_OF);
                        logger.info("exist:" + (count > 0));
                        return count > 0;
                    }
                }
            }
        }
        return false;
    }


    public List<Map<String, Object>> list(String table, String sort) throws Exception {
        return list(table, null, null, null, null, sort, 0, 0);
    }


    public List<Map<String, Object>> list(String table, String select_fields, String alias_condition, Map<String, List<String>> httpParameters, List<Condition> conditions, String sort, int limit, int start) throws Exception {
        StringBuffer where = new StringBuffer();
        StringBuffer order_limit = new StringBuffer();
        StringBuffer select = new StringBuffer();
        List<Object> in = new LinkedList<>();
        select.append(_SELECT_);
        if (select_fields != null) {
            //"_SELECT_ * _FROM_ "
            select.append(select_fields);
        } else {
            select.append(_ALL_);
        }
        select.append(_FROM_);
        if (alias_condition != null && !alias_condition.trim().isEmpty()) {
            where.append(PostgresqlSqlUtils.escape(alias_condition));
        }

        if (sort != null) {
            if (sort.contains(":")) {
                String[] sort_ = sort.split(":");
                order_limit.append(_ORDER_BY_).append(sort_[0]).append(" ").append(sort_[1]);
            } else {
                order_limit.append(_ORDER_BY_).append(sort);
            }
        }

        boolean withCondition = false;
        try {
            withCondition = ConditionUtils.where(httpParameters, conditions, where, in);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        if (!withCondition) {
            ParamUtils.where(httpParameters, where, in);
        }
        if (start == 0 && limit == 0) {
            logger.info("no limits");
        } else {
            if (start > 0) {
                order_limit.append(_OFFSET_).append(_COND_);
                in.add(start);
            } else {
                order_limit.append(_OFFSET_).append(_COND_);
                in.add(0);
            }
            if (limit > 0) {
                order_limit.append(_LIMIT_).append(_COND_);
                in.add(limit);
            } else {
                order_limit.append(_LIMIT_).append(_COND_);
                in.add(10);
            }
        }
        try (Connection connection = dataSource.getConnection()) {

            if (where.length() > 0) {
                where = new StringBuffer(_WHERE_).append(where);
            }
            logger.info("LIST query: " + select.toString() + PostgresqlSqlUtils.escape(table) + where + order_limit);
            return PostgresqlSqlUtils.executeQueryList(connection, select.toString() + PostgresqlSqlUtils.escape(table) + where.toString() + order_limit.toString(), in);
        }

    }

    public List<Map<String, Object>> list(String query, Map<String, List<String>> httpParameters, List<Condition> conditions, String sort, int limit, int start) throws Exception {
        StringBuffer where = new StringBuffer();
        StringBuffer order_limit = new StringBuffer();
        StringBuffer select = new StringBuffer(query);
        List<Object> in = new LinkedList<>();

        if (sort != null) {
            if (sort.contains(":")) {
                String[] sort_ = sort.split(":");
                order_limit.append(_ORDER_BY_).append(sort_[0]).append(" ").append(sort_[1]);
            } else {
                order_limit.append(_ORDER_BY_).append(sort);
            }
        }

        boolean withCondition = false;
        try {
            withCondition = ConditionUtils.where(httpParameters, conditions, where, in);
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        if (!withCondition) {
            ParamUtils.where(httpParameters, where, in);
        }
        if (start == 0 && limit == 0) {
            logger.info("no limits");
        } else {
            if (start > 0) {
                order_limit.append(_OFFSET_).append(_COND_);
                in.add(start);
            } else {
                order_limit.append(_OFFSET_).append(_COND_);
                in.add(0);
            }
            if (limit > 0) {
                order_limit.append(_LIMIT_).append(_COND_);
                in.add(limit);
            } else {
                order_limit.append(_LIMIT_).append(_COND_);
                in.add(10);
            }
        }
        try (Connection connection = dataSource.getConnection()) {
            if (where.length() > 0 && !select.toString().contains(_WHERE_)) {
                where = new StringBuffer(_WHERE_).append(where);
            } else {
                where = new StringBuffer(where);
            }
            logger.info("LIST query: " + select.toString() + where + order_limit);
            return PostgresqlSqlUtils.executeQueryList(connection, select.toString() + where.toString() + order_limit.toString(), in);
        }

    }

    public List<Map<String, Object>> list(String query) throws Exception {
        List<Object> in = new LinkedList<>();
        try (Connection connection = dataSource.getConnection()) {
            logger.info("LIST query: " + query);
            return PostgresqlSqlUtils.executeQueryList(connection, query, in);
        }

    }

    public Map<String, Object> create(String table, String table_key, Map<String, Object> map) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String query = PostgresqlSqlUtils.create(table, map);
            logger.info("CREATE QUERY: " + query);
            PostgresqlSqlUtils.executeQueryCreate(connection, query, map, table_key);
        }
        return map;
    }

    public boolean query(String query, List<Object> values) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            logger.info("EXECUTE QUERY: " + query);
            return PostgresqlSqlUtils.executeQuery(connection, query, values);
        } catch (Exception e) {
            logger.error("error: ", e);
            return false;
        }

    }


    public Map<String, Object> update(String table, String table_key, Map<String, Object> map, String key) throws
            Exception {
        Map<String, Object> keys = new HashMap<>();
        List<Object> in = new LinkedList<>();
        keys.put(table_key, key);
        String query = PostgresqlSqlUtils.update(table, map, keys, in);
        try (Connection connection = dataSource.getConnection()) {
            logger.info("UPDATE QUERY: " + query);
            PostgresqlSqlUtils.executeQueryUpdate(connection, query, in);
        }
        return map;
    }

    public Map<String, Object> fetch(String select_fields, String table, String table_key, String uuid) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (select_fields == null) {
                select_fields = " * ";
            }
            logger.info("FETCH QUERY: " + "_SELECT_ * _FROM_ " + PostgresqlSqlUtils.escape(table) + " _WHERE_ " + table_key + " = ?");
            PreparedStatement preparedStatement = connection.prepareStatement(_SELECT_ + select_fields + _FROM_ + PostgresqlSqlUtils.escape(table)
                    + _WHERE_ + PostgresqlSqlUtils.escape(table_key) + " = ?");
            preparedStatement.setObject(1, uuid);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return PostgresqlSqlUtils.single(resultSet);
            }
        }
    }

    public boolean delete(String table, String table_key, String uuid) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            logger.info("DELETE QUERY: " + DELETE_FROM + table + _WHERE_ + table_key + " = ? ");
            PreparedStatement preparedStatement = connection.prepareStatement(DELETE_FROM + PostgresqlSqlUtils.escape(table) + _WHERE_
                    + PostgresqlSqlUtils.escape(table_key) + " = ?");
            preparedStatement.setObject(1, uuid);
            int result = preparedStatement.executeUpdate();
            return result > 0;
        }
    }

    public void batch(String[] queries) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Statement statement = connection.createStatement();
            for (String query : queries) {
                logger.info("BATCH QUERY: " + query);
                statement.addBatch(query);
            }
            statement.executeBatch();
            statement.close();
        }
    }

    public boolean executeQuery(String sql) throws Exception {
        Statement statement = null;
        try (Connection connection = dataSource.getConnection()) {
            statement = connection.createStatement();
            int result = statement.executeUpdate(sql);
            if (result > 0) {
                return true;
            }
        } finally {
            if (statement != null)
                statement.close();
        }
        return false;
    }

    public boolean verifyTable(String tableName) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(SHOW_TABLES + "('" + tableName + "')");
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    Object resul = resultSet.getObject(1);
                    if (resul != null)
                        return true;
                }
            }
        }
        return false;
    }

//    @Override
//    public UserDetails login(String username, String password) throws Exception {
//        if (username == null) {
//            throw new Exception("login must contain username in 'username' field");
//        }
//        if (password == null) {
//            throw new Exception("login must contain password in 'password' field");
//        }
//        Map<String, Object> map = null;
//        try (Connection connection = dataSource.getConnection()) {
//            logger.info("login QUERY: " + LOGIN_QUERY);
//            PreparedStatement preparedStatement = connection.prepareStatement(LOGIN_QUERY);
//            preparedStatement.setObject(1, username);
//            try (ResultSet resultSet = preparedStatement.executeQuery()) {
//                map = PostgresqlSqlUtils.single(resultSet);
//            }
//        }
//        if (map == null) {
//            logger.info("password not found for username: " + username);
//            throw new Exception("invalid username/password");
//        }
//        String passwordOnDb = (String) map.get("password");
//        String encrPassword = PasswordUtils.createPassword(password);
//        if (encrPassword.equals(passwordOnDb)) {
//            return new UserDetails(username, getRoles(username));
//        }
//        throw new Exception("Failure in authentication");
//    }


    private List<String> getRoles(String username) throws Exception {
        List<String> roles = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            logger.info("roles QUERY: " + ROLES_QUERY);
            PreparedStatement preparedStatement = connection.prepareStatement(ROLES_QUERY);
            preparedStatement.setObject(1, username);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    roles.add(resultSet.getString(1));
                }
            }
        }
        return roles;
    }

    @Override
    public List<String> roles(String username) throws Exception {
        if (username == null) {
            throw new Exception("username is null!");
        }
        return getRoles(username);
    }

    public String getUserRoleQuery() {
        return INSERT_ROLE_QUERY;
    }

    @Override
    public String getJoinTableQuery() {
        return joinTableQuery;
    }

    @Override
    public String escape(String name) {
        return PostgresqlSqlUtils.escape(name);
    }

    @Override
    public String fieldDefinition2Sql(FieldDefinition fieldDefinition) {
        return PostgresqlFieldDefinitionUtils.sql(fieldDefinition);
    }

    @Override
    public String createTableSql(Metadata metadata, List<FieldDefinition> fields, List<String> joiQueries, List<Condition> conditions) {
        StringBuffer sb = new StringBuffer(" CREATE TABLE " + escape(metadata.table_name) + " (");
        if (metadata.table_key_type.equals("autoincrement")) {
            sb.append(escape(metadata.table_key) + " SERIAL ");
        } else {
            sb.append(escape(metadata.table_key) + " VARCHAR(50) NOT NULL ");
        }

        for (FieldDefinition fieldDefinition : fields) {
            if (fieldDefinition.sql_definition != null && !fieldDefinition.sql_definition.trim().isEmpty()) {
                sb.append(",").append(fieldDefinition.sql_definition);
            } else {
                sb.append(",").append(fieldDefinition2Sql(fieldDefinition));
            }
            if ("multijoin".equals(fieldDefinition.type)) {
                String join_table_name = metadata.table_name + "_" + fieldDefinition.join_table_name;
                String table_id = metadata.table_name + "_id";
                String join_table_id = fieldDefinition.join_table_name + "_id";
                joiQueries.add(String.format(getJoinTableQuery(), join_table_name, table_id, join_table_id));
                Condition condition = new Condition();
                condition.metadata_multijoin_uuid = metadata.uuid;
                condition.uuid = UUID.randomUUID().toString();
                condition.metadata_name = fieldDefinition.join_table_name;
                condition.metadata_uuid = ""; //?? dove lo dovrei prendere??
                condition.condition = metadata.table_name + "_id_nn && join_table_nn";
                condition.query_params = metadata.table_name + "_id";
                condition.sub_query = fieldDefinition.join_table_key + " in (select " + join_table_id + " from " + join_table_name + " where " + table_id + " = ?)";
                conditions.add(condition);
            }
        }
        sb.append(", PRIMARY KEY (" + escape(metadata.table_key) + ")").append(") ;");
        logger.info("QUERY CREATION TABLE: " + sb.toString());
        return sb.toString();
    }

}

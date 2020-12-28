package org.acme;

import io.quarkus.reactive.datasource.ReactiveDataSource;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Pool;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

@ApplicationScoped
public class DbProducer {

    @ConfigProperty(name = "snello.dbtype")
    String dbtype;

    @Inject
    @ReactiveDataSource("postgresql")
    PgPool pgPool;

    @Inject
    @ReactiveDataSource("mysql")
    MySQLPool mysqlPool;

    public DbProducer() {
        System.out.println("DbProducer");
    }

    @Produces
    @DbType
    public Pool db() throws Exception {
        System.out.println("dbtype: " + dbtype);
        switch (dbtype) {
            case "mysql":
                return mysqlPool;
            case "postgresql":
                return pgPool;
            default:
                throw new Exception("no dbtype");
        }
    }
}

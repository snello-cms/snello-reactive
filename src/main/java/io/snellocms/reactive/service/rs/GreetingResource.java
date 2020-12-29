package io.snellocms.reactive.service.rs;

import io.snellocms.reactive.service.producer.DbType;
import io.vertx.mutiny.sqlclient.Pool;
import org.jboss.resteasy.reactive.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriInfo;

@Path("/hello-resteasy")
@ApplicationScoped
public class GreetingResource {

    @Inject
    @DbType
    Pool client;

    @GET
    @Path("/{id: .*/*}")
    public String params(@RestPath String id,
                         @RestQuery String q,
                         @RestHeader int h,
                         @RestForm String f,
                         @RestMatrix String m,
                         @RestCookie String c,
                         UriInfo uriInfo) {
        System.out.println(client.getConnection());
        return "params: id: " + id + ", q: " + q + ", h: " + h + ", f: " + f + ", m: " + m + ", c: " + c;
    }
}

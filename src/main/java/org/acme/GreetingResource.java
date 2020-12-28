package org.acme;

import io.vertx.mutiny.sqlclient.Pool;
import org.jboss.resteasy.reactive.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
                         @RestCookie String c) {
        System.out.println(client.getConnection());
        return "params: id: " + id + ", q: " + q + ", h: " + h + ", f: " + f + ", m: " + m + ", c: " + c;
    }
}

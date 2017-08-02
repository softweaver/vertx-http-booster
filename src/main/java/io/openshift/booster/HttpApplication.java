package io.openshift.booster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public class HttpApplication extends AbstractVerticle {

  protected static final String template = "Hello, %s!";

  // User log
  private static final Logger LOGGER = LogManager.getLogger(HttpApplication.class);

  @Override
  public void start(Future<Void> future) {
    // Create a router object.
    Router router = Router.router(vertx);

    router.get("/api/greeting").handler(this::greeting);
    router.get("/*").handler(StaticHandler.create());

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration, default to 8080.
            config().getInteger("http.port", 8080), ar -> {
              if (ar.succeeded()) {
                LOGGER.info("Server started on port {}", ar.result().actualPort());
                future.complete();
              } else {
                LOGGER.error("Server unable to start", ar.cause());
                future.fail(ar.cause());
              }
            });

  }

  private void greeting(RoutingContext rc) {
    String name = rc.request().getParam("name");
    if (name == null) {
      name = "World";
    }
    LOGGER.info("Handling greeting request with name `{}`", name);

    JsonObject response = new JsonObject()
        .put("content", String.format(template, name));

    rc.response()
        .putHeader(CONTENT_TYPE, "application/json; charset=utf-8")
        .end(response.encodePrettily());
  }
}

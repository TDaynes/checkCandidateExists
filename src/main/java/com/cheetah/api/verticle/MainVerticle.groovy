package com.cheetah.api.verticle

import io.vertx.core.AbstractVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router


class MainVerticle extends AbstractVerticle {
  @Override
  void start() {
    println("MainVerticle - Deployed")

    def options = new DeploymentOptions().setInstances(20)
    vertx.deployVerticle("com.cheetah.api.verticle.CheckCandidateExistsVerticle", options)

    // TODO: Initialise 3 bloom filters as global variables. This way they don't need
    //        generating during each candidate check

    // Router to handle candidate exists posts
    def router = Router.router(vertx)
    router.route().handler { context ->
      def queryParams = context.queryParams()
      String candidateId = queryParams.get("id") ?: ""
      String firstName = queryParams.get("firstName") ?: ""
      String lastName = queryParams.get("lastName") ?: ""

      // If no values past, then return error
      if(candidateId=="" && firstName=="" && lastName==""){
        context.json(
          new JsonObject()
            .put("code", 400)
            .put("description", "Request missing fields. Example of a valid request: http://localhost:8888/?id=1&firstName=John&lastName=Smith")
        )
        return
      }
      // If no id past, then user must enter both first and last names, else return error
      else if(candidateId=="" && (firstName=="" || lastName=="") ){
        context.json(
          new JsonObject()
            .put("code", 400)
            .put("description", "Request must contain both first and last name. Example of a valid request: http://localhost:8888/?firstName=John&lastName=Smith")
        )
        return
      }

      def message = new JsonObject().put("id", candidateId).put("firstName", firstName).put("lastName", lastName)
      vertx.eventBus().request("com.cheetah.api.verticle.CheckCandidateExists", message, response -> {
        if(response.succeeded()){
          def candidateExistsResponse = response.result().body()
          context.json(
            new JsonObject()
              .put("code", candidateExistsResponse["code"])
              .put("candidateExists", candidateExistsResponse["candidateExists"]?:"false")
              .put("description", candidateExistsResponse["description"]?:"")
          )
        }
        // TODO: Handle failed responses.
      })

    }

    // Create the HTTP server
    vertx.createHttpServer()
    // Handle every request using the router
      .requestHandler(router)
    // Start listening
      .listen(8888)
    // Print the port
      .onSuccess { server ->
        println("HTTP server started on port " + server.actualPort())
      }
  }
}

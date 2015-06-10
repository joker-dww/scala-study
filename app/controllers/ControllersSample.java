package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ium.concerto.manager.RecommenderManager;
import org.codehaus.jackson.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.libs.Json;
import play.libs.Json.*;
import play.mvc.Controller;
import play.mvc.Result;

@Component
public class ControllersSample extends Controller {

    @Autowired
    private RecommenderManager recommenderManager;

    public Result home(){
        return ok("Hi!");
    }

    public Result getStatus() {
        ObjectNode result = Json.newObject();
        result.put("exampleField1", "foobar");
        result.put("exampleField2", "Hello world!"+recommenderManager.getRecommenderStatus());
        return ok(result);
    }

}

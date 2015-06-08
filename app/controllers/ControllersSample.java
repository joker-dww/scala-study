package controllers;

import org.springframework.stereotype.Component;
import play.mvc.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import play.mvc.Result;

@Component
public class ControllersSample extends Controller {

    @Autowired
    //private String appDAO;

    public Result home(){
        return ok("Hi!");
    }

}

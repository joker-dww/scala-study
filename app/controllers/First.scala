package controllers

import com.ium.concerto.manager.RecommenderManager
import controllers.Application._
import manager.RecommenderManager2
import play.api.libs.json.Json._
import play.api.mvc._
import play.libs.Json
import play.mvc.Result
import org.springframework.beans.factory.annotation.Autowired

/**
 * Created by joker on 2015. 6. 11..
 */
object First extends Controller{
  //@Autowired
  private var recommenderManager: RecommenderManager = new RecommenderManager

  def view = Action {

    val recommenderManager = new RecommenderManager2
    val msg = recommenderManager.getData

    Ok(views.html.cat(msg))
  }

  def getStatus = Action {
    val result = Map("exampleField2" -> ("Hello world!" + recommenderManager.getRecommenderStatus) )
    Ok(toJson(result))
  }

}

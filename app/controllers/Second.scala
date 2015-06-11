package controllers

import com.ium.concerto.manager.RecommenderManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.api.libs.json.Json._
import play.api.mvc.{Action, Controller}

@Component
class Second extends Controller{
  @Autowired
  private var recommenderManager: RecommenderManager = null

  def getStatus = Action {

    val result = Map("exampleField2" -> ("Hello world!" + recommenderManager.getRecommenderStatus) )
    Ok(toJson(result))

  }

}

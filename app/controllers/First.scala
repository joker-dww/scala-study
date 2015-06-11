package controllers

import com.ium.concerto.manager.RecommenderManager
import com.ium.concerto.model.RecommenderStatus
import com.ium.concerto.service.recommender.RecommenderStatusService
import manager.RecommenderManager2
import org.springframework.context.annotation.{Bean, ComponentScan}
import play.api.libs.json.Json._
import play.api.mvc._
import org.springframework.beans.factory.annotation.Autowired

object First extends Controller{
  //@Autowired
  //private var recommenderManager: RecommenderManager = new RecommenderManager
  @Autowired
  private var recommenderStatusService: RecommenderStatusService = null

  def instance() = this

  def view = Action {

    val recommenderManager = new RecommenderManager2
    val msg = recommenderManager.getData

    Ok(views.html.cat(msg))
  }

  def getStatus = Action {
    //var recommenderManager = new RecommenderManager

    //var recommenderStatusService = new RecommenderStatusService
    val result = Map("exampleField2" -> ("Hello world!" + recommenderStatusService.getRecommenderStatus()) )
    //val result = Map("exampleField2" -> ("Hello world!" + recommenderManager.getRecommenderStatus) )
    Ok(toJson(result))
  }

}

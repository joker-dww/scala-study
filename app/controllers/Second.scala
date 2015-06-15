package controllers

import java.util

import com.ium.concerto.manager.{PeopleGroupAssignManager, RecommenderManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.api.libs.json.Json._
import play.api.mvc.{Action, Controller}

import java.text.SimpleDateFormat
import java.util.Date

import scala.collection.JavaConverters._

@Component
class Second extends Controller{

  @Autowired
  private var recommenderManager: RecommenderManager = null

  @Autowired
  private var statAssignedGroupList: PeopleGroupAssignManager = null

  def getStatus = Action {

    val result = Map("exampleField2" -> ("Hello world!" + recommenderManager.getRecommenderStatus) )
    Ok(toJson(result))

  }

  def getStatAssignedGroup(dateString: String) = Action {

    val targetDate = dateString match {
        case "" => new SimpleDateFormat("yyyyMMdd").format(new Date())
        case _ => dateString
    }

    var ls = statAssignedGroupList.getStatAssignedGroup(targetDate)

    Ok(views.html.stat.getStatAssignedGroup(
      dateString,
      ls.asInstanceOf[java.util.List[Array[java.lang.Object]]]
      )
    )
  }

}

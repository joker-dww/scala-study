package controllers

import com.ium.concerto.manager.{PeopleGroupAssignManager, RecommenderManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import play.api.libs.json.Json._
import play.api.mvc.{Action, Controller}

import java.text.SimpleDateFormat
import java.util.Date

import views.html.helper.form

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

  def getStatAssignedGroup(dateString: Option[String]) = Action {
    val targetDate = dateString match {
        case None => new SimpleDateFormat("yyyyMMdd").format(new Date())
        case _ => dateString.toString
    }

    var ls = statAssignedGroupList.getStatAssignedGroup(targetDate)

    Ok(views.html.stat.getStatAssignedGroup(
      targetDate,
      ls.asInstanceOf[java.util.List[Array[java.lang.Object]]]
      )
    )
  }

}

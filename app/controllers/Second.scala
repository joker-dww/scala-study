package controllers

import com.ium.concerto.manager.{PeopleGroupAssignManager, RecommenderManager}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.tuple
import play.api.data.Forms.text
import play.api.libs.json.Json._
import play.api.mvc.{Action, Controller}

import java.text.SimpleDateFormat
import java.util.Date

@Component
class Second extends Controller{

  @Autowired
  private var recommenderManager: RecommenderManager = null

  @Autowired
  private var statAssignedGroupList: PeopleGroupAssignManager = null


  val baseForm = Form(
    tuple(
      "dateString" -> text,
      "" -> text
    )
  )

  def getStatus = Action {

    val result = Map("exampleField2" -> ("Hello world!" + recommenderManager.getRecommenderStatus) )
    Ok(toJson(result))

  }

  def getStatAssignedGroup = Action { implicit request =>

    var targetDate = new SimpleDateFormat("yyyyMMdd").format(new Date())
    //Logger.debug(s"Result=${request.method}")
    if (request.method == "POST") {

      val values = baseForm.bindFromRequest.data
      targetDate = values("dateString") match {
        case "" => targetDate
        case _ => values("dateString")
      }

    }

    var ls = statAssignedGroupList.getStatAssignedGroup(targetDate)

    Ok(views.html.stat.getStatAssignedGroup(
      targetDate,
      ls.asInstanceOf[java.util.List[Array[java.lang.Object]]]
      )
    )
  }

}

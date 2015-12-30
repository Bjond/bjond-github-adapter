package controllers

import _root_.com.bjond.RegistrationService
import controllers.config.ServiceConfiguration
import play.api.mvc._
import org.coursera.autoschema.AutoSchema.createSchema
import org.log4s._
import play.api.data._
import play.api.data.Forms._
import javax.inject.Inject
import play.api.i18n._

case class ServerData(server: String)

class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  private[this] val logger = getLogger
  private[this] val configClass = new ServiceConfiguration()

  val serverForm = Form(
    mapping(
      "server" -> text
    )(ServerData.apply)(ServerData.unapply)
  )

  def registerService = Action { implicit request =>
    val serverData = serverForm.bindFromRequest.get
    val server = serverData.server
    val registrationService = new RegistrationService();
    registrationService.registerService(server);
    Ok(views.html.index("Service Successfully Registered!", serverForm.bindFromRequest))
  }

  def index = Action {
    Ok(views.html.index("Please enter the URL or IP Address of the server to which this server will be registered.", serverForm))
  }

  def schema = Action {
    val schema = createSchema[configClass.GroupConfiguration]
    Ok(schema)
  }

  def userSchema = Action {
    Ok("{result: 'ok'}")
  }

}

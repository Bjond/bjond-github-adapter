package controllers

import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Enumerator
import play.api.libs.json._
import play.api.mvc._
import org.coursera.autoschema.AutoSchema.createSchema
import play.api.data._
import play.api.data.Forms._
import javax.inject.Inject
import play.api.i18n._
import scala.concurrent.Future
import play.api.libs.ws._
import play.api.Play.current
import play.Logger

case class ServerData(server: String)

class Application @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  val eventService = new EventService()

  implicit val groupConfigurationWrites: Writes[GroupConfiguration] = (
        (JsPath \ "groupid").write[String] and
        (JsPath \ "gitHubAPIKey").write[String] and
        (JsPath \ "username").write[String] and
        (JsPath \ "password").write[String]
    )(unlift(GroupConfiguration.unapply))

  implicit val groupConfigurationReads: Reads[GroupConfiguration] = (
        (JsPath \ "groupid").read[String] and
        (JsPath \ "gitHubAPIKey").read[String] and
        (JsPath \ "username").read[String] and
        (JsPath \ "password").read[String]
    )(GroupConfiguration.apply _)

  implicit val userConfigurationWrites: Writes[UserConfiguration] = (
        (JsPath \ "groupid").write[String] and
        (JsPath \ "userid").write[String] and
        (JsPath \ "gitHubUsername").write[String]
    )(unlift(UserConfiguration.unapply))

  implicit val userConfigurationReads: Reads[UserConfiguration] = (
        (JsPath \ "groupid").read[String] and
        (JsPath \ "userid").read[String] and
        (JsPath \ "gitHubUsername").read[String]
    )(UserConfiguration.apply _)

  val serverForm = Form(
    mapping(
      "server" -> text
    )(ServerData.apply)(ServerData.unapply)
  )

  def registerService = Action { implicit request =>
    val serverData = serverForm.bindFromRequest.get
    val server = serverData.server
    val registrationService = new RegistrationService()
    registrationService.registerService(server)
    Ok(views.html.index("Service Successfully Registered!", serverForm.bindFromRequest))
  }

  def index = Action {
    Ok(views.html.index("Please enter the URL or IP Address of the server to which this server will be registered.", serverForm))
  }

  def schema = Action {
    val schema = createSchema[GroupConfiguration]
    Ok(eventService.getJWTPayload(schema).getCompactSerialization)
  }

  def userSchema = Action {
    val schema = createSchema[UserConfiguration]
    Ok(eventService.getJWTPayload(schema).getCompactSerialization)
  }

  def configureGroup(groupid: String, environment: String) = Action.async { implicit request =>
    val body = request.body
    val mongoService = new MongoService()
    val extracted = eventService.unpackJWTPayload(body.asText.get)
    val config = Json.fromJson[GroupConfiguration](extracted)
    val future = mongoService.insertGroupConfig(groupid, config.get)
    future.map {
      response => Result(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")),
        body = Enumerator(response.message.getBytes())
      )
    }
  }

  def configureUser(groupid: String, userid: String, environment: String) = Action.async { implicit request =>
    val body = request.body
    val mongoService = new MongoService()
    val extracted = eventService.unpackJWTPayload(body.asText.get)
    val config = Json.fromJson[UserConfiguration](extracted)
    val future = mongoService.insertUserConfig(groupid, userid, config.get)
    future.map {
      response => Result(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")),
        body = Enumerator(response.message.getBytes())
      )
    }
  }

  def getGroupConfiguration(groupid: String) = Action.async {
    val mongoService = new MongoService()
    val future = mongoService.getGroupConfiguration(groupid)
    future.map {
      response => if(response.isDefined) Ok(eventService.getJWTPayload(Json.toJson(response.get)).getCompactSerialization) else Ok(eventService.getJWTPayload(Json.toJson(new GroupConfiguration("", "", "", ""))).getCompactSerialization)
    }
  }

  def getUserConfiguration(groupid: String, userid: String) = Action.async {
    val mongoService = new MongoService()
    val future = mongoService.getUserConfiguration(groupid, userid)
    future.map {
      response => if(response.isDefined) Ok(eventService.getJWTPayload(Json.toJson(response.get)).getCompactSerialization) else Ok(eventService.getJWTPayload(Json.toJson(new UserConfiguration("", "", ""))).getCompactSerialization)
    }
  }

  def registerBjondEndpoint(groupid: String, environment: String) = Action.async { implicit request =>
    val mongoService = new MongoService()
    val endpoint = request.getQueryString("endpoint")
    val future = mongoService.insertGroupEndpoint(groupid, endpoint.get)
    future.map {
      response => Result(
        header = ResponseHeader(200, Map(CONTENT_TYPE -> "text/plain")),
        body = Enumerator(response.message.getBytes())
      )
    }
  }

  def handleGitHubEvent(groupid: String) = Action.async { implicit request =>
    val body = request.body.asJson
    Logger.info(body.get.toString())
    val mongoService = new MongoService()
    val future = mongoService.getGroupEndpoint(groupid)
    val eventType = request.headers.get("X-GitHub-Event")
    val eventService = new EventService()
    future.map {
      response => // TODO: The URL is hardcoded. Might use Postgresql; mongo is a PITA.
        val futureResponse: Future[String] = eventService.fireEvent("http://localhost:8080/server-core/services/integrationmanager/event/2751b72d-9e33-43c3-aa38-a584006e67bc/fae29c14-a2bc-11f5-9121-1a5c11784914", body, eventType.get, groupid).map {
    			response => (response.json \ "status").as[String]}
        futureResponse.recover {
          case e: Exception =>
            Logger.error(e.getMessage);
            Status(500)
        }
        Ok("{result: 'ok'}")
    }
  }

}
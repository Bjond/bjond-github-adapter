package com.bjond

import play.api._
import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import play.mvc.Http.Response
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.functional.syntax._

class RegistrationService extends Controller {

	implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
	case class ServiceDefinition(id: String, name: String, author: String, description: String, iconURL: String, configURL: String, availableFieldsURL: String)

	implicit val serviceDefinitionWrites: Writes[ServiceDefinition] = (
	  (JsPath \ "id").write[String] and
	  (JsPath \ "name").write[String] and
	  (JsPath \ "author").write[String] and
	  (JsPath \ "description").write[String] and
	  (JsPath \ "iconURL").write[String] and
	  (JsPath \ "configURL").write[String] and
	  (JsPath \ "availableFieldsURL").write[String]
	)(unlift(ServiceDefinition.unapply))

	def registerService(url: String) {
		val fullURL = url + "/server-core/services/integrationmanager/register"
		val postData = ServiceDefinition("fae29c14-a2bc-11f5-9121-1a5c11784914", "GitHub Integration", "Bjönd, Inc", 
			"With this service you can react to events in yoru GitHub oganization or repository. For instance, you can send someone a task when an issue is assigned, or notify a manager when that task expires.",
			"http://localhost:9000/assets/images/github-logo.png", "http://localhost:9000/config", "http://localhost:9000/fields")

		val futureResponse: Future[String] = WS.url(fullURL).post(Json.toJson(postData)).map {
			response => (response.json \ "status").as[String]
		}
	}

}
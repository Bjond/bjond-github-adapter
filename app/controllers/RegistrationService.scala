package controllers

import play.api.mvc._
import play.api.libs.ws._
import play.api.Play.current
import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.JsValue.jsValueToJsLookup

class RegistrationService extends Controller {

	implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
	case class EventField(id: String, jsonKey: String, name: String, description: String, fieldType: String, event: String)
	case class EventDefinition(id: String, jsonKey: String, name: String, description: String, fields: Set[EventField])
	case class ServiceDefinition(id: String, name: String, author: String, description: String, iconURL: String, rootEndpoint: String, integrationEvent: Set[EventDefinition])

	implicit val eventFieldWrites: Writes[EventField] = (
	  (JsPath \ "id").write[String] and
	  (JsPath \ "jsonKey").write[String] and
	  (JsPath \ "name").write[String] and
	  (JsPath \ "description").write[String] and
	  (JsPath \ "fieldType").write[String] and
	  (JsPath \ "event").write[String]
	)(unlift(EventField.unapply))

	implicit val eventDefinitionWrites: Writes[EventDefinition] = (
	  (JsPath \ "id").write[String] and
	  (JsPath \ "jsonKey").write[String] and
	  (JsPath \ "name").write[String] and
	  (JsPath \ "description").write[String] and
	  (JsPath \ "fields").write[Set[EventField]]
	)(unlift(EventDefinition.unapply))

	implicit val serviceDefinitionWrites: Writes[ServiceDefinition] = (
	  (JsPath \ "id").write[String] and
	  (JsPath \ "name").write[String] and
	  (JsPath \ "author").write[String] and
	  (JsPath \ "description").write[String] and
	  (JsPath \ "iconURL").write[String] and
	  (JsPath \ "rootEndpoint").write[String] and
	  (JsPath \ "integrationEvent").write[Set[EventDefinition]]
	)(unlift(ServiceDefinition.unapply))

	def registerService(url: String) {
		val fullURL = url + "/server-core/services/integrationmanager/register"

		val pullRequestEvent = EventDefinition("cb29a497-a4c3-4672-bfa7-9b184d9b76d2", "pull_request", "GitHub Pull Request", "This will fire when an action is taken on a pull request.", Set(
		    EventField("30bacb69-2720-4bb1-8595-cd4d72ec6025", "action", "Action", "The is the action performed on the pull request.", "String", "cb29a497-a4c3-4672-bfa7-9b184d9b76d2"),
		    EventField("529389db-e116-4c30-b105-b594694b2322", "assignee", "Assignee", "The user to whom the Pull Request is assigned.", "Person", "cb29a497-a4c3-4672-bfa7-9b184d9b76d2"),
		    EventField("e5bc46b5-dcd5-4588-a05e-6b409992f7bd", "repo", "Repository Name", "The name of the repository", "String", "cb29a497-a4c3-4672-bfa7-9b184d9b76d2")))

		val pullRequestCommentEvent = EventDefinition("7c96b035-9502-4535-be70-bb052dc5f05c", "pull_request_review_comment", "GitHub Pull Request Comment", "A comment was made on a Pull Request.", Set(
		    EventField("2d9f94ec-3b0e-4ef5-bf02-954c034c1c04", "pull_request", "Pull Request", "The Pull Request ID.", "String", "7c96b035-9502-4535-be70-bb052dc5f05c"),
		    EventField("41036e11-7e62-4337-8376-0801aad85aaf", "user", "User", "The creator of the Pull Request.", "Person", "7c96b035-9502-4535-be70-bb052dc5f05c"),
		    EventField("c49b8734-6371-4b72-bc9e-547093245469", "assignee", "Assignee", "The user to whom the Pull Request is assigned.", "Person", "7c96b035-9502-4535-be70-bb052dc5f05c"),
		    EventField("25f66f71-720a-45b3-9264-5a3f6b2ea82a", "repo", "Repository Name", "The name of the repository.", "String", "7c96b035-9502-4535-be70-bb052dc5f05c")))

		val issueCommentEvent = EventDefinition("2c3ffb6f-ddda-4f21-93f9-257d46271010", "issue_comment", "GitHub Issue Comment", "A comment was made on an issue.", Set(
		    EventField("b262dd2d-fe97-41d1-9443-faff95234512", "pull_request", "Pull Request", "The Pull Request ID.", "String", "2c3ffb6f-ddda-4f21-93f9-257d46271010"),
		    EventField("e0f996be-15ea-485a-9199-0c18e1e1a7c1", "user", "User", "The creator of the Pull Request.", "Person", "2c3ffb6f-ddda-4f21-93f9-257d46271010"),
		    EventField("e2667bca-5088-44b0-a940-5fb51a169fd3", "assignee", "Assignee", "The user to whom the Pull Request is assigned.", "Person", "2c3ffb6f-ddda-4f21-93f9-257d46271010"),
		    EventField("2784d9c4-d231-4dd3-94f7-fe9a02c29dc8", "repo", "Repository Name", "The name of the repository.", "String", "2c3ffb6f-ddda-4f21-93f9-257d46271010")))

		val pushEvent = EventDefinition("d6ac9d97-93fb-4b15-900f-f627223d5193", "push", "GitHub Push", "Triggers when someone pushes a change to a branch.", Set(
		    EventField("a24033cc-21b3-4a83-969c-d244042a6338", "pusher", "Pusher", "The person who performed the push.", "Person", "d6ac9d97-93fb-4b15-900f-f627223d5193"),
		    EventField("1939ac70-60af-41d9-b36c-fce2fcf625d5", "ref", "Reference", "The full ref of the push. Ths includes the repository and branch.", "String", "d6ac9d97-93fb-4b15-900f-f627223d5193"),
		    EventField("adfb3566-523c-40fd-a7fc-ad2dc66747a1", "repo", "Repository Name", "The name of the repository.", "String", "d6ac9d97-93fb-4b15-900f-f627223d5193")))

		val prPushEvent = EventDefinition("62e75091-173c-4230-87b6-421b5f3d1769", "push", "GitHub Pull Request Push", "Triggers when someone pushes a change to a pull request.", Set(
		    EventField("023a858f-3cc1-4979-85c7-bd7b60010a21", "pusher", "Pusher", "The person who performed the push.", "Person", "62e75091-173c-4230-87b6-421b5f3d1769"),
		    EventField("f4f12e92-3ebf-49ec-9c3d-1003c64b971b", "ref", "Reference", "The full ref of the push. Ths includes the repository and branch.", "String", "62e75091-173c-4230-87b6-421b5f3d1769"),
		    EventField("df890b3f-0b7d-4531-ac6a-686d3e607ea6", "assignee", "Assignee", "The user to whom the Pull Request is assigned.", "Person", "62e75091-173c-4230-87b6-421b5f3d1769"),
		    EventField("410fea37-aea6-43ae-ac4f-620af781bf67", "repo", "Repository Name", "The name of the repository.", "String", "62e75091-173c-4230-87b6-421b5f3d1769")))

		val availableEvents = Set(pullRequestEvent, pullRequestCommentEvent, pushEvent, issueCommentEvent, prPushEvent)

		val postData = ServiceDefinition("fae29c14-a2bc-11f5-9121-1a5c11784914", "GitHub Integration", "Bjönd, Inc",
			"With this service you can react to events in your GitHub oganization or repository. For instance, you can send someone a task when an issue is assigned, or notify a manager when that task expires.",
			"http://localhost:9000/assets/images/github-logo.png", "http://localhost:9000/config", availableEvents)

		val futureResponse: Future[String] = WS.url(fullURL).post(Json.toJson(postData)).map {
			response => (response.json \ "status").as[String]
		}
	}

}

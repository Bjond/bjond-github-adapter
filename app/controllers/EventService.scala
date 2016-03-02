package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

case class IssueComment(assignee: String, repo: String, pull_request: String, user: String)

class EventService extends Controller {
  
  implicit val groupConfigurationWrites: Writes[IssueComment] = (
        (JsPath \ "assignee").write[String] and
        (JsPath \ "repo").write[String] and
        (JsPath \ "pull_request").write[String] and
        (JsPath \ "user").write[String]
    )(unlift(IssueComment.unapply))
  
  implicit val groupConfigurationReads: Reads[IssueComment] = (
        (JsPath \ "assignee").read[String] and
        (JsPath \ "repo").read[String] and
        (JsPath \ "pull_request").read[String] and
        (JsPath \ "user").read[String]
    )(IssueComment.apply _)
  
  val commentEventTransformer = (
      (JsPath \ "assignee").json.copyFrom((JsPath \ "issue" \ "assignee" \ "login").json.pick) and
      (JsPath \ "user").json.copyFrom((JsPath \ "issue" \ "user" \ "login").json.pick) and 
      (JsPath \ "repo").json.copyFrom((JsPath \ "repository" \ "name").json.pick) and
      (JsPath \ "pull_request").json.copyFrom((JsPath \ "issue" \ "number").json.pick.orElse((JsPath \ "action").json.pick))
    ) reduce
  
  def getBodyType(body: Option[JsValue], eventType: String): JsValue = {
    {
      val test = body.get.transform(commentEventTransformer)
      val json = body.get.transform(commentEventTransformer).get
      setEvent(json, eventType)
    }
  }
  
  def setEvent(json: JsValue, eventType: String): JsValue = {
    val event: String = {
      if (eventType.equals("issue_comment") && !(json \ "pull_request").equals(null)) {
        "pull_request_review_comment"
      }
      else {
        eventType
      }
    }
    json.as[JsObject] + ("event" -> Json.toJson(event))
  }
  
  def fireEvent(url: String, json: Option[JsValue], event: String): Future[WSResponse] = {
    val bjondJson = getBodyType(json, event)
    WS.url(url).post(bjondJson)
  }
  
}
package controllers

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.validation.ValidationError
import play.api.libs.json.Reads._
import play.api.libs.ws._
import scala.concurrent.Future
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global

case class IssueComment(assignee: String, repo: String, pull_request: Option[String], user: String)
case class CodePush(repo: String, ref: String, pusher: String)

class EventService extends Controller {
  
  implicit val issueCommentWrites: Writes[IssueComment] = (
        (JsPath \ "assignee").write[String] and
        (JsPath \ "repo").write[String] and
        (JsPath \ "pull_request").writeNullable[String] and
        (JsPath \ "user").write[String]
    )(unlift(IssueComment.unapply))
  
  implicit val issueCommentReads: Reads[IssueComment] = (
        (JsPath \ "assignee").read[String] and
        (JsPath \ "repo").read[String] and
        (JsPath \ "pull_request").readNullable[String] and
        (JsPath \ "user").read[String]
    )(IssueComment.apply _)
    
  implicit val codePushWrites: Writes[CodePush] = (
        (JsPath \ "repo").write[String] and
        (JsPath \ "ref").write[String] and
        (JsPath \ "pusher").write[String]
    )(unlift(CodePush.unapply))
  
  implicit val codePushReads: Reads[CodePush] = (
        (JsPath \ "repo").read[String] and
        (JsPath \ "ref").read[String] and
        (JsPath \ "pusher").read[String]
    )(CodePush.apply _)
      
  val prCommentEventTransformer = (
      (JsPath \ "assignee").json.copyFrom((JsPath \ "issue" \ "assignee" \ "login").json.pick) and
      (JsPath \ "user").json.copyFrom((JsPath \ "issue" \ "user" \ "login").json.pick) and 
      (JsPath \ "repo").json.copyFrom((JsPath \ "repository" \ "name").json.pick) and
      (JsPath \ "pull_request").json.copyFrom((JsPath \ "issue" \ "number").json.pick)
    ) reduce
    
  val commentEventTransformer = (
      (JsPath \ "assignee").json.copyFrom((JsPath \ "issue" \ "assignee" \ "login").json.pick) and
      (JsPath \ "user").json.copyFrom((JsPath \ "issue" \ "user" \ "login").json.pick) and 
      (JsPath \ "repo").json.copyFrom((JsPath \ "repository" \ "name").json.pick) 
    ) reduce
    
  val pushEventTransformer = (
      (JsPath \ "repo").json.copyFrom((JsPath \ "head" \ "repo" \ "name").json.pick) and 
      (JsPath \ "ref").json.copyFrom((JsPath \ "head" \ "ref").json.pick) and 
      (JsPath \ "pusher").json.copyFrom((JsPath \ "sender" \ "login").json.pick) 
    ) reduce
  
  def getBodyType(body: Option[JsValue], eventType: String): JsValue = {
    {
      val filteredEventType: String = getEvent(body.get, eventType)
      val json = filteredEventType match {
        case "pull_request_review_comment" => body.get.transform(commentEventTransformer).get
        case "issue_comment" => body.get.transform(prCommentEventTransformer).get
        case "push" => body.get.transform(pushEventTransformer).get
      }
      setEvent(json, filteredEventType)
    }
  }
  
  def setEvent(json: JsValue, eventType: String): JsValue = {
    val event: String = getEvent(json, eventType)
    json.as[JsObject] + ("event" -> Json.toJson(event))
  }
  
  def getEvent(json: JsValue, eventType: String): String = { 
    {
      if (eventType.equals("issue_comment") && !(json \ "issue" \ "pull_request").equals(null)) {
        "pull_request_review_comment"
      }
      else {
        eventType
      }
    }
  }
  
  def fireEvent(url: String, json: Option[JsValue], event: String): Future[WSResponse] = {
    val bjondJson = getBodyType(json, event)
    WS.url(url).post(bjondJson)
  }
  
}
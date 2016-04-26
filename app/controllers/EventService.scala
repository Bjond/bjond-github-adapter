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
import scala.concurrent._
import scala.concurrent.duration._

case class IssueComment(assignee: String, repo: String, pull_request: Option[String], user: String)
case class CodePush(repo: String, ref: String, pusher: String)
case class PRCodePush(repo: String, assignee: String, ref: String, pusher: String)

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
    
  implicit val prCodePushWrites: Writes[PRCodePush] = (
        (JsPath \ "repo").write[String] and
        (JsPath \ "assignee").write[String] and
        (JsPath \ "ref").write[String] and
        (JsPath \ "pusher").write[String]
    )(unlift(PRCodePush.unapply))
  
  implicit val prCodePushReads: Reads[PRCodePush] = (
        (JsPath \ "repo").read[String] and
        (JsPath \ "assignee").read[String] and
        (JsPath \ "ref").read[String] and
        (JsPath \ "pusher").read[String]
    )(PRCodePush.apply _)
      
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
    
  val prPushEventTransformer = (
      (JsPath \ "repo").json.copyFrom((JsPath \ "repository" \ "name").json.pick) and
      (JsPath \ "assignee").json.copyFrom((JsPath \ "pull_request" \ "assignee" \ "login").json.pick) and
      (JsPath \ "ref").json.copyFrom((JsPath \ "ref").json.pick) and 
      (JsPath \ "pusher").json.copyFrom((JsPath \ "pusher" \ "name").json.pick) 
    ) reduce
  
  def getBodyType(body: Option[JsValue], eventType: String, groupid: String): JsValue = {
    {
      val filteredEventType: String = getEvent(body.get, eventType)
      val json = filteredEventType match {
        case "pull_request" => body.get.transform(commentEventTransformer).get
        case "pull_request_review_comment" => body.get.transform(commentEventTransformer).get
        case "issue_comment" => body.get.transform(prCommentEventTransformer).get
        case "push" => body.get.transform(pushEventTransformer).get
        case "pull_request_push" => body.get.transform(prPushEventTransformer).get
      }
      val newJson = setEvent(json, filteredEventType)
      transformUsers(newJson, groupid, List("user", "assignee"))
    }
  }
  
  def getServiceId(body: Option[JsValue], eventType: String): String = {
    val filteredEventType: String = getEvent(body.get, eventType)
    val eventid = filteredEventType match {
        case "pull_request_review_comment" => "7c96b035-9502-4535-be70-bb052dc5f05c"
        case "issue_comment" => "2c3ffb6f-ddda-4f21-93f9-257d46271010"
        case "push" => "d6ac9d97-93fb-4b15-900f-f627223d5193"
        case "pull_request_push" => "62e75091-173c-4230-87b6-421b5f3d1769"
        case "pull_request" => "cb29a497-a4c3-4672-bfa7-9b184d9b76d2"
        case _ => ""
     }
    eventid
  }
  
  def setEvent(json: JsValue, eventType: String): JsValue = {
    val event: String = getEvent(json, eventType)
    json.as[JsObject] + ("event" -> Json.toJson(event))
  }
  
  def transformUsers(json: JsValue, groupid: String, personkeys: List[String]): JsValue =  {
    var returnVal: JsValue = json
    for (personkey <- personkeys) {
      val mongoService = new MongoService()
      val user = (json \ personkey).get.as[String]
      val bjondUserFuture = mongoService.getBjondID(groupid, user)
      val bjondUser = Await.result(bjondUserFuture, 2 seconds).get.userid
      returnVal = json.as[JsObject] + (personkey -> Json.toJson(bjondUser))
    }
    returnVal
  }
  
  def getEvent(json: JsValue, eventType: String): String = { 
    {
      if (eventType.equals("issue_comment") && !(json \ "issue" \ "pull_request").equals(null)) {
        "pull_request_review_comment"
      }
      else if(eventType.equals("push") && !(json \ "pull_request").equals(null)) {
        "pull_request_push"
      }
      else {
        eventType
      }
    }
  }
  
  def fireEvent(url: String, json: Option[JsValue], event: String, groupid: String): Future[WSResponse] = {
    val bjondJson = getBodyType(json, event, groupid)
    WS.url(url + "/" + getServiceId(json, event)).post(bjondJson)
  }
  
}
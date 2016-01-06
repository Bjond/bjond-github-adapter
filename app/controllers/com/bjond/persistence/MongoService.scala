package controllers.com.bjond.persistence

import controllers.config._
import play.api.libs.json.{JsObject, JsValue, JsPath, Writes}
import play.api.mvc.AnyContent
import play.libs.Json
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONDocument}
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import scala.util.{Success, Failure}

/**
  * Created by bcflynn on 12/30/15.
  */
class MongoService {

  implicit object GroupConfigurationWriter extends BSONDocumentWriter[GroupConfiguration] {
    def write(config: GroupConfiguration): BSONDocument = BSONDocument(
      "groupid" -> config.groupid,
      "gitHubAPIKey" -> config.gitHubAPIKey,
      "username" -> config.username,
      "password" -> config.password)
  }

  implicit object GroupConfigurationReader extends BSONDocumentReader[GroupConfiguration] {
    def read(doc: BSONDocument): GroupConfiguration = {
      GroupConfiguration(
        doc.getAs[String]("groupid").get,
        doc.getAs[String]("gitHubAPIKey").get,
        doc.getAs[String]("username").get,
        doc.getAs[String]("password").get)
    }
  }

  implicit object BjondRegistrationWriter extends BSONDocumentWriter[BjondRegistration] {
    def write(registration: BjondRegistration): BSONDocument = BSONDocument (
      "id" -> registration.id,
      "url" -> registration.url)
  }

  implicit object BjondRegistrationReader extends BSONDocumentReader[BjondRegistration] {
    def read(doc: BSONDocument): BjondRegistration = {
      BjondRegistration(
        doc.getAs[String]("id").get,
        doc.getAs[String]("url").get
      )
    }
  }

  def connect() : DefaultDB = {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    connection("bjond_github_adapter")
  }

  def insertGroupConfig(groupid: String, config: GroupConfiguration): Future[WriteResult] = {
    val db = connect
    val configuration = config.copy(groupid, config.gitHubAPIKey, config.username, config.password)
    val selector = BSONDocument("groupid" -> groupid)
    db[BSONCollection]("configurations").update(selector, configuration, upsert = true)
  }

  def getGroupConfiguration(groupid: String): Future[Option[GroupConfiguration]] = {
    val db = connect
    val query = BSONDocument("groupid" -> groupid)
    db[BSONCollection]("configurations").find(query).one[GroupConfiguration]
  }

  def insertGroupEndpoint(groupid: String, url: String): Future[WriteResult] = {
    val db = connect
    val endpoint = new BjondRegistration(groupid, url)
    val selector = BSONDocument("id" -> groupid)
    db[BSONCollection]("endpoints").update(selector, endpoint, upsert = true)
  }

  def getGroupEndpoint(groupid: String) : Future[Option[BjondRegistration]] = {
    val db = connect
    val query = BSONDocument("id" -> groupid)
    db[BSONCollection]("endpoints").find(query).one[BjondRegistration]
  }

}
